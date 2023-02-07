package org.eclipse.tips.json.internal;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.tips.json.internal.messages"; //$NON-NLS-1$
	public static String JsonTipProvider_1;
	public static String JsonTipProvider_4;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
