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

import java.net.Authenticator;
import java.net.PasswordAuthentication;

import org.eclipse.core.pki.util.LogUtil;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public class PKIProperties extends Authenticator {


	private String keyStore = ""; //$NON-NLS-1$
	private String keyStoreType = ""; //$NON-NLS-1$
	private String keyStoreProvider = ""; //$NON-NLS-1$
	private String username = null;
	private transient String keyStorePassword = ""; //$NON-NLS-1$
	private static PKI lastPKI=null;
	private static PKIProperties sslProperties=null;
	public static PKIProperties getNewInstance() {
		return new PKIProperties();
	}
	public static PKIProperties getInstance() {
		if ( sslProperties == null ) {
			synchronized(PKIProperties.class) {
				if ( sslProperties == null ) {
					sslProperties = new PKIProperties();
					try {
						sslProperties.load();
					} catch(Exception ignoreException) {
						ignoreException.printStackTrace();
					}
				}
			}
		}
		return sslProperties;
	}
	private PKIProperties() {}
	@Override
	public PasswordAuthentication getPasswordAuthentication() {
		PasswordAuthentication auth = null;

		try {
			auth = new PasswordAuthentication(this.getUsername(), this.getKeyStorePassword().toCharArray() );
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return auth;
	}
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
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public void restore() {
		System.out.println("PKIProperties - restore"); //$NON-NLS-1$
		try {
			if (( this.getKeyStore() != null ) &&
				( this.getKeyStoreType() != null ) &&
				( this.getKeyStoreProvider() != null) &&
				( this.getKeyStorePassword() != null) ) {

				if ( !(this.getKeyStore().isEmpty()) ) {
					System.setProperty("javax.net.ssl.keyStore", this.getKeyStore()); //$NON-NLS-1$
				}

				if ( !(this.getKeyStoreType().isEmpty()) ) {
					System.setProperty("javax.net.ssl.keyStoreType", this.getKeyStoreType()); //$NON-NLS-1$
				}

				if ( !(this.getKeyStoreProvider().isEmpty() )) {
					System.setProperty("javax.net.ssl.keyStoreProvider", this.getKeyStoreProvider()); //$NON-NLS-1$
				}

				if ( !(this.getKeyStorePassword().isEmpty() )) {
					if ( lastPKI != null ) {
						if ( lastPKI.getKeyStorePassword().isEmpty() ) {
							System.clearProperty("javax.net.ssl.keyStorePassword"); //$NON-NLS-1$
						}
					} else {
						System.setProperty("javax.net.ssl.keyStorePassword", getKeyStorePassword()); //$NON-NLS-1$
					}
				}
			} else {
				clear();
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void load() {

		if (System.getProperty("javax.net.ssl.keyStore") != null) { //$NON-NLS-1$
			LogUtil.logInfo("PKIProperties keystorePKI" + System.getProperty("javax.net.ssl.keyStore")); //$NON-NLS-1$ //$NON-NLS-2$
			sslProperties.setKeyStore(System.getProperty("javax.net.ssl.keyStore")); //$NON-NLS-1$
		} else {
			sslProperties.setKeyStore(""); //$NON-NLS-1$
		}
		if (System.getProperty("javax.net.ssl.keyStoreType") != null) { //$NON-NLS-1$
			sslProperties.setKeyStoreType(System.getProperty("javax.net.ssl.keyStoreType")); //$NON-NLS-1$
		} else {
			sslProperties.setKeyStoreType(""); //$NON-NLS-1$
		}
		if (System.getProperty("javax.net.ssl.keyStoreProvider") != null) { //$NON-NLS-1$
			sslProperties.setKeyStoreProvider(System.getProperty("javax.net.ssl.keyStoreProvider")); //$NON-NLS-1$
		} else {
			if (System.getProperty("javax.net.ssl.keyStoreType") != null) { //$NON-NLS-1$
				if (System.getProperty("javax.net.ssl.keyStoreType").equalsIgnoreCase("pkcs12")) { //$NON-NLS-1$ //$NON-NLS-2$
					System.clearProperty("javax.net.ssl.keyStoreProvider"); //$NON-NLS-1$
				} else {
					sslProperties.setKeyStoreProvider(""); //$NON-NLS-1$
				}
			}
		}
		if (System.getProperty("javax.net.ssl.keyStorePassword") != null) { //$NON-NLS-1$
			sslProperties.setKeyStorePassword(System.getProperty("javax.net.ssl.keyStorePassword")); //$NON-NLS-1$

		} else {
			sslProperties.setKeyStorePassword(""); //$NON-NLS-1$
		}

		sslProperties.setUsername(System.getProperty("user.name")); //$NON-NLS-1$
	}
	public void setLastPkiValue( PKI pki ) {
		lastPKI = pki;
	}
	public void clear() {
		System.out.println("PKIProperties - CLESAR ALL PROPR"); //$NON-NLS-1$
		System.clearProperty("javax.net.ssl.keyStoreProvider"); //$NON-NLS-1$
		System.clearProperty("javax.net.ssl.keyStore"); //$NON-NLS-1$
		System.clearProperty("javax.net.ssl.keyStoreProvider"); //$NON-NLS-1$
		System.clearProperty("javax.net.ssl.keyStorePassword"); //$NON-NLS-1$
	}
	public void dump() {
		StringBuffer sb = new StringBuffer();
		sb.append("javax.net.ssl.keyStore="); //$NON-NLS-1$
		sb.append(sslProperties.getKeyStore());
		sb.append("\n"); //$NON-NLS-1$
		sb.append("javax.net.ssl.keyStoreType="); //$NON-NLS-1$
		sb.append(sslProperties.getKeyStoreType());
		sb.append("\n"); //$NON-NLS-1$
		sb.append("javax.net.ssl.keyStoreProvider="); //$NON-NLS-1$
		sb.append(sslProperties.getKeyStoreProvider());
		sb.append("\n"); //$NON-NLS-1$

		Status status = new Status(IStatus.INFO, sb.toString(), null);
		LogUtil.logInfo(status.getMessage());
	}
}
