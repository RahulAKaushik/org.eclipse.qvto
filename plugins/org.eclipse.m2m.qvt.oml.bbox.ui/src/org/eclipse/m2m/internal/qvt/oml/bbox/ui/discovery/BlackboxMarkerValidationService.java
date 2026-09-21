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

/**
 * Owns replaceable validation jobs per project, independently of tree expansion
 * and display scope. Validation uses All Visible to Project so narrowing the
 * tree cannot clear dependency failures from Problems. The plug-in activator
 * schedules validation and cancels outstanding jobs when it stops.
 */
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
						new ProjectBlackboxDiscoveryService().validate(project, monitor);
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
			// Serialize project marker writes; the delay coalesces build deltas.
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
