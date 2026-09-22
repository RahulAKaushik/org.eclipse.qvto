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
package org.eclipse.m2m.internal.qvt.oml.bbox.ui.navigator;

import java.util.EnumMap;
import java.util.Map;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.QVTBBoxPluginImages;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxNodeKind;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxTreeNode;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.ProjectBlackboxDiscoveryService;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

/**
 * Shared presentation adapter for both trees. Nodes supply text and semantic
 * roles; this UI-thread provider owns only generated error-overlay images.
 */
public class BlackboxNavigatorLabelProvider extends LabelProvider {

	private final Map<BlackboxNodeKind, Image> errorImages = new EnumMap<BlackboxNodeKind, Image>(
			BlackboxNodeKind.class);

	@Override
	public String getText(Object element) {
		return element instanceof BlackboxTreeNode ? ((BlackboxTreeNode) element).getLabel() : super.getText(element);
	}

	@Override
	public Image getImage(Object element) {
		if (!(element instanceof BlackboxTreeNode)) {
			return super.getImage(element);
		}
		BlackboxTreeNode node = (BlackboxTreeNode) element;
		BlackboxNodeKind kind = node.getNodeKind();
		Image base = baseImage(kind);
		boolean hasErrors = node.hasErrors();
		if (element instanceof BlackboxRootNode) {
			// Complete-scope markers remain visible even before tree discovery.
			hasErrors |= ProjectBlackboxDiscoveryService
					.hasBlackboxProblemMarkers(((BlackboxRootNode) element).getProject());
		}
		if (!hasErrors || (kind != BlackboxNodeKind.UNIT && kind != BlackboxNodeKind.GROUP
				&& kind != BlackboxNodeKind.PROJECT)) {
			return base;
		}
		Image decorated = errorImages.get(kind);
		if (decorated == null) {
			ImageDescriptor overlay = PlatformUI.getWorkbench().getSharedImages()
					.getImageDescriptor(ISharedImages.IMG_DEC_FIELD_ERROR);
			decorated = new DecorationOverlayIcon(base, overlay, IDecoration.BOTTOM_RIGHT).createImage();
			errorImages.put(kind, decorated);
		}
		return decorated;
	}

	private Image baseImage(BlackboxNodeKind kind) {
		ISharedImages images = PlatformUI.getWorkbench().getSharedImages();
		switch (kind) {
		case UNIT:
			return QVTBBoxPluginImages.getInstance().getImage(QVTBBoxPluginImages.QVTO_BLACKBOX);
		case PROJECT:
			return images.getImage(ISharedImages.IMG_OBJ_PROJECT);
		case GROUP:
		case MODULE:
			return images.getImage(ISharedImages.IMG_OBJ_FOLDER);
		case OPERATION:
			return images.getImage(ISharedImages.IMG_OBJ_FILE);
		case WARNING:
			return images.getImage(ISharedImages.IMG_OBJS_WARN_TSK);
		case ERROR:
			return images.getImage(ISharedImages.IMG_OBJS_ERROR_TSK);
		default:
			return images.getImage(ISharedImages.IMG_OBJS_INFO_TSK);
		}
	}

	@Override
	public void dispose() {
		// Registry/workbench base images are shared and must not be disposed here.
		for (Image image : errorImages.values()) {
			image.dispose();
		}
		errorImages.clear();
		super.dispose();
	}
}
