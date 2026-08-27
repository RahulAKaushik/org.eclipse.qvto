package org.eclipse.m2m.internal.tests.qvt.oml.ui.blackbox;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.global.GlobalBlackboxDiscoveryGeneration;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.global.GlobalBlackboxDiscoveryService;
import org.junit.Test;

public class GlobalBlackboxDiscoveryLifecycleTest {

	@Test
	public void onlyLatestGenerationMayPublish() {
		GlobalBlackboxDiscoveryGeneration generations = new GlobalBlackboxDiscoveryGeneration();
		int first = generations.start();
		int second = generations.start();

		assertFalse(generations.isCurrent(first));
		assertTrue(generations.isCurrent(second));
	}

	@Test
	public void invalidationRejectsInFlightGeneration() {
		GlobalBlackboxDiscoveryGeneration generations = new GlobalBlackboxDiscoveryGeneration();
		int generation = generations.start();

		generations.invalidate();

		assertFalse(generations.isCurrent(generation));
	}

	@Test
	public void canceledDiscoveryDoesNotReturnPartialResult() {
		NullProgressMonitor monitor = new NullProgressMonitor();
		monitor.setCanceled(true);

		try {
			new GlobalBlackboxDiscoveryService().discover(monitor);
			fail("Expected discovery cancellation"); //$NON-NLS-1$
		} catch (OperationCanceledException e) {
			// Expected: cancellation is not converted into a diagnostic result.
		}
	}
}
