package org.eclipse.m2m.internal.qvt.oml.bbox.ui.navigator;

import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.Messages;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.QVTBBoxPluginImages;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxDiagnosticInfo;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxModuleInfo;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxOperationInfo;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxUnitInfo;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

public class BlackboxNavigatorLabelProvider extends LabelProvider {

	@Override
	public String getText(Object element) {
		if (element instanceof BlackboxRootNode) {
			return Messages.BlackboxNavigator_root;
		}
		if (element instanceof BlackboxLoadingNode) {
			return Messages.BlackboxNavigator_loading;
		}
		if (element instanceof BlackboxUnitInfo) {
			BlackboxUnitInfo unit = (BlackboxUnitInfo) element;
			String suffix = unit.isUsed() ? Messages.BlackboxNavigator_usedSuffix : Messages.BlackboxNavigator_availableSuffix;
			if (unit.hasErrors()) {
				return unit.getQualifiedName() + suffix + Messages.BlackboxNavigator_failedSuffix;
			}
			return unit.getQualifiedName() + suffix;
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
			return QVTBBoxPluginImages.getInstance().getImage(QVTBBoxPluginImages.QVTO_BLACKBOX);
		}
		if (element instanceof BlackboxModuleInfo) {
			return images.getImage(ISharedImages.IMG_OBJ_FOLDER);
		}
		if (element instanceof BlackboxLoadingNode) {
			return images.getImage(ISharedImages.IMG_OBJS_INFO_TSK);
		}
		if (element instanceof BlackboxUnitInfo) {
			BlackboxUnitInfo unit = (BlackboxUnitInfo) element;
			String imageKey = unit.hasErrors() ? QVTBBoxPluginImages.QVTO_BLACKBOX_ERROR : QVTBBoxPluginImages.QVTO_BLACKBOX;
			return QVTBBoxPluginImages.getInstance().getImage(imageKey);
		}
		if (element instanceof BlackboxDiagnosticInfo) {
			BlackboxDiagnosticInfo diagnostic = (BlackboxDiagnosticInfo) element;
			if (diagnostic.getSeverity() == Diagnostic.WARNING) {
				return images.getImage(ISharedImages.IMG_OBJS_WARN_TSK);
			}
			return images.getImage(ISharedImages.IMG_OBJS_ERROR_TSK);
		}
		if (element instanceof BlackboxOperationInfo) {
			return images.getImage(ISharedImages.IMG_OBJ_FILE);
		}
		return super.getImage(element);
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
