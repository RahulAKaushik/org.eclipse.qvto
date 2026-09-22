package org.eclipse.m2m.internal.qvt.oml.bbox.ui.navigator;

import org.eclipse.core.resources.IProject;

public class BlackboxRootNode {

	private final IProject project;
	private volatile boolean hasErrors;

	public BlackboxRootNode(IProject project) {
		this.project = project;
	}

	public IProject getProject() {
		return project;
	}

	public boolean hasErrors() {
		return hasErrors;
	}

	void setHasErrors(boolean hasErrors) {
		this.hasErrors = hasErrors;
	}

	@Override
	public int hashCode() {
		return project.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof BlackboxRootNode == false) {
			return false;
		}
		return project.equals(((BlackboxRootNode) obj).project);
	}
}
