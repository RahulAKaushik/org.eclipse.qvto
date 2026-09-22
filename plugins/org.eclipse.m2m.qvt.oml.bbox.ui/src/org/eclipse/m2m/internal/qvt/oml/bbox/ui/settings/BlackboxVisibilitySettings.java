package org.eclipse.m2m.internal.qvt.oml.bbox.ui.settings;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.Messages;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.QVTBBoxUIPlugin;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxVisibilityScope;
import org.eclipse.osgi.util.NLS;
import org.osgi.service.prefs.BackingStoreException;

public final class BlackboxVisibilitySettings {

	public interface Listener {
		void scopeChanged(IProject project);
	}

	public static final BlackboxVisibilityScope DEFAULT_SCOPE = BlackboxVisibilityScope.PROJECT_VISIBLE;

	private static final String PREFERENCE_KEY = "blackboxVisibilityScope"; //$NON-NLS-1$
	private static final List<Listener> LISTENERS = new CopyOnWriteArrayList<Listener>();

	private BlackboxVisibilitySettings() {
	}

	public static BlackboxVisibilityScope getScope(IProject project) {
		String value = preferences(project).get(PREFERENCE_KEY, null);
		BlackboxVisibilityScope scope = BlackboxVisibilityScope.fromPreferenceValue(value);
		return scope != null ? scope : DEFAULT_SCOPE;
	}

	public static void setScope(IProject project, BlackboxVisibilityScope newScope) throws CoreException {
		if (newScope == null) {
			throw new IllegalArgumentException("Scope must not be null"); //$NON-NLS-1$
		}

		BlackboxVisibilityScope oldScope = getScope(project);
		IEclipsePreferences preferences = preferences(project);
		if (newScope == DEFAULT_SCOPE) {
			preferences.remove(PREFERENCE_KEY);
		} else {
			preferences.put(PREFERENCE_KEY, newScope.getPreferenceValue());
		}
		try {
			preferences.flush();
		} catch (BackingStoreException e) {
			String message = NLS.bind(Messages.BlackboxSettings_saveError, project.getName());
			throw new CoreException(QVTBBoxUIPlugin.createStatus(IStatus.ERROR, message, e));
		}

		if (oldScope != newScope) {
			for (Listener listener : LISTENERS) {
				listener.scopeChanged(project);
			}
		}
	}

	public static void addListener(Listener listener) {
		LISTENERS.add(listener);
	}

	public static void removeListener(Listener listener) {
		LISTENERS.remove(listener);
	}

	private static IEclipsePreferences preferences(IProject project) {
		if (project == null) {
			throw new IllegalArgumentException("Project must not be null"); //$NON-NLS-1$
		}
		return new ProjectScope(project).getNode(QVTBBoxUIPlugin.PLUGIN_ID);
	}
}
