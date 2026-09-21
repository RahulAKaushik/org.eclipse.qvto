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
package org.eclipse.m2m.internal.qvt.oml.bbox.ui.global;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxTreeNode;

/**
 * Root model for one workspace-wide discovery run. The fixed top-level groups
 * own all nested groups and unit models.
 * <p>
 * The model is built and sorted off the UI thread, then published as one
 * complete viewer input on the UI thread.
 */
public class GlobalBlackboxDiscoveryResult implements BlackboxTreeNode {

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
		groups = Collections.unmodifiableList(
				Arrays.asList(workspaceProjects, javaLibraries, eclipsePlatform, runtimeRegistrations));
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

	@Override
	public List<GlobalBlackboxGroup> getChildren() {
		return getGroups();
	}

	@Override
	public String getLabel() {
		return ""; //$NON-NLS-1$
	}
}
