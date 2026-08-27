package org.eclipse.m2m.internal.tests.qvt.oml.ui.blackbox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.URI;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxDiagnosticInfo;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxUnitInfo;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.global.GlobalBlackboxLabelProvider;
import org.junit.Test;

public class BlackboxUnitLabelTest {

	@Test
	public void failureIsRepresentedByDecorationNotTextSuffix() {
		BlackboxUnitInfo successful = new BlackboxUnitInfo(null, "example.Library", //$NON-NLS-1$
				URI.createURI("qvto://blackbox/example.Library")); //$NON-NLS-1$
		BlackboxUnitInfo failed = new BlackboxUnitInfo(null, "example.Library", //$NON-NLS-1$
				URI.createURI("qvto://blackbox/example.Library")); //$NON-NLS-1$
		failed.addDiagnostic(new BlackboxDiagnosticInfo(failed, Diagnostic.ERROR, "Expected failure")); //$NON-NLS-1$

		GlobalBlackboxLabelProvider labels = new GlobalBlackboxLabelProvider();
		try {
			assertTrue(failed.hasErrors());
			assertEquals("example.Library", labels.getText(successful)); //$NON-NLS-1$
			assertEquals(labels.getText(successful), labels.getText(failed));
		} finally {
			labels.dispose();
		}
	}
}
