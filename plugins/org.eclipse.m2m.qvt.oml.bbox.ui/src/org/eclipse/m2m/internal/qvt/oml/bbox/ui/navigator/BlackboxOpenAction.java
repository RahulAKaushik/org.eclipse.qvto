package org.eclipse.m2m.internal.qvt.oml.bbox.ui.navigator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.Messages;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.QVTBBoxUIPlugin;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxDiscoveryResult;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxModuleInfo;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxOperationInfo;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxUnitInfo;
import org.eclipse.ui.IEditorPart;

public class BlackboxOpenAction extends Action {

	private OpenTarget target;

	public BlackboxOpenAction() {
		super(Messages.BlackboxNavigator_open);
		setId(QVTBBoxUIPlugin.PLUGIN_ID + ".open"); //$NON-NLS-1$
		setEnabled(false);
	}

	public void selectionChanged(IStructuredSelection selection) {
		target = resolve(selection);
		setEnabled(target != null);
	}

	@Override
	public void run() {
		OpenTarget currentTarget = target;
		if (currentTarget == null) {
			return;
		}

		try {
			IEditorPart editor = JavaUI.openInEditor(currentTarget.type);
			if (currentTarget.operationName != null) {
				IMethod method = findUniqueMethod(currentTarget.type, currentTarget.operationName);
				if (method != null) {
					JavaUI.revealInEditor(editor, method.getPrimaryElement());
				}
			}
		} catch (CoreException e) {
			QVTBBoxUIPlugin.log(e);
		}
	}

	private OpenTarget resolve(IStructuredSelection selection) {
		if (selection == null || selection.size() != 1) {
			return null;
		}

		Object selected = selection.getFirstElement();
		BlackboxUnitInfo unit = getUnit(selected);
		if (unit == null || !(unit.getParent() instanceof BlackboxDiscoveryResult)) {
			return null;
		}

		Object resultParent = ((BlackboxDiscoveryResult) unit.getParent()).getParent();
		if (!(resultParent instanceof IProject)) {
			return null;
		}

		IJavaProject javaProject = JavaCore.create((IProject) resultParent);
		if (!javaProject.exists()) {
			return null;
		}

		try {
			IType type = javaProject.findType(unit.getQualifiedName());
			if (type == null) {
				return null;
			}
			String operationName = selected instanceof BlackboxOperationInfo
					? ((BlackboxOperationInfo) selected).getName()
					: null;
			return new OpenTarget(type, operationName);
		} catch (CoreException e) {
			QVTBBoxUIPlugin.log(e);
			return null;
		}
	}

	private BlackboxUnitInfo getUnit(Object element) {
		if (element instanceof BlackboxUnitInfo) {
			return (BlackboxUnitInfo) element;
		}
		if (element instanceof BlackboxModuleInfo) {
			return ((BlackboxModuleInfo) element).getParent();
		}
		if (element instanceof BlackboxOperationInfo) {
			return ((BlackboxOperationInfo) element).getParent().getParent();
		}
		return null;
	}

	private IMethod findUniqueMethod(IType type, String operationName) throws CoreException {
		IMethod result = null;
		for (IMethod method : type.getMethods()) {
			if (!operationName.equals(method.getElementName())) {
				continue;
			}
			if (result != null) {
				return null;
			}
			result = method;
		}
		return result;
	}

	private static class OpenTarget {

		final IType type;
		final String operationName;

		OpenTarget(IType type, String operationName) {
			this.type = type;
			this.operationName = operationName;
		}
	}
}
