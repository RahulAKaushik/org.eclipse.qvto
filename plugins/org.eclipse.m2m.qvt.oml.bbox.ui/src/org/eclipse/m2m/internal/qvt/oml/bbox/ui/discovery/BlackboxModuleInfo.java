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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.m2m.internal.qvt.oml.bbox.ui.Messages;
import org.eclipse.osgi.util.NLS;

/** A loaded module whose parent is its containing {@link BlackboxUnitInfo}. */
public class BlackboxModuleInfo implements BlackboxTreeNode, BlackboxNavigable {

	private final BlackboxUnitInfo parent;
	private final String name;
	private final List<String> packageURIs = new ArrayList<String>();
	private final List<BlackboxOperationInfo> operations = new ArrayList<BlackboxOperationInfo>();

	public BlackboxModuleInfo(BlackboxUnitInfo parent, String name) {
		this.parent = parent;
		this.name = name;
	}

	public BlackboxUnitInfo getParent() {
		return parent;
	}

	public String getName() {
		return name;
	}

	public List<String> getPackageURIs() {
		return Collections.unmodifiableList(packageURIs);
	}

	public void addPackageURI(String packageURI) {
		if (packageURI != null && packageURI.length() > 0 && !packageURIs.contains(packageURI)) {
			packageURIs.add(packageURI);
		}
	}

	public List<BlackboxOperationInfo> getOperations() {
		return Collections.unmodifiableList(operations);
	}

	public void addOperation(BlackboxOperationInfo operation) {
		operations.add(operation);
	}

	@Override
	public List<BlackboxOperationInfo> getChildren() {
		return getOperations();
	}

	@Override
	public String getLabel() {
		return packageURIs.isEmpty()
				? NLS.bind(Messages.BlackboxNavigator_moduleLabel, name, Integer.valueOf(operations.size()))
				: NLS.bind(Messages.BlackboxNavigator_moduleLabelWithPackages,
						new Object[] { name, Integer.valueOf(operations.size()), String.join(", ", packageURIs) }); //$NON-NLS-1$
	}

	@Override
	public BlackboxNodeKind getNodeKind() {
		return BlackboxNodeKind.MODULE;
	}

	@Override
	public BlackboxUnitInfo getNavigationUnit() {
		return parent;
	}
}
