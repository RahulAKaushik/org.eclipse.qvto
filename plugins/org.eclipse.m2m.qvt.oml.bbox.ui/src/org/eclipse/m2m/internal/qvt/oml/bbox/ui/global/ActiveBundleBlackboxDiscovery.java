package org.eclipse.m2m.internal.qvt.oml.bbox.ui.global;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.QVTBBoxUIPlugin;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxDescriptorLoader;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxDiagnosticInfo;
import org.eclipse.m2m.internal.qvt.oml.blackbox.BlackboxRegistry;
import org.eclipse.m2m.internal.qvt.oml.blackbox.BlackboxUnitDescriptor;
import org.eclipse.m2m.internal.qvt.oml.blackbox.ResolutionContext;
import org.eclipse.m2m.internal.qvt.oml.blackbox.ResolutionContextImpl;
import org.eclipse.m2m.qvt.oml.blackbox.java.Module;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

final class ActiveBundleBlackboxDiscovery {

	private final BlackboxDescriptorLoader descriptorLoader;
	private final ActiveBundleDescriptorFilter descriptorFilter;

	ActiveBundleBlackboxDiscovery(BlackboxDescriptorLoader descriptorLoader,
			ActiveBundleDescriptorFilter descriptorFilter) {
		this.descriptorLoader = descriptorLoader;
		this.descriptorFilter = descriptorFilter;
	}

	void discover(GlobalBlackboxDiscoveryResult result, Set<BlackboxDescriptorIdentity> attributedDescriptors,
			EPackage.Registry packageRegistry, IProgressMonitor monitor) {
		Bundle owner = FrameworkUtil.getBundle(ActiveBundleBlackboxDiscovery.class);
		BundleContext bundleContext = owner != null ? owner.getBundleContext() : null;
		if (bundleContext == null) {
			return;
		}

		for (Bundle bundle : bundleContext.getBundles()) {
			checkCanceled(monitor);
			if (!isCandidate(bundle)) {
				continue;
			}
			discoverBundle(result, attributedDescriptors, packageRegistry, monitor, bundle);
		}
	}

	private void discoverBundle(GlobalBlackboxDiscoveryResult result,
			Set<BlackboxDescriptorIdentity> attributedDescriptors, EPackage.Registry packageRegistry,
			IProgressMonitor monitor, Bundle bundle) {
		String bundleId = bundle.getSymbolicName();
		GlobalBlackboxGroup group = null;
		try {
			ResolutionContext context = bundleContext(bundleId);
			Collection<BlackboxUnitDescriptor> descriptors = BlackboxRegistry.INSTANCE
					.getCompilationUnitDescriptors(context);
			Set<BlackboxDescriptorIdentity> bundleKeys = new HashSet<BlackboxDescriptorIdentity>();
			for (BlackboxUnitDescriptor descriptor : descriptors) {
				checkCanceled(monitor);
				if (!descriptorFilter.accepts(descriptor, bundle)) {
					continue;
				}
				BlackboxDescriptorIdentity key = BlackboxDescriptorIdentity.of(descriptor);
				if (!bundleKeys.add(key)) {
					continue;
				}
				if (group == null) {
					group = createGroup(result, bundleId);
				}
				group.addChild(descriptorLoader.load(group, descriptor, descriptor.getQualifiedName(), packageRegistry));
				attributedDescriptors.add(key);
			}
		} catch (RuntimeException e) {
			addFailure(result, group, bundleId, e);
		} catch (LinkageError e) {
			addFailure(result, group, bundleId, e);
		}
	}

	private static boolean isCandidate(Bundle bundle) {
		return bundle.getState() == Bundle.ACTIVE && bundle.getSymbolicName() != null
				&& resolvesModuleAnnotation(bundle) && hasModuleAnnotationWire(bundle);
	}

	private static GlobalBlackboxGroup createGroup(GlobalBlackboxDiscoveryResult result, String bundleId) {
		GlobalBlackboxGroup group = new GlobalBlackboxGroup(result.getActivePlugins(), GlobalBlackboxGroupKind.BUNDLE,
				bundleId, bundleId);
		result.getActivePlugins().addChild(group);
		return group;
	}

	private static void addFailure(GlobalBlackboxDiscoveryResult result, GlobalBlackboxGroup group, String bundleId,
			Throwable throwable) {
		QVTBBoxUIPlugin.log(throwable);
		GlobalBlackboxGroup target = group != null ? group : createGroup(result, bundleId);
		target.addChild(new BlackboxDiagnosticInfo(target, Diagnostic.ERROR, safeMessage(throwable)));
	}

	private static ResolutionContext bundleContext(String bundleId) {
		URI uri = URI.createPlatformPluginURI(bundleId + "/", true); //$NON-NLS-1$
		return new ResolutionContextImpl(uri);
	}

	private static boolean resolvesModuleAnnotation(Bundle bundle) {
		try {
			// The OSGi provider can only recognize annotations with this class identity.
			return bundle.loadClass(Module.class.getName()) == Module.class;
		} catch (ClassNotFoundException e) {
			return false;
		} catch (RuntimeException e) {
			return false;
		} catch (LinkageError e) {
			return false;
		}
	}

	private static boolean hasModuleAnnotationWire(Bundle bundle) {
		Bundle moduleBundle = FrameworkUtil.getBundle(Module.class);
		if (moduleBundle == null || moduleBundle.equals(bundle)) {
			return true;
		}

		BundleWiring wiring = bundle.adapt(BundleWiring.class);
		if (wiring == null) {
			return false;
		}
		// loadClass may succeed through Equinox buddy policy without a declared dependency.
		for (BundleWire wire : wiring.getRequiredWires(null)) {
			BundleWiring providerWiring = wire.getProviderWiring();
			if (providerWiring != null && moduleBundle.equals(providerWiring.getBundle())) {
				return true;
			}
			if (BundleNamespace.BUNDLE_NAMESPACE.equals(wire.getCapability().getNamespace())
					&& reexportsModuleAnnotation(providerWiring, moduleBundle, new HashSet<Bundle>())) {
				return true;
			}
		}
		return false;
	}

	private static boolean reexportsModuleAnnotation(BundleWiring wiring, Bundle moduleBundle,
			Set<Bundle> visitedBundles) {
		if (wiring == null || !visitedBundles.add(wiring.getBundle())) {
			return false;
		}
		for (BundleWire wire : wiring.getRequiredWires(BundleNamespace.BUNDLE_NAMESPACE)) {
			String visibility = wire.getRequirement().getDirectives()
					.get(BundleNamespace.REQUIREMENT_VISIBILITY_DIRECTIVE);
			if (!BundleNamespace.VISIBILITY_REEXPORT.equals(visibility)) {
				continue;
			}
			BundleWiring providerWiring = wire.getProviderWiring();
			if (providerWiring != null && (moduleBundle.equals(providerWiring.getBundle())
					|| reexportsModuleAnnotation(providerWiring, moduleBundle, visitedBundles))) {
				return true;
			}
		}
		return false;
	}

	private static void checkCanceled(IProgressMonitor monitor) {
		if (monitor != null && monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
	}

	private static String safeMessage(Throwable throwable) {
		String message = null;
		try {
			message = throwable.getMessage();
		} catch (RuntimeException e) {
			// Keep diagnostics robust even for exceptions with broken message implementations.
		}
		return message != null ? message : throwable.getClass().getName();
	}
}
