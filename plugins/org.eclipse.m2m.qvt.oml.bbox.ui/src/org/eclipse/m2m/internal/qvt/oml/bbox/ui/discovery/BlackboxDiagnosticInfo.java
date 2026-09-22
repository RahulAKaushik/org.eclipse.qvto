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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.emf.common.util.Diagnostic;

/**
 * A diagnostic tree node. Its parent is the result, unit, group, or diagnostic
 * that owns it in the displayed tree.
 */
public class BlackboxDiagnosticInfo implements BlackboxTreeNode {

	private final BlackboxTreeNode parent;
	private final int severity;
	private final String message;
	private final List<BlackboxDiagnosticInfo> children = new ArrayList<BlackboxDiagnosticInfo>();

	public BlackboxDiagnosticInfo(BlackboxTreeNode parent, int severity, String message) {
		this.parent = parent;
		this.severity = severity;
		this.message = message;
	}

	public BlackboxTreeNode getParent() {
		return parent;
	}

	public int getSeverity() {
		return severity;
	}

	public String getMessage() {
		return message;
	}

	public List<BlackboxDiagnosticInfo> getChildren() {
		return Collections.unmodifiableList(children);
	}

	public void addChild(BlackboxDiagnosticInfo child) {
		children.add(child);
	}

	public boolean isError() {
		return severity == Diagnostic.ERROR || severity == Diagnostic.CANCEL;
	}

	public boolean hasErrors() {
		if (isError()) {
			return true;
		}
		for (BlackboxDiagnosticInfo child : children) {
			if (child.hasErrors()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String getLabel() {
		return message;
	}

	@Override
	public BlackboxNodeKind getNodeKind() {
		return isError() ? BlackboxNodeKind.ERROR
				: severity == Diagnostic.WARNING ? BlackboxNodeKind.WARNING : BlackboxNodeKind.INFO;
	}
}
