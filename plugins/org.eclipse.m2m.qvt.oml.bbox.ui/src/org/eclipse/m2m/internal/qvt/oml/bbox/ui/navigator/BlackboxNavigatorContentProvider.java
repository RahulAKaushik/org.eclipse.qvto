package org.eclipse.m2m.internal.qvt.oml.bbox.ui.navigator;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.Messages;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.QVTBBoxUIPlugin;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxDiagnosticInfo;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxDiscoveryResult;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxModuleInfo;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxOperationInfo;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxProjectDependencies;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxUnitInfo;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxVisibilityScope;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.ProjectBlackboxDiscoveryService;
import org.eclipse.m2m.internal.qvt.oml.emf.util.urimap.MetamodelURIMappingHelper;
import org.eclipse.m2m.internal.qvt.oml.project.QVTOProjectPlugin;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.progress.WorkbenchJob;

public class BlackboxNavigatorContentProvider implements ITreeContentProvider {

	private final ProjectBlackboxDiscoveryService discoveryService = new ProjectBlackboxDiscoveryService();
	private final Map<IProject, BlackboxDiscoveryResult> cache = new HashMap<IProject, BlackboxDiscoveryResult>();
	private final Map<IProject, Job> discoveryJobs = new HashMap<IProject, Job>();
	private final IResourceChangeListener resourceChangeListener;
	private final BlackboxVisibilitySettings.Listener scopeListener;
	private Viewer viewer;

