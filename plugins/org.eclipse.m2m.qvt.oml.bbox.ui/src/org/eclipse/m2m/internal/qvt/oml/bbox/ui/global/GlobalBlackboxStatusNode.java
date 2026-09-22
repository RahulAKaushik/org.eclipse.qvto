package org.eclipse.m2m.internal.qvt.oml.bbox.ui.global;

/** A leaf status whose parent is the global group in which it is displayed. */
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
