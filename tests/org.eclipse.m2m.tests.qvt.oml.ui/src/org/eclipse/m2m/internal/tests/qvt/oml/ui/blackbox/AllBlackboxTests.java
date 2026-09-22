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

import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@SuiteClasses({ BlackboxTreeModelTest.class, GlobalBlackboxDiscoveryCompositionTest.class,
		BlackboxDescriptorLoaderTest.class,
		BlackboxDescriptorCandidatesTest.class, BlackboxVisibilityScopeTest.class,
		ActiveBundleDescriptorFilterTest.class, ExtensionContributionNameResolverTest.class,
		GlobalBlackboxIdentityTest.class, BlackboxUnitLabelTest.class, BlackboxDiagnosticUtilTest.class,
		GlobalBlackboxDiscoveryLifecycleTest.class })
public class AllBlackboxTests {
}
