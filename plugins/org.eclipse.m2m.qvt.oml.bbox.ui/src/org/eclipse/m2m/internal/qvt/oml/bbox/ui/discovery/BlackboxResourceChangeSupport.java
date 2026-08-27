package org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery;

import org.eclipse.core.resources.IResource;
import org.eclipse.m2m.internal.qvt.oml.emf.util.urimap.MetamodelURIMappingHelper;

/**
 * Defines which workspace resource changes can affect blackbox discovery.
 */
public final class BlackboxResourceChangeSupport {

	private BlackboxResourceChangeSupport() {
	}

	public static boolean isRelevant(IResource resource) {
		if (resource == null || resource.getType() != IResource.FILE) {
			return false;
		}

		String name = resource.getName();
		String extension = resource.getFileExtension();
		return "qvto".equals(extension) //$NON-NLS-1$
				|| "java".equals(extension) //$NON-NLS-1$
				|| "class".equals(extension) //$NON-NLS-1$
				|| "jar".equals(extension) //$NON-NLS-1$
				|| "plugin.xml".equals(name) //$NON-NLS-1$
				|| "MANIFEST.MF".equals(name) //$NON-NLS-1$
				|| ".classpath".equals(name) //$NON-NLS-1$
				|| isMetamodelFileName(name)
				|| MetamodelURIMappingHelper.getMappingFileHandle(resource.getProject()).equals(resource);
	}

	private static boolean isMetamodelFileName(String fileName) {
		return fileName.endsWith(".ecore") //$NON-NLS-1$
				|| fileName.endsWith(".xcore") //$NON-NLS-1$
				|| fileName.endsWith(".emof") //$NON-NLS-1$
				|| fileName.endsWith(".oclinecore"); //$NON-NLS-1$
	}
}
