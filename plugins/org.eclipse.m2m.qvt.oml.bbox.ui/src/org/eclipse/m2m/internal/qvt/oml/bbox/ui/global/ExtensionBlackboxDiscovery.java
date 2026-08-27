package org.eclipse.m2m.internal.qvt.oml.bbox.ui.global;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.m2m.internal.qvt.oml.blackbox.ResolutionContext;
import org.eclipse.m2m.internal.qvt.oml.blackbox.ResolutionContextImpl;

final class ExtensionBlackboxDiscovery {

	private static final String EXTENSION_POINT = "javaBlackboxUnits"; //$NON-NLS-1$
	private static final String QVT_PLUGIN_ID = "org.eclipse.m2m.qvt.oml"; //$NON-NLS-1$
	private static final String NAME_ATTRIBUTE = "name"; //$NON-NLS-1$
	private static final String NAMESPACE_ATTRIBUTE = "namespace"; //$NON-NLS-1$
	private static final String CLASS_ATTRIBUTE = "class"; //$NON-NLS-1$

	private final GlobalBlackboxUnitResolver unitResolver;
	private final ExtensionContributionNameResolver nameResolver = new ExtensionContributionNameResolver();

	ExtensionBlackboxDiscovery(GlobalBlackboxUnitResolver unitResolver) {
		this.unitResolver = unitResolver;
	}

	void discover(GlobalBlackboxDiscoveryResult result, Set<BlackboxDescriptorIdentity> attributedDescriptors,
			EPackage.Registry packageRegistry, IProgressMonitor monitor) {
		Map<String, GlobalBlackboxGroup> bundleGroups = new LinkedHashMap<String, GlobalBlackboxGroup>();
		Set<GlobalBlackboxOriginIdentity> contributionKeys = new HashSet<GlobalBlackboxOriginIdentity>();
		IConfigurationElement[] elements = Platform.getExtensionRegistry()
				.getConfigurationElementsFor(QVT_PLUGIN_ID, EXTENSION_POINT);
		for (IConfigurationElement element : elements) {
			checkCanceled(monitor);
			String contributor = element.getContributor().getName();
			String qualifiedName = nameResolver.resolve(element.getName(), contributor,
					element.getAttribute(NAME_ATTRIBUTE), element.getAttribute(NAMESPACE_ATTRIBUTE),
					element.getAttribute(CLASS_ATTRIBUTE));
			if (qualifiedName == null
					|| !contributionKeys.add(new GlobalBlackboxOriginIdentity(contributor, qualifiedName))) {
				continue;
			}
			GlobalBlackboxGroup group = bundleGroups.get(contributor);
			if (group == null) {
				group = new GlobalBlackboxGroup(result.getExtensionContributions(), GlobalBlackboxGroupKind.BUNDLE,
						contributor, contributor);
				bundleGroups.put(contributor, group);
				result.getExtensionContributions().addChild(group);
			}
			unitResolver.addResolvedUnit(group, qualifiedName, bundleContext(contributor), packageRegistry,
					attributedDescriptors);
		}
	}

	private static ResolutionContext bundleContext(String bundleId) {
		URI uri = URI.createPlatformPluginURI(bundleId + "/", true); //$NON-NLS-1$
		return new ResolutionContextImpl(uri);
	}

	private static void checkCanceled(IProgressMonitor monitor) {
		if (monitor != null && monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
	}
}
