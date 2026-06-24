package org.eclipse.m2m.internal.qvt.oml.bbox.ui.navigator;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxDiagnosticInfo;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxDiscoveryResult;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxModuleInfo;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxOperationInfo;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxUnitInfo;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.ProjectBlackboxDiscoveryService;
import org.eclipse.m2m.internal.qvt.oml.project.QVTOProjectPlugin;

public class BlackboxNavigatorContentProvider implements ITreeContentProvider {

	private final ProjectBlackboxDiscoveryService discoveryService = new ProjectBlackboxDiscoveryService();
	private final Map<IProject, BlackboxDiscoveryResult> cache = new HashMap<IProject, BlackboxDiscoveryResult>();

	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof IProject) {
			IProject project = (IProject) parentElement;
			return isQVTProject(project) ? new Object[] { new BlackboxRootNode(project) } : new Object[0];
		}

		if (parentElement instanceof BlackboxRootNode) {
			BlackboxDiscoveryResult result = getResult(((BlackboxRootNode) parentElement).getProject());
			Object[] children = new Object[result.getUnits().size() + result.getDiagnostics().size()];
			int index = 0;
			for (Object unit : result.getUnits()) {
				children[index++] = unit;
			}
			for (Object diagnostic : result.getDiagnostics()) {
				children[index++] = diagnostic;
			}
			return children;
		}

		if (parentElement instanceof BlackboxUnitInfo) {
			BlackboxUnitInfo unit = (BlackboxUnitInfo) parentElement;
			Object[] children = new Object[unit.getModules().size() + unit.getDiagnostics().size()];
			int index = 0;
			for (Object module : unit.getModules()) {
				children[index++] = module;
			}
			for (Object diagnostic : unit.getDiagnostics()) {
				children[index++] = diagnostic;
			}
			return children;
		}

		if (parentElement instanceof BlackboxModuleInfo) {
			return ((BlackboxModuleInfo) parentElement).getOperations().toArray();
		}

		return new Object[0];
	}

	public Object getParent(Object element) {
		if (element instanceof BlackboxRootNode) {
			return ((BlackboxRootNode) element).getProject();
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
			Object parent = ((BlackboxDiagnosticInfo) element).getParent();
			if (parent instanceof BlackboxDiscoveryResult) {
				return ((BlackboxDiscoveryResult) parent).getParent();
			}
			return parent;
		}
		return null;
	}

	public boolean hasChildren(Object element) {
		if (element instanceof IProject) {
			return isQVTProject((IProject) element);
		}
		if (element instanceof BlackboxRootNode) {
			return true;
		}
		if (element instanceof BlackboxUnitInfo) {
			BlackboxUnitInfo unit = (BlackboxUnitInfo) element;
			return !unit.getModules().isEmpty() || !unit.getDiagnostics().isEmpty();
		}
		if (element instanceof BlackboxModuleInfo) {
			return !((BlackboxModuleInfo) element).getOperations().isEmpty();
		}
		return false;
	}

	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	public void dispose() {
		cache.clear();
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		cache.clear();
	}

	private BlackboxDiscoveryResult getResult(IProject project) {
		BlackboxDiscoveryResult result = cache.get(project);
		if (result == null) {
			result = discoveryService.discover(project);
			cache.put(project, result);
		}
		return result;
	}

	private boolean isQVTProject(IProject project) {
		try {
			return project != null && project.isAccessible() && project.hasNature(QVTOProjectPlugin.NATURE_ID);
		} catch (CoreException e) {
			return false;
		}
	}
}
