package org.eclipse.m2m.internal.tests.qvt.oml.ui.blackbox.integration;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import org.eclipse.core.runtime.Platform;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.global.ActiveBundleCandidateSelector;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.global.GlobalBlackboxDiscoveryService;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

public class ActiveBundleCandidateSelectorTest {

	@Test
	public void selectsDirectAndIndirectDependentsOnly() {
		Bundle testBundle = FrameworkUtil.getBundle(getClass());
		BundleContext bundleContext = testBundle != null ? testBundle.getBundleContext() : null;
		assertNotNull(bundleContext);

		Collection<Bundle> candidates = new ActiveBundleCandidateSelector().select(bundleContext);
		assertTrue(candidates.contains(FrameworkUtil.getBundle(GlobalBlackboxDiscoveryService.class)));

		Bundle indirectDependent = Platform.getBundle("org.eclipse.m2m.tests.qvto.deployedTransfProject"); //$NON-NLS-1$
		assertNotNull(indirectDependent);
		assertTrue(candidates.contains(indirectDependent));

		Bundle unrelated = Platform.getBundle("org.junit"); //$NON-NLS-1$
		assertNotNull(unrelated);
		assertFalse(candidates.contains(unrelated));
	}
}
