package org.eclipse.m2m.internal.qvt.oml.bbox.ui.global;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.impl.EPackageRegistryImpl;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxDescriptorLoader;

/**
 * Coordinates the discovery sources shown in the workspace-wide blackbox view.
 * This non-UI service builds a complete result on the calling background
 * thread; the view is responsible for publishing that result on the UI thread.
 */
public class GlobalBlackboxDiscoveryService {

	private final BlackboxDescriptorLoader descriptorLoader = new BlackboxDescriptorLoader();
	private final GlobalBlackboxUnitResolver unitResolver = new GlobalBlackboxUnitResolver(descriptorLoader);
	private final WorkspaceBlackboxDiscovery workspaceDiscovery = new WorkspaceBlackboxDiscovery(unitResolver);
	private final ExtensionBlackboxDiscovery extensionDiscovery = new ExtensionBlackboxDiscovery(unitResolver);
	private final ActiveBundleBlackboxDiscovery activeBundleDiscovery = new ActiveBundleBlackboxDiscovery(
			descriptorLoader, new ActiveBundleDescriptorFilter());
	private final RuntimeBlackboxDiscovery runtimeDiscovery = new RuntimeBlackboxDiscovery(descriptorLoader);

	public GlobalBlackboxDiscoveryResult discover(IProgressMonitor monitor) {
		SubMonitor progress = SubMonitor.convert(monitor, 100);
		progress.checkCanceled();
		GlobalBlackboxDiscoveryResult result = new GlobalBlackboxDiscoveryResult();
		Set<BlackboxDescriptorIdentity> attributedDescriptors = new HashSet<BlackboxDescriptorIdentity>();
		EPackage.Registry packageRegistry = new EPackageRegistryImpl(EPackage.Registry.INSTANCE);

		workspaceDiscovery.discover(result, attributedDescriptors, progress.split(35));
		progress.checkCanceled();
		extensionDiscovery.discover(result, attributedDescriptors, packageRegistry, progress.split(10));
		progress.checkCanceled();
		activeBundleDiscovery.discover(result, attributedDescriptors, packageRegistry, progress.split(45));
		progress.checkCanceled();
		runtimeDiscovery.discover(result, attributedDescriptors, packageRegistry, progress.split(10));
		progress.checkCanceled();
		result.sort();
		return result;
	}
}
