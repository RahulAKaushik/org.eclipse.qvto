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
package org.eclipse.m2m.internal.tests.qvt.oml.ui.blackbox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.emf.common.util.URI;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.global.BlackboxDescriptorIdentity;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.global.GlobalBlackboxOriginIdentity;
import org.junit.Test;

public class GlobalBlackboxIdentityTest {

	@Test
	public void deduplicatesSameUnitWithinOneOrigin() {
		Set<GlobalBlackboxOriginIdentity> identities = new HashSet<GlobalBlackboxOriginIdentity>();
		identities.add(new GlobalBlackboxOriginIdentity("project", "example.Library")); //$NON-NLS-1$ //$NON-NLS-2$
		identities.add(new GlobalBlackboxOriginIdentity("project", "example.Library")); //$NON-NLS-1$ //$NON-NLS-2$

		assertEquals(1, identities.size());
	}

	@Test
	public void preservesSameQualifiedNameAcrossOrigins() {
		Set<GlobalBlackboxOriginIdentity> identities = new HashSet<GlobalBlackboxOriginIdentity>();
		identities.add(new GlobalBlackboxOriginIdentity("project.one", "example.Library")); //$NON-NLS-1$ //$NON-NLS-2$
		identities.add(new GlobalBlackboxOriginIdentity("project.two", "example.Library")); //$NON-NLS-1$ //$NON-NLS-2$

		assertEquals(2, identities.size());
	}

	@Test
	public void fieldsCannotCollideThroughStringDelimiters() {
		GlobalBlackboxOriginIdentity left = new GlobalBlackboxOriginIdentity("a|b", "c"); //$NON-NLS-1$ //$NON-NLS-2$
		GlobalBlackboxOriginIdentity right = new GlobalBlackboxOriginIdentity("a", "b|c"); //$NON-NLS-1$ //$NON-NLS-2$

		assertFalse(left.equals(right));
	}

	@Test
	public void descriptorIdentityIncludesQualifiedNameAndUri() {
		URI firstURI = URI.createURI("qvto://blackbox/example.Library?osgi=bundle.one"); //$NON-NLS-1$
		URI secondURI = URI.createURI("qvto://blackbox/example.Library?osgi=bundle.two"); //$NON-NLS-1$
		Set<BlackboxDescriptorIdentity> identities = new HashSet<BlackboxDescriptorIdentity>();
		identities.add(new BlackboxDescriptorIdentity("example.Library", firstURI)); //$NON-NLS-1$
		identities.add(new BlackboxDescriptorIdentity("example.Library", firstURI)); //$NON-NLS-1$
		identities.add(new BlackboxDescriptorIdentity("example.Library", secondURI)); //$NON-NLS-1$
		identities.add(new BlackboxDescriptorIdentity("example.Alias", firstURI)); //$NON-NLS-1$

		assertEquals(3, identities.size());
	}
}
