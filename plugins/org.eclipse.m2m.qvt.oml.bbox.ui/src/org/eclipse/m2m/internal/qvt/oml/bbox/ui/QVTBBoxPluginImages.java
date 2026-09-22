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
