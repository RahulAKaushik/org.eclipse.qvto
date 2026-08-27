package org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery;

/** A blackbox operation whose parent is its containing module. */
public class BlackboxOperationInfo {

	private final BlackboxModuleInfo parent;
	private final String name;
	private final String signature;

	public BlackboxOperationInfo(BlackboxModuleInfo parent, String name, String signature) {
		this.parent = parent;
		this.name = name;
		this.signature = signature;
	}

	public BlackboxModuleInfo getParent() {
		return parent;
	}

	public String getName() {
		return name;
	}

	public String getSignature() {
		return signature;
	}
}
