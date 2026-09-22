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
package org.eclipse.m2m.internal.qvt.oml.bbox.ui.navigator;

import org.eclipse.core.resources.IProject;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.Messages;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxNodeKind;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxTreeNode;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxVisibilityScope;

/**
 * Project Explorer root for one project and visibility scope. Discovery jobs
 * may update the volatile error summary before requesting a UI-thread refresh.
 */
public class BlackboxRootNode implements BlackboxTreeNode {

	private final IProject project;
	private final BlackboxVisibilityScope scope;
	private volatile boolean hasErrors;

	public BlackboxRootNode(IProject project, BlackboxVisibilityScope scope) {
		this.project = project;
		this.scope = scope;
	}

	public IProject getProject() {
		return project;
	}

	public BlackboxVisibilityScope getScope() {
		return scope;
	}

	public boolean hasErrors() {
		return hasErrors;
	}

	void setHasErrors(boolean hasErrors) {
		this.hasErrors = hasErrors;
	}

	@Override
	public int hashCode() {
		return 31 * project.hashCode() + scope.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof BlackboxRootNode == false) {
			return false;
		}
		BlackboxRootNode other = (BlackboxRootNode) obj;
		return project.equals(other.project) && scope == other.scope;
	}

	@Override
	public String getLabel() {
		switch (scope) {
		case PROJECT_VISIBLE:
			return Messages.BlackboxNavigator_rootProjectVisible;
		case PROJECT_ONLY:
			return Messages.BlackboxNavigator_rootProjectOnly;
		default:
			return Messages.BlackboxNavigator_rootProjectDependencies;
		}
	}

	@Override
	public BlackboxNodeKind getNodeKind() {
		return BlackboxNodeKind.UNIT;
	}

	@Override
	public boolean hasChildren() {
		return true;
	} // Discovery is deferred to the content provider.
}
