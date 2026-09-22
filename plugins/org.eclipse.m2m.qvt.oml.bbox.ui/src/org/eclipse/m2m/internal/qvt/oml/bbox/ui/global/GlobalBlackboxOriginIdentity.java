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
 * Deduplicates a qualified unit name within one project, library, or contributor
 * origin while preserving the same name from other origins. Separate fields
 * avoid collisions that delimiter-concatenated string keys could introduce.
 */
public final class GlobalBlackboxOriginIdentity {

	private final String originKey;
	private final String qualifiedName;

	public GlobalBlackboxOriginIdentity(String originKey, String qualifiedName) {
		this.originKey = originKey;
		this.qualifiedName = qualifiedName;
	}

	@Override
	public int hashCode() {
		int result = originKey != null ? originKey.hashCode() : 0;
		return 31 * result + (qualifiedName != null ? qualifiedName.hashCode() : 0);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof GlobalBlackboxOriginIdentity == false) {
			return false;
		}
		GlobalBlackboxOriginIdentity other = (GlobalBlackboxOriginIdentity) obj;
		return equals(originKey, other.originKey) && equals(qualifiedName, other.qualifiedName);
	}

	private static boolean equals(Object left, Object right) {
		return left == null ? right == null : left.equals(right);
	}
}
