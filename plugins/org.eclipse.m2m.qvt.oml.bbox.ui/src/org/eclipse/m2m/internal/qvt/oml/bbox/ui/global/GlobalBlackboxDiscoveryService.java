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

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.impl.EPackageRegistryImpl;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxDescriptorLoader;

/**
 * Coordinates ordered discovery sources on the background thread. Sources are
 * composed, not mutually exclusive: runtime fallback must follow attribution by
 * earlier sources. Only the view publishes the completed result to the UI.
 */
public class GlobalBlackboxDiscoveryService {

	/** Couples a source to its progress allocation without parallel configuration lists. */
	public static final class Step {
		private final BlackboxDiscoverySource source;
		private final int work;

		public Step(BlackboxDiscoverySource source, int work) {
			if (source == null || work <= 0) {
				throw new IllegalArgumentException();
			}
			this.source = source;
			this.work = work;
		}
	}

	private final Step[] steps;

	public GlobalBlackboxDiscoveryService() {
		BlackboxDescriptorLoader loader = new BlackboxDescriptorLoader();
		GlobalBlackboxUnitResolver resolver = new GlobalBlackboxUnitResolver(loader);
		steps = new Step[] { new Step(new WorkspaceBlackboxDiscovery(resolver), 35),
				new Step(new ExtensionBlackboxDiscovery(resolver), 10),
				new Step(new ActiveBundleBlackboxDiscovery(loader, new ActiveBundleDescriptorFilter(),
						new ActiveBundleCandidateSelector()), 45),
				new Step(new RuntimeBlackboxDiscovery(loader), 10) };
	}

	/** Internal composition seam; callers own source order, never global mutable hooks. */
	public GlobalBlackboxDiscoveryService(Step... steps) {
		this.steps = steps.clone();
		for (Step step : this.steps) {
			if (step == null) {
				throw new IllegalArgumentException();
			}
		}
	}

	public GlobalBlackboxDiscoveryResult discover(IProgressMonitor monitor) {
		int work = 0;
		for (Step step : steps) {
			work += step.work;
		}
		SubMonitor progress = SubMonitor.convert(monitor, work);
		progress.checkCanceled();
		GlobalBlackboxDiscoveryResult result = new GlobalBlackboxDiscoveryResult();
		Set<BlackboxDescriptorIdentity> attributedDescriptors = new HashSet<BlackboxDescriptorIdentity>();
		EPackage.Registry packageRegistry = new EPackageRegistryImpl(EPackage.Registry.INSTANCE);
		for (Step step : steps) {
			step.source.discover(result, attributedDescriptors, packageRegistry, progress.split(step.work));
			progress.checkCanceled();
		}
		result.sort();
		return result;
	}
}
