/*******************************************************************************
 * Copyright (c) 2007, 2018 Borland Software Corporation and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors:
 *     Borland Software Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2m.internal.qvt.oml.bbox.ui;

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxMarkerValidationService;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxProjectDependencies;
import org.eclipse.m2m.internal.qvt.oml.emf.util.urimap.MetamodelURIMappingHelper;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class QVTBBoxUIPlugin extends Plugin {

	// The plug-in NATURE_ID
	public static final String PLUGIN_ID = "org.eclipse.m2m.qvt.oml.bbox.ui"; //$NON-NLS-1$

	// The shared instance
	private static QVTBBoxUIPlugin plugin;
	private IResourceChangeListener resourceChangeListener;
	
	/**
	 * The constructor
	 */
	public QVTBBoxUIPlugin() {
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.runtime.Plugins#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		resourceChangeListener = new IResourceChangeListener() {
			public void resourceChanged(IResourceChangeEvent event) {
				scheduleAffectedProjects(event);
			}
		};
		ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceChangeListener, IResourceChangeEvent.POST_CHANGE);
		scheduleAllProjects();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.runtime.Plugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		if (resourceChangeListener != null) {
			ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceChangeListener);
			resourceChangeListener = null;
		}
		BlackboxMarkerValidationService.cancelAll();
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static QVTBBoxUIPlugin getDefault() {
		return plugin;
	}
	
    public static void log(Throwable e) {
        log(new Status(IStatus.ERROR, PLUGIN_ID, 0, Messages.QVTBBoxUIPlugin_unexpectedError, e));
    }

	public static void log(IStatus status) {
        getDefault().getLog().log(status);
    }
	
	public static IStatus createStatus(int severity, String message, Throwable throwable) {
		return new Status(severity, PLUGIN_ID, message != null ? message : "", throwable); //$NON-NLS-1$
	}
	
	public static IStatus createStatus(int severity, String message, int code) {
		return new Status(severity, PLUGIN_ID, code, message != null ? message : "", null); //$NON-NLS-1$
	}
	
	public static IStatus createStatus(int severity, String message) {
		return createStatus(severity, message, null);
	}	

	private void scheduleAllProjects() {
		for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
			BlackboxMarkerValidationService.schedule(project);
		}
	}

	private void scheduleAffectedProjects(IResourceChangeEvent event) {
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
			log(e);
		}

		for (IProject project : BlackboxProjectDependencies.includeDependentQVTProjects(affectedProjects)) {
			BlackboxMarkerValidationService.schedule(project);
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
