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
package org.eclipse.m2m.internal.qvt.oml.bbox.ui.navigator;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionConstants;
import org.eclipse.ui.navigator.ICommonMenuConstants;

public class BlackboxNavigatorActionProvider extends CommonActionProvider {

	private final BlackboxOpenAction openAction = new BlackboxOpenAction();

	@Override
	public void fillContextMenu(IMenuManager menu) {
		IStructuredSelection selection = getSelection();
		openAction.selectionChanged(selection);
		if (openAction.isEnabled()) {
			menu.appendToGroup(ICommonMenuConstants.GROUP_OPEN, openAction);
		}
	}

	@Override
	public void fillActionBars(IActionBars actionBars) {
		openAction.selectionChanged(getSelection());
		if (openAction.isEnabled()) {
			actionBars.setGlobalActionHandler(ICommonActionConstants.OPEN, openAction);
		}
	}

	private IStructuredSelection getSelection() {
		if (getContext() == null) {
			return null;
		}
		ISelection selection = getContext().getSelection();
		return selection instanceof IStructuredSelection ? (IStructuredSelection) selection : null;
	}
}
