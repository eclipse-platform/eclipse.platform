/*******************************************************************************
 * Copyright (c) 2023 Eclipse Platform, Security Group and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Eclipse Platform - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.pki.pkiselection;

import org.eclipse.core.pki.util.KeyStoreFormat;

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
