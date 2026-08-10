package org.eclipse.m2m.internal.qvt.oml.bbox.ui.global;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.Messages;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxDiagnosticInfo;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxModuleInfo;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxOperationInfo;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxUnitInfo;

public class GlobalBlackboxContentProvider implements ITreeContentProvider {

	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof GlobalBlackboxDiscoveryResult) {
			return ((GlobalBlackboxDiscoveryResult) inputElement).getGroups().toArray();
		}
		if (inputElement instanceof GlobalBlackboxLoadingNode) {
			return new Object[] { inputElement };
		}
		return new Object[0];
	}

	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof GlobalBlackboxGroup) {
			GlobalBlackboxGroup group = (GlobalBlackboxGroup) parentElement;
			if (group.getChildren().isEmpty()) {
				return new Object[] { new GlobalBlackboxStatusNode(group, Messages.GlobalBlackboxView_empty) };
			}
			return group.getChildren().toArray();
		}
		if (parentElement instanceof BlackboxUnitInfo) {
			BlackboxUnitInfo unit = (BlackboxUnitInfo) parentElement;
			Object[] children = new Object[unit.getModules().size() + unit.getDiagnostics().size()];
			int index = 0;
			for (BlackboxModuleInfo module : unit.getModules()) {
				children[index++] = module;
			}
			for (BlackboxDiagnosticInfo diagnostic : unit.getDiagnostics()) {
				children[index++] = diagnostic;
			}
			return children;
		}
		if (parentElement instanceof BlackboxModuleInfo) {
			return ((BlackboxModuleInfo) parentElement).getOperations().toArray();
		}
		if (parentElement instanceof BlackboxDiagnosticInfo) {
			return ((BlackboxDiagnosticInfo) parentElement).getChildren().toArray();
		}
		return new Object[0];
	}

	public Object getParent(Object element) {
		if (element instanceof GlobalBlackboxGroup) {
			return ((GlobalBlackboxGroup) element).getParent();
		}
		if (element instanceof GlobalBlackboxStatusNode) {
			return ((GlobalBlackboxStatusNode) element).getParent();
		}
		if (element instanceof BlackboxUnitInfo) {
			return ((BlackboxUnitInfo) element).getParent();
		}
		if (element instanceof BlackboxModuleInfo) {
			return ((BlackboxModuleInfo) element).getParent();
		}
		if (element instanceof BlackboxOperationInfo) {
			return ((BlackboxOperationInfo) element).getParent();
		}
		if (element instanceof BlackboxDiagnosticInfo) {
			return ((BlackboxDiagnosticInfo) element).getParent();
		}
		return null;
	}

	public boolean hasChildren(Object element) {
		if (element instanceof GlobalBlackboxGroup) {
			return true;
		}
		if (element instanceof BlackboxUnitInfo) {
			BlackboxUnitInfo unit = (BlackboxUnitInfo) element;
			return !unit.getModules().isEmpty() || !unit.getDiagnostics().isEmpty();
		}
		if (element instanceof BlackboxModuleInfo) {
			return !((BlackboxModuleInfo) element).getOperations().isEmpty();
		}
		if (element instanceof BlackboxDiagnosticInfo) {
			return !((BlackboxDiagnosticInfo) element).getChildren().isEmpty();
		}
		return false;
	}

	public void dispose() {
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}
}
