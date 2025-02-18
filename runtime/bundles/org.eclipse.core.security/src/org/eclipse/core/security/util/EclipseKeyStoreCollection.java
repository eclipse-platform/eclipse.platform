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
package org.eclipse.core.security.util;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;


public class EclipseKeyStoreCollection {
	private static EclipseKeyStoreCollection INSTANCE;
	protected final int DIGITAL_SIGNATURE=0;
	private final int KEY_ENCIPHERMENT = 2;
	protected static PKIProperties pkiProperties=PKIProperties.getInstance();;
	private EclipseKeyStoreCollection() {}
	public static EclipseKeyStoreCollection getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new EclipseKeyStoreCollection();
		}
		return INSTANCE;
	}
	public ArrayList getList(KeyStore keyStore) {
		ArrayList list = new ArrayList<String>();
		try {
			String alias=null;
			Enumeration aliases = keyStore.aliases();
			while (aliases.hasMoreElements()) {
				alias = (String) aliases.nextElement();
				X509Certificate certificate = (X509Certificate) keyStore.getCertificate(alias);
				certificate.checkValidity();
				if ( isDigitalSignature(certificate.getKeyUsage()) ) {
					PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, null);
					if ( privateKey != null) {
						list.add( alias );
					}
				}
			}
		} catch (KeyStoreException e) {
			e.printStackTrace();
		} catch (CertificateExpiredException e) {
			e.printStackTrace();
		} catch (CertificateNotYetValidException e) {
			e.printStackTrace();
		} catch (UnrecoverableKeyException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		
		return list;
		
	}
	private boolean isDigitalSignature(boolean[] ba) {
		if ( ba != null) {
			
			return ba[DIGITAL_SIGNATURE];
		} else {
			return false;
		}
	}
	private boolean isKeyEncipherment(boolean[] ba) {
		if ( ba != null) {
			
			return ba[KEY_ENCIPHERMENT];
		} else {
			return false;
		}
	}
}
