package org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery;

/**
 * Creates a usable diagnostic message even for malformed provider exceptions.
 */
public final class BlackboxDiagnosticUtil {

	private BlackboxDiagnosticUtil() {
	}

	public static String getMessage(Throwable throwable) {
		String message = null;
		try {
			message = throwable.getMessage();
		} catch (RuntimeException e) {
			// A provider exception must not prevent reporting the original failure.
		}
		return message != null ? message : throwable.getClass().getName();
	}
}
