package org.eclipse.m2m.internal.qvt.oml.bbox.ui.navigator;

import org.eclipse.core.resources.IProject;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxVisibilityScope;

/**
 * Project Explorer root for one project and visibility scope. Discovery jobs
 * may update the volatile error summary before requesting a UI-thread refresh.
 */
public class BlackboxRootNode {

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
}
