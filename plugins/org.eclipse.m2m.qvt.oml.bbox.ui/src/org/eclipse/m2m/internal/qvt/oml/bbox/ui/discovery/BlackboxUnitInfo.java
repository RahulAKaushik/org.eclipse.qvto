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

import org.eclipse.emf.common.util.URI;

/**
 * A discovered blackbox unit. The parent is a
 * {@link BlackboxDiscoveryResult} in Project Explorer or a global-view group.
 */
public class BlackboxUnitInfo implements BlackboxTreeNode, BlackboxNavigable {

	private final BlackboxTreeNode parent;
	private final String qualifiedName;
	private final URI uri;
	private boolean loaded;
	private final List<BlackboxModuleInfo> modules = new ArrayList<BlackboxModuleInfo>();
	private final List<BlackboxDiagnosticInfo> diagnostics = new ArrayList<BlackboxDiagnosticInfo>();

	public BlackboxUnitInfo(BlackboxTreeNode parent, String qualifiedName, URI uri) {
		this.parent = parent;
		this.qualifiedName = qualifiedName;
		this.uri = uri;
	}

	public BlackboxTreeNode getParent() {
		return parent;
	}

	public String getQualifiedName() {
		return qualifiedName;
	}

	public URI getURI() {
		return uri;
	}

	public boolean isLoaded() {
		return loaded;
	}

	public void setLoaded(boolean loaded) {
		this.loaded = loaded;
	}

	public List<BlackboxModuleInfo> getModules() {
		return Collections.unmodifiableList(modules);
	}

	public void addModule(BlackboxModuleInfo module) {
		modules.add(module);
	}

	public List<BlackboxDiagnosticInfo> getDiagnostics() {
		return Collections.unmodifiableList(diagnostics);
	}

	public void addDiagnostic(BlackboxDiagnosticInfo diagnostic) {
		diagnostics.add(diagnostic);
	}

	public boolean hasErrors() {
		for (BlackboxDiagnosticInfo diagnostic : diagnostics) {
			if (diagnostic.hasErrors()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean hasChildren() {
		return !modules.isEmpty() || !diagnostics.isEmpty();
	}

	@Override
	public List<BlackboxTreeNode> getChildren() {
		List<BlackboxTreeNode> children = new ArrayList<BlackboxTreeNode>(modules);
		children.addAll(diagnostics);
		return Collections.unmodifiableList(children);
	}

	@Override
	public String getLabel() {
		return qualifiedName;
	}

	@Override
	public BlackboxNodeKind getNodeKind() {
		return BlackboxNodeKind.UNIT;
	}

	@Override
	public BlackboxUnitInfo getNavigationUnit() {
		return this;
	}
}
