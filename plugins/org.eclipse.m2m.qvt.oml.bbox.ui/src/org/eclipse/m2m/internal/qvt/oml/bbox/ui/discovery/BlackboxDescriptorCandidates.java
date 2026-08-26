package org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.m2m.internal.qvt.oml.blackbox.BlackboxUnitDescriptor;

/**
 * Ordered project-discovery candidates keyed by blackbox qualified name.
 */
public final class BlackboxDescriptorCandidates {

	private final Map<String, Candidate> candidates = new LinkedHashMap<String, Candidate>();

	public void add(String qualifiedName, BlackboxUnitDescriptor descriptor, EPackage.Registry packageRegistry) {
		Candidate existing = candidates.get(qualifiedName);
		if (existing == null || (existing.getDescriptor() == null && descriptor != null)) {
			candidates.put(qualifiedName, new Candidate(qualifiedName, descriptor, packageRegistry));
		}
	}

	public Collection<Candidate> values() {
		return Collections.unmodifiableCollection(candidates.values());
	}

	public static final class Candidate {

		private final String qualifiedName;
		private final BlackboxUnitDescriptor descriptor;
		private final EPackage.Registry packageRegistry;

		private Candidate(String qualifiedName, BlackboxUnitDescriptor descriptor,
				EPackage.Registry packageRegistry) {
			this.qualifiedName = qualifiedName;
			this.descriptor = descriptor;
			this.packageRegistry = packageRegistry;
		}

		public String getQualifiedName() {
			return qualifiedName;
		}

		public BlackboxUnitDescriptor getDescriptor() {
			return descriptor;
		}

		public EPackage.Registry getPackageRegistry() {
			return packageRegistry;
		}
	}
}
