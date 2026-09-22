package org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.QVTBBoxUIPlugin;
import org.eclipse.m2m.internal.qvt.oml.project.QVTOProjectPlugin;

public final class BlackboxMarkerValidationService {

	private static final Map<IProject, Job> JOBS = new HashMap<IProject, Job>();

	private BlackboxMarkerValidationService() {
	}

	public static void schedule(final IProject project) {
		if (!isQVTProject(project)) {
			return;
		}

		synchronized (JOBS) {
			Job previousJob = JOBS.get(project);
			if (previousJob != null) {
				previousJob.cancel();
			}

			Job job = new Job("Validate QVTo blackboxes for " + project.getName()) { //$NON-NLS-1$
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						if (monitor.isCanceled() || !isQVTProject(project)) {
							return Status.CANCEL_STATUS;
						}
						new ProjectBlackboxDiscoveryService().discover(project, true);
						return Status.OK_STATUS;
					} catch (RuntimeException e) {
						QVTBBoxUIPlugin.log(e);
						return Status.CANCEL_STATUS;
					} catch (LinkageError e) {
						QVTBBoxUIPlugin.log(e);
						return Status.CANCEL_STATUS;
					} finally {
						synchronized (JOBS) {
							if (JOBS.get(project) == this) {
								JOBS.remove(project);
							}
						}
					}
				}
			};
			job.setRule(project);
			job.setSystem(true);
			JOBS.put(project, job);
			job.schedule(500L);
		}
	}

	public static void cancelAll() {
		synchronized (JOBS) {
			for (Job job : JOBS.values()) {
				job.cancel();
			}
			JOBS.clear();
		}
	}

	private static boolean isQVTProject(IProject project) {
		try {
			return project != null && project.isAccessible() && project.hasNature(QVTOProjectPlugin.NATURE_ID);
		} catch (CoreException e) {
			return false;
		}
	}
}
