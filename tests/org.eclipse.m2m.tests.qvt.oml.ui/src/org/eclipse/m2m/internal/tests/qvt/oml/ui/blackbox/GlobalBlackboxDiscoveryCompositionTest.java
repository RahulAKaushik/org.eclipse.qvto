/*******************************************************************************
 * Copyright (c) 2026 Rahul Arun Kaushik and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Rahul Arun Kaushik - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2m.internal.tests.qvt.oml.ui.blackbox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxUnitInfo;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.global.BlackboxDescriptorIdentity;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.global.BlackboxDiscoverySource;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.global.GlobalBlackboxDiscoveryResult;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.global.GlobalBlackboxDiscoveryService.Step;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.global.GlobalBlackboxDiscoveryService;
import org.junit.Test;

public class GlobalBlackboxDiscoveryCompositionTest {

	@Test
	public void sourcesRunInOrderShareAttributionAndSortBeforeReturning() {
		List<String> calls = new ArrayList<String>();
		List<EPackage.Registry> registries = new ArrayList<EPackage.Registry>();
		BlackboxDescriptorIdentity identity = new BlackboxDescriptorIdentity("unit", null); //$NON-NLS-1$
		BlackboxDiscoverySource first = (result, attributed, registry, monitor) -> {
			calls.add("first"); //$NON-NLS-1$
			registries.add(registry);
			attributed.add(identity);
			result.getRuntimeRegistrations()
					.addChild(new BlackboxUnitInfo(result.getRuntimeRegistrations(), "z", null)); //$NON-NLS-1$
		};
		BlackboxDiscoverySource second = (result, attributed, registry, monitor) -> {
			calls.add("second"); //$NON-NLS-1$
			registries.add(registry);
			assertTrue(attributed.contains(identity));
			result.getRuntimeRegistrations()
					.addChild(new BlackboxUnitInfo(result.getRuntimeRegistrations(), "a", null)); //$NON-NLS-1$
		};
		GlobalBlackboxDiscoveryResult result = new GlobalBlackboxDiscoveryService(new Step(first, 3),
				new Step(second, 1)).discover(new NullProgressMonitor());
		assertEquals(Arrays.asList("first", "second"), calls); //$NON-NLS-1$ //$NON-NLS-2$
		assertSame(registries.get(0), registries.get(1));
		assertEquals("a", result.getRuntimeRegistrations().getChildren().get(0).getLabel()); //$NON-NLS-1$
	}

	@Test
	public void cancellationBetweenSourcesPreventsLaterSourceAndPartialReturn() {
		NullProgressMonitor monitor = new NullProgressMonitor();
		BlackboxDiscoverySource cancel = (result, attributed, registry, progress) -> monitor.setCanceled(true);
		BlackboxDiscoverySource later = (result, attributed, registry, progress) -> fail("Later source must not run"); //$NON-NLS-1$
		try {
			new GlobalBlackboxDiscoveryService(new Step(cancel, 1), new Step(later, 1)).discover(monitor);
			fail("Canceled discovery must not return a partial tree"); //$NON-NLS-1$
		} catch (OperationCanceledException expected) {
		}
	}

	@Test
	public void compositionIsDefensivelyCopiedAndEachRunGetsFreshState() {
		List<EPackage.Registry> registries = new ArrayList<EPackage.Registry>();
		BlackboxDiscoverySource source = (result, attributed, registry, monitor) -> {
			assertTrue(attributed.isEmpty());
			attributed.add(new BlackboxDescriptorIdentity("unit", null)); //$NON-NLS-1$
			registries.add(registry);
		};
		Step[] steps = { new Step(source, 1) };
		GlobalBlackboxDiscoveryService service = new GlobalBlackboxDiscoveryService(steps);
		steps[0] = new Step((result, attributed, registry, monitor) -> fail("Changed external array"), 1); //$NON-NLS-1$
		assertNotSame(service.discover(new NullProgressMonitor()), service.discover(new NullProgressMonitor()));
		assertNotSame(registries.get(0), registries.get(1));
	}
}
