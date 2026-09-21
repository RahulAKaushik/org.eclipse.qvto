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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxResourceChangeSupport;
import org.junit.Test;

public class BlackboxResourceChangeSupportTest {

	@Test
	public void recognizesOnlyDiscoveryInputs() {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("BlackboxResourcePolicy"); //$NON-NLS-1$

		assertTrue(BlackboxResourceChangeSupport.isRelevant(project.getFile("src/Library.java"))); //$NON-NLS-1$
		assertTrue(BlackboxResourceChangeSupport.isRelevant(project.getFile("transforms/Copy.qvto"))); //$NON-NLS-1$
		assertTrue(BlackboxResourceChangeSupport.isRelevant(project.getFile("lib/library.jar"))); //$NON-NLS-1$
		assertTrue(BlackboxResourceChangeSupport.isRelevant(project.getFile("META-INF/MANIFEST.MF"))); //$NON-NLS-1$
		assertTrue(BlackboxResourceChangeSupport.isRelevant(project.getFile("model.ecore"))); //$NON-NLS-1$
		assertFalse(BlackboxResourceChangeSupport.isRelevant(project.getFile("README.txt"))); //$NON-NLS-1$
		assertFalse(BlackboxResourceChangeSupport.isRelevant(project.getFolder("src"))); //$NON-NLS-1$
	}
}
