package org.eclipse.m2m.internal.qvt.oml.bbox.ui.global;

import java.util.Set;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxDescriptorLoader;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxUnitInfo;
import org.eclipse.m2m.internal.qvt.oml.blackbox.BlackboxRegistry;
import org.eclipse.m2m.internal.qvt.oml.blackbox.BlackboxUnitDescriptor;
import org.eclipse.m2m.internal.qvt.oml.blackbox.ResolutionContext;

final class GlobalBlackboxUnitResolver {

	private final BlackboxDescriptorLoader descriptorLoader;

	GlobalBlackboxUnitResolver(BlackboxDescriptorLoader descriptorLoader) {
		this.descriptorLoader = descriptorLoader;
	}

	void addResolvedUnit(GlobalBlackboxGroup group, String qualifiedName, ResolutionContext context,
			EPackage.Registry packageRegistry, Set<BlackboxDescriptorIdentity> attributedDescriptors) {
		BlackboxUnitDescriptor descriptor = BlackboxRegistry.INSTANCE.getCompilationUnitDescriptor(qualifiedName, context);
		BlackboxUnitInfo unit = descriptorLoader.load(group, descriptor, qualifiedName, packageRegistry);
		group.addChild(unit);
		if (descriptor != null) {
			attributedDescriptors.add(BlackboxDescriptorIdentity.of(descriptor));
		}
	}
}
