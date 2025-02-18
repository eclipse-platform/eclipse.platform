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
package org.eclipse.core.security.managers;


import java.util.Optional;
import java.io.File;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.security.Provider;
import java.security.SecureRandom;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import org.eclipse.core.runtime.RegistryFactory;
import org.eclipse.core.runtime.spi.RegistryStrategy;
import org.eclipse.core.security.ActivateSecurity;
import org.eclipse.core.security.incoming.IncomingSystemProperty;
import org.eclipse.core.security.incoming.SecurityFileSnapshot;
import org.eclipse.core.security.state.X509SecurityState;
import org.eclipse.core.security.util.KeyStoreFormat;
import org.eclipse.core.security.util.PKIProperties;
import org.eclipse.core.security.managers.KeyStoreManager;
import org.eclipse.core.security.managers.ConfigureTrust;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.CoreException;



import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

public class KeystoreSetup  {
	static boolean isPkcs11Installed = false;
	static boolean isKeyStoreLoaded = false;
	PKIProperties pkiInstance = null;
	Properties pkiProperties = null;
	SSLContext sslContext = null;
	protected static KeyStore keyStore = null;
	private static final int DIGITAL_SIGNATURE = 0;
	private static final int KEY_CERT_SIGN = 5;
	private static final int CRL_SIGN = 6;
	
	private static KeystoreSetup INSTANCE;
	private KeystoreSetup() {}
	public static KeystoreSetup getInstance() {
        if(INSTANCE == null) {
            INSTANCE = new KeystoreSetup();
        }
        return INSTANCE;
    }
	public void installKeystore() {
		Optional<KeyStore> keystoreContainer = null;
		
		try {
			
			keystoreContainer = Optional.ofNullable(
					KeyStoreManager.getInstance().getKeyStore(System.getProperty("javax.net.ssl.keyStore"), //$NON-NLS-1$
							System.getProperty("javax.net.ssl.keyStorePassword"), //$NON-NLS-1$
							KeyStoreFormat.valueOf(System.getProperty("javax.net.ssl.keyStoreType")))); //$NON-NLS-1$

			if ((keystoreContainer.isEmpty()) || (!(KeyStoreManager.getInstance().isKeyStoreInitialized()))) {
				ActivateSecurity.getInstance().log("Failed to Load a Keystore."); //$NON-NLS-1$
				X509SecurityState.getInstance().setPKCS12on(false);
				System.clearProperty("javax.net.ssl.keyStoreType"); //$NON-NLS-1$
				System.clearProperty("javax.net.ssl.keyStore"); //$NON-NLS-1$
				System.clearProperty("javax.net.ssl.keyStoreProvider"); //$NON-NLS-1$
				System.clearProperty("javax.net.ssl.keyStorePassword"); //$NON-NLS-1$
				//SecurityFileSnapshot.getInstance().restoreProperties();
			} else {
				ActivateSecurity.getInstance().log("A Keystore and Password are detected."); //$NON-NLS-1$
				keyStore = keystoreContainer.get();
				setKeyStoreLoaded(true);
			}
		} catch (Exception e) {
			ActivateSecurity.getInstance().log("Exception Loading a Keystore:"+e.getMessage()); //$NON-NLS-1$
		}
	}
	public void setPkiContext() {
		TrustManager[] tm=null;
		KeyManager[] km = null;
		if (IncomingSystemProperty.getInstance().checkTrustStoreType()) {
			ActivateSecurity.getInstance().log("Activating TrustManager Initialization."); //$NON-NLS-1$
			if ((IncomingSystemProperty.getInstance().checkTrustStore())) {
				X509SecurityState.getInstance().setTrustOn(true);
				Optional<X509TrustManager> PKIXtrust = ConfigureTrust.getInstance().setUp();
				if (PKIXtrust.isEmpty()) {
					ActivateSecurity.getInstance().log("Invalid TrustManager Initialization."); //$NON-NLS-1$
					return;
				} 
				tm = new TrustManager[] { ConfigureTrust.getInstance() };
				ActivateSecurity.getInstance().log("TrustManager Initialization Done."); //$NON-NLS-1$
			} else {
				ActivateSecurity.getInstance().log("Invalid TrustManager Initialization."); //$NON-NLS-1$
				return;
			}
		}
		
		if (isKeyStoreLoaded()) {
			if (KeyStoreManager.getInstance().isKeyStoreInitialized()) {
				ActivateSecurity.getInstance().log("A KeyStore detected."); //$NON-NLS-1$
				try {
					km = new KeyManager[] { KeyStoreManager.getInstance() };
				} catch (Exception e) {
					ActivateSecurity.getInstance().log("No such Algorithm Initialization Error."); //$NON-NLS-1$
				} 
			} else {
				ActivateSecurity.getInstance().log("Valid KeyStore not found."); //$NON-NLS-1$
			}
		} 
		activateSecureContext(km,tm);
	}
	public void activateSecureContext( KeyManager[] km, TrustManager[] tm ) {
		try {
			
			SSLContext ctx = SSLContext.getInstance("TLS");//$NON-NLS-1$
			ctx.init(km, tm, null);
			SSLContext.setDefault(ctx);
			HttpsURLConnection.setDefaultSSLSocketFactory(ctx.getSocketFactory());
			setSSLContext(ctx);
			pkiInstance = PKIProperties.getInstance();
			pkiInstance.load();
			ActivateSecurity.getInstance().setSSLContext(ctx);
			setUserEmail();
			
			
			//ActivateSecurity.getInstance().completeSecureContext();
			//sslContextFactory.getDefault().setDefault(ctx);
			
			
			
			ActivateSecurity.getInstance().log("SSLContextFactory has been configured with SSLContext default."); //$NON-NLS-1$
		} catch (KeyManagementException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} 
	}
	public SSLContext getSSLContext() {
		return INSTANCE.sslContext;
	}

	public void setSSLContext(SSLContext context) {
		this.sslContext = context;
	}
	public boolean isKeyStoreLoaded() {
		return ActivateSecurity.getInstance().isKeyStoreLoaded();
	}

	private void setKeyStoreLoaded(boolean isKeyStoreLoaded) {
		ActivateSecurity.getInstance().setKeyStoreLoaded(isKeyStoreLoaded);
	}
	private void setUserEmail() {
		try {
			Enumeration<String> en = keyStore.aliases();
			while (en.hasMoreElements()) {
				String alias = en.nextElement();
				// System.out.println(" " + alias);
				Certificate cert = keyStore.getCertificate(alias);
				if (cert.getType().equalsIgnoreCase("X.509")) {
					X509Certificate X509 = (X509Certificate) cert;

					//
					// we need to make sure this is a digital certificate instead of a server
					// cert or something
					//
					if (isDigitalSignature(X509.getKeyUsage())) {
						Collection<List<?>> altnames = X509.getSubjectAlternativeNames();
						if (altnames != null) {
							for (List<?> item : altnames) {
								Integer type = (Integer) item.get(0);
								if (type == 1)
									try {
										String userEmail = item.toArray()[1].toString();
										System.setProperty("mail.smtp.user", userEmail);
									} catch (Exception e) {
										e.printStackTrace();
									}
							}
						}

					}

				}
			}
		} catch (Exception err) {

		}
	}

	private static boolean isDigitalSignature(boolean[] ba) {
		if (ba != null) {
			return ba[DIGITAL_SIGNATURE] && !ba[KEY_CERT_SIGN] && !ba[CRL_SIGN];
		} else {
			return false;
		}
	}
}
