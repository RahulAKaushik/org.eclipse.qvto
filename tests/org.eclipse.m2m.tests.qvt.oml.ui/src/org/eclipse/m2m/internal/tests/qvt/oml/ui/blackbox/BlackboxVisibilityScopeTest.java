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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxVisibilityScope;
import org.junit.Test;

public class BlackboxVisibilityScopeTest {

	@Test
	public void preferenceValuesRoundTripAndRejectUnknownValues() {
		for (BlackboxVisibilityScope scope : BlackboxVisibilityScope.values()) {
			assertSame(scope, BlackboxVisibilityScope.fromPreferenceValue(scope.getPreferenceValue()));
		}
		assertNull(BlackboxVisibilityScope.fromPreferenceValue(null));
		assertNull(BlackboxVisibilityScope.fromPreferenceValue("unknown")); //$NON-NLS-1$
	}

	@Test
	public void projectOnlyExcludesDependenciesAndRegistryDescriptors() {
		BlackboxVisibilityScope scope = BlackboxVisibilityScope.PROJECT_ONLY;

		assertFalse(scope.includesJavaDependencies());
		assertFalse(scope.includesRegistryDescriptors());
	}

	@Test
	public void projectDependenciesIncludesClasspathWithoutRegistryDescriptors() {
		BlackboxVisibilityScope scope = BlackboxVisibilityScope.PROJECT_DEPENDENCIES;

		assertTrue(scope.includesJavaDependencies());
		assertFalse(scope.includesRegistryDescriptors());
	}

	@Test
	public void projectVisibleIncludesClasspathAndRegistryDescriptors() {
		BlackboxVisibilityScope scope = BlackboxVisibilityScope.PROJECT_VISIBLE;

		assertTrue(scope.includesJavaDependencies());
		assertTrue(scope.includesRegistryDescriptors());
	}
}
