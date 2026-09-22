package org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery;

public enum BlackboxVisibilityScope {

	PROJECT_VISIBLE(true, true),
	PROJECT_DEPENDENCIES(true, false),
	PROJECT_ONLY(false, false);

	private final boolean includesJavaDependencies;
	private final boolean includesRegistryDescriptors;

	private BlackboxVisibilityScope(boolean includesJavaDependencies, boolean includesRegistryDescriptors) {
		this.includesJavaDependencies = includesJavaDependencies;
		this.includesRegistryDescriptors = includesRegistryDescriptors;
	}

	public boolean includesJavaDependencies() {
		return includesJavaDependencies;
	}

	public boolean includesRegistryDescriptors() {
		return includesRegistryDescriptors;
	}
}
