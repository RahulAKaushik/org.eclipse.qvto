package org.eclipse.m2m.internal.qvt.oml.bbox.ui.navigator;

import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.QVTBBoxPluginImages;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxDiagnosticInfo;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxModuleInfo;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxOperationInfo;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxUnitInfo;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

public class BlackboxNavigatorLabelProvider extends LabelProvider {

	@Override
	public String getText(Object element) {
		if (element instanceof BlackboxRootNode) {
			return "QVTo Blackboxes"; //$NON-NLS-1$
		}
		if (element instanceof BlackboxLoadingNode) {
			return "Loading..."; //$NON-NLS-1$
		}
		if (element instanceof BlackboxUnitInfo) {
			BlackboxUnitInfo unit = (BlackboxUnitInfo) element;
			String suffix = unit.isUsed() ? " (used)" : " (available)"; //$NON-NLS-1$ //$NON-NLS-2$
			if (unit.hasErrors()) {
				return unit.getQualifiedName() + suffix + " - failed"; //$NON-NLS-1$
			}
			return unit.getQualifiedName() + suffix;
		}
		if (element instanceof BlackboxModuleInfo) {
			BlackboxModuleInfo module = (BlackboxModuleInfo) element;
			String text = module.getName() + " (" + module.getOperations().size() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
			if (!module.getPackageURIs().isEmpty()) {
				text += " - " + join(module); //$NON-NLS-1$
			}
			return text;
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
