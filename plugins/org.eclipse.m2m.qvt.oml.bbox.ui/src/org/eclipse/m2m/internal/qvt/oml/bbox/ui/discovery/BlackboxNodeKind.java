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

/** Semantic presentation roles; image allocation remains in the label provider. */
public enum BlackboxNodeKind {
	GROUP(0), PROJECT(0), UNIT(1), MODULE(2), OPERATION(2), INFO(2), WARNING(2), ERROR(2);

	private final int sortOrder;

	BlackboxNodeKind(int sortOrder) {
		this.sortOrder = sortOrder;
	}

	public int getSortOrder() {
		return sortOrder;
	}
}
