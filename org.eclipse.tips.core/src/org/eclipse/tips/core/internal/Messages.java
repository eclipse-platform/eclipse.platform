package org.eclipse.tips.core.internal;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.tips.core.internal.messages"; //$NON-NLS-1$
	public static String FinalTip_2;
	public static String FinalTip_3;
	public static String FinalTip_4;
	public static String TipManager_0;
	public static String TipManager_2;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
