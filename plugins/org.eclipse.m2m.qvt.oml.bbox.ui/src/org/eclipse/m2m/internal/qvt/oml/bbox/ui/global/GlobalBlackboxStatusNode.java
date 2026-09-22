package org.eclipse.m2m.internal.qvt.oml.bbox.ui.global;

final class GlobalBlackboxStatusNode {

	private final Object parent;
	private final String message;

	GlobalBlackboxStatusNode(Object parent, String message) {
		this.parent = parent;
		this.message = message;
	}

	Object getParent() {
		return parent;
	}

	String getMessage() {
		return message;
	}
}
