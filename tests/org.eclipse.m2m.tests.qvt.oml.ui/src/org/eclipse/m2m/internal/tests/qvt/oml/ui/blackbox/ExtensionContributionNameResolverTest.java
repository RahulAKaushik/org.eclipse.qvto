package org.eclipse.m2m.internal.tests.qvt.oml.ui.blackbox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.eclipse.m2m.internal.qvt.oml.bbox.ui.global.ExtensionContributionNameResolver;
import org.junit.Test;

public class ExtensionContributionNameResolverTest {

	private final ExtensionContributionNameResolver resolver = new ExtensionContributionNameResolver();

	@Test
	public void resolvesUnitWithExplicitNamespace() {
		assertEquals("custom.namespace.Library", //$NON-NLS-1$
				resolver.resolve("unit", "contributor.bundle", "Library", "custom.namespace", null)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

	@Test
	public void resolvesUnitWithContributorOrEmptyNamespace() {
		assertEquals("contributor.bundle.Library", //$NON-NLS-1$
				resolver.resolve("unit", "contributor.bundle", "Library", null, null)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertEquals("Library", resolver.resolve("unit", "contributor.bundle", "Library", "", null)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

	@Test
	public void resolvesLibraryClassAndAlias() {
		assertEquals("example.library.Implementation", //$NON-NLS-1$
				resolver.resolve("library", "contributor.bundle", null, null, //$NON-NLS-1$ //$NON-NLS-2$
						"example.library.Implementation")); //$NON-NLS-1$
		assertEquals("example.library.PublicName", //$NON-NLS-1$
				resolver.resolve("library", "contributor.bundle", "PublicName", null, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						"example.library.Implementation")); //$NON-NLS-1$
		assertEquals("PublicName", resolver.resolve("library", "contributor.bundle", "PublicName", null, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				"Implementation")); //$NON-NLS-1$
	}

	@Test
	public void rejectsIncompleteOrUnknownElements() {
		assertNull(resolver.resolve("unit", "contributor.bundle", null, null, null)); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull(resolver.resolve("library", "contributor.bundle", "Alias", null, null)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertNull(resolver.resolve("unknown", "contributor.bundle", "Library", null, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				"example.Library")); //$NON-NLS-1$
	}
}
