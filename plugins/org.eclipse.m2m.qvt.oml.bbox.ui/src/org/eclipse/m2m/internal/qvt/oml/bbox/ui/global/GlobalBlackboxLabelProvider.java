package org.eclipse.m2m.internal.qvt.oml.bbox.ui.global;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.Messages;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.navigator.BlackboxNavigatorLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

public class GlobalBlackboxLabelProvider extends BlackboxNavigatorLabelProvider {

	private Image folderErrorImage;
	private Image projectErrorImage;

	@Override
	public String getText(Object element) {
		if (element instanceof GlobalBlackboxLoadingNode) {
			return Messages.BlackboxNavigator_loading;
		}
		if (element instanceof GlobalBlackboxStatusNode) {
			return ((GlobalBlackboxStatusNode) element).getMessage();
		}
		if (element instanceof GlobalBlackboxGroup) {
			GlobalBlackboxGroup group = (GlobalBlackboxGroup) element;
			switch (group.getKind()) {
				case WORKSPACE_PROJECTS:
					return Messages.GlobalBlackboxView_workspaceProjects;
				case JAVA_LIBRARIES:
					return Messages.GlobalBlackboxView_javaLibraries;
				case ECLIPSE_PLATFORM:
					return Messages.GlobalBlackboxView_eclipsePlatform;
				case EXTENSION_CONTRIBUTIONS:
					return Messages.GlobalBlackboxView_extensionContributions;
				case ACTIVE_PLUGINS:
					return Messages.GlobalBlackboxView_activePlugins;
				case RUNTIME_REGISTRATIONS:
					return Messages.GlobalBlackboxView_runtimeRegistrations;
				default:
					return group.getLabel();
			}
		}
		return super.getText(element);
	}

	@Override
	public Image getImage(Object element) {
		ISharedImages images = PlatformUI.getWorkbench().getSharedImages();
		if (element instanceof GlobalBlackboxLoadingNode) {
			return images.getImage(ISharedImages.IMG_OBJS_INFO_TSK);
		}
		if (element instanceof GlobalBlackboxStatusNode) {
			return images.getImage(ISharedImages.IMG_OBJS_INFO_TSK);
		}
		if (element instanceof GlobalBlackboxGroup) {
			GlobalBlackboxGroup group = (GlobalBlackboxGroup) element;
			Image base = group.getKind() == GlobalBlackboxGroupKind.PROJECT
					? images.getImage(ISharedImages.IMG_OBJ_PROJECT)
					: images.getImage(ISharedImages.IMG_OBJ_FOLDER);
			if (!group.hasErrors()) {
				return base;
			}
			Image errorImage = group.getKind() == GlobalBlackboxGroupKind.PROJECT
					? projectErrorImage
					: folderErrorImage;
			if (errorImage == null || errorImage.isDisposed()) {
				ImageDescriptor overlay = images.getImageDescriptor(ISharedImages.IMG_DEC_FIELD_ERROR);
				errorImage = new DecorationOverlayIcon(base, overlay, IDecoration.BOTTOM_RIGHT).createImage();
				if (group.getKind() == GlobalBlackboxGroupKind.PROJECT) {
					projectErrorImage = errorImage;
				} else {
					folderErrorImage = errorImage;
				}
			}
			return errorImage;
		}
		return super.getImage(element);
	}

	@Override
	public void dispose() {
		if (folderErrorImage != null) {
			folderErrorImage.dispose();
			folderErrorImage = null;
		}
		if (projectErrorImage != null) {
			projectErrorImage.dispose();
			projectErrorImage = null;
		}
		super.dispose();
	}
}
