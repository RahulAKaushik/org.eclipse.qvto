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

/**
 * Project-tree discovery policies. Both dependency-aware scopes include Java
 * project and library dependencies; only PROJECT_VISIBLE also enumerates
 * registry providers. Preference values are stable independently of enum names.
 */
public enum BlackboxVisibilityScope {

	PROJECT_VISIBLE("projectVisible", true, true), //$NON-NLS-1$
	PROJECT_DEPENDENCIES("projectDependencies", true, false), //$NON-NLS-1$
	PROJECT_ONLY("projectOnly", false, false); //$NON-NLS-1$

	private final String preferenceValue;
	private final boolean includesJavaDependencies;
	private final boolean includesRegistryDescriptors;

	private BlackboxVisibilityScope(String preferenceValue, boolean includesJavaDependencies,
			boolean includesRegistryDescriptors) {
		this.preferenceValue = preferenceValue;
		this.includesJavaDependencies = includesJavaDependencies;
		this.includesRegistryDescriptors = includesRegistryDescriptors;
	}

	public String getPreferenceValue() {
		return preferenceValue;
	}

	/**
	 * Returns null for missing or unknown values so settings can apply the default.
	 */
	public static BlackboxVisibilityScope fromPreferenceValue(String value) {
		for (BlackboxVisibilityScope scope : values()) {
			if (scope.preferenceValue.equals(value)) {
				return scope;
			}
		}
		return null;
	}

	public boolean includesJavaDependencies() {
		return includesJavaDependencies;
	}

	public boolean includesRegistryDescriptors() {
		return includesRegistryDescriptors;
	}
}
