package org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery;

public enum BlackboxVisibilityScope {

	PROJECT_VISIBLE("projectVisible", true, true), //$NON-NLS-1$
	PROJECT_DEPENDENCIES("projectDependencies", true, false), //$NON-NLS-1$
	PROJECT_ONLY("projectOnly", false, false); //$NON-NLS-1$

	private final String preferenceValue;
	private final boolean includesJavaDependencies;
	private final boolean includesRegistryDescriptors;

	private BlackboxVisibilityScope(String preferenceValue, boolean includesJavaDependencies,
			boolean includesRegistryDescriptors) {
		this.preferenceValue = preferenceValue;
		this.includesJavaDependencies = includesJavaDependencies;
		this.includesRegistryDescriptors = includesRegistryDescriptors;
	}

	public String getPreferenceValue() {
		return preferenceValue;
	}

	public static BlackboxVisibilityScope fromPreferenceValue(String value) {
		for (BlackboxVisibilityScope scope : values()) {
			if (scope.preferenceValue.equals(value)) {
				return scope;
			}
		}
		return null;
	}

	public boolean includesJavaDependencies() {
		return includesJavaDependencies;
	}

	public boolean includesRegistryDescriptors() {
		return includesRegistryDescriptors;
	}
}
