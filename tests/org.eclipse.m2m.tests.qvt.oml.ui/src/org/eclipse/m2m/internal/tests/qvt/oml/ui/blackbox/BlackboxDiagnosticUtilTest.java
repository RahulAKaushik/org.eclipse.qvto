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

import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxDiagnosticUtil;
import org.junit.Test;

public class BlackboxDiagnosticUtilTest {

	@Test
	public void usesExceptionMessageWhenAvailable() {
		assertEquals("failure", BlackboxDiagnosticUtil.getMessage(new RuntimeException("failure"))); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void fallsBackToExceptionTypeForMissingMessage() {
		assertEquals(RuntimeException.class.getName(), BlackboxDiagnosticUtil.getMessage(new RuntimeException()));
	}

	@Test
	public void toleratesBrokenMessageImplementation() {
		RuntimeException failure = new RuntimeException() {
			private static final long serialVersionUID = 1L;

			@Override
			public String getMessage() {
				throw new IllegalStateException();
			}
		};

		assertEquals(failure.getClass().getName(), BlackboxDiagnosticUtil.getMessage(failure));
	}
}
