package org.eclipse.m2m.internal.qvt.oml.bbox.ui;

import java.net.URL;

import org.eclipse.m2m.internal.qvt.oml.common.ui.PluginImages;

public class QVTBBoxPluginImages extends PluginImages {

	public static final String QVTO_BLACKBOX = "qvto-blackbox"; //$NON-NLS-1$

	public static QVTBBoxPluginImages getInstance() {
		return INSTANCE;
	}

	private QVTBBoxPluginImages() {
		super(BASE_URL);
	}

	@Override
	protected void declareImages() {
		declareRegistryImage(QVTO_BLACKBOX, "icons/qvto-blackbox.png"); //$NON-NLS-1$
	}

	private static final URL BASE_URL = QVTBBoxUIPlugin.getDefault().getBundle().getEntry("/"); //$NON-NLS-1$

	private static final QVTBBoxPluginImages INSTANCE = new QVTBBoxPluginImages();
}
