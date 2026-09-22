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
		Bundle ownerBundle = FrameworkUtil.getBundle(GlobalBlackboxDiscoveryService.class);
		BundleContext bundleContext = ownerBundle != null ? ownerBundle.getBundleContext() : null;
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
