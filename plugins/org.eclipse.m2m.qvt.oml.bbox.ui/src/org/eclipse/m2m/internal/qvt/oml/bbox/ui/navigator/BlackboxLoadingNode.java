package org.eclipse.m2m.internal.qvt.oml.bbox.ui.navigator;

public class BlackboxLoadingNode {

	private final BlackboxRootNode parent;

	public BlackboxLoadingNode(BlackboxRootNode parent) {
		this.parent = parent;
	}

	public BlackboxRootNode getParent() {
		return parent;
	}
}
