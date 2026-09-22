package org.eclipse.m2m.internal.qvt.oml.bbox.ui.navigator;

/** Loading placeholder whose parent is the project's blackbox root node. */
public class BlackboxLoadingNode {

	private final BlackboxRootNode parent;

	public BlackboxLoadingNode(BlackboxRootNode parent) {
		this.parent = parent;
	}

	public BlackboxRootNode getParent() {
		return parent;
	}
}
