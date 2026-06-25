package org.eclipse.m2m.internal.qvt.oml.bbox.ui.navigator;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

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
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.QVTBBoxUIPlugin;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxDiagnosticInfo;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxDiscoveryResult;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxModuleInfo;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxOperationInfo;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxUnitInfo;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.ProjectBlackboxDiscoveryService;
import org.eclipse.m2m.internal.qvt.oml.emf.util.mmregistry.MetamodelRegistry;
import org.eclipse.m2m.internal.qvt.oml.emf.util.urimap.MetamodelURIMappingHelper;
import org.eclipse.m2m.internal.qvt.oml.project.QVTOProjectPlugin;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.progress.WorkbenchJob;
import org.eclipse.core.runtime.jobs.Job;

public class BlackboxNavigatorContentProvider implements ITreeContentProvider {

	private final ProjectBlackboxDiscoveryService discoveryService = new ProjectBlackboxDiscoveryService();
	private final Map<IProject, BlackboxDiscoveryResult> cache = new HashMap<IProject, BlackboxDiscoveryResult>();
	private final Map<IProject, Job> discoveryJobs = new HashMap<IProject, Job>();
	private final IResourceChangeListener resourceChangeListener;
	private Viewer viewer;

	public BlackboxNavigatorContentProvider() {
		resourceChangeListener = new IResourceChangeListener() {
			public void resourceChanged(IResourceChangeEvent event) {
				invalidateAffectedProjects(event);
			}
		};
		ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceChangeListener, IResourceChangeEvent.POST_CHANGE);
	}

	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof IProject) {
			IProject project = (IProject) parentElement;
			return isQVTProject(project) ? new Object[] { new BlackboxRootNode(project) } : new Object[0];
		}

		if (parentElement instanceof BlackboxRootNode) {
			BlackboxRootNode root = (BlackboxRootNode) parentElement;
			BlackboxDiscoveryResult result = getResult(root);
			if (result == null) {
				return new Object[] { new BlackboxLoadingNode(root) };
			}
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
		return false;
	}

	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	public void dispose() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceChangeListener);
		synchronized (cache) {
			cache.clear();
			for (Job job : discoveryJobs.values()) {
				job.cancel();
			}
			discoveryJobs.clear();
		}
		viewer = null;
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		synchronized (cache) {
			cache.clear();
			for (Job job : discoveryJobs.values()) {
				job.cancel();
			}
			discoveryJobs.clear();
		}
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

	private void scheduleDiscovery(final BlackboxRootNode root) {
		final IProject project = root.getProject();
		Job job = new Job("Discover QVTo blackboxes for " + project.getName()) { //$NON-NLS-1$
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					if (monitor.isCanceled() || !isQVTProject(project)) {
						return Status.CANCEL_STATUS;
					}
					BlackboxDiscoveryResult result = discoveryService.discover(project, false);
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

		WorkbenchJob refreshJob = new WorkbenchJob("Refresh QVTo blackboxes") { //$NON-NLS-1$
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
		try {
			delta.accept(new IResourceDeltaVisitor() {
				public boolean visit(IResourceDelta resourceDelta) throws CoreException {
					IResource resource = resourceDelta.getResource();
					if (resource == null) {
						return true;
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

		for (IProject project : affectedProjects) {
			invalidate(project);
		}
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

		BlackboxRootNode root = new BlackboxRootNode(project);
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

	private boolean isRelevantResource(IResource resource) {
		if (resource.getType() != IResource.FILE) {
			return false;
		}

		String name = resource.getName();
		String extension = resource.getFileExtension();
		return "qvto".equals(extension) //$NON-NLS-1$
				|| "java".equals(extension) //$NON-NLS-1$
				|| "class".equals(extension) //$NON-NLS-1$
				|| "plugin.xml".equals(name) //$NON-NLS-1$
				|| "MANIFEST.MF".equals(name) //$NON-NLS-1$
				|| ".classpath".equals(name) //$NON-NLS-1$
				|| MetamodelRegistry.isMetamodelFileName(name)
				|| MetamodelURIMappingHelper.getMappingFileHandle(resource.getProject()).equals(resource);
	}
}
