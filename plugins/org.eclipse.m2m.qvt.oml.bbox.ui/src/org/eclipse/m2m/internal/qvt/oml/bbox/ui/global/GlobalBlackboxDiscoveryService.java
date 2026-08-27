package org.eclipse.m2m.internal.qvt.oml.bbox.ui.global;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.impl.EPackageRegistryImpl;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.QVTBBoxUIPlugin;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxDescriptorLoader;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxDiagnosticInfo;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.ProjectBlackboxDiscoveryService;
import org.eclipse.m2m.internal.qvt.oml.blackbox.BlackboxRegistry;
import org.eclipse.m2m.internal.qvt.oml.blackbox.BlackboxUnitDescriptor;
import org.eclipse.m2m.internal.qvt.oml.blackbox.ResolutionContext;
import org.eclipse.m2m.internal.qvt.oml.blackbox.ResolutionContextImpl;
import org.eclipse.m2m.internal.qvt.oml.emf.util.URIUtils;
import org.eclipse.m2m.qvt.oml.blackbox.java.Module;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

public class GlobalBlackboxDiscoveryService {

	private final BlackboxDescriptorLoader descriptorLoader = new BlackboxDescriptorLoader();
	private final ActiveBundleDescriptorFilter activeBundleDescriptorFilter = new ActiveBundleDescriptorFilter();
	private final GlobalBlackboxUnitResolver unitResolver = new GlobalBlackboxUnitResolver(descriptorLoader);
	private final ExtensionBlackboxDiscovery extensionDiscovery = new ExtensionBlackboxDiscovery(unitResolver);
	private final RuntimeBlackboxDiscovery runtimeDiscovery = new RuntimeBlackboxDiscovery(descriptorLoader);

	public GlobalBlackboxDiscoveryResult discover(IProgressMonitor monitor) {
		SubMonitor progress = SubMonitor.convert(monitor, 100);
		GlobalBlackboxDiscoveryResult result = new GlobalBlackboxDiscoveryResult();
		Set<BlackboxDescriptorIdentity> attributedDescriptors = new HashSet<BlackboxDescriptorIdentity>();
		EPackage.Registry packageRegistry = globalPackageRegistry();

		discoverWorkspace(result, attributedDescriptors, progress.split(35));
		extensionDiscovery.discover(result, attributedDescriptors, packageRegistry, progress.split(10));
		discoverActiveBundles(result, attributedDescriptors, packageRegistry, progress.split(45));
		runtimeDiscovery.discover(result, attributedDescriptors, packageRegistry, progress.split(10));
		result.sort();
		return result;
	}

	private void discoverWorkspace(GlobalBlackboxDiscoveryResult result,
			Set<BlackboxDescriptorIdentity> attributedDescriptors,
			IProgressMonitor monitor) {
		SubMonitor progress = SubMonitor.convert(monitor);
		List<IJavaProject> javaProjects = new ArrayList<IJavaProject>();
		Map<IProject, IJavaProject> accessibleProjects = new LinkedHashMap<IProject, IJavaProject>();
		Map<String, GlobalBlackboxGroup> projectGroups = new LinkedHashMap<String, GlobalBlackboxGroup>();
		Map<String, GlobalBlackboxGroup> libraryGroups = new LinkedHashMap<String, GlobalBlackboxGroup>();
		Map<IProject, JavaProjectContext> projectContexts = new LinkedHashMap<IProject, JavaProjectContext>();
		Map<String, JavaProjectContext> libraryContexts = new LinkedHashMap<String, JavaProjectContext>();
		Set<GlobalBlackboxOriginIdentity> workspaceKeys = new LinkedHashSet<GlobalBlackboxOriginIdentity>();
		Set<GlobalBlackboxOriginIdentity> libraryKeys = new LinkedHashSet<GlobalBlackboxOriginIdentity>();

		for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
			checkCanceled(progress);
			if (isAccessibleJavaProject(project)) {
				IJavaProject javaProject = JavaCore.create(project);
				javaProjects.add(javaProject);
				accessibleProjects.put(project, javaProject);
			}
		}
		if (javaProjects.isEmpty()) {
			return;
		}

