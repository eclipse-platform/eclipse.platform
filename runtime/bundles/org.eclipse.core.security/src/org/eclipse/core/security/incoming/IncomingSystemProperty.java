/*******************************************************************************
 * Copyright (c) 2025 Eclipse Platform, Security Group and others.
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
package org.eclipse.core.security.incoming;

import java.util.Optional;
import org.eclipse.core.security.ActivateSecurity;
import org.eclipse.core.security.encryption.NormalizeGCM;
import org.eclipse.core.security.state.X509SecurityState;

public class IncomingSystemProperty {
	private static IncomingSystemProperty INSTANCE;
	private IncomingSystemProperty() {}
	public static IncomingSystemProperty getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new IncomingSystemProperty();
		}
		return INSTANCE;
	}

	public boolean checkType() {
		Optional<String> type = null;

		type = Optional.ofNullable(System.getProperty("javax.net.ssl.keyStoreType")); //$NON-NLS-1$
		if (type.isEmpty()) {
			ActivateSecurity.getInstance().log("Continue without javax.net.ssl.keyStoreType.");//$NON-NLS-1$
			X509SecurityState.getInstance().setTrustOn(true);
			return true;
		}
		if (type.get().equalsIgnoreCase("PKCS11")) { //$NON-NLS-1$
			X509SecurityState.getInstance().setPKCS11on(true);
			return true;
		}
		if (type.get().equalsIgnoreCase("PKCS12")) { //$NON-NLS-1$
			X509SecurityState.getInstance().setPKCS12on(true);
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
			X509SecurityState.getInstance().setPKCS11on(false);
			X509SecurityState.getInstance().setPKCS12on(false);
			//ActivateSecurity.getInstance().log("No Keystore is set, javax.net.ssl.keyStore."); //$NON-NLS-1$
			//return false;
		}
		keyStorePassword = Optional.ofNullable(System.getProperty("javax.net.ssl.keyStorePassword")); //$NON-NLS-1$
		if (keyStorePassword.isEmpty()) {
			//ActivateSecurity.getInstance().log("A Keystore Password is required, javax.net.ssl.keyStorePassword"); //$NON-NLS-1$
			//return false;
		} else {
			PasswordDecrypted = Optional.ofNullable(System.getProperty("javax.net.ssl.decryptedPassword")); //$NON-NLS-1$
			PasswordEncrypted = Optional.ofNullable(System.getProperty("javax.net.ssl.encryptedPassword")); //$NON-NLS-1$
			if ((PasswordEncrypted.isEmpty()) || (!(PasswordDecrypted.isEmpty()))) {
				// Password is not encrypted
			} else {
				if (PasswordEncrypted.get().toString().equalsIgnoreCase("true")) { //$NON-NLS-1$
					salt = new String(System.getProperty("user.name") + pin).getBytes(); //$NON-NLS-1$
					String passwd = NormalizeGCM.getInstance().decrypt(keyStorePassword.get().toString(), pin,
							new String(salt));
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
			ActivateSecurity.getInstance().log("No incoming javax.net.ssl.trustStoreType."); //$NON-NLS-1$
			return false;
		}
		return true;

	}

	public boolean checkTrustStore() {
		Optional<String> trustStore = null;
		Optional<String> trustStorePassword = null;
		trustStore = Optional.ofNullable(System.getProperty("javax.net.ssl.trustStore")); //$NON-NLS-1$
		if (trustStore.isEmpty()) {
			ActivateSecurity.getInstance().log("No truststore is set, javax.net.ssl.trustStore."); //$NON-NLS-1$
			return false;
		}
		trustStorePassword = Optional.ofNullable(System.getProperty("javax.net.ssl.trustStorePassword")); //$NON-NLS-1$
		if (trustStorePassword.isEmpty()) {
			ActivateSecurity.getInstance().log("A truststore Password is required, javax.net.ssl.trustStorePassword."); //$NON-NLS-1$
			return false;
		}
		return true;
	}
}
