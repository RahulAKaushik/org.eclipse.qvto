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
		assertTrue(filter.matchesBundleQuery(URI.createURI("qvto://blackbox/example.Library?osgi=" + BUNDLE_ID), //$NON-NLS-1$
				BUNDLE_ID));
		assertFalse(filter.matchesBundleQuery(URI.createURI("qvto://blackbox/example.Library?osgi=other.bundle"), //$NON-NLS-1$
				BUNDLE_ID));
		assertFalse(filter.matchesBundleQuery(URI.createURI("qvto://blackbox/example.Library?jdt=" + BUNDLE_ID), //$NON-NLS-1$
				BUNDLE_ID));
		assertFalse(filter.matchesBundleQuery(URI.createURI("qvto://blackbox/example.Library"), BUNDLE_ID)); //$NON-NLS-1$
		assertFalse(filter.matchesBundleQuery(null, BUNDLE_ID));
		assertFalse(
				filter.matchesBundleQuery(URI.createURI("qvto://blackbox/example.Library?osgi=" + BUNDLE_ID), null)); //$NON-NLS-1$
	}
}
