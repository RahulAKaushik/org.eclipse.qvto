package org.eclipse.m2m.internal.tests.qvt.oml.ui.blackbox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.impl.EPackageRegistryImpl;
import org.eclipse.m2m.internal.qvt.oml.ast.env.QvtOperationalModuleEnv;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxDescriptorCandidates;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxDescriptorCandidates.Candidate;
import org.eclipse.m2m.internal.qvt.oml.blackbox.BlackboxProvider;
import org.eclipse.m2m.internal.qvt.oml.blackbox.BlackboxUnit;
import org.eclipse.m2m.internal.qvt.oml.blackbox.BlackboxUnitDescriptor;
import org.eclipse.m2m.internal.qvt.oml.blackbox.LoadContext;
import org.eclipse.m2m.internal.qvt.oml.blackbox.ResolutionContext;
import org.eclipse.m2m.internal.qvt.oml.expressions.ImperativeOperation;
import org.eclipse.m2m.internal.qvt.oml.expressions.OperationalTransformation;
import org.eclipse.m2m.internal.qvt.oml.stdlib.CallHandler;
import org.junit.Test;

public class BlackboxDescriptorCandidatesTest {

	@Test
	public void deduplicatesByQualifiedNameAndKeepsFirstResolvedDescriptor() {
		BlackboxDescriptorCandidates candidates = new BlackboxDescriptorCandidates();
		EPackage.Registry firstRegistry = new EPackageRegistryImpl();
		BlackboxUnitDescriptor firstDescriptor = descriptor("example.Library"); //$NON-NLS-1$

		candidates.add("example.Library", firstDescriptor, firstRegistry); //$NON-NLS-1$
		candidates.add("example.Library", descriptor("example.Library"), new EPackageRegistryImpl()); //$NON-NLS-1$ //$NON-NLS-2$
		candidates.add("example.Library", null, new EPackageRegistryImpl()); //$NON-NLS-1$

		assertEquals(1, candidates.values().size());
		Candidate candidate = candidates.values().iterator().next();
		assertSame(firstDescriptor, candidate.getDescriptor());
		assertSame(firstRegistry, candidate.getPackageRegistry());
	}

	@Test
	public void promotesUnresolvedCandidateWithoutChangingInsertionOrder() {
		BlackboxDescriptorCandidates candidates = new BlackboxDescriptorCandidates();
		EPackage.Registry resolvedRegistry = new EPackageRegistryImpl();
		BlackboxUnitDescriptor resolvedDescriptor = descriptor("example.First"); //$NON-NLS-1$

		candidates.add("example.First", null, new EPackageRegistryImpl()); //$NON-NLS-1$
		candidates.add("example.Second", descriptor("example.Second"), new EPackageRegistryImpl()); //$NON-NLS-1$ //$NON-NLS-2$
		candidates.add("example.First", resolvedDescriptor, resolvedRegistry); //$NON-NLS-1$

		assertEquals(2, candidates.values().size());
		Iterator<Candidate> iterator = candidates.values().iterator();
		Candidate first = iterator.next();
		assertEquals("example.First", first.getQualifiedName()); //$NON-NLS-1$
		assertSame(resolvedDescriptor, first.getDescriptor());
		assertSame(resolvedRegistry, first.getPackageRegistry());
		assertEquals("example.Second", iterator.next().getQualifiedName()); //$NON-NLS-1$
	}

	private static BlackboxUnitDescriptor descriptor(String qualifiedName) {
		return new TestDescriptor(qualifiedName);
	}

	private static final BlackboxProvider PROVIDER = new BlackboxProvider() {
		@Override
		public Collection<? extends BlackboxUnitDescriptor> getUnitDescriptors(ResolutionContext resolutionContext) {
			return Collections.emptyList();
		}

		@Override
		public BlackboxUnitDescriptor getUnitDescriptor(String qualifiedName, ResolutionContext resolutionContext) {
			return null;
		}

		@Override
		public void cleanup() {
		}
	};

	private static final class TestDescriptor extends BlackboxUnitDescriptor {

		TestDescriptor(String qualifiedName) {
			super(PROVIDER, qualifiedName);
		}

		@Override
		public BlackboxUnit load(LoadContext context) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Collection<CallHandler> getBlackboxCallHandler(ImperativeOperation operation,
				QvtOperationalModuleEnv env) {
			return Collections.emptyList();
		}

		@Override
		public Collection<CallHandler> getBlackboxCallHandler(OperationalTransformation transformation,
				QvtOperationalModuleEnv env) {
			return Collections.emptyList();
		}
	}
}