	public BlackboxNavigatorContentProvider() {
		resourceChangeListener = new IResourceChangeListener() {
			public void resourceChanged(IResourceChangeEvent event) {
				invalidateAffectedProjects(event);
			}
		};
		ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceChangeListener, IResourceChangeEvent.POST_CHANGE);
		scopeListener = new BlackboxVisibilitySettings.Listener() {
			public void scopeChanged() {
				resetForScopeChange();
			}
		};
		BlackboxVisibilitySettings.addListener(scopeListener);
	}

	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof IProject) {
			IProject project = (IProject) parentElement;
			return isQVTProject(project) ? new Object[] { createRootNode(project) } : new Object[0];
		}

		if (parentElement instanceof BlackboxRootNode) {
			BlackboxRootNode root = (BlackboxRootNode) parentElement;
			BlackboxDiscoveryResult result = getResult(root);
			if (result == null) {
				return new Object[] { new BlackboxLoadingNode(root) };
			}
			root.setHasErrors(result.hasErrors());
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

		if (parentElement instanceof BlackboxDiagnosticInfo) {
			return ((BlackboxDiagnosticInfo) parentElement).getChildren().toArray();
		}

		return new Object[0];
	}

	public Object getParent(Object element) {
		if (element instanceof BlackboxRootNode) {
			return ((BlackboxRootNode) element).getProject();
		}
		if (element instanceof BlackboxLoadingNode) {
			return ((BlackboxLoadingNode) element).getParent();
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
		if (element instanceof BlackboxDiagnosticInfo) {
			return !((BlackboxDiagnosticInfo) element).getChildren().isEmpty();
		}
		return false;
	}

	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	public void dispose() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceChangeListener);
		BlackboxVisibilitySettings.removeListener(scopeListener);
		clearDiscoveryState();
		viewer = null;
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		clearDiscoveryState();
		this.viewer = viewer;
	}

	private BlackboxDiscoveryResult getResult(BlackboxRootNode root) {
		IProject project = root.getProject();
		synchronized (cache) {
			BlackboxDiscoveryResult result = cache.get(project);
			if (result != null) {
				return result;
			}
			if (!discoveryJobs.containsKey(project)) {
				scheduleDiscovery(root);
			}
		}
		return null;
	}

	private BlackboxRootNode createRootNode(IProject project) {
		BlackboxVisibilityScope scope = BlackboxVisibilitySettings.getScope();
		BlackboxRootNode root = new BlackboxRootNode(project, scope);
		BlackboxDiscoveryResult result = null;
		synchronized (cache) {
			result = cache.get(project);
		}
		if (result != null) {
			root.setHasErrors(result.hasErrors());
		} else if (scope == BlackboxVisibilityScope.PROJECT_VISIBLE) {
			root.setHasErrors(ProjectBlackboxDiscoveryService.hasBlackboxProblemMarkers(project));
		}
		return root;
	}

	private void scheduleDiscovery(final BlackboxRootNode root) {
		final IProject project = root.getProject();
		Job job = new Job(NLS.bind(Messages.BlackboxNavigator_discoveryJobName, project.getName())) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					if (monitor.isCanceled() || !isQVTProject(project)) {
						return Status.CANCEL_STATUS;
					}
					BlackboxDiscoveryResult result = discoveryService.discover(project, root.getScope(), false, monitor);
					root.setHasErrors(result.hasErrors());
					synchronized (cache) {
						if (discoveryJobs.get(project) == this) {
							if (!monitor.isCanceled()) {
								cache.put(project, result);
							}
							discoveryJobs.remove(project);
						}
					}
					if (!monitor.isCanceled()) {
						refresh(root);
					}
					return Status.OK_STATUS;
				} catch (RuntimeException e) {
					QVTBBoxUIPlugin.log(e);
					synchronized (cache) {
						if (discoveryJobs.get(project) == this) {
							discoveryJobs.remove(project);
						}
					}
					refresh(root);
					return Status.CANCEL_STATUS;
				} catch (LinkageError e) {
					QVTBBoxUIPlugin.log(e);
					synchronized (cache) {
						if (discoveryJobs.get(project) == this) {
							discoveryJobs.remove(project);
						}
					}
					refresh(root);
					return Status.CANCEL_STATUS;
				}
			}
		};
		job.setUser(false);
		synchronized (cache) {
			if (discoveryJobs.containsKey(project)) {
				return;
			}
			discoveryJobs.put(project, job);
		}
		job.schedule();
	}

	private void refresh(final BlackboxRootNode root) {
		final Viewer currentViewer = viewer;
		if (currentViewer == null) {
			return;
		}

		WorkbenchJob refreshJob = new WorkbenchJob(Messages.BlackboxNavigator_refreshJobName) {
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				Control control = currentViewer.getControl();
				if (control == null || control.isDisposed()) {
					return Status.CANCEL_STATUS;
				}
				if (currentViewer instanceof StructuredViewer) {
					((StructuredViewer) currentViewer).refresh(root);
				}
				return Status.OK_STATUS;
			}
		};
		refreshJob.setSystem(true);
		refreshJob.schedule();
	}

	private void invalidateAffectedProjects(IResourceChangeEvent event) {
		IResourceDelta delta = event.getDelta();
		if (delta == null) {
			return;
		}

		final Set<IProject> affectedProjects = new LinkedHashSet<IProject>();
		final Set<IProject> markerChangedProjects = new LinkedHashSet<IProject>();
		try {
			delta.accept(new IResourceDeltaVisitor() {
				public boolean visit(IResourceDelta resourceDelta) throws CoreException {
					IResource resource = resourceDelta.getResource();
					if (resource == null) {
						return true;
					}
					IProject project = resource.getProject();
					if (project != null && hasBlackboxMarkerDelta(resourceDelta)) {
						markerChangedProjects.add(project);
					}
					if (resource.getType() == IResource.PROJECT) {
						return true;
					}
					if (isRelevantResource(resource)) {
						affectedProjects.add(resource.getProject());
						return false;
					}
					return true;
				}
			});
		} catch (CoreException e) {
			QVTBBoxUIPlugin.log(e);
		}

		Set<IProject> projectsToInvalidate = affectedProjects.isEmpty()
				? affectedProjects
				: BlackboxProjectDependencies.includeDependentQVTProjects(affectedProjects);
		for (IProject project : projectsToInvalidate) {
			invalidate(project);
		}
		for (IProject project : markerChangedProjects) {
			if (!affectedProjects.contains(project)) {
				refresh(createRootNode(project));
			}
		}
	}

	private boolean hasBlackboxMarkerDelta(IResourceDelta resourceDelta) {
		for (IMarkerDelta markerDelta : resourceDelta.getMarkerDeltas()) {
			if (ProjectBlackboxDiscoveryService.isBlackboxProblemMarker(markerDelta)) {
				return true;
			}
		}
		return false;
	}

	private void invalidate(IProject project) {
		Job job = null;
		boolean hasCachedResult = false;
		synchronized (cache) {
			hasCachedResult = cache.containsKey(project);
			job = discoveryJobs.remove(project);
		}
		if (job != null) {
			job.cancel();
		}

		BlackboxRootNode root = createRootNode(project);
		if (hasCachedResult) {
			scheduleDiscovery(root);
		} else {
			refresh(root);
		}
	}

	private boolean isQVTProject(IProject project) {
		try {
			return project != null && project.isAccessible() && project.hasNature(QVTOProjectPlugin.NATURE_ID);
		} catch (CoreException e) {
			return false;
		}
	}

	private void resetForScopeChange() {
		clearDiscoveryState();
		final Viewer currentViewer = viewer;
		if (currentViewer == null) {
			return;
		}
		WorkbenchJob refreshJob = new WorkbenchJob(Messages.BlackboxNavigator_refreshJobName) {
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				Control control = currentViewer.getControl();
				if (control == null || control.isDisposed()) {
					return Status.CANCEL_STATUS;
				}
				currentViewer.refresh();
				return Status.OK_STATUS;
			}
		};
		refreshJob.setSystem(true);
		refreshJob.schedule();
	}

	private void clearDiscoveryState() {
		synchronized (cache) {
			cache.clear();
			for (Job job : discoveryJobs.values()) {
				job.cancel();
			}
			discoveryJobs.clear();
		}
	}

	private boolean isRelevantResource(IResource resource) {
		if (resource.getType() != IResource.FILE) {
			return false;
		}

		String name = resource.getName();
		String extension = resource.getFileExtension();
		return "qvto".equals(extension) //$NON-NLS-1$
				|| "java".equals(extension) //$NON-NLS-1$
				|| "class".equals(extension) //$NON-NLS-1$
				|| "jar".equals(extension) //$NON-NLS-1$
					|| "plugin.xml".equals(name) //$NON-NLS-1$
					|| "MANIFEST.MF".equals(name) //$NON-NLS-1$
					|| ".classpath".equals(name) //$NON-NLS-1$
					|| isMetamodelFileName(name)
					|| MetamodelURIMappingHelper.getMappingFileHandle(resource.getProject()).equals(resource);
		}

	private boolean isMetamodelFileName(String fileName) {
		return fileName.endsWith(".ecore") //$NON-NLS-1$
				|| fileName.endsWith(".xcore") //$NON-NLS-1$
				|| fileName.endsWith(".emof") //$NON-NLS-1$
				|| fileName.endsWith(".oclinecore"); //$NON-NLS-1$
	}
}
