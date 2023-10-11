package org.eclipse.ui.internal.util;

import java.security.KeyStore;

public class ChangedPressedFieldEditorStatus {

	private static boolean isPKISaveCertificateChecked = false;
	private static boolean isJKSSaveTrustStoreChecked = false;
	private static boolean ispkiChangedPressed = false;
	private static boolean isjksChangedPressed = false;

	private static KeyStore pkiUserKeyStore = null;
	private static KeyStore previousUserKeyStore = null;
	private static KeyStore jksTrustStore = null;

	public ChangedPressedFieldEditorStatus() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @return the isPKISaveCertificateChecked
	 */
	public static boolean isPKISaveCertificateChecked() {
		return isPKISaveCertificateChecked;
	}

	/**
	 * @param isPKISaveCertificateChecked the isPKISaveCertificateChecked to set
	 */
	public static void setPKISaveCertificateChecked(
			boolean isPKISaveCertificateChecked) {
		ChangedPressedFieldEditorStatus.isPKISaveCertificateChecked = isPKISaveCertificateChecked;
	}

	/**
	 * @return the isJKSSaveTrustStoreChecked
	 */
	public static boolean isJKSSaveTrustStoreChecked() {
		return isJKSSaveTrustStoreChecked;
	}

	/**
	 * @param isJKSSaveTrustStoreChecked the isJKSSaveTrustStoreChecked to set
	 */
	public static void setJKSSaveTrustStoreChecked(
			boolean isJKSSaveTrustStoreChecked) {
		ChangedPressedFieldEditorStatus.isJKSSaveTrustStoreChecked = isJKSSaveTrustStoreChecked;
	}

	/**
	 * @return the pkiChangedPressed
	 */
	public static boolean isPkiChangedPressed() {
		return ispkiChangedPressed;
	}

	/**
	 * @param pkiChangedPressed the pkiChangedPressed to set
	 */
	public static void setPkiChangedPressed(boolean pkiChangedPressed) {
		ChangedPressedFieldEditorStatus.ispkiChangedPressed = pkiChangedPressed;
	}

	/**
	 * @return the jksChangedPressed
	 */
	public static boolean isJksChangedPressed() {
		return isjksChangedPressed;
	}

	/**
	 * @param jksChangedPressed the jksChangedPressed to set
	 */
	public static void setJksChangedPressed(boolean jksChangedPressed) {
		ChangedPressedFieldEditorStatus.isjksChangedPressed = jksChangedPressed;
	}

	/**
	 * @return user key store.
	 */
	public static KeyStore getPkiUserKeyStore() {
		return pkiUserKeyStore;
	}

	/**
	 * Sets the user key store.
	 * @param pkiUserKeyStore
	 */
	public static void setPkiUserKeyStore(KeyStore pkiUserKeyStore) {
		ChangedPressedFieldEditorStatus.previousUserKeyStore = ChangedPressedFieldEditorStatus.pkiUserKeyStore;
		ChangedPressedFieldEditorStatus.pkiUserKeyStore = pkiUserKeyStore;
	}
	public static KeyStore getPreviousUserKeyStore() {
		return previousUserKeyStore;
	}

	/**
	 * @return the trust store.
	 */
	public static KeyStore getJksTrustStore() {
		return jksTrustStore;
	}

	/**
	 * Sets the trust store.
	 * @param jksTrustStore
	 */
	public static void setJksTrustStore(KeyStore jksTrustStore) {
		ChangedPressedFieldEditorStatus.jksTrustStore = jksTrustStore;
	}
}
