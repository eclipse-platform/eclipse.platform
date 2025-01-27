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
package org.eclipse.core.pki.pkiselection;

public class PKI {
	private String keyStore = ""; //$NON-NLS-1$
	private String keyStoreType = ""; //$NON-NLS-1$
	private String keyStoreProvider = ""; //$NON-NLS-1$
	private transient String keyStorePassword = ""; //$NON-NLS-1$
	private boolean isSecureStorage=false;
	public PKI() {}
	public String getKeyStore() {
		return keyStore;
	}
	public void setKeyStore(String keyStore) {
		this.keyStore = keyStore;
	}
	public String getKeyStoreType() {
		return keyStoreType;
	}
	public void setKeyStoreType(String keyStoreType) {
		this.keyStoreType = keyStoreType;
	}
	public String getKeyStoreProvider() {
		return keyStoreProvider;
	}
	public void setKeyStoreProvider(String keyStoreProvider) {
		this.keyStoreProvider = keyStoreProvider;
	}
	public String getKeyStorePassword() {
		return keyStorePassword;
	}
	public void setKeyStorePassword(String keyStorePassword) {
		this.keyStorePassword = keyStorePassword;
	}

	public boolean isSecureStorage() {
		return isSecureStorage;
	}
	public void setSecureStorage(boolean isSecureStorage) {
		this.isSecureStorage = isSecureStorage;
	}
	public void reSetSystem() {
		try {
			if ( this.getKeyStore() != null )  {
				System.setProperty("javax.net.ssl.keyStore", this.getKeyStore()); //$NON-NLS-1$
			} else {
				System.clearProperty("javax.net.ssl.keyStore"); //$NON-NLS-1$
			}

			if ( this.getKeyStoreType() != null )  {
				System.setProperty("javax.net.ssl.keyStoreType", this.getKeyStoreType()); //$NON-NLS-1$
			} else {
				System.clearProperty("javax.net.ssl.keyStoreType"); //$NON-NLS-1$
			}

			if( this.getKeyStoreProvider() != null) {
				// System.out.println("PKI - CLEARING keystoreprovider"); //$NON-NLS-1$
				if ( this.getKeyStoreProvider().isEmpty()) {
					System.clearProperty("javax.net.ssl.keyStoreProvider"); //$NON-NLS-1$
				} else {
					if (this.getKeyStoreType().equalsIgnoreCase("PKCS12")) { //$NON-NLS-1$
						System.clearProperty("javax.net.ssl.keyStoreProvider"); //$NON-NLS-1$
					} else {
						System.setProperty("javax.net.ssl.keyStoreProvider", this.getKeyStoreProvider()); //$NON-NLS-1$
					}
				}
			} else {
				System.clearProperty("javax.net.ssl.keyStoreProvider"); //$NON-NLS-1$
			}

			if ( this.getKeyStorePassword() != null) {
				System.setProperty("javax.net.ssl.keyStorePassword", getKeyStorePassword()); //$NON-NLS-1$
			} else {
				System.clearProperty("javax.net.ssl.keyStorePassword"); //$NON-NLS-1$
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}