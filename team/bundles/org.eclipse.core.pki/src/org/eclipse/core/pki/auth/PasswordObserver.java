/**
 * Copyright (c) 2014 Codetrails GmbH.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Marcel Bruch - initial API and implementation.
 */
package org.eclipse.core.pki.auth;

import java.util.Observable;
import java.util.Observer;
import java.util.Optional;
import java.io.File;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.security.SecureRandom;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;


import org.eclipse.core.runtime.RegistryFactory;
import org.eclipse.core.runtime.spi.RegistryStrategy;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.pki.util.LogUtil;
import org.eclipse.core.pki.util.ConfigureTrust;
import org.eclipse.core.pki.util.KeyStoreManager;
import org.eclipse.core.pki.util.KeyStoreFormat;
import org.eclipse.core.pki.pkiselection.SecurityOpRequest;

import org.eclipse.core.pki.pkiselection.PKIProperties;

@SuppressWarnings("restriction")
public class PasswordObserver implements Observer, Subscriber {
	static boolean isPkcs11Installed = false;
	static boolean isKeyStoreLoaded = false;
	PKIProperties pkiInstance = null;
	Properties pkiProperties = null;
	//SSLContext sslContext = null;
	private Subscription subscription;
	protected static KeyStore keyStore = null;
	private static final int DIGITAL_SIGNATURE = 0;
	private static final int KEY_CERT_SIGN = 5;
	private static final int CRL_SIGN = 6;
	public PasswordObserver() {

	}

	public void update(Observable obj, Object arg) {
		Optional<KeyStore> keystoreContainer = null;
		String pw = (String) arg;
		//LogUtil.logWarning("PasswordObserver- BREAK for INPUT:"+pw);
		System.setProperty("javax.net.ssl.keyStorePassword", pw); //$NON-NLS-1$
		KeystoreSetup setup = KeystoreSetup.getInstance();
		setup.installKeystore();
	}
	public void onSubscribe(Subscription subscription) {
		// TODO Auto-generated method stub
		this.subscription = subscription;
	}

	public void onNext(Object item) {
		// TODO Auto-generated method stub
		SecurityOpRequest.INSTANCE.setConnected(true);
	}

	public void onError(Throwable throwable) {
		// TODO Auto-generated method stub
	}

	public void onComplete() {
		// TODO Auto-generated method stub
	}
}
