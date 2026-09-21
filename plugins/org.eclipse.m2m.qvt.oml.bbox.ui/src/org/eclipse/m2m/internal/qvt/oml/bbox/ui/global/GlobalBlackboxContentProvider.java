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

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.Messages;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxTreeNode;

/** Adapts typed discovery nodes to JFace, including empty-group placeholders. */
public class GlobalBlackboxContentProvider implements ITreeContentProvider {

	public Object[] getElements(Object inputElement) {
		return inputElement instanceof GlobalBlackboxLoadingNode ? new Object[] { inputElement }
				: getChildren(inputElement);
	}

	public Object[] getChildren(Object parentElement) {
		if (!(parentElement instanceof BlackboxTreeNode)) {
			return new Object[0];
		}
		BlackboxTreeNode node = (BlackboxTreeNode) parentElement;
		if (node instanceof GlobalBlackboxGroup && node.getChildren().isEmpty()) {
			return new Object[] { new GlobalBlackboxStatusNode(node, Messages.GlobalBlackboxView_empty) };
		}
		return node.getChildren().toArray();
	}

	public Object getParent(Object element) {
		return element instanceof BlackboxTreeNode ? ((BlackboxTreeNode) element).getParent() : null;
	}

	public boolean hasChildren(Object element) {
		return element instanceof BlackboxTreeNode && ((BlackboxTreeNode) element).hasChildren();
	}

	public void dispose() {
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}
}
