package org.eclipse.m2m.internal.tests.qvt.oml.ui.blackbox.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.URI;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxDiagnosticInfo;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxProblemMarkerSynchronizer;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxUnitInfo;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.ProjectBlackboxDiscoveryService;
import org.eclipse.m2m.internal.qvt.oml.project.QVTOProjectPlugin;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class BlackboxProblemMarkerSynchronizerTest {

	private static final String QUALIFIED_NAME = "marker.test.Library"; //$NON-NLS-1$
	private static final URI UNIT_URI = URI.createURI("qvto://blackbox/marker.test.Library"); //$NON-NLS-1$
	private static int projectSequence;

	private final NullProgressMonitor monitor = new NullProgressMonitor();
	private final BlackboxProblemMarkerSynchronizer synchronizer = new BlackboxProblemMarkerSynchronizer();
	private IProject project;

	@Before
	public void setUp() throws Exception {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		project = workspace.getRoot().getProject("BlackboxMarker" + (++projectSequence)); //$NON-NLS-1$
		if (project.exists()) {
			project.delete(true, true, monitor);
		}
		project.create(monitor);
		project.open(monitor);
	}

	@After
	public void tearDown() throws Exception {
		if (project.exists()) {
			project.delete(true, true, monitor);
		}
	}

	@Test
	public void createsMarkersForLeafErrorsOnly() throws Exception {
		BlackboxDiagnosticInfo projectParent = error(this, "project parent"); //$NON-NLS-1$
		projectParent.addChild(error(projectParent, "project leaf")); //$NON-NLS-1$

		BlackboxUnitInfo unit = new BlackboxUnitInfo(this, QUALIFIED_NAME, UNIT_URI);
		BlackboxDiagnosticInfo unitParent = error(unit, "unit parent"); //$NON-NLS-1$
		unitParent.addChild(error(unitParent, "unit leaf")); //$NON-NLS-1$
		unit.addDiagnostic(unitParent);

		synchronizer.synchronize(project, Collections.singletonList(projectParent), Collections.singletonList(unit));

		Map<String, IMarker> markers = markersByMessage();
		assertEquals(2, markers.size());
		assertMarker(markers.get("QVTo blackbox: project leaf"), project.getFullPath().toString()); //$NON-NLS-1$
		assertMarker(markers.get("QVTo blackbox: " + QUALIFIED_NAME + " - unit leaf"), UNIT_URI.toString()); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(markers.containsKey("QVTo blackbox: project parent")); //$NON-NLS-1$
		assertFalse(markers.containsKey("QVTo blackbox: " + QUALIFIED_NAME + " - unit parent")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void replacesOwnedMarkersAndPreservesOtherQvtoMarkers() throws Exception {
		IMarker otherMarker = project.createMarker(QVTOProjectPlugin.PROBLEM_MARKER);
		otherMarker.setAttribute(IMarker.MESSAGE, "unrelated QVTo problem"); //$NON-NLS-1$

		synchronizeProjectError("old failure"); //$NON-NLS-1$
		assertEquals(2, markersByMessage().size());

		synchronizeProjectError("new failure"); //$NON-NLS-1$

		Map<String, IMarker> markers = markersByMessage();
		assertTrue(otherMarker.exists());
		assertEquals(2, markers.size());
		assertTrue(markers.containsKey("unrelated QVTo problem")); //$NON-NLS-1$
		assertTrue(markers.containsKey("QVTo blackbox: new failure")); //$NON-NLS-1$
		assertFalse(markers.containsKey("QVTo blackbox: old failure")); //$NON-NLS-1$
	}

	@Test
	public void removesStaleMarkersWhenDiscoveryHasNoErrors() throws Exception {
		synchronizeProjectError("temporary failure"); //$NON-NLS-1$
		assertTrue(ProjectBlackboxDiscoveryService.hasBlackboxProblemMarkers(project));

		synchronizer.synchronize(project, Collections.<BlackboxDiagnosticInfo>emptyList(),
				Collections.<BlackboxUnitInfo>emptyList());

		assertFalse(ProjectBlackboxDiscoveryService.hasBlackboxProblemMarkers(project));
		assertEquals(0, markersByMessage().size());
	}

	private void synchronizeProjectError(String message) throws Exception {
		synchronizer.synchronize(project, Collections.singletonList(error(this, message)),
				Collections.<BlackboxUnitInfo>emptyList());
	}

	private Map<String, IMarker> markersByMessage() throws Exception {
		Map<String, IMarker> result = new HashMap<String, IMarker>();
		for (IMarker marker : project.findMarkers(QVTOProjectPlugin.PROBLEM_MARKER, false, IResource.DEPTH_ZERO)) {
			result.put(marker.getAttribute(IMarker.MESSAGE, ""), marker); //$NON-NLS-1$
		}
		return result;
	}

	private static BlackboxDiagnosticInfo error(Object parent, String message) {
		return new BlackboxDiagnosticInfo(parent, Diagnostic.ERROR, message);
	}

	private static void assertMarker(IMarker marker, String location) throws Exception {
		assertTrue(marker != null && marker.exists());
		assertEquals(IMarker.SEVERITY_ERROR,
				marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO));
		assertEquals(location, marker.getAttribute(IMarker.LOCATION, "")); //$NON-NLS-1$
	}
}
