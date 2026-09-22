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

import org.eclipse.m2m.internal.qvt.oml.bbox.ui.Messages;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxTreeNode;

/** UI-thread placeholder used as the global viewer input during initial load. */
final class GlobalBlackboxLoadingNode implements BlackboxTreeNode {

	static final GlobalBlackboxLoadingNode INSTANCE = new GlobalBlackboxLoadingNode();

	private GlobalBlackboxLoadingNode() {
	}

	@Override
	public String getLabel() {
		return Messages.BlackboxNavigator_loading;
	}
}
