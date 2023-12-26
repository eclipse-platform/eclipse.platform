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

	public boolean checkKeyStore() {
		Optional<String> keyStore = null;
		Optional<String> keyStorePassword = null;
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
		}
		return true;
	}

}
