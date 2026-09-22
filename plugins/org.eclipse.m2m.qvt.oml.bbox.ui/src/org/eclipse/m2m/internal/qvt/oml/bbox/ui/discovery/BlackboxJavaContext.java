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

import org.eclipse.jdt.core.IJavaProject;

/** A resolution/navigation context, which need not physically own a binary unit. */
public interface BlackboxJavaContext {
	/** Returns null for origins with no JDT navigation context. */
	IJavaProject getJavaProject();
}
