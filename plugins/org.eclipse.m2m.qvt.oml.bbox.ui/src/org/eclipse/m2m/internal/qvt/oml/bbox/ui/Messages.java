package org.eclipse.m2m.internal.qvt.oml.bbox.ui;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

	private static final String BUNDLE_NAME = "org.eclipse.m2m.internal.qvt.oml.bbox.ui.messages"; //$NON-NLS-1$

	public static String BlackboxDiscovery_descriptorNotResolved;
	public static String BlackboxDiscovery_markerMessage;
	public static String BlackboxDiscovery_projectMarkerMessage;

	public static String BlackboxMarkerValidation_jobName;

	public static String BlackboxNavigator_discoveryJobName;
	public static String BlackboxNavigator_failedSuffix;
	public static String BlackboxNavigator_loading;
	public static String BlackboxNavigator_moduleLabel;
	public static String BlackboxNavigator_moduleLabelWithPackages;
	public static String BlackboxNavigator_open;
	public static String BlackboxNavigator_refreshJobName;
	public static String BlackboxNavigator_rootProjectDependencies;
	public static String BlackboxNavigator_rootProjectOnly;
	public static String BlackboxNavigator_rootProjectVisible;
	public static String BlackboxNavigator_scopeMenu;
	public static String BlackboxNavigator_scopeProjectDependencies;
	public static String BlackboxNavigator_scopeProjectOnly;
	public static String BlackboxNavigator_scopeProjectVisible;

	public static String QVTBBoxUIPlugin_unexpectedError;

	static {
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
