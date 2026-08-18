package org.eclipse.m2m.internal.qvt.oml.bbox.ui.global;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;

import org.eclipse.core.runtime.Platform;
import org.eclipse.m2m.qvt.oml.blackbox.java.Module;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

/** Selects installed bundles that may define annotated QVTo blackboxes. */
final class ActiveBundleCandidateSelector {

	Collection<Bundle> select(BundleContext bundleContext) {
		Bundle moduleBundle = FrameworkUtil.getBundle(Module.class);
		IPluginModelBase moduleModel = moduleBundle != null
				? PluginRegistry.findModel(moduleBundle.getSymbolicName()) : null;
		BundleDescription moduleDescription = moduleModel != null ? moduleModel.getBundleDescription() : null;
		State state = moduleDescription != null ? moduleDescription.getContainingState() : null;
		if (state == null) {
			return Arrays.asList(bundleContext.getBundles());
		}

		BundleDescription[] dependents = state.getStateHelper()
				.getDependentBundles(new BundleDescription[] { moduleDescription });
		Collection<Bundle> candidates = new LinkedHashSet<Bundle>();
		for (BundleDescription dependent : dependents) {
			String symbolicName = dependent.getSymbolicName();
			Bundle bundle = symbolicName != null ? Platform.getBundle(symbolicName) : null;
			if (bundle != null) {
				candidates.add(bundle);
			}
		}
		return candidates;
	}
}
