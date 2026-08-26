package org.eclipse.m2m.internal.tests.qvt.oml.ui.blackbox;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxVisibilityScope;
import org.junit.Test;

public class BlackboxVisibilityScopeTest {

	@Test
	public void projectOnlyExcludesDependenciesAndRegistryDescriptors() {
		BlackboxVisibilityScope scope = BlackboxVisibilityScope.PROJECT_ONLY;

		assertFalse(scope.includesJavaDependencies());
		assertFalse(scope.includesRegistryDescriptors());
	}

	@Test
	public void projectDependenciesIncludesClasspathWithoutRegistryDescriptors() {
		BlackboxVisibilityScope scope = BlackboxVisibilityScope.PROJECT_DEPENDENCIES;

		assertTrue(scope.includesJavaDependencies());
		assertFalse(scope.includesRegistryDescriptors());
	}

	@Test
	public void projectVisibleIncludesClasspathAndRegistryDescriptors() {
		BlackboxVisibilityScope scope = BlackboxVisibilityScope.PROJECT_VISIBLE;

		assertTrue(scope.includesJavaDependencies());
		assertTrue(scope.includesRegistryDescriptors());
	}
}
