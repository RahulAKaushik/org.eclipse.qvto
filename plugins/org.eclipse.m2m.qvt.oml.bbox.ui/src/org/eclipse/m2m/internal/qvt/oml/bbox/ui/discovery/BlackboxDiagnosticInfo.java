package org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.emf.common.util.Diagnostic;

public class BlackboxDiagnosticInfo {

	private final Object parent;
	private final int severity;
	private final String message;
	private final List<BlackboxDiagnosticInfo> children = new ArrayList<BlackboxDiagnosticInfo>();

	public BlackboxDiagnosticInfo(Object parent, int severity, String message) {
		this.parent = parent;
		this.severity = severity;
		this.message = message;
	}

	public Object getParent() {
		return parent;
	}

	public int getSeverity() {
		return severity;
	}

	public String getMessage() {
		return message;
	}

	public List<BlackboxDiagnosticInfo> getChildren() {
		return Collections.unmodifiableList(children);
	}

	public void addChild(BlackboxDiagnosticInfo child) {
		children.add(child);
	}

	public boolean isError() {
		return severity == Diagnostic.ERROR || severity == Diagnostic.CANCEL;
	}

	public boolean hasErrors() {
		if (isError()) {
			return true;
		}
		for (BlackboxDiagnosticInfo child : children) {
			if (child.hasErrors()) {
				return true;
			}
		}
		return false;
	}
}
