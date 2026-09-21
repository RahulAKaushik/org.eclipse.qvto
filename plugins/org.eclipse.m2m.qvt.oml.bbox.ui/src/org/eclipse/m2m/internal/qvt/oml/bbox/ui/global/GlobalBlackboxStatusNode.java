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

import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxTreeNode;

/** A leaf status whose parent is the global group in which it is displayed. */
final class GlobalBlackboxStatusNode implements BlackboxTreeNode {

	private final BlackboxTreeNode parent;
	private final String message;

	GlobalBlackboxStatusNode(BlackboxTreeNode parent, String message) {
		this.parent = parent;
		this.message = message;
	}

	public BlackboxTreeNode getParent() {
		return parent;
	}

	String getMessage() {
		return message;
	}

	@Override
	public String getLabel() {
		return message;
	}
}
