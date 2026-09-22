package org.eclipse.m2m.internal.qvt.oml.bbox.ui.navigator;

import org.eclipse.core.resources.IProject;

public class BlackboxRootNode {

	private final IProject project;

	public BlackboxRootNode(IProject project) {
		this.project = project;
	}

	public IProject getProject() {
		return project;
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
