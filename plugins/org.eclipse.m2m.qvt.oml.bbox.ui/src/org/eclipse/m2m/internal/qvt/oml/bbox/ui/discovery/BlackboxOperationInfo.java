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

/** A blackbox operation whose parent is its containing module. */
public class BlackboxOperationInfo implements BlackboxTreeNode, BlackboxNavigable {

	private final BlackboxModuleInfo parent;
	private final String name;
	private final String signature;

	public BlackboxOperationInfo(BlackboxModuleInfo parent, String name, String signature) {
		this.parent = parent;
		this.name = name;
		this.signature = signature;
	}

	public BlackboxModuleInfo getParent() {
		return parent;
	}

	public String getName() {
		return name;
	}

	public String getSignature() {
		return signature;
	}

	@Override
	public String getLabel() {
		return signature;
	}

	@Override
	public BlackboxNodeKind getNodeKind() {
		return BlackboxNodeKind.OPERATION;
	}

	@Override
	public BlackboxUnitInfo getNavigationUnit() {
		return parent.getParent();
	}

	@Override
	public String getOperationName() {
		return name;
	}
}
