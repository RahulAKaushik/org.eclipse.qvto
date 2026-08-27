package org.eclipse.m2m.internal.qvt.oml.bbox.ui.global;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.QVTBBoxUIPlugin;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxDescriptorLoader;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxDiagnosticInfo;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxDiagnosticUtil;
import org.eclipse.m2m.internal.qvt.oml.blackbox.BlackboxRegistry;
import org.eclipse.m2m.internal.qvt.oml.blackbox.BlackboxUnitDescriptor;
import org.eclipse.m2m.internal.qvt.oml.blackbox.ResolutionContext;
import org.eclipse.m2m.internal.qvt.oml.blackbox.ResolutionContextImpl;

final class RuntimeBlackboxDiscovery {

	private final BlackboxDescriptorLoader descriptorLoader;

	RuntimeBlackboxDiscovery(BlackboxDescriptorLoader descriptorLoader) {
		this.descriptorLoader = descriptorLoader;
	}

	void discover(GlobalBlackboxDiscoveryResult result, Set<BlackboxDescriptorIdentity> attributedDescriptors,
			EPackage.Registry packageRegistry, IProgressMonitor monitor) {
		try {
			ResolutionContext context = new ResolutionContextImpl(URI.createURI("/")); //$NON-NLS-1$
			Set<BlackboxDescriptorIdentity> keys = new HashSet<BlackboxDescriptorIdentity>();
			for (BlackboxUnitDescriptor descriptor : BlackboxRegistry.INSTANCE.getCompilationUnitDescriptors(context)) {
				checkCanceled(monitor);
				if (descriptor == null) {
					continue;
				}
				BlackboxDescriptorIdentity key = BlackboxDescriptorIdentity.of(descriptor);
				if (!keys.add(key) || attributedDescriptors.contains(key)) {
					continue;
				}
				GlobalBlackboxGroup group = result.getRuntimeRegistrations();
				group.addChild(descriptorLoader.load(group, descriptor, descriptor.getQualifiedName(), packageRegistry));
			}
		} catch (OperationCanceledException e) {
			throw e;
		} catch (RuntimeException e) {
			addFailure(result, e);
		} catch (LinkageError e) {
			addFailure(result, e);
		}
	}

	private static void addFailure(GlobalBlackboxDiscoveryResult result, Throwable throwable) {
		QVTBBoxUIPlugin.log(throwable);
		GlobalBlackboxGroup group = result.getRuntimeRegistrations();
		group.addChild(new BlackboxDiagnosticInfo(group, Diagnostic.ERROR,
				BlackboxDiagnosticUtil.getMessage(throwable)));
	}

	private static void checkCanceled(IProgressMonitor monitor) {
		if (monitor != null && monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
	}

}
