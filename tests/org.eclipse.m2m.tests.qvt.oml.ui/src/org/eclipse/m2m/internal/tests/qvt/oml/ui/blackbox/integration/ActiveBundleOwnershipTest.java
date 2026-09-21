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
package org.eclipse.m2m.internal.tests.qvt.oml.ui.blackbox.integration;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.m2m.internal.qvt.oml.bbox.ui.global.ActiveBundleDescriptorFilter;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.global.GlobalBlackboxDiscoveryService;
import org.eclipse.m2m.qvt.oml.blackbox.java.Module;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public class ActiveBundleOwnershipTest {

	private final ActiveBundleDescriptorFilter filter = new ActiveBundleDescriptorFilter();

	@Test
	public void acceptsClassFromItsDefiningBundle() {
		Bundle moduleBundle = FrameworkUtil.getBundle(Module.class);
		assertNotNull(moduleBundle);

		assertTrue(filter.isDefinedByBundle(Module.class.getName(), moduleBundle));
	}

	@Test
	public void rejectsImportedClassClaimedByAnotherBundle() {
		Bundle moduleBundle = FrameworkUtil.getBundle(Module.class);
		Bundle uiBundle = FrameworkUtil.getBundle(GlobalBlackboxDiscoveryService.class);
		assertNotNull(moduleBundle);
		assertNotNull(uiBundle);
		assertFalse(moduleBundle.equals(uiBundle));

		assertFalse(filter.isDefinedByBundle(Module.class.getName(), uiBundle));
		assertFalse(filter.isDefinedByBundle("missing.blackbox.Library", uiBundle)); //$NON-NLS-1$
	}
}
