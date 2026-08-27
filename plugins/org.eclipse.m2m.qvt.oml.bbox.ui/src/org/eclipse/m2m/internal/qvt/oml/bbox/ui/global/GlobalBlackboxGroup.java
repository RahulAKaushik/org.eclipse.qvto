package org.eclipse.m2m.internal.qvt.oml.bbox.ui.global;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxDiagnosticInfo;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxUnitInfo;

/**
 * A provenance group in the global tree. Its parent is either the global
 * result or another group; children are nested groups, units, or diagnostics.
 */
public class GlobalBlackboxGroup {

	private final Object parent;
	private final GlobalBlackboxGroupKind kind;
	private final String key;
	private final String label;
	private final IJavaProject javaProject;
	private final List<Object> children = new ArrayList<Object>();

	public GlobalBlackboxGroup(Object parent, GlobalBlackboxGroupKind kind, String key, String label) {
		this(parent, kind, key, label, null);
	}

	public GlobalBlackboxGroup(Object parent, GlobalBlackboxGroupKind kind, String key, String label,
			IJavaProject javaProject) {
		this.parent = parent;
		this.kind = kind;
		this.key = key;
		this.label = label;
		this.javaProject = javaProject;
	}

	public Object getParent() {
		return parent;
	}

	public GlobalBlackboxGroupKind getKind() {
		return kind;
	}

	public String getKey() {
		return key;
	}

	public String getLabel() {
		return label;
	}

	/**
	 * Returns the Java project used to resolve an Open action. For binary units
	 * this is a project whose classpath exposes the unit, not necessarily the
	 * physical owner of the descriptor or class file.
	 */
	public IJavaProject getJavaProject() {
		return javaProject;
	}

	public List<Object> getChildren() {
		return Collections.unmodifiableList(children);
	}

	public void addChild(Object child) {
		children.add(child);
	}

	public boolean hasErrors() {
		for (Object child : children) {
			if (child instanceof GlobalBlackboxGroup && ((GlobalBlackboxGroup) child).hasErrors()) {
				return true;
			}
			if (child instanceof BlackboxUnitInfo && ((BlackboxUnitInfo) child).hasErrors()) {
				return true;
			}
			if (child instanceof BlackboxDiagnosticInfo && ((BlackboxDiagnosticInfo) child).hasErrors()) {
				return true;
			}
		}
		return false;
	}

	public void sortChildren() {
		for (Object child : children) {
			if (child instanceof GlobalBlackboxGroup) {
				((GlobalBlackboxGroup) child).sortChildren();
			}
		}
		Collections.sort(children, new Comparator<Object>() {
			public int compare(Object left, Object right) {
				int kindComparison = Integer.valueOf(order(left)).compareTo(Integer.valueOf(order(right)));
				if (kindComparison != 0) {
					return kindComparison;
				}
				return label(left).compareTo(label(right));
			}
		});
	}

	private static int order(Object element) {
		if (element instanceof GlobalBlackboxGroup) {
			return 0;
		}
		if (element instanceof BlackboxUnitInfo) {
			return 1;
		}
		return 2;
	}

	private static String label(Object element) {
		if (element instanceof GlobalBlackboxGroup) {
			GlobalBlackboxGroup group = (GlobalBlackboxGroup) element;
			return group.getLabel() != null ? group.getLabel() : group.getKey();
		}
		if (element instanceof BlackboxUnitInfo) {
			return ((BlackboxUnitInfo) element).getQualifiedName();
		}
		if (element instanceof BlackboxDiagnosticInfo) {
			String message = ((BlackboxDiagnosticInfo) element).getMessage();
			return message != null ? message : ""; //$NON-NLS-1$
		}
		return String.valueOf(element);
	}
}
