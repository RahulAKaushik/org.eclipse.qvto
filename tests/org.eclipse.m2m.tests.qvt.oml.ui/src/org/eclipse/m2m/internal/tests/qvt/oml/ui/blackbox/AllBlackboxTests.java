package org.eclipse.m2m.internal.tests.qvt.oml.ui.blackbox;

import org.eclipse.m2m.internal.tests.qvt.oml.ui.blackbox.integration.ActiveBundleOwnershipTest;
import org.eclipse.m2m.internal.tests.qvt.oml.ui.blackbox.integration.BlackboxProblemMarkerSynchronizerTest;
import org.eclipse.m2m.internal.tests.qvt.oml.ui.blackbox.integration.BlackboxResourceChangeSupportTest;
import org.eclipse.m2m.internal.tests.qvt.oml.ui.blackbox.integration.BlackboxVisibilitySettingsTest;
import org.eclipse.m2m.internal.tests.qvt.oml.ui.blackbox.integration.ProjectBlackboxJavaSearchTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({BlackboxDescriptorLoaderTest.class, BlackboxDescriptorCandidatesTest.class,
	BlackboxVisibilityScopeTest.class, ActiveBundleDescriptorFilterTest.class,
	ExtensionContributionNameResolverTest.class, GlobalBlackboxIdentityTest.class, BlackboxUnitLabelTest.class,
	BlackboxDiagnosticUtilTest.class,
	GlobalBlackboxDiscoveryLifecycleTest.class,
	ActiveBundleOwnershipTest.class, BlackboxProblemMarkerSynchronizerTest.class,
	BlackboxResourceChangeSupportTest.class, BlackboxVisibilitySettingsTest.class,
	ProjectBlackboxJavaSearchTest.class})
public class AllBlackboxTests {
}
