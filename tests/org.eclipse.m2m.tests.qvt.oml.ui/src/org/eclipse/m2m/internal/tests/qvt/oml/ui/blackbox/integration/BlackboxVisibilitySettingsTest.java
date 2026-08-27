package org.eclipse.m2m.internal.tests.qvt.oml.ui.blackbox.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.QVTBBoxUIPlugin;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxVisibilityScope;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.settings.BlackboxVisibilitySettings;
import org.junit.After;
import org.junit.Test;

public class BlackboxVisibilitySettingsTest {

	private static final String PREFERENCE_KEY = "blackboxVisibilityScope"; //$NON-NLS-1$

	private final List<IProject> projects = new ArrayList<IProject>();

	@After
	public void deleteProjects() throws CoreException {
		for (IProject project : projects) {
			if (project.exists()) {
				project.delete(true, true, null);
			}
		}
	}

	@Test
	public void storesScopesIndependentlyPerProject() throws Exception {
		IProject first = createProject("First"); //$NON-NLS-1$
		IProject second = createProject("Second"); //$NON-NLS-1$

		assertEquals(BlackboxVisibilityScope.PROJECT_VISIBLE, BlackboxVisibilitySettings.getScope(first));
		assertEquals(BlackboxVisibilityScope.PROJECT_VISIBLE, BlackboxVisibilitySettings.getScope(second));

		BlackboxVisibilitySettings.setScope(first, BlackboxVisibilityScope.PROJECT_ONLY);
		BlackboxVisibilitySettings.setScope(second, BlackboxVisibilityScope.PROJECT_DEPENDENCIES);

		assertEquals(BlackboxVisibilityScope.PROJECT_ONLY, BlackboxVisibilitySettings.getScope(first));
		assertEquals(BlackboxVisibilityScope.PROJECT_DEPENDENCIES,
				BlackboxVisibilitySettings.getScope(second));
	}

	@Test
	public void invalidPersistedValueFallsBackToProjectVisible() throws Exception {
		IProject project = createProject("Invalid"); //$NON-NLS-1$
		IEclipsePreferences preferences = new ProjectScope(project).getNode(QVTBBoxUIPlugin.PLUGIN_ID);
		preferences.put(PREFERENCE_KEY, "unknown"); //$NON-NLS-1$
		preferences.flush();

		assertEquals(BlackboxVisibilityScope.PROJECT_VISIBLE, BlackboxVisibilitySettings.getScope(project));
	}

	@Test
	public void selectingDefaultRemovesExplicitPreference() throws Exception {
		IProject project = createProject("Default"); //$NON-NLS-1$
		IEclipsePreferences preferences = new ProjectScope(project).getNode(QVTBBoxUIPlugin.PLUGIN_ID);

		BlackboxVisibilitySettings.setScope(project, BlackboxVisibilityScope.PROJECT_ONLY);
		assertEquals(BlackboxVisibilityScope.PROJECT_ONLY.getPreferenceValue(),
				preferences.get(PREFERENCE_KEY, null));

		BlackboxVisibilitySettings.setScope(project, BlackboxVisibilityScope.PROJECT_VISIBLE);

		assertNull(preferences.get(PREFERENCE_KEY, null));
	}

	@Test
	public void notifiesOnlyWhenAProjectsEffectiveScopeChanges() throws Exception {
		final IProject first = createProject("ListenerFirst"); //$NON-NLS-1$
		final IProject second = createProject("ListenerSecond"); //$NON-NLS-1$
		final List<IProject> changedProjects = new ArrayList<IProject>();
		BlackboxVisibilitySettings.Listener listener = new BlackboxVisibilitySettings.Listener() {
			public void scopeChanged(IProject project) {
				changedProjects.add(project);
			}
		};
		BlackboxVisibilitySettings.addListener(listener);
		try {
			BlackboxVisibilitySettings.setScope(first, BlackboxVisibilityScope.PROJECT_ONLY);
			BlackboxVisibilitySettings.setScope(first, BlackboxVisibilityScope.PROJECT_ONLY);
			BlackboxVisibilitySettings.setScope(second, BlackboxVisibilityScope.PROJECT_DEPENDENCIES);
		} finally {
			BlackboxVisibilitySettings.removeListener(listener);
		}

		assertEquals(Arrays.asList(first, second), changedProjects);
	}

	private IProject createProject(String suffix) throws CoreException {
		String name = "BlackboxVisibilitySettingsTest_" + suffix + "_" + System.nanoTime(); //$NON-NLS-1$ //$NON-NLS-2$
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
		project.create(null);
		project.open(null);
		projects.add(project);
		return project;
	}
}
