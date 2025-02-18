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

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.ProviderException;
import java.security.SecureRandom;
import java.security.Security;
import java.util.ArrayList;
import java.util.Optional;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.eclipse.core.security.ActivateSecurity;
import org.eclipse.core.security.util.EclipseKeyStoreCollection;


public class AuthenticationBase implements AuthenticationService {
	
	private static AuthenticationBase INSTANCE;
	protected SSLContext sslContext;
	protected String pin;
	static KeyStore.PasswordProtection pp = new KeyStore.PasswordProtection("".toCharArray()); //$NON-NLS-1$
	protected boolean is9;
	protected String pkiProvider = "SunPKCS11"; // or could be FIPS provider :SunPKCS11-FIPS //$NON-NLS-1$
	protected String providerName = null;
	protected String cfgDirectory = null;
	protected String fingerprint;
	KeyStore keyStore = null;
	private AuthenticationBase() {}
	public static AuthenticationBase getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new AuthenticationBase();
		}
		return INSTANCE;
	}
	
	@Override
	public KeyStore initialize(char[] p) {
		pp = new KeyStore.PasswordProtection(p);
		String pin = new String(p);
		try {
			
			Optional<KeyStore>keyStoreContainer = Optional.ofNullable(configure());
			if (keyStoreContainer.isEmpty() ) {
				return null;
			} else {
				keyStore=keyStoreContainer.get();
			}
			try {
				/*
				 * Only load the store if the pin is a valuye other than the default setting of
				 * "pin" Otherwise the store will be preloaded by the default loading of the
				 * keystore, dynamically
				 */
				if (!(pin.equalsIgnoreCase("pin"))) { //$NON-NLS-1$
					PkiCallbackHandler pkiCB = new PkiCallbackHandler();
					PkiLoadParameter lp = new PkiLoadParameter();
					lp.setWaitForSlot(true);
				    lp.setProtectionParameter(pp);
				   
				    lp.setEventHandler(pkiCB);
					keyStore.load(lp);
					sslContext=AuthenticationBase.INSTANCE.setSSLContext(keyStore);
					ActivateSecurity.getInstance().log("SSL context PROTOCOL:"+sslContext.getProtocol()); //$NON-NLS-1$
				}

			} catch (Exception e) {
				/*
				 * An incorrect PiN could have been entered. AND thats OK, they can try again.
				 */
				ActivateSecurity.getInstance().log("Unable to load KeyStore, Bad Pin?"); //$NON-NLS-1$
				return null;
			}
			System.setProperty("javax.net.ssl.keyStoreProvider", "SunPKCS11"); //$NON-NLS-1$ //$NON-NLS-2$
			System.setProperty("https.protocols", "TLSv1.1,TLSv1.2,TLSv1.3"); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (Exception e) {
			e.printStackTrace();
		}
		/*
		 * Set the context AFTER you set the keystore...
		 */
		return keyStore;
	}

	private KeyStore configure() {
		Optional<String> configurationDirectory = null;
		Optional<String>providerContainer = null;
		Provider prototype = null;
		String securityProvider = null;
		KeyStore keyStore = null;
		String errorMessage=null;
		is9 = true;

		configurationDirectory = Optional.ofNullable(System.getProperty("javax.net.ssl.cfgFileLocation")); //$NON-NLS-1$
		if (configurationDirectory.isEmpty()) {
			// Where is it for Windoz
			//TBD:  find default setting
			setCfgDirectory(new String("/etc/opensc")); //$NON-NLS-1$
		} else {
			setCfgDirectory(configurationDirectory.get().toString());
		}

		if (Files.exists(Paths.get(getCfgDirectory()))) {
			ActivateSecurity.getInstance().log("PKCS11 configure  DIR:" + getCfgDirectory()); //$NON-NLS-1$
			providerContainer=Optional.ofNullable(
							System.getProperty("javax.net.ssl.keyStoreProvider")); //$NON-NLS-1$
			if (providerContainer.isEmpty() ) {
				securityProvider = pkiProvider;
			} else {
				securityProvider = providerContainer.get().toString();
			}
			prototype = Security.getProvider(securityProvider);
			if (prototype == null) {
				ActivateSecurity.getInstance().log("Configuring PKCS11 Provider not found."); //$NON-NLS-1$
			}

			try {
				Provider provider = prototype.configure(getCfgDirectory());
				providerName = provider.getName();
				Security.addProvider(provider);
				keyStore = KeyStore.getInstance("pkcs11", provider.getName() ); //$NON-NLS-1$
				setPkiProvider(provider.getName());
			} catch (KeyStoreException e) {
				errorMessage=e.getMessage()+" Problem loading the keystore.";
			} catch (InvalidParameterException e) {
				errorMessage=e.getMessage()+" You have provided an invalid parameter.";
			} catch (UnsupportedOperationException e) {
				errorMessage=e.getMessage()+" Operation is not supported at this time.";
			} catch (NullPointerException e) {
				errorMessage=e.getMessage()+" A Null Pointer was found.";
			} catch (NoSuchProviderException e) {
				errorMessage=e.getMessage()+" The PKCS11 provider could not be found.";
			} catch (ProviderException e) {
				errorMessage=e.getMessage()+" No PKCS11 Configuration found.";
			}
			Optional<String> errorContainer = Optional.ofNullable(errorMessage);
			if ( !(errorContainer.isEmpty())) {
				Security.removeProvider(providerName);
				ActivateSecurity.getInstance().log(errorMessage); //$NON-NLS-1$
			}
		}
		return keyStore;
	}
	public KeyStore getKeyStore() {
		return keyStore;
	}

	public SSLContext getSSLContext() {
		return this.sslContext;
	}
		

	public boolean isPkcs11Setup() {
		
		if ((getCfgDirectory() !=null ) && ( getPkiProvider() != null)) {
			return true;
		}
		return false;

	}

	public SSLContext setSSLContext(KeyStore keyStore) {
		
		try {
			sslContext = SSLContext.getInstance("TLSv1.3"); //$NON-NLS-1$
			
			Optional<X509TrustManager> PKIXtrust = ConfigureTrust.getInstance().setUp();
			if (PKIXtrust.isEmpty()) {
				ActivateSecurity.getInstance().log("Invalid TrustManager Initialization."); //$NON-NLS-1$
			} else {
				
				KeyManager[] km = new KeyManager[] { KeyStoreManager.getInstance() };
				TrustManager[] tm = new TrustManager[] { ConfigureTrust.getInstance() };
				
				sslContext.init(km, tm, new SecureRandom());
				SSLContext.setDefault(sslContext);
				HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sslContext;
	}

	public String getPkiProvider() {
		return pkiProvider;
	}

	public void setPkiProvider(String pkiProvider) {
		this.pkiProvider = pkiProvider;
	}

	public boolean isJava9() {
		return is9;
	}

	public String getFingerprint() {
		return fingerprint;
	}

	public static void setFingerprint(String fingerprint) {
		AuthenticationBase.INSTANCE.fingerprint = fingerprint;
	}

	public KeyManager getCustomKeyManager(KeyStore keyStore) {
		CustomKeyManager keyManager = null;
		try {
			keyManager = new CustomKeyManager(keyStore, "".toCharArray(), null); //$NON-NLS-1$
			keyManager.setSelectedFingerprint(getFingerprint());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return keyManager;
	}
	public ArrayList getList() {
		return EclipseKeyStoreCollection.getInstance().getList(keyStore);
	}

	public boolean isJavaModulesBased() {
		try {
			Class.forName("java.lang.Module"); //$NON-NLS-1$
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}
	
	public String getCfgDirectory() {
		return cfgDirectory;
	}

	public void setCfgDirectory(String cfgDirectory) {
		this.cfgDirectory = cfgDirectory;
	}
	public String getPin() {
		return pin;
	}
	public void setPin(String pin) {
		this.pin = pin;
		pp = new KeyStore.PasswordProtection(pin.toCharArray());
	}
	public void logoff() {
		try {
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	public boolean login() {
		Provider provider = Security.getProvider(getPkiProvider());
		if ( provider != null) { 
			
			try {
				provider.clear();
				return true;
			}  catch (SecurityException e) {
				e.printStackTrace();
			} 
		}
		return false;
	}
}
