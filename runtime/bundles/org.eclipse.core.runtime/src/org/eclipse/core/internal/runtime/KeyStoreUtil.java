/*******************************************************************************
 * Copyright (c) 2025 SAP SE and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.runtime;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;
import org.eclipse.osgi.service.environment.Constants;

public class KeyStoreUtil {

	@SuppressWarnings("nls")
	private static final String SYSTEM_PROPERTY_MERGE_TRUST = "eclipse.platform.mergeTrust";

	private final String os;

	private static final record KeyStoreAndPassword(KeyStore keyStore, char[] password) {
	}

	public KeyStoreUtil(String os) {
		this.os = os;
	}

	@SuppressWarnings("nls")
	public void setUpSslContext() throws GeneralSecurityException, IOException {

		if (!Boolean.getBoolean(SYSTEM_PROPERTY_MERGE_TRUST)) {
			return;
		}

		List<KeyStoreAndPassword> keyStores = new ArrayList<>();
		// null will loads JVM cacerts OR store indicated by "javax.net.ssl.trustStore" properties
		keyStores.add(new KeyStoreAndPassword(null, null));
		if (System.getProperty("javax.net.ssl.trustStore", "").isEmpty()) {
			if (Constants.OS_MACOSX.equals(os)) {
				keyStores.add(createKeyStore("KeychainStore", "Apple"));
			} else if (Constants.OS_WIN32.equals(os)) {
				keyStores.add(createKeyStore("Windows-ROOT", null));
			}
		}
		List<X509TrustManager> trustManagers = new ArrayList<>();
		for (KeyStoreAndPassword storeAndPassword : keyStores) {
			trustManagers.add(createX509TrustManager(storeAndPassword.keyStore()));
		}
		TrustManager[] tm = { new CollectionTrustManager(trustManagers) };

		KeyManager[] km = {};
		KeyStoreAndPassword keyStore = createKeyStoreFromSystemProperties();
		if (keyStore != null) {
			km = new KeyManager[] { createX509KeyManager(keyStore.keyStore(), keyStore.password()) };
		}

		SSLContext sslContext = SSLContext.getInstance("TLS");
		initSSLContext(sslContext, tm, km, null);
		SSLContext.setDefault(sslContext);
	}

	private KeyStoreAndPassword createKeyStore(String type, String provider)
			throws GeneralSecurityException, IOException {
		KeyStore keyStore;
		if (provider == null) {
			keyStore = KeyStore.getInstance(type);
		} else {
			keyStore = KeyStore.getInstance(type, provider);
		}
		keyStore.load(null, null);
		return new KeyStoreAndPassword(keyStore, null);
	}

	protected X509TrustManager createX509TrustManager(KeyStore keyStore) throws GeneralSecurityException {
		TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		tmf.init(keyStore);
		return Arrays.stream(tmf.getTrustManagers()).filter(X509TrustManager.class::isInstance) //
				.map(X509TrustManager.class::cast) //
				.findFirst().orElse(null);
	}

	protected X509KeyManager createX509KeyManager(KeyStore keyStore, char[] password) throws GeneralSecurityException {
		KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		kmf.init(keyStore, password);
		return Arrays.stream(kmf.getKeyManagers()).filter(X509KeyManager.class::isInstance) //
				.map(X509KeyManager.class::cast) //
				.findFirst().orElse(null);
	}

	protected void initSSLContext(SSLContext context, TrustManager[] trustManagers, KeyManager[] keyManagers,
			SecureRandom random) throws KeyManagementException {
		context.init(keyManagers, trustManagers, random);
	}

	/**
	 * Coding based on
	 * sun.security.ssl.SSLContextImpl.DefaultManagersHolder.getKeyManagers() with
	 * minor adjustments (access properties directy without AccessController, return
	 * nothing if properties not set).
	 */
	@SuppressWarnings("nls")
	private KeyStoreAndPassword createKeyStoreFromSystemProperties() throws GeneralSecurityException, IOException {
		String p11KeyStore = "PKCS11";
		String none = "NONE";
		String keyStore = System.getProperty("javax.net.ssl.keyStore", "");
		String keyStoreType = System.getProperty("javax.net.ssl.keyStoreType", "");
		String keyStoreProvider = System.getProperty("javax.net.ssl.keyStoreProvider", "");
		String keyStorePassword = System.getProperty("javax.net.ssl.keyStorePassword", "");

		if (keyStoreType.isEmpty()) {
			if (keyStore.isEmpty()) {
				return null;
			}
			keyStoreType = KeyStore.getDefaultType();
		}
		if (p11KeyStore.equals(keyStoreType) && !none.equals(keyStore)) {
			throw new IllegalArgumentException("if keyStoreType is " + p11KeyStore + ", then keyStore must be " + none);
		}
		char[] passwd = null;
		if (!keyStorePassword.isEmpty()) {
			passwd = keyStorePassword.toCharArray();
		}
		KeyStore ks = null;
		if (keyStoreProvider.isEmpty()) {
			ks = KeyStore.getInstance(keyStoreType);
		} else {
			ks = KeyStore.getInstance(keyStoreType, keyStoreProvider);
		}
		if (!keyStore.isEmpty() && !none.equals(keyStore)) {
			try (InputStream is = new FileInputStream(keyStore)) {
				ks.load(is, passwd);
			}
		} else {
			ks.load(null, passwd);
		}
		return new KeyStoreAndPassword(ks, p11KeyStore.equals(keyStoreType) ? null : passwd);
	}

}
