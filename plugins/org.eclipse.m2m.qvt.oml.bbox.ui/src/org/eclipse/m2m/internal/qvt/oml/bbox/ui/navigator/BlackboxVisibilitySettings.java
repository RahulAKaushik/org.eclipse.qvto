package org.eclipse.m2m.internal.qvt.oml.bbox.ui.navigator;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxVisibilityScope;

public final class BlackboxVisibilitySettings {

	public interface Listener {
		void scopeChanged();
	}

	private static final List<Listener> LISTENERS = new CopyOnWriteArrayList<Listener>();
	private static volatile BlackboxVisibilityScope scope = BlackboxVisibilityScope.PROJECT_VISIBLE;

	private BlackboxVisibilitySettings() {
	}

	public static BlackboxVisibilityScope getScope() {
		return scope;
	}

	public static void setScope(BlackboxVisibilityScope newScope) {
		if (newScope == null || newScope == scope) {
			return;
		}
		scope = newScope;
		for (Listener listener : LISTENERS) {
			listener.scopeChanged();
		}
	}

	public static void addListener(Listener listener) {
		LISTENERS.add(listener);
	}

	public static void removeListener(Listener listener) {
		LISTENERS.remove(listener);
	}
}
