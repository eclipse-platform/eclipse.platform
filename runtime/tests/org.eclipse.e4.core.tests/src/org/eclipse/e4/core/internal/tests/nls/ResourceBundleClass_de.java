package org.eclipse.e4.core.internal.tests.nls;

import java.util.ListResourceBundle;

public class ResourceBundleClass_de extends ListResourceBundle {
	@Override
	protected Object[][] getContents() {
		return new Object[][] {
			new Object[] { "message", "ResourceBundleClassNachricht" },
			new Object[] { "message_one", "ResourceBundleClassNachrichtUnderscore" },
			new Object[] { "messageOne", "ResourceBundleClassNachrichtCamelCase" },
			new Object[] { "message_two", "ResourceBundleNachrichtMessageUnderscoreDot" },
				new Object[] { "messageFour", "Die Idee ist von {0}" },
				new Object[] { "messageFive_Sub", "ResourceBundleClassNachrichtCamelCaseAndUnderscoreOriginal" },
				new Object[] { "message_six__sub", "ResourceBundleClassNachrichtCamelCaseAndUnderscoreDeCamelCasified" },
				new Object[] { "message.seven..sub",
						"ResourceBundleClassNachrichtCamelCaseAndUnderscoreDeCamelCasifiedAndDeUnderscorified" },
				new Object[] { "messageEight.Sub", "ResourceBundleClassNachrichtCamelCaseAndUnderscoreDeUnderscorified" },
				new Object[] { "message_nine._sub",
						"ResourceBundleClassNachrichtCamelCaseAndUnderscoreDeUnderscorifiedAndDeCamelCasified" }
		};
	}
}