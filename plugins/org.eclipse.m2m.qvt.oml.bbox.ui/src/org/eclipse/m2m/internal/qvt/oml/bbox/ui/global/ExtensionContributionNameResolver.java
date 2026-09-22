package org.eclipse.m2m.internal.qvt.oml.bbox.ui.global;

public final class ExtensionContributionNameResolver {

	public String resolve(String elementName, String contributor, String name, String namespace, String className) {
		if ("unit".equals(elementName)) { //$NON-NLS-1$
			if (name == null) {
				return null;
			}
			String effectiveNamespace = namespace != null ? namespace : contributor;
			return effectiveNamespace == null || effectiveNamespace.length() == 0
					? name : effectiveNamespace + "." + name; //$NON-NLS-1$
		}
		if ("library".equals(elementName)) { //$NON-NLS-1$
			if (className == null) {
				return null;
			}
			if (name == null) {
				return className;
			}
			int separator = className.lastIndexOf('.');
			return separator < 0 ? name : className.substring(0, separator + 1) + name;
		}
		return null;
	}
}
