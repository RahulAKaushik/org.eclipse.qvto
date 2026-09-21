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

import org.eclipse.emf.common.util.URI;
import org.eclipse.m2m.internal.qvt.oml.blackbox.BlackboxUnitDescriptor;

/**
 * Identifies a registry descriptor by qualified name and URI, independently of
 * its Java object identity. Used to suppress repeated enumeration, including
 * entries already attributed before the runtime fallback phase.
 */
public final class BlackboxDescriptorIdentity {

	private final String qualifiedName;
	private final URI uri;

	public BlackboxDescriptorIdentity(String qualifiedName, URI uri) {
		this.qualifiedName = qualifiedName;
		this.uri = uri;
	}

	public static BlackboxDescriptorIdentity of(BlackboxUnitDescriptor descriptor) {
		return new BlackboxDescriptorIdentity(descriptor.getQualifiedName(), descriptor.getURI());
	}

	@Override
	public int hashCode() {
		int result = qualifiedName != null ? qualifiedName.hashCode() : 0;
		return 31 * result + (uri != null ? uri.hashCode() : 0);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof BlackboxDescriptorIdentity == false) {
			return false;
		}
		BlackboxDescriptorIdentity other = (BlackboxDescriptorIdentity) obj;
		return equals(qualifiedName, other.qualifiedName) && equals(uri, other.uri);
	}

	private static boolean equals(Object left, Object right) {
		return left == null ? right == null : left.equals(right);
	}
}
