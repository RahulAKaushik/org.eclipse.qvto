package org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery;

import org.eclipse.emf.common.util.Diagnostic;

public class BlackboxDiagnosticInfo {

	private final Object parent;
	private final int severity;
	private final String message;

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

	public boolean isError() {
		return severity == Diagnostic.ERROR || severity == Diagnostic.CANCEL;
	}
}
