package org.eclipse.m2m.internal.qvt.oml.bbox.ui.global;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Root model for one workspace-wide discovery run. The fixed top-level groups
 * own all nested groups and unit models.
 * <p>
 * The model is built and sorted off the UI thread, then published as one
 * complete viewer input on the UI thread.
 */
public class GlobalBlackboxDiscoveryResult {

	private final GlobalBlackboxGroup workspaceProjects = new GlobalBlackboxGroup(this,
			GlobalBlackboxGroupKind.WORKSPACE_PROJECTS, "workspace", null); //$NON-NLS-1$
	private final GlobalBlackboxGroup javaLibraries = new GlobalBlackboxGroup(this,
			GlobalBlackboxGroupKind.JAVA_LIBRARIES, "libraries", null); //$NON-NLS-1$
	private final GlobalBlackboxGroup eclipsePlatform = new GlobalBlackboxGroup(this,
			GlobalBlackboxGroupKind.ECLIPSE_PLATFORM, "platform", null); //$NON-NLS-1$
	private final GlobalBlackboxGroup extensionContributions = new GlobalBlackboxGroup(eclipsePlatform,
			GlobalBlackboxGroupKind.EXTENSION_CONTRIBUTIONS, "extensions", null); //$NON-NLS-1$
	private final GlobalBlackboxGroup activePlugins = new GlobalBlackboxGroup(eclipsePlatform,
			GlobalBlackboxGroupKind.ACTIVE_PLUGINS, "activePlugins", null); //$NON-NLS-1$
	private final GlobalBlackboxGroup runtimeRegistrations = new GlobalBlackboxGroup(this,
			GlobalBlackboxGroupKind.RUNTIME_REGISTRATIONS, "runtime", null); //$NON-NLS-1$
	private final List<GlobalBlackboxGroup> groups;

	public GlobalBlackboxDiscoveryResult() {
		eclipsePlatform.addChild(extensionContributions);
		eclipsePlatform.addChild(activePlugins);
		groups = Collections.unmodifiableList(Arrays.asList(workspaceProjects, javaLibraries, eclipsePlatform,
				runtimeRegistrations));
	}

	public List<GlobalBlackboxGroup> getGroups() {
		return groups;
	}

	public GlobalBlackboxGroup getWorkspaceProjects() {
		return workspaceProjects;
	}

	public GlobalBlackboxGroup getJavaLibraries() {
		return javaLibraries;
	}

	public GlobalBlackboxGroup getExtensionContributions() {
		return extensionContributions;
	}

	public GlobalBlackboxGroup getActivePlugins() {
		return activePlugins;
	}

	public GlobalBlackboxGroup getRuntimeRegistrations() {
		return runtimeRegistrations;
	}

	public void sort() {
		for (GlobalBlackboxGroup group : groups) {
			group.sortChildren();
		}
	}
}
