package org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.Messages;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.QVTBBoxUIPlugin;
import org.eclipse.m2m.internal.qvt.oml.project.QvtProjectUtil;
import org.eclipse.osgi.util.NLS;

public final class BlackboxMarkerValidationService {

	private static final Map<IProject, Job> JOBS = new HashMap<IProject, Job>();

	private BlackboxMarkerValidationService() {
	}

	public static void schedule(final IProject project) {
		if (project == null || !QvtProjectUtil.isQvtProject(project)) {
			return;
		}

		synchronized (JOBS) {
			Job previousJob = JOBS.get(project);
			if (previousJob != null) {
				previousJob.cancel();
			}

			Job job = new Job(NLS.bind(Messages.BlackboxMarkerValidation_jobName, project.getName())) {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						if (monitor.isCanceled() || !QvtProjectUtil.isQvtProject(project)) {
							return Status.CANCEL_STATUS;
						}
						new ProjectBlackboxDiscoveryService().discover(project, true, monitor);
						return Status.OK_STATUS;
					} catch (OperationCanceledException e) {
						return Status.CANCEL_STATUS;
					} catch (RuntimeException e) {
						return validationFailed(project, e);
					} catch (LinkageError e) {
						return validationFailed(project, e);
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

	private static IStatus validationFailed(IProject project, Throwable throwable) {
		String message = NLS.bind(Messages.BlackboxMarkerValidation_failed, project.getName());
		IStatus status = QVTBBoxUIPlugin.createStatus(IStatus.ERROR, message, throwable);
		QVTBBoxUIPlugin.log(status);
		return status;
	}

	public static void cancelAll() {
		synchronized (JOBS) {
			for (Job job : JOBS.values()) {
				job.cancel();
			}
			JOBS.clear();
		}
	}

}
