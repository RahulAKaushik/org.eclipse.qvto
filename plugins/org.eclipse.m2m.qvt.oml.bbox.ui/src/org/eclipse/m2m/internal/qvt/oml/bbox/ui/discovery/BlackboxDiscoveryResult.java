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

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

/**
 * Project-scoped discovery tree with a typed Eclipse project context. This
 * result is not a displayed node; the navigator maps it to the synthetic root.
 * <p>
 * Discovery jobs populate this model off the UI thread. Once published to a
 * viewer, consumers treat it and its children as read-only.
 */
public class BlackboxDiscoveryResult implements BlackboxTreeNode, BlackboxJavaContext {

	private final IProject project;
	private final List<BlackboxUnitInfo> units = new ArrayList<BlackboxUnitInfo>();
	private final List<BlackboxDiagnosticInfo> diagnostics = new ArrayList<BlackboxDiagnosticInfo>();

	public BlackboxDiscoveryResult(IProject project) {
		this.project = project;
	}

	public IProject getProject() {
		return project;
	}

	public List<BlackboxUnitInfo> getUnits() {
		return Collections.unmodifiableList(units);
	}

	void addUnit(BlackboxUnitInfo unit) {
		units.add(unit);
	}

	void sortUnits(java.util.Comparator<BlackboxUnitInfo> comparator) {
		Collections.sort(units, comparator);
	}

	public List<BlackboxDiagnosticInfo> getDiagnostics() {
		return Collections.unmodifiableList(diagnostics);
	}

	void addDiagnostic(BlackboxDiagnosticInfo diagnostic) {
		diagnostics.add(diagnostic);
	}

	public boolean hasErrors() {
		for (BlackboxUnitInfo unit : units) {
			if (unit.hasErrors()) {
				return true;
			}
		}
		for (BlackboxDiagnosticInfo diagnostic : diagnostics) {
			if (diagnostic.hasErrors()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public IJavaProject getJavaProject() {
		return project != null ? JavaCore.create(project) : null;
	}

	@Override
	public List<BlackboxTreeNode> getChildren() {
		List<BlackboxTreeNode> children = new ArrayList<BlackboxTreeNode>(units);
		children.addAll(diagnostics);
		return Collections.unmodifiableList(children);
	}

	@Override
	public String getLabel() {
		return project != null ? project.getName() : ""; //$NON-NLS-1$
	}
}
