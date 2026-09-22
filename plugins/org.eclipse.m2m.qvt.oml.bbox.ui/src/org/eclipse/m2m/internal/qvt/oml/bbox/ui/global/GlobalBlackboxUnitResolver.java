package org.eclipse.m2m.internal.qvt.oml.bbox.ui.global;

import java.util.Set;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.Messages;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.QVTBBoxUIPlugin;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxDescriptorLoader;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxDiagnosticInfo;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxDiagnosticUtil;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxUnitInfo;
import org.eclipse.m2m.internal.qvt.oml.blackbox.BlackboxRegistry;
import org.eclipse.m2m.internal.qvt.oml.blackbox.BlackboxUnitDescriptor;
import org.eclipse.m2m.internal.qvt.oml.blackbox.ResolutionContext;
import org.eclipse.osgi.util.NLS;

final class GlobalBlackboxUnitResolver {

	private final BlackboxDescriptorLoader descriptorLoader;

	GlobalBlackboxUnitResolver(BlackboxDescriptorLoader descriptorLoader) {
		this.descriptorLoader = descriptorLoader;
	}

	void addResolvedUnit(GlobalBlackboxGroup group, String qualifiedName, ResolutionContext context,
			EPackage.Registry packageRegistry, Set<BlackboxDescriptorIdentity> attributedDescriptors) {
		try {
			BlackboxUnitDescriptor descriptor = BlackboxRegistry.INSTANCE.getCompilationUnitDescriptor(qualifiedName,
					context);
			BlackboxUnitInfo unit = descriptorLoader.load(group, descriptor, qualifiedName, packageRegistry);
			group.addChild(unit);
			if (descriptor != null) {
				attributedDescriptors.add(BlackboxDescriptorIdentity.of(descriptor));
			}
		} catch (OperationCanceledException e) {
			throw e;
		} catch (RuntimeException e) {
			addFailure(group, qualifiedName, e);
		} catch (LinkageError e) {
			addFailure(group, qualifiedName, e);
		}
	}

	private static void addFailure(GlobalBlackboxGroup group, String qualifiedName, Throwable throwable) {
		String message = NLS.bind(Messages.BlackboxDiscovery_unitFailed,
				new Object[] { qualifiedName, BlackboxDiagnosticUtil.getMessage(throwable) });
		IStatus status = QVTBBoxUIPlugin.createStatus(IStatus.ERROR, message, throwable);
		QVTBBoxUIPlugin.log(status);
		group.addChild(new BlackboxDiagnosticInfo(group, Diagnostic.ERROR, message));
	}
}
