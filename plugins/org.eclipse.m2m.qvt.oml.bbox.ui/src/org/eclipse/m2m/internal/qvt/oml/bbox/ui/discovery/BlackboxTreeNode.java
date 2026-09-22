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
package org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery;

import java.util.Collections;
import java.util.List;

/**
 * Read-only traversal of the model built by discovery jobs. Implementations may
 * populate their own collections before publication; viewers must not mutate
 * them. Presentation metadata is text and semantic kind only, never SWT images.
 * Eclipse resources and invisible result roots are adapted by content providers.
 */
public interface BlackboxTreeNode {

	default BlackboxTreeNode getParent() {
		return null;
	}

	default List<? extends BlackboxTreeNode> getChildren() {
		return Collections.emptyList();
	}

	default boolean hasChildren() {
		return !getChildren().isEmpty();
	}

	default boolean hasErrors() {
		for (BlackboxTreeNode child : getChildren()) {
			if (child.hasErrors()) {
				return true;
			}
		}
		return false;
	}

	String getLabel();

	default BlackboxNodeKind getNodeKind() {
		return BlackboxNodeKind.INFO;
	}

	default String getSortKey() {
		return getLabel() != null ? getLabel() : ""; //$NON-NLS-1$
	}
}
