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
package org.eclipse.m2m.internal.tests.qvt.oml.ui.blackbox.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxDiagnosticInfo;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxDiscoveryResult;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxTreeNode;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxUnitInfo;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.navigator.BlackboxNavigatorContentProvider;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.navigator.BlackboxRootNode;
import org.junit.Test;

public class BlackboxTreeIntegrationTest {
	@Test
	public void projectResultChildrenMapToSyntheticRootAndNewNodesNeedNoDispatch() throws Exception {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("BlackboxTree_" + System.nanoTime()); //$NON-NLS-1$
		NullProgressMonitor monitor = new NullProgressMonitor();
		project.create(monitor);
		project.open(monitor);
		BlackboxNavigatorContentProvider content = new BlackboxNavigatorContentProvider();
		try {
			BlackboxDiscoveryResult result = new BlackboxDiscoveryResult(project);
			BlackboxUnitInfo unit = new BlackboxUnitInfo(result, "library", null); //$NON-NLS-1$
			BlackboxDiagnosticInfo diagnostic = new BlackboxDiagnosticInfo(result, Diagnostic.ERROR, "error"); //$NON-NLS-1$
			Object root = content.getParent(unit);
			assertTrue(root instanceof BlackboxRootNode);
			assertEquals(root, content.getParent(diagnostic));
			assertSame(project, content.getParent(root));
			BlackboxTreeNode child = new BlackboxTreeNode() {
				public BlackboxTreeNode getParent() {
					return unit;
				}

				public String getLabel() {
					return "new node"; //$NON-NLS-1$
				}
			};
			assertSame(unit, content.getParent(child));
			assertFalse(content.hasChildren(child));
			assertEquals(0, content.getChildren(child).length);
		} finally {
			content.dispose();
			project.delete(true, true, monitor);
		}
	}
}
