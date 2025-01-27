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
package org.eclipse.core.pki.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Optional;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public enum ConfigureTrust implements X509TrustManager {
	MANAGER;

	protected X509TrustManager pkixTrustManager = null;

	public Optional<X509TrustManager> setUp() {
		KeyStore keyStore = null;
		String storeLocation = null;
		String trustType = null;
		String passwd = "changeit"; //$NON-NLS-1$
		try {
			Optional<String> trustStoreFile = Optional.ofNullable(System.getProperty("javax.net.ssl.trustStore")); //$NON-NLS-1$
			if (trustStoreFile.isEmpty()) {
				storeLocation = System.getProperty("java.home") + //$NON-NLS-1$
						"/lib/security/cacerts" //$NON-NLS-1$
								.replace("/", FileSystems.getDefault().getSeparator()); //$NON-NLS-1$
			} else {
				storeLocation = trustStoreFile.get().toString();
			}
			//FileInputStream fs = new FileInputStream(storeLocation);

			InputStream fs = Files.newInputStream(Paths.get(storeLocation));
			
			Optional<String> trustStoreFileType = Optional
					.ofNullable(System.getProperty("javax.net.ssl.trustStoreType")); //$NON-NLS-1$
			if (trustStoreFileType.isEmpty()) {
				trustType = KeyStore.getDefaultType();
			} else {
				trustType = trustStoreFileType.get().toString();
			}
			keyStore = KeyStore.getInstance(trustType);

			Optional<String> trustStorePassword = Optional
					.ofNullable(System.getProperty("javax.net.ssl.trustStorePassword")); //$NON-NLS-1$
			if (trustStorePassword.isEmpty()) {
				LogUtil.logInfo("ConfigureTrust using default Password since none provided.");
				passwd="changeit";
			} else {
				passwd = trustStorePassword.get().toString();
			}

			keyStore.load(fs, passwd.toCharArray());

			TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX"); //$NON-NLS-1$
			tmf.init(keyStore);
			TrustManager tms[] = tmf.getTrustManagers();
			for (TrustManager tm : tms) {
				if (tm instanceof X509TrustManager) {
					pkixTrustManager = (X509TrustManager) tm;
					LogUtil.logInfo("Initialization PKIX Trust Manager Complete"); //$NON-NLS-1$
					break;
				}
			}


		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			LogUtil.logError("ConfigureTrust - No algorythm found, ", e); //$NON-NLS-1$
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			LogUtil.logError("ConfigureTrust - Initialize keystore Error, ", e); //$NON-NLS-1$
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			LogUtil.logError("ConfigureTrust - No File Found:", e); //$NON-NLS-1$
		} catch (CertificateException e) {
			// TODO Auto-generated catch block
			LogUtil.logError("ConfigureTrust - Certificate Error", e); //$NON-NLS-1$
		} catch (IOException e) {
			// TODO Auto-generated catch block
			LogUtil.logError("ConfigureTrust - I/O Error, bad password?", e); //$NON-NLS-1$
		}
		return Optional.ofNullable(pkixTrustManager);
	}

	@Override
	public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		// TODO Auto-generated method stub
		pkixTrustManager.checkClientTrusted(chain, authType);
	}

	@Override
	public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		// TODO Auto-generated method stub
		pkixTrustManager.checkServerTrusted(chain, authType);
	}

	@Override
	public X509Certificate[] getAcceptedIssuers() {
		// TODO Auto-generated method stub
		return pkixTrustManager.getAcceptedIssuers();

	}

}
