package org.eclipse.m2m.internal.qvt.oml.bbox.ui.global;

import org.eclipse.emf.common.util.URI;
import org.eclipse.m2m.internal.qvt.oml.blackbox.BlackboxUnitDescriptor;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public final class ActiveBundleDescriptorFilter {

	private static final String OSGI_QUERY_PREFIX = "osgi="; //$NON-NLS-1$

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
