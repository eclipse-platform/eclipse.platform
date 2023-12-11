package org.eclipse.ui.pki.pkiselection;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.pki.util.KeyStoreFormat;

public class PKCS12WidgetSelectedActions {
	
	public static void setKeyStoreFormat(){
		PKCSSelected.setPkcs12Selected(true);
		PKCSSelected.setPkcs11Selected(false);
		PKCSSelected.setKeystoreformat(KeyStoreFormat.PKCS12);
	}

	public static void userInterfaceDisplay(Text certpathtext, Label pkiLabel,
		Label passwordlabel, Text passwordtext, Button browseButton, boolean visiblity) {
		certpathtext.setVisible(visiblity);
		pkiLabel.setVisible(visiblity);
		browseButton.setVisible(visiblity);
		passwordlabel.setVisible(visiblity);
		passwordtext.setVisible(visiblity);		
	}
}
