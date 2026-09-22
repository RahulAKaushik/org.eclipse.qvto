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
package org.eclipse.m2m.internal.tests.qvt.oml.ui.blackbox;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxDiagnosticInfo;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxModuleInfo;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxNodeKind;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxOperationInfo;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxTreeNode;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxUnitInfo;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.global.GlobalBlackboxContentProvider;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.global.GlobalBlackboxDiscoveryResult;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.global.GlobalBlackboxGroup;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.global.GlobalBlackboxGroupKind;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.navigator.BlackboxNavigatorLabelProvider;
import org.junit.Test;

public class BlackboxTreeModelTest {

	@Test
	public void unitChildrenKeepModulesBeforeDiagnosticsAndTheirParents() {
		BlackboxUnitInfo unit = new BlackboxUnitInfo(null, "library", null); //$NON-NLS-1$
		BlackboxModuleInfo module = new BlackboxModuleInfo(unit, "module"); //$NON-NLS-1$
		BlackboxDiagnosticInfo diagnostic = new BlackboxDiagnosticInfo(unit, Diagnostic.ERROR, "failure"); //$NON-NLS-1$
		unit.addDiagnostic(diagnostic);
		unit.addModule(module);
		assertEquals(Arrays.asList(module, diagnostic), unit.getChildren());
		for (BlackboxTreeNode child : unit.getChildren()) {
			assertSame(unit, child.getParent());
		}
		assertTrue(unit.hasErrors());
		try {
			unit.getChildren().clear();
			fail("Expected read-only children"); //$NON-NLS-1$
		} catch (UnsupportedOperationException expected) {
		}
	}

	@Test
	public void errorsAggregateThroughGroupsWithoutLosingDiagnosticSeverity() {
		GlobalBlackboxDiscoveryResult result = new GlobalBlackboxDiscoveryResult();
		GlobalBlackboxGroup group = result.getWorkspaceProjects();
		BlackboxUnitInfo unit = new BlackboxUnitInfo(group, "library", null); //$NON-NLS-1$
		BlackboxDiagnosticInfo warning = new BlackboxDiagnosticInfo(unit, Diagnostic.WARNING, "warning"); //$NON-NLS-1$
		warning.addChild(new BlackboxDiagnosticInfo(warning, Diagnostic.CANCEL, "canceled")); //$NON-NLS-1$
		unit.addDiagnostic(warning);
		group.addChild(unit);
		assertTrue(result.hasErrors());
		assertEquals(Diagnostic.WARNING, warning.getSeverity());
		assertEquals(BlackboxNodeKind.WARNING, warning.getNodeKind());
		assertEquals(BlackboxNodeKind.ERROR, warning.getChildren().get(0).getNodeKind());
	}

	@Test
	public void sortingPreservesGroupsThenUnitsThenDiagnostics() {
		GlobalBlackboxGroup group = new GlobalBlackboxDiscoveryResult().getWorkspaceProjects();
		BlackboxUnitInfo z = new BlackboxUnitInfo(group, "z", null); //$NON-NLS-1$
		BlackboxUnitInfo a = new BlackboxUnitInfo(group, "a", null); //$NON-NLS-1$
		GlobalBlackboxGroup nested = new GlobalBlackboxGroup(group, GlobalBlackboxGroupKind.PROJECT, "p", null); //$NON-NLS-1$
		BlackboxDiagnosticInfo diagnostic = new BlackboxDiagnosticInfo(group, Diagnostic.ERROR, null);
		group.addChild(z);
		group.addChild(diagnostic);
		group.addChild(nested);
		group.addChild(a);
		group.sortChildren();
		assertEquals(Arrays.asList(nested, a, z, diagnostic), group.getChildren());
	}

	@Test
	public void emptyGroupsRemainExpandableAndPlaceholderHasCorrectParent() {
		GlobalBlackboxGroup group = new GlobalBlackboxDiscoveryResult().getJavaLibraries();
		GlobalBlackboxContentProvider content = new GlobalBlackboxContentProvider();
		assertTrue(group.getChildren().isEmpty());
		assertTrue(content.hasChildren(group));
		Object[] children = content.getChildren(group);
		assertEquals(1, children.length);
		assertSame(group, content.getParent(children[0]));
		assertFalse(content.hasChildren(children[0]));
		assertFalse(((BlackboxTreeNode) children[0]).hasErrors());
	}

	@Test
	public void newNodeUsesGenericTraversalLabelsAndErrorAggregation() {
		BlackboxTreeNode leaf = new BlackboxTreeNode() {
			public String getLabel() {
				return "custom leaf"; //$NON-NLS-1$
			}

			public boolean hasErrors() {
				return true;
			}
		};
		BlackboxTreeNode node = new BlackboxTreeNode() {
			public String getLabel() {
				return "custom branch"; //$NON-NLS-1$
			}

			public List<? extends BlackboxTreeNode> getChildren() {
				return Arrays.asList(leaf);
			}
		};
		GlobalBlackboxContentProvider content = new GlobalBlackboxContentProvider();
		BlackboxNavigatorLabelProvider labels = new BlackboxNavigatorLabelProvider();
		try {
			assertArrayEquals(new Object[] { leaf }, content.getChildren(node));
			assertTrue(content.hasChildren(node));
			assertFalse(content.hasChildren(leaf));
			assertTrue(node.hasErrors());
			assertEquals("custom branch", labels.getText(node)); //$NON-NLS-1$
			assertEquals("custom leaf", labels.getText(leaf)); //$NON-NLS-1$
		} finally {
			labels.dispose();
		}
	}

	@Test
	public void navigationCapabilityPreservesOwningUnitAndOperationName() {
		BlackboxUnitInfo unit = new BlackboxUnitInfo(null, "library", null); //$NON-NLS-1$
		BlackboxModuleInfo module = new BlackboxModuleInfo(unit, "module"); //$NON-NLS-1$
		BlackboxOperationInfo operation = new BlackboxOperationInfo(module, "op", "op() : String"); //$NON-NLS-1$ //$NON-NLS-2$
		assertSame(unit, module.getNavigationUnit());
		assertSame(unit, operation.getNavigationUnit());
		assertNull(unit.getOperationName());
		assertNull(module.getOperationName());
		assertEquals("op", operation.getOperationName()); //$NON-NLS-1$
		assertEquals(operation.getSignature(), operation.getLabel());
		assertFalse(operation.hasChildren());
		assertFalse(operation.hasErrors());
	}
}
