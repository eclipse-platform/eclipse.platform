package org.eclipse.ui.internal.pkiselection;

import org.eclipse.ui.internal.util.KeyStoreFormat;

public class PKCSSelected {

	private static boolean pkcs11Selected = false;
	private static boolean pkcs12Selected = false;
	private static KeyStoreFormat keystoreformat = KeyStoreFormat.PKCS11;


	public static boolean isPkcs11Selected() {
		return pkcs11Selected;
	}
	public static void setPkcs11Selected(boolean pkcs11Selected) {
		PKCSSelected.pkcs12Selected = false;
		PKCSSelected.pkcs11Selected = pkcs11Selected;
	}
	public static boolean isPkcs12Selected() {
		return pkcs12Selected;
	}
	public static void setPkcs12Selected(boolean pkcs12Selected) {
		PKCSSelected.pkcs11Selected = false;
		PKCSSelected.pkcs12Selected = pkcs12Selected;
	}
	public static KeyStoreFormat getKeystoreformat() {
		return keystoreformat;
	}
	public static void setKeystoreformat(KeyStoreFormat keystoreformat) {
		PKCSSelected.keystoreformat = keystoreformat;
	}
}
