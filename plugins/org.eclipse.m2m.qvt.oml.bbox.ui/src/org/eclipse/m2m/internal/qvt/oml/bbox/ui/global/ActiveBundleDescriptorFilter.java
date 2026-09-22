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
 *     Christopher Gerking - bundle discovery changes
 *******************************************************************************/
package org.eclipse.m2m.internal.qvt.oml.bbox.ui.global;

import org.eclipse.emf.common.util.URI;
import org.eclipse.m2m.internal.qvt.oml.blackbox.BlackboxUnitDescriptor;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public final class ActiveBundleDescriptorFilter {

	private static final String OSGI_QUERY_PREFIX = "osgi="; //$NON-NLS-1$

	/**
	 * Accepts only descriptors attributed to this bundle whose Java class is
	 * physically defined by the same bundle.
	 */
	public boolean accepts(BlackboxUnitDescriptor descriptor, Bundle bundle) {
		return descriptor != null && bundle != null && bundle.getSymbolicName() != null
				&& matchesBundleQuery(descriptor.getURI(), bundle.getSymbolicName())
				&& isDefinedByBundle(descriptor.getQualifiedName(), bundle);
	}

	public boolean matchesBundleQuery(URI descriptorURI, String bundleId) {
		return descriptorURI != null && bundleId != null
				&& (OSGI_QUERY_PREFIX + bundleId).equals(descriptorURI.query());
	}

	public boolean isDefinedByBundle(String qualifiedName, Bundle bundle) {
		if (qualifiedName == null || bundle == null) {
			return false;
		}
		try {
			Class<?> moduleClass = bundle.loadClass(qualifiedName);
			return bundle.equals(FrameworkUtil.getBundle(moduleClass));
		} catch (ClassNotFoundException e) {
			return false;
		} catch (RuntimeException e) {
			return false;
		} catch (LinkageError e) {
			return false;
		}
	}
}
