/*******************************************************************************
 * Copyright (c) 2026 Rahul Arun Kaushik and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Rahul Arun Kaushik - initial API and implementation
 *******************************************************************************/
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
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.Messages;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.QVTBBoxUIPlugin;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxDiscoveryResult;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxProjectDependencies;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxResourceChangeSupport;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxTreeNode;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxVisibilityScope;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.ProjectBlackboxDiscoveryService;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.settings.BlackboxVisibilitySettings;
import org.eclipse.m2m.internal.qvt.oml.project.QvtProjectUtil;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.progress.WorkbenchJob;

/**
 * Owns the Project Explorer discovery cache, jobs, and invalidation listeners.
 * Child requests schedule background discovery; viewer refreshes run through
 * WorkbenchJob on the UI thread. Marker validation has a separate lifecycle and
 * is not triggered by these display-only discoveries.
 */
public class BlackboxNavigatorContentProvider implements ITreeContentProvider {

	private final ProjectBlackboxDiscoveryService discoveryService = new ProjectBlackboxDiscoveryService();
	// The cache monitor protects both maps, including job-identity publication checks.
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
		ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceChangeListener,
				IResourceChangeEvent.POST_CHANGE);
		scopeListener = new BlackboxVisibilitySettings.Listener() {
			public void scopeChanged(IProject project) {
				resetForScopeChange(project);
			}
		};
		BlackboxVisibilitySettings.addListener(scopeListener);
	}

	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof IProject) {
			IProject project = (IProject) parentElement;
			return QvtProjectUtil.isQvtProject(project) ? new Object[] { createRootNode(project) } : new Object[0];
		}

		if (parentElement instanceof BlackboxRootNode) {
			BlackboxRootNode root = (BlackboxRootNode) parentElement;
			BlackboxDiscoveryResult result = getResult(root);
			if (result == null) {
				return new Object[] { new BlackboxLoadingNode(root) };
			}
			root.setHasErrors(result.hasErrors());
			return result.getChildren().toArray();
		}
		return parentElement instanceof BlackboxTreeNode ? ((BlackboxTreeNode) parentElement).getChildren().toArray()
				: new Object[0];
	}

	public Object getParent(Object element) {
		if (element instanceof BlackboxRootNode) {
			return ((BlackboxRootNode) element).getProject();
		}
		if (element instanceof BlackboxTreeNode) {
			BlackboxTreeNode parent = ((BlackboxTreeNode) element).getParent();
			// A result is an invisible container; its children are displayed under
			// the synthetic root, not directly under the Eclipse project.
			if (parent instanceof BlackboxDiscoveryResult) {
				return createRootNode(((BlackboxDiscoveryResult) parent).getProject());
			}
			return parent;
		}
		return null;
	}

	public boolean hasChildren(Object element) {
		if (element instanceof IProject) {
			return QvtProjectUtil.isQvtProject((IProject) element);
		}
		return element instanceof BlackboxTreeNode && ((BlackboxTreeNode) element).hasChildren();
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
		BlackboxVisibilityScope scope = BlackboxVisibilitySettings.getScope(project);
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
					if (monitor.isCanceled() || !QvtProjectUtil.isQvtProject(project)) {
						return Status.CANCEL_STATUS;
					}
					BlackboxDiscoveryResult result = discoveryService.discoverForDisplay(project, root.getScope(),
							monitor);
					root.setHasErrors(result.hasErrors());
					synchronized (cache) {
						// Cancellation is cooperative; a replaced job must not publish.
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
				} catch (OperationCanceledException e) {
					removeDiscoveryJob(project, this);
					return Status.CANCEL_STATUS;
				} catch (RuntimeException e) {
					return discoveryFailed(project, root, this, e);
				} catch (LinkageError e) {
					return discoveryFailed(project, root, this, e);
				}
			}
		};
		job.setSystem(true);
		job.setPriority(Job.DECORATE);
		synchronized (cache) {
			if (discoveryJobs.containsKey(project)) {
				return;
			}
			discoveryJobs.put(project, job);
		}
		job.schedule();
	}

	private IStatus discoveryFailed(IProject project, BlackboxRootNode root, Job job, Throwable throwable) {
		String message = NLS.bind(Messages.BlackboxNavigator_discoveryFailed, project.getName());
		IStatus status = QVTBBoxUIPlugin.createStatus(IStatus.ERROR, message, throwable);
		QVTBBoxUIPlugin.log(status);
		removeDiscoveryJob(project, job);
		refresh(root);
		return status;
	}

	private void removeDiscoveryJob(IProject project, Job job) {
		synchronized (cache) {
			if (discoveryJobs.get(project) == job) {
				discoveryJobs.remove(project);
			}
		}
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
					if (BlackboxResourceChangeSupport.isRelevant(resource)) {
						affectedProjects.add(resource.getProject());
						return false;
					}
					return true;
				}
			});
		} catch (CoreException e) {
			QVTBBoxUIPlugin.log(e);
		}

		Set<IProject> projectsToInvalidate = affectedProjects.isEmpty() ? affectedProjects
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
		// Retain the last complete tree while a resource-triggered reload runs.
		// Scope changes instead clear the cache because the displayed policy changed.
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

	private void resetForScopeChange(final IProject project) {
		Job job = null;
		synchronized (cache) {
			cache.remove(project);
			job = discoveryJobs.remove(project);
		}
		if (job != null) {
			job.cancel();
		}

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
					((StructuredViewer) currentViewer).refresh(project);
				} else {
					currentViewer.refresh();
				}
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

}
