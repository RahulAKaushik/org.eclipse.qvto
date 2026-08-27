package org.eclipse.m2m.internal.tests.qvt.oml.ui.blackbox;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.emf.common.util.URI;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.global.ActiveBundleDescriptorFilter;
import org.junit.Test;

public class ActiveBundleDescriptorFilterTest {

	private static final String BUNDLE_ID = "example.bundle"; //$NON-NLS-1$
	private final ActiveBundleDescriptorFilter filter = new ActiveBundleDescriptorFilter();

	@Test
	public void matchesOnlyTheExactOsgiBundleQuery() {
		assertTrue(filter.matchesBundleQuery(
				URI.createURI("qvto://blackbox/example.Library?osgi=" + BUNDLE_ID), BUNDLE_ID)); //$NON-NLS-1$
		assertFalse(filter.matchesBundleQuery(
				URI.createURI("qvto://blackbox/example.Library?osgi=other.bundle"), BUNDLE_ID)); //$NON-NLS-1$
		assertFalse(filter.matchesBundleQuery(
				URI.createURI("qvto://blackbox/example.Library?jdt=" + BUNDLE_ID), BUNDLE_ID)); //$NON-NLS-1$
		assertFalse(filter.matchesBundleQuery(URI.createURI("qvto://blackbox/example.Library"), BUNDLE_ID)); //$NON-NLS-1$
		assertFalse(filter.matchesBundleQuery(null, BUNDLE_ID));
		assertFalse(filter.matchesBundleQuery(
				URI.createURI("qvto://blackbox/example.Library?osgi=" + BUNDLE_ID), null)); //$NON-NLS-1$
	}
}
