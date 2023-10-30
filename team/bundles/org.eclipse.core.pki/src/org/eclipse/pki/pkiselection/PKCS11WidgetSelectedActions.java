package org.eclipse.pki.pkiselection;

import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.pki.util.KeyStoreFormat;

public class PKCS11WidgetSelectedActions {
	
	public static void setKeyStoreFormat(){
		PKCSSelected.setPkcs11Selected(true);
		PKCSSelected.setPkcs12Selected(false);
		PKCSSelected.setKeystoreformat(KeyStoreFormat.PKCS11);
	}
	
	/**
	 * Causes the PKI Certificate to be visible or not.
	 * @param password The Text box to enter the path.
	 * @param passwordlabel The PKI Certificate label.
	 * @param visibility true to make all 3 fields visible, false to make it not visible.
	 */
	public static void userInterfaceDisplay(Text password, Label passwordlabel, boolean visibility){
		if(password != null) password.setVisible(visibility);
		passwordlabel.setVisible(visibility);
	}

}
