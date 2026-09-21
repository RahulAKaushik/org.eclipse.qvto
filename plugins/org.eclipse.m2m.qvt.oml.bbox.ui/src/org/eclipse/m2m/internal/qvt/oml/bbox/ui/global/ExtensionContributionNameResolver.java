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

/**
 * Mirrors the registry naming rules for unit and legacy library contributions.
 * A missing unit namespace defaults to the contributor; an explicitly empty
 * namespace stays empty. A library alias replaces only the class's simple name.
 */
public final class ExtensionContributionNameResolver {

	public String resolve(String elementName, String contributor, String name, String namespace, String className) {
		if ("unit".equals(elementName)) { //$NON-NLS-1$
			if (name == null) {
				return null;
			}
			String effectiveNamespace = namespace != null ? namespace : contributor;
			return effectiveNamespace == null || effectiveNamespace.length() == 0
					? name : effectiveNamespace + "." + name; //$NON-NLS-1$
		}
		if ("library".equals(elementName)) { //$NON-NLS-1$
			if (className == null) {
				return null;
			}
			if (name == null) {
				return className;
			}
			int separator = className.lastIndexOf('.');
			return separator < 0 ? name : className.substring(0, separator + 1) + name;
		}
		return null;
	}
}
