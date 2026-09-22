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
package org.eclipse.m2m.internal.qvt.oml.bbox.ui.global;

/**
 * Tracks which replaceable global discovery request may publish its result.
 */
public final class GlobalBlackboxDiscoveryGeneration {

	private int current;

	public synchronized int start() {
		return ++current;
	}

	public synchronized boolean isCurrent(int generation) {
		return generation == current;
	}

	public synchronized void invalidate() {
		current++;
	}
}
