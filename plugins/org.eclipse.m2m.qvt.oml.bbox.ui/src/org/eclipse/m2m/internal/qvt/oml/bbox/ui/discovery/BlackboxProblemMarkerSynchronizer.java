package org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.Messages;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.QVTBBoxUIPlugin;
import org.eclipse.m2m.internal.qvt.oml.project.QVTOProjectPlugin;
import org.eclipse.osgi.util.NLS;

public final class BlackboxProblemMarkerSynchronizer {

	private static final String MARKER_ATTRIBUTE = QVTBBoxUIPlugin.PLUGIN_ID + ".blackboxMarker"; //$NON-NLS-1$

	public static boolean hasProblemMarkers(IProject project) {
		try {
			if (project == null || !project.isAccessible()) {
				return false;
			}
			for (IMarker marker : project.findMarkers(QVTOProjectPlugin.PROBLEM_MARKER, false,
					IResource.DEPTH_ZERO)) {
				if (isBlackboxMarker(marker)
						&& marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO) == IMarker.SEVERITY_ERROR) {
					return true;
				}
			}
		} catch (CoreException e) {
			QVTBBoxUIPlugin.log(e);
		}
		return false;
	}

	public static boolean isProblemMarker(IMarkerDelta markerDelta) {
		return markerDelta != null
				&& markerDelta.isSubtypeOf(QVTOProjectPlugin.PROBLEM_MARKER)
				&& markerDelta.getAttribute(MARKER_ATTRIBUTE, false);
	}

	public void synchronize(IProject project, Iterable<BlackboxDiagnosticInfo> projectDiagnostics,
			Iterable<BlackboxUnitInfo> units) throws CoreException {
		deleteMarkers(project);
		for (BlackboxDiagnosticInfo diagnostic : projectDiagnostics) {
			createMarkers(project, diagnostic);
		}
		for (BlackboxUnitInfo unit : units) {
			for (BlackboxDiagnosticInfo diagnostic : unit.getDiagnostics()) {
				createMarkers(project, unit, diagnostic);
			}
		}
	}

	private static boolean isBlackboxMarker(IMarker marker) throws CoreException {
		return marker.getAttribute(MARKER_ATTRIBUTE, false);
	}

	private void deleteMarkers(IProject project) throws CoreException {
		for (IMarker marker : project.findMarkers(QVTOProjectPlugin.PROBLEM_MARKER, false, IResource.DEPTH_ZERO)) {
			if (isBlackboxMarker(marker)) {
				marker.delete();
			}
		}
	}

	private void createMarkers(IProject project, BlackboxUnitInfo unit, BlackboxDiagnosticInfo diagnostic)
			throws CoreException {
		boolean childMarkerCreated = false;
		for (BlackboxDiagnosticInfo child : diagnostic.getChildren()) {
			if (child.hasErrors()) {
				createMarkers(project, unit, child);
				childMarkerCreated = true;
			}
		}
		if (!childMarkerCreated && diagnostic.isError()) {
			createMarker(project, unit, diagnostic);
		}
	}

	private void createMarkers(IProject project, BlackboxDiagnosticInfo diagnostic) throws CoreException {
		boolean childMarkerCreated = false;
		for (BlackboxDiagnosticInfo child : diagnostic.getChildren()) {
			if (child.hasErrors()) {
				createMarkers(project, child);
				childMarkerCreated = true;
			}
		}
		if (!childMarkerCreated && diagnostic.isError()) {
			createMarker(project, diagnostic);
		}
	}

	private void createMarker(IProject project, BlackboxUnitInfo unit, BlackboxDiagnosticInfo diagnostic)
			throws CoreException {
		IMarker marker = project.createMarker(QVTOProjectPlugin.PROBLEM_MARKER);
		marker.setAttribute(MARKER_ATTRIBUTE, true);
		marker.setAttribute(IMarker.MESSAGE, NLS.bind(Messages.BlackboxDiscovery_markerMessage,
				unit.getQualifiedName(), diagnostic.getMessage()));
		marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
		marker.setAttribute(IMarker.LOCATION,
				unit.getURI() != null ? unit.getURI().toString() : unit.getQualifiedName());
	}

	private void createMarker(IProject project, BlackboxDiagnosticInfo diagnostic) throws CoreException {
		IMarker marker = project.createMarker(QVTOProjectPlugin.PROBLEM_MARKER);
		marker.setAttribute(MARKER_ATTRIBUTE, true);
		marker.setAttribute(IMarker.MESSAGE, NLS.bind(Messages.BlackboxDiscovery_projectMarkerMessage,
				diagnostic.getMessage()));
		marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
		marker.setAttribute(IMarker.LOCATION, project.getFullPath().toString());
	}
}
