package org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Project-scoped discovery tree. Its parent is the project represented above
 * the blackbox root node.
 * <p>
 * Discovery jobs populate this model off the UI thread. Once published to a
 * viewer, consumers treat it and its children as read-only.
 */
public class BlackboxDiscoveryResult {

	private final Object parent;
	private final List<BlackboxUnitInfo> units = new ArrayList<BlackboxUnitInfo>();
	private final List<BlackboxDiagnosticInfo> diagnostics = new ArrayList<BlackboxDiagnosticInfo>();

	public BlackboxDiscoveryResult(Object parent) {
		this.parent = parent;
	}

	public Object getParent() {
		return parent;
	}

	public List<BlackboxUnitInfo> getUnits() {
		return Collections.unmodifiableList(units);
	}

	void addUnit(BlackboxUnitInfo unit) {
		units.add(unit);
	}

	void sortUnits(java.util.Comparator<BlackboxUnitInfo> comparator) {
		Collections.sort(units, comparator);
	}

	public List<BlackboxDiagnosticInfo> getDiagnostics() {
		return Collections.unmodifiableList(diagnostics);
	}

	void addDiagnostic(BlackboxDiagnosticInfo diagnostic) {
		diagnostics.add(diagnostic);
	}

	public boolean hasErrors() {
		for (BlackboxUnitInfo unit : units) {
			if (unit.hasErrors()) {
				return true;
			}
		}
		for (BlackboxDiagnosticInfo diagnostic : diagnostics) {
			if (diagnostic.hasErrors()) {
				return true;
			}
		}
		return false;
	}
}
