package org.eclipse.core.pki;

public class DebugLogger {
	public static boolean enableDebugLogging = false;

	public static void setEnableDebugLogging(boolean enableDebugLogging) {
		DebugLogger.enableDebugLogging = enableDebugLogging;
	}

	public static void printDebug(String message) {
		if (enableDebugLogging || checkForFlag()) {
			System.out.println(message);
		}
	}

	private static boolean checkForFlag() {
		String debugFlag = System.getProperty("debug.verbose"); //$NON-NLS-1$
		if (debugFlag != null) {
			debugFlag = debugFlag.trim().toLowerCase();
			if (debugFlag.equals("true")) { //$NON-NLS-1$
				DebugLogger.setEnableDebugLogging(true);
				return true;
			}
		}
		return false;
	}
}
