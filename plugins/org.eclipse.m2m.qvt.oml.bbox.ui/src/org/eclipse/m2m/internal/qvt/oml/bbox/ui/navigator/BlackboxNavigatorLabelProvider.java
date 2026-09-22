package org.eclipse.m2m.internal.qvt.oml.bbox.ui.navigator;

import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.Messages;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.QVTBBoxPluginImages;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxDiagnosticInfo;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.ProjectBlackboxDiscoveryService;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxModuleInfo;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxOperationInfo;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxUnitInfo;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

public class BlackboxNavigatorLabelProvider extends LabelProvider {

	private Image blackboxErrorImage;

	@Override
	public String getText(Object element) {
		if (element instanceof BlackboxRootNode) {
			BlackboxRootNode root = (BlackboxRootNode) element;
			switch (root.getScope()) {
				case PROJECT_VISIBLE:
					return Messages.BlackboxNavigator_rootProjectVisible;
				case PROJECT_ONLY:
					return Messages.BlackboxNavigator_rootProjectOnly;
				case PROJECT_DEPENDENCIES:
				default:
					return Messages.BlackboxNavigator_rootProjectDependencies;
			}
		}
		if (element instanceof BlackboxLoadingNode) {
			return Messages.BlackboxNavigator_loading;
		}
		if (element instanceof BlackboxUnitInfo) {
			return ((BlackboxUnitInfo) element).getQualifiedName();
		}
		if (element instanceof BlackboxModuleInfo) {
			BlackboxModuleInfo module = (BlackboxModuleInfo) element;
			if (!module.getPackageURIs().isEmpty()) {
				return NLS.bind(Messages.BlackboxNavigator_moduleLabelWithPackages,
						new Object[] { module.getName(), Integer.valueOf(module.getOperations().size()), join(module) });
			}
			return NLS.bind(Messages.BlackboxNavigator_moduleLabel,
					module.getName(), Integer.valueOf(module.getOperations().size()));
		}
		if (element instanceof BlackboxOperationInfo) {
			return ((BlackboxOperationInfo) element).getSignature();
		}
		if (element instanceof BlackboxDiagnosticInfo) {
			return ((BlackboxDiagnosticInfo) element).getMessage();
		}
		return super.getText(element);
	}

	@Override
	public Image getImage(Object element) {
		ISharedImages images = PlatformUI.getWorkbench().getSharedImages();
		if (element instanceof BlackboxRootNode) {
			BlackboxRootNode root = (BlackboxRootNode) element;
			return getBlackboxImage(root.hasErrors()
					|| ProjectBlackboxDiscoveryService.hasBlackboxProblemMarkers(root.getProject()));
		}
		if (element instanceof BlackboxModuleInfo) {
			return images.getImage(ISharedImages.IMG_OBJ_FOLDER);
		}
		if (element instanceof BlackboxLoadingNode) {
			return images.getImage(ISharedImages.IMG_OBJS_INFO_TSK);
		}
		if (element instanceof BlackboxUnitInfo) {
			BlackboxUnitInfo unit = (BlackboxUnitInfo) element;
			return getBlackboxImage(unit.hasErrors());
		}
		if (element instanceof BlackboxDiagnosticInfo) {
			BlackboxDiagnosticInfo diagnostic = (BlackboxDiagnosticInfo) element;
			if (diagnostic.getSeverity() == Diagnostic.WARNING) {
				return images.getImage(ISharedImages.IMG_OBJS_WARN_TSK);
			}
			if (diagnostic.isError()) {
				return images.getImage(ISharedImages.IMG_OBJS_ERROR_TSK);
			}
			return images.getImage(ISharedImages.IMG_OBJS_INFO_TSK);
		}
		if (element instanceof BlackboxOperationInfo) {
			return images.getImage(ISharedImages.IMG_OBJ_FILE);
		}
		return super.getImage(element);
	}

	@Override
	public void dispose() {
		if (blackboxErrorImage != null) {
			blackboxErrorImage.dispose();
			blackboxErrorImage = null;
		}
		super.dispose();
	}

	private Image getBlackboxImage(boolean hasErrors) {
		Image baseImage = QVTBBoxPluginImages.getInstance().getImage(QVTBBoxPluginImages.QVTO_BLACKBOX);
		if (!hasErrors) {
			return baseImage;
		}
		if (blackboxErrorImage == null || blackboxErrorImage.isDisposed()) {
			ImageDescriptor errorOverlay = PlatformUI.getWorkbench().getSharedImages()
					.getImageDescriptor(ISharedImages.IMG_DEC_FIELD_ERROR);
			blackboxErrorImage = new DecorationOverlayIcon(baseImage, errorOverlay, IDecoration.BOTTOM_RIGHT)
					.createImage();
		}
		return blackboxErrorImage;
	}

	private static String join(BlackboxModuleInfo module) {
		StringBuilder result = new StringBuilder();
		for (String packageURI : module.getPackageURIs()) {
			if (result.length() > 0) {
				result.append(", "); //$NON-NLS-1$
			}
			result.append(packageURI);
		}
		return result.toString();
	}
}
