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

import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EPackage;

/**
 * One source in an ordered global discovery run. Implementations append their
 * origins and record attributed descriptors for the final runtime fallback.
 * Calls are sequential on the discovery thread; cancellation must propagate.
 * Workspace discovery uses project metamodel registries rather than the supplied
 * global registry. No source writes project markers or publishes to a viewer.
 */
public interface BlackboxDiscoverySource {
	void discover(GlobalBlackboxDiscoveryResult result, Set<BlackboxDescriptorIdentity> attributedDescriptors,
			EPackage.Registry packageRegistry, IProgressMonitor monitor);
}