		int includeMask = IJavaSearchScope.SOURCES | IJavaSearchScope.APPLICATION_LIBRARIES;
		for (IType type : findAnnotatedTypes(javaProjects, includeMask, progress)) {
			checkCanceled(progress);
			IPackageFragmentRoot root = (IPackageFragmentRoot) type.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
			if (root == null) {
				continue;
			}

			int rootKind;
			try {
				rootKind = root.getKind();
			} catch (JavaModelException e) {
				QVTBBoxUIPlugin.log(e);
				continue;
			}

			if (rootKind == IPackageFragmentRoot.K_SOURCE) {
				IJavaProject javaProject = type.getJavaProject();
				IProject project = javaProject.getProject();
				if (!accessibleProjects.containsKey(project)) {
					continue;
				}
				GlobalBlackboxOriginIdentity key = new GlobalBlackboxOriginIdentity(project.getName(),
						type.getFullyQualifiedName());
				if (!workspaceKeys.add(key)) {
					continue;
				}
				GlobalBlackboxGroup group = projectGroups.get(project.getName());
				if (group == null) {
					group = new GlobalBlackboxGroup(result.getWorkspaceProjects(), GlobalBlackboxGroupKind.PROJECT,
							project.getName(), project.getName(), javaProject);
					projectGroups.put(project.getName(), group);
					result.getWorkspaceProjects().addChild(group);
				}
				JavaProjectContext context = getProjectContext(projectContexts, javaProject);
				unitResolver.addResolvedUnit(group, type.getFullyQualifiedName(), context.resolutionContext,
						context.packageRegistry, attributedDescriptors);
			} else if (rootKind == IPackageFragmentRoot.K_BINARY) {
				String rootKey = root.getPath().toString();
				GlobalBlackboxOriginIdentity key = new GlobalBlackboxOriginIdentity(rootKey,
						type.getFullyQualifiedName());
				if (!libraryKeys.add(key)) {
					continue;
				}
				GlobalBlackboxGroup group = libraryGroups.get(rootKey);
				if (group == null) {
					IJavaProject javaProject = type.getJavaProject();
					JavaProjectContext context = getProjectContext(projectContexts, javaProject);
					group = new GlobalBlackboxGroup(result.getJavaLibraries(), GlobalBlackboxGroupKind.LIBRARY,
							rootKey, libraryLabel(root, rootKey), javaProject);
					libraryGroups.put(rootKey, group);
					libraryContexts.put(rootKey, context);
					result.getJavaLibraries().addChild(group);
				}
				JavaProjectContext context = libraryContexts.get(rootKey);
				unitResolver.addResolvedUnit(group, type.getFullyQualifiedName(), context.resolutionContext,
						context.packageRegistry, attributedDescriptors);
			}
		}
	}

	private void discoverActiveBundles(GlobalBlackboxDiscoveryResult result,
			Set<BlackboxDescriptorIdentity> attributedDescriptors, EPackage.Registry packageRegistry,
			IProgressMonitor monitor) {
		Bundle owner = FrameworkUtil.getBundle(GlobalBlackboxDiscoveryService.class);
		BundleContext bundleContext = owner != null ? owner.getBundleContext() : null;
		if (bundleContext == null) {
			return;
		}

		for (Bundle bundle : bundleContext.getBundles()) {
			checkCanceled(monitor);
			if (bundle.getState() != Bundle.ACTIVE || bundle.getSymbolicName() == null) {
				continue;
			}
			if (!resolvesModuleAnnotation(bundle)) {
				continue;
			}
			if (!hasModuleAnnotationWire(bundle)) {
				continue;
			}
			String bundleId = bundle.getSymbolicName();
			GlobalBlackboxGroup group = null;
			try {
				ResolutionContext context = bundleContext(bundleId);
				Collection<BlackboxUnitDescriptor> descriptors = BlackboxRegistry.INSTANCE
						.getCompilationUnitDescriptors(context);
				Set<BlackboxDescriptorIdentity> bundleKeys = new HashSet<BlackboxDescriptorIdentity>();
				for (BlackboxUnitDescriptor descriptor : descriptors) {
					checkCanceled(monitor);
					if (!activeBundleDescriptorFilter.accepts(descriptor, bundle)) {
						continue;
					}
					BlackboxDescriptorIdentity key = BlackboxDescriptorIdentity.of(descriptor);
					if (!bundleKeys.add(key)) {
						continue;
					}
					if (group == null) {
						group = new GlobalBlackboxGroup(result.getActivePlugins(), GlobalBlackboxGroupKind.BUNDLE,
								bundleId, bundleId);
						result.getActivePlugins().addChild(group);
					}
					group.addChild(descriptorLoader.load(group, descriptor, descriptor.getQualifiedName(),
							packageRegistry));
					attributedDescriptors.add(key);
				}
			} catch (RuntimeException e) {
				QVTBBoxUIPlugin.log(e);
				if (group == null) {
					group = new GlobalBlackboxGroup(result.getActivePlugins(), GlobalBlackboxGroupKind.BUNDLE,
							bundleId, bundleId);
					result.getActivePlugins().addChild(group);
				}
				group.addChild(new BlackboxDiagnosticInfo(group, Diagnostic.ERROR, safeMessage(e)));
			} catch (LinkageError e) {
				QVTBBoxUIPlugin.log(e);
				if (group == null) {
					group = new GlobalBlackboxGroup(result.getActivePlugins(), GlobalBlackboxGroupKind.BUNDLE,
							bundleId, bundleId);
					result.getActivePlugins().addChild(group);
				}
				group.addChild(new BlackboxDiagnosticInfo(group, Diagnostic.ERROR, safeMessage(e)));
			}
		}
	}

	private Collection<IType> findAnnotatedTypes(List<IJavaProject> javaProjects, int includeMask,
			IProgressMonitor monitor) {
		final Map<String, IType> types = new LinkedHashMap<String, IType>();
		try {
			SearchPattern pattern = SearchPattern.createPattern(Module.class.getCanonicalName(),
					IJavaSearchConstants.ANNOTATION_TYPE, IJavaSearchConstants.ANNOTATION_TYPE_REFERENCE,
					SearchPattern.R_EXACT_MATCH);
			SearchParticipant[] participants = { SearchEngine.getDefaultSearchParticipant() };
			IJavaElement[] elements = javaProjects.toArray(new IJavaElement[javaProjects.size()]);
			IJavaSearchScope scope = SearchEngine.createJavaSearchScope(elements, includeMask);
			SearchRequestor requestor = new SearchRequestor() {
				@Override
				public void acceptSearchMatch(SearchMatch match) {
					if (match.getElement() instanceof IType) {
						IType type = (IType) match.getElement();
						types.put(type.getHandleIdentifier(), type);
					}
				}
			};
			new SearchEngine().search(pattern, participants, scope, requestor, monitor);
		} catch (CoreException e) {
			QVTBBoxUIPlugin.log(e);
		}
		return types.values();
	}

	private static JavaProjectContext getProjectContext(Map<IProject, JavaProjectContext> contexts,
			IJavaProject javaProject) {
		IProject project = javaProject.getProject();
		JavaProjectContext context = contexts.get(project);
		if (context == null) {
			context = new JavaProjectContext(project);
			contexts.put(project, context);
		}
		return context;
	}

	private static boolean isAccessibleJavaProject(IProject project) {
		try {
			return project != null && project.isAccessible() && project.hasNature(JavaCore.NATURE_ID);
		} catch (CoreException e) {
			return false;
		}
	}

	private static ResolutionContext bundleContext(String bundleId) {
		URI uri = URI.createPlatformPluginURI(bundleId + "/", true); //$NON-NLS-1$
		return new ResolutionContextImpl(uri);
	}

	private static EPackage.Registry globalPackageRegistry() {
		return new EPackageRegistryImpl(EPackage.Registry.INSTANCE);
	}

	private static String libraryLabel(IPackageFragmentRoot root, String fallback) {
		if (root != null && root.getElementName() != null && root.getElementName().length() > 0) {
			return root.getElementName();
		}
		return fallback;
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

	private static class JavaProjectContext {

		final ResolutionContext resolutionContext;
		final EPackage.Registry packageRegistry;

		JavaProjectContext(IProject project) {
			resolutionContext = new ResolutionContextImpl(URIUtils.getResourceURI(project));
			packageRegistry = ProjectBlackboxDiscoveryService.createPackageRegistry(project);
		}
	}
}
