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
package org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery;

/**
 * Creates a usable diagnostic message even for malformed provider exceptions.
 */
public final class BlackboxDiagnosticUtil {

	private BlackboxDiagnosticUtil() {
	}

	public static String getMessage(Throwable throwable) {
		String message = null;
		try {
			message = throwable.getMessage();
		} catch (RuntimeException e) {
			// A provider exception must not prevent reporting the original failure.
		}
		return message != null ? message : throwable.getClass().getName();
	}
}
