/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.pki.auth;

import java.util.Optional;

import org.eclipse.core.pki.util.LogUtil;
import org.eclipse.core.pki.util.NormalizeAES256;

public enum IncomingSystemProperty {
	SETTINGS;

	public boolean checkType() {
		Optional<String> type = null;

		type = Optional.ofNullable(System.getProperty("javax.net.ssl.keyStoreType")); //$NON-NLS-1$
		if (type.isEmpty()) {
			LogUtil.logError("No incoming System Properties are set for PKI.", null); //$NON-NLS-1$
			return false;
		}
		if (type.get().equalsIgnoreCase("PKCS11")) { //$NON-NLS-1$
			PKIState.CONTROL.setPKCS11on(true);
			return true;
		}
		if (type.get().equalsIgnoreCase("PKCS12")) { //$NON-NLS-1$
			PKIState.CONTROL.setPKCS12on(true);
			return true;
		}
		return false;
	}

	public boolean checkKeyStore(String pin) {
		byte[] salt = new byte[16];
		Optional<String> keyStore = null;
		Optional<String> keyStorePassword = null;
		Optional<String> PasswordEncrypted = null;
		Optional<String> PasswordDecrypted = null;
		keyStore = Optional.ofNullable(System.getProperty("javax.net.ssl.keyStore")); //$NON-NLS-1$
		if (keyStore.isEmpty()) {
			PKIState.CONTROL.setPKCS11on(false);
			PKIState.CONTROL.setPKCS12on(false);
			LogUtil.logError("No Keystore is set, javax.net.ssl.keyStore", null); //$NON-NLS-1$
			return false;
		}
		keyStorePassword = Optional.ofNullable(System.getProperty("javax.net.ssl.keyStorePassword")); //$NON-NLS-1$
		if (keyStorePassword.isEmpty()) {
			LogUtil.logError("A Keystore Password is required, javax.net.ssl.keyStorePassword", null); //$NON-NLS-1$
			return false;
		} else {
			PasswordDecrypted = Optional.ofNullable(System.getProperty("javax.net.ssl.decryptedPassword")); //$NON-NLS-1$
			PasswordEncrypted = Optional.ofNullable(System.getProperty("javax.net.ssl.encryptedPassword")); //$NON-NLS-1$
			if ((PasswordEncrypted.isEmpty()) || (!(PasswordDecrypted.isEmpty()))) {
				// Password is not encrypted
			} else {
				if (PasswordEncrypted.get().toString().equalsIgnoreCase("true")) { //$NON-NLS-1$
					salt = new String(System.getProperty("user.name") + pin).getBytes(); //$NON-NLS-1$
					String passwd = NormalizeAES256.DECRYPT.decrypt(keyStorePassword.get().toString(), pin,
							new String(salt));
					LogUtil.logInfo("IncomingSystemProperty - decrypt passwd:" + passwd); //$NON-NLS-1$
					System.setProperty("javax.net.ssl.keyStorePassword", passwd); //$NON-NLS-1$
				}
			}
		}
		return true;
	}

	public boolean checkTrustStoreType() {
		Optional<String> type = null;

		type = Optional.ofNullable(System.getProperty("javax.net.ssl.trustStoreType")); //$NON-NLS-1$
		if (type.isEmpty()) {
			LogUtil.logError("No incoming System Properties are set for PKI.", null); //$NON-NLS-1$
			return false;
		}
		return true;

	}

	public boolean checkTrustStore() {
		Optional<String> trustStore = null;
		Optional<String> trustStorePassword = null;
		trustStore = Optional.ofNullable(System.getProperty("javax.net.ssl.trustStore")); //$NON-NLS-1$
		if (trustStore.isEmpty()) {
			LogUtil.logError("No truststore is set, javax.net.ssl.trustStore", null); //$NON-NLS-1$
			return false;
		}
		trustStorePassword = Optional.ofNullable(System.getProperty("javax.net.ssl.trustStorePassword")); //$NON-NLS-1$
		if (trustStorePassword.isEmpty()) {
			LogUtil.logError("A truststore Password is required, javax.net.ssl.trustStorePassword", null); //$NON-NLS-1$
			return false;
		}
		return true;
	}

}
