package org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BlackboxModuleInfo {

	private final BlackboxUnitInfo parent;
	private final String name;
	private final List<String> packageURIs = new ArrayList<String>();
	private final List<BlackboxOperationInfo> operations = new ArrayList<BlackboxOperationInfo>();

	public BlackboxModuleInfo(BlackboxUnitInfo parent, String name) {
		this.parent = parent;
		this.name = name;
	}

	public BlackboxUnitInfo getParent() {
		return parent;
	}

	public String getName() {
		return name;
	}

	public List<String> getPackageURIs() {
		return Collections.unmodifiableList(packageURIs);
	}

	public void addPackageURI(String packageURI) {
		if (packageURI != null && packageURI.length() > 0 && !packageURIs.contains(packageURI)) {
			packageURIs.add(packageURI);
		}
	}

	public List<BlackboxOperationInfo> getOperations() {
		return Collections.unmodifiableList(operations);
	}

	public void addOperation(BlackboxOperationInfo operation) {
		operations.add(operation);
	}
}
