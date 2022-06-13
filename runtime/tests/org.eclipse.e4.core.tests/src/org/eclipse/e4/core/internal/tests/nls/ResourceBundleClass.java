package org.eclipse.e4.core.internal.tests.nls;

import java.util.ListResourceBundle;

public class ResourceBundleClass extends ListResourceBundle {
	@Override
	protected Object[][] getContents() {
		return new Object[][] {
			new Object[] { "message", "ResourceBundleClassMessage" },
			new Object[] { "message_one", "ResourceBundleClassMessageUnderscore" },
			new Object[] { "messageOne", "ResourceBundleClassMessageCamelCase" },
			new Object[] { "message_two", "ResourceBundleClassMessageUnderscoreDot" },
			new Object[] { "messageThree", "ResourceBundleClassCamelCaseDot" },
				new Object[] { "messageFour", "The idea is from {0}" },
				new Object[] { "messageFive_Sub", "ResourceBundleClassMessageCamelCaseAndUnderscoreOriginal" },
				new Object[] { "message_six__sub", "ResourceBundleClassMessageCamelCaseAndUnderscoreDeCamelCasified" },
				new Object[] { "message.seven..sub",
						"ResourceBundleClassMessageCamelCaseAndUnderscoreDeCamelCasifiedAndDeUnderscorified" },
				new Object[] { "messageEight.Sub", "ResourceBundleClassMessageCamelCaseAndUnderscoreDeUnderscorified" },
				new Object[] { "message_nine._sub",
						"ResourceBundleClassMessageCamelCaseAndUnderscoreDeUnderscorifiedAndDeCamelCasified" }
		};
	}
}