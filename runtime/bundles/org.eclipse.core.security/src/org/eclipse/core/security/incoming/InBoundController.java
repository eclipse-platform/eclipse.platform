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
package org.eclipse.core.security.incoming;

import java.security.KeyStore;
import java.util.Optional;

import org.eclipse.core.security.ActivateSecurity;
import org.eclipse.core.security.managers.AuthenticationBase;
import org.eclipse.core.security.managers.KeyStoreManager;
import org.eclipse.core.security.managers.KeystoreSetup;
import org.eclipse.core.security.state.X509SecurityState;

public class InBoundController {
	private static InBoundController INSTANCE;
	protected final String pin = "#Gone2Boat@Bay"; //$NON-NLS-1$
	Optional<KeyStore> keystoreContainer = null;//$NON-NLS-1$
	protected static KeyStore keyStore = null;//$NON-NLS-1$
	private InBoundController() {
	}

	public static InBoundController getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new InBoundController();
		}
		return INSTANCE;
	}

	public void controller() {
		Optional<String> keystoreTypeContainer = null;
		Optional<String> decryptedPw;
		/*
		 * First see if parameters were passed into eclipse via the command line -D
		 */
		keystoreTypeContainer = Optional.ofNullable(System.getProperty("javax.net.ssl.keyStoreType")); //$NON-NLS-1$

		Optional<String> testKeyContainer = Optional.ofNullable(
				System.getProperty("core.key"));
		if (!(testKeyContainer.isEmpty() ))  {
			String testKey = testKeyContainer.get().toString().trim();
			if (testKey.equalsIgnoreCase("eclipse.core.pki.testing")) {
				return;
			}
		}
		if (keystoreTypeContainer.isEmpty()) {
			//
			// Incoming parameter as -DkeystoreType was empty so CHECK in .pki file
			//
			
			if (PublicKeySecurity.getInstance().isTurnedOn()) {
				PublicKeySecurity.getInstance().getPkiPropertyFile(pin);
			}
		}
	}
}
