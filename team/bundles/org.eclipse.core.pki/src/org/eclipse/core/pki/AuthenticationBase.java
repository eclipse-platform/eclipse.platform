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
package org.eclipse.core.pki;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.util.ArrayList;
import java.util.Optional;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.eclipse.core.pki.AuthenticationService;
import org.eclipse.core.pki.util.ConfigureTrust;
import org.eclipse.core.pki.util.KeyStoreManager;
import org.eclipse.core.pki.util.LogUtil;

public enum AuthenticationBase implements AuthenticationService {
	INSTANCE;

	protected SSLContext sslContext;
	protected String pin;
	static KeyStore.PasswordProtection pp = new KeyStore.PasswordProtection("".toCharArray()); //$NON-NLS-1$
	// private static final String javaVersion = System.getProperty("java.version");
	protected boolean is9;
	protected String pkiProvider = "SunPKCS11"; // or could be FIPS provider :SunPKCS11-FIPS //$NON-NLS-1$
	protected Provider provider = null;
	protected String cfgDirectory = null;
	protected String fingerprint;
	KeyStore keyStore = null;
	@Override
	public KeyStore initialize(char[] p) {
		// TODO Auto-generated method stub
		pp = new KeyStore.PasswordProtection(p);
		String pin = new String(p);
		try {

			//LogUtil.logInfo("Before configure keyStore with PIN:"+pin); //$NON-NLS-1$
			
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
					System.out.println("AuthenticationBase SSL context PROTOCOL:" + sslContext.getProtocol()); //$NON-NLS-1$
				}

			} catch (Exception e) {
				/*
				 * An incorrect PiN could have been entered. AND thats OK, they can try again.
				 */
				// TODO Auto-generated catch block
//				IStatus status = new Status (IStatus.ERROR, AuthenticationPlugin.getPluginId(),"Did you enter an invalid PiN? ");
//				AuthenticationPlugin.getDefault().getLog().log(status);
				e.printStackTrace();
			}
			// System.setProperty("javax.net.ssl.keyStoreProvider", "SunPKCS11");
			System.setProperty("javax.net.ssl.keyStoreProvider", "SunPKCS11"); //$NON-NLS-1$ //$NON-NLS-2$
			System.setProperty("https.protocols", "TLSv1.1,TLSv1.2,TLSv1.3"); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		/*
		 * TDB: TODO: Set the context AFTER you set the keystore...
		 */

//		this.sslContext= setSSLContext(keyStore );
		return keyStore;
	}

	private KeyStore configure() {
		Optional<String> configurationDirectory = null;
		Optional<String>providerContainer = null;
		Provider prototype = null;
		String securityProvider = null;
		//String cfgDirectory = "TBD"; //$NON-NLS-1$
		KeyStore keyStore = null;
		String errorMessage=null;
		is9 = true;

		// System.out.println("In configure CFG STORED FILE LOC:" +

		configurationDirectory = Optional.ofNullable(System.getProperty("javax.net.ssl.cfgFileLocation")); //$NON-NLS-1$
		if (configurationDirectory.isEmpty()) {
			// Where is it for Windoz
			//TBD:  find default setting
			setCfgDirectory(new String("/etc/opensc")); //$NON-NLS-1$
		} else {
			setCfgDirectory(configurationDirectory.get().toString());
		}

		if (Files.exists(Paths.get(getCfgDirectory()))) {
			LogUtil.logInfo("AuthenticationBase - PKCS11 configure  DIR:" + getCfgDirectory()); //$NON-NLS-1$
			providerContainer=Optional.ofNullable(
							System.getProperty("javax.net.ssl.keyStoreProvider")); //$NON-NLS-1$

			if (providerContainer.isEmpty() ) {
				securityProvider = pkiProvider;
			} else {
				securityProvider = providerContainer.get().toString();
			}
			prototype = Security.getProvider(securityProvider);
			if (prototype == null) {
				LogUtil.logInfo("In configure  PROVIDER NOT FOUND"); //$NON-NLS-1$
			}

			try {
				provider = prototype.configure(getCfgDirectory());
				Security.addProvider(provider);
				keyStore = KeyStore.getInstance("pkcs11", provider.getName() ); //$NON-NLS-1$
				setPkiProvider(provider.getName());
			} catch (KeyStoreException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				errorMessage=e.getMessage();
			} catch (InvalidParameterException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				errorMessage=e.getMessage();
			} catch (UnsupportedOperationException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				errorMessage=e.getMessage();
			} catch (NullPointerException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				errorMessage=e.getMessage();
			} catch (NoSuchProviderException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				errorMessage=e.getMessage();
			}
			Optional<String> errorContainer = Optional.ofNullable(errorMessage);
			if ( !(errorContainer.isEmpty())) {
				Security.removeProvider(provider.getName());
				LogUtil.logError(errorMessage, null);
			}
		}

		// listProviders();

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
			//System.out.println("In setSSLContext initialize TLS"); //$NON-NLS-1$
			// sslContext = SSLContext.getInstance("TLS");
			sslContext = SSLContext.getInstance("TLSv1.3"); //$NON-NLS-1$
			
			Optional<X509TrustManager> PKIXtrust = ConfigureTrust.MANAGER.setUp();
			if (PKIXtrust.isEmpty()) {
				LogUtil.logError("Invalid TrustManager Initialization.", null); //$NON-NLS-1$
			} else {
				
				KeyManager[] km = new KeyManager[] { KeyStoreManager.INSTANCE };
				TrustManager[] tm = new TrustManager[] { ConfigureTrust.MANAGER };
				
				sslContext.init(km, tm, new SecureRandom());
				SSLContext.setDefault(sslContext);
				HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return keyManager;
	}
	public ArrayList getList() {
		return EclipseKeyStoreCollection.PILE.getList(keyStore);
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
			//System.out.println("SSLPkcs11Provider   LOGOFF  INVOKATION:");
			//provider.clear();
			
			//System.out.println("SSLPkcs11Provider   LOGOFF   DONE");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	public boolean login() {
		//System.out.println("SSLPkcs11Provider LOGIN");
		Provider provider = Security.getProvider(getPkiProvider());
		if ( provider != null) { 
			
			try {
				provider.clear();
			}  catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
		}
		
		return false;
	}

	@Override
	public String findPkcs11CfgLocation() {
		// TODO Auto-generated method stub
		// AuthenticationPlugin.getDefault().getPreferenceStore()
		// .getString(AuthenticationPreferences.PKCS11_CFG_FILE_LOCATION);

		Pkcs11Location location = new Pkcs11Location();
		Pkcs11LocationImpl.getPkcs11LocationInstance();
		return location.getJavaPkcs11Cfg();
	}

	/*
	 * private static void listProviders() {
	 *
	 * Provider[] providers = Security.getProviders(); for (Provider provider :
	 * providers) { System.out.println("In configurejdk9 PROVIDER:" +
	 * provider.getName()); //$NON-NLS-1$
	 * System.out.println("In configurejdk9 PROVIDER INFO:" +
	 * provider.getInfo()); //$NON-NLS-1$ } }
	 */

	/*
	 * private static boolean isFips() { boolean enabled = false; Provider[]
	 * providers = Security.getProviders(); for (Provider provider : providers) { if
	 * (provider.getName().contains("FIPS")) { //$NON-NLS-1$
	 *
	 * for (Provider.Service service : provider.getServices() ) {
	 * System.out.println("FIPS Algorithm:"+ service.getAlgorithm()); }
	 *
	 * System.out.println("FIPS Provider:" + provider.getName()); //$NON-NLS-1$
	 * enabled = true; }
	 *
	 * } return enabled; }
	 */
}
