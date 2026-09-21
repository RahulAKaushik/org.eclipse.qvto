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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.Messages;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxJavaContext;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxNodeKind;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxTreeNode;

/**
 * A provenance group in the global tree. Its parent is either the global
 * result or another group; children are nested groups, units, or diagnostics.
 */
public class GlobalBlackboxGroup implements BlackboxTreeNode, BlackboxJavaContext {

	private final BlackboxTreeNode parent;
	private final GlobalBlackboxGroupKind kind;
	private final String key;
	private final String label;
	private final IJavaProject javaProject;
	private final List<BlackboxTreeNode> children = new ArrayList<BlackboxTreeNode>();

	public GlobalBlackboxGroup(BlackboxTreeNode parent, GlobalBlackboxGroupKind kind, String key, String label) {
		this(parent, kind, key, label, null);
	}

	public GlobalBlackboxGroup(BlackboxTreeNode parent, GlobalBlackboxGroupKind kind, String key, String label,
			IJavaProject javaProject) {
		this.parent = parent;
		this.kind = kind;
		this.key = key;
		this.label = label;
		this.javaProject = javaProject;
	}

	public BlackboxTreeNode getParent() {
		return parent;
	}

	public GlobalBlackboxGroupKind getKind() {
		return kind;
	}

	public String getKey() {
		return key;
	}

	public String getLabel() {
		switch (kind) {
		case WORKSPACE_PROJECTS:
			return Messages.GlobalBlackboxView_workspaceProjects;
		case JAVA_LIBRARIES:
			return Messages.GlobalBlackboxView_javaLibraries;
		case ECLIPSE_PLATFORM:
			return Messages.GlobalBlackboxView_eclipsePlatform;
		case EXTENSION_CONTRIBUTIONS:
			return Messages.GlobalBlackboxView_extensionContributions;
		case ACTIVE_PLUGINS:
			return Messages.GlobalBlackboxView_activePlugins;
		case RUNTIME_REGISTRATIONS:
			return Messages.GlobalBlackboxView_runtimeRegistrations;
		default:
			return label;
		}
	}

	/**
	 * Returns the Java project used to resolve an Open action. For binary units
	 * this is a project whose classpath exposes the unit, not necessarily the
	 * physical owner of the descriptor or class file.
	 */
	public IJavaProject getJavaProject() {
		return javaProject;
	}

	public List<BlackboxTreeNode> getChildren() {
		return Collections.unmodifiableList(children);
	}

	public void addChild(BlackboxTreeNode child) {
		children.add(child);
	}

	public void sortChildren() {
		for (BlackboxTreeNode child : children) {
			if (child instanceof GlobalBlackboxGroup) {
				((GlobalBlackboxGroup) child).sortChildren();
			}
		}
		Collections.sort(children, new Comparator<BlackboxTreeNode>() {
			public int compare(BlackboxTreeNode left, BlackboxTreeNode right) {
				int kindComparison = Integer.valueOf(left.getNodeKind().getSortOrder())
						.compareTo(Integer.valueOf(right.getNodeKind().getSortOrder()));
				if (kindComparison != 0) {
					return kindComparison;
				}
				return left.getSortKey().compareTo(right.getSortKey());
			}
		});
	}

	@Override
	public String getSortKey() {
		return label != null ? label : key;
	}

	@Override
	public BlackboxNodeKind getNodeKind() {
		return kind == GlobalBlackboxGroupKind.PROJECT ? BlackboxNodeKind.PROJECT : BlackboxNodeKind.GROUP;
	}

	@Override
	public boolean hasChildren() {
		return true;
	} // Empty groups display a status placeholder.
}
