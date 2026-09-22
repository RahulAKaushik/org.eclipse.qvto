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
import static org.junit.Assert.assertTrue;

import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.URI;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxDiagnosticInfo;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxUnitInfo;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.navigator.BlackboxNavigatorLabelProvider;
import org.junit.Test;

public class BlackboxUnitLabelTest {

	@Test
	public void failureIsRepresentedByDecorationNotTextSuffix() {
		BlackboxUnitInfo successful = new BlackboxUnitInfo(null, "example.Library", //$NON-NLS-1$
				URI.createURI("qvto://blackbox/example.Library")); //$NON-NLS-1$
		BlackboxUnitInfo failed = new BlackboxUnitInfo(null, "example.Library", //$NON-NLS-1$
				URI.createURI("qvto://blackbox/example.Library")); //$NON-NLS-1$
		failed.addDiagnostic(new BlackboxDiagnosticInfo(failed, Diagnostic.ERROR, "Expected failure")); //$NON-NLS-1$

		BlackboxNavigatorLabelProvider labels = new BlackboxNavigatorLabelProvider();
		try {
			assertTrue(failed.hasErrors());
			assertEquals("example.Library", labels.getText(successful)); //$NON-NLS-1$
			assertEquals(labels.getText(successful), labels.getText(failed));
		} finally {
			labels.dispose();
		}
	}
}
