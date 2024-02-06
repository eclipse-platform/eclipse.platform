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

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Optional;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.eclipse.core.pki.AuthenticationService;
import org.eclipse.core.pki.util.ConfigureTrust;
import org.eclipse.core.pki.util.LogUtil;

public enum AuthenticationBase implements AuthenticationService {
	INSTANCE;

	protected SSLContext sslContext;
	static KeyStore.PasswordProtection pp = new KeyStore.PasswordProtection("".toCharArray()); //$NON-NLS-1$
	// private static final String javaVersion = System.getProperty("java.version");
	protected boolean is9;
	protected String PKI_PROVIDER = "SunPKCS11"; // or could be FIPS provider :SunPKCS11-FIPS //$NON-NLS-1$
	protected String fingerprint;

	@Override
	public KeyStore initialize(char[] p) {
		// TODO Auto-generated method stub
		pp = new KeyStore.PasswordProtection(p);
		KeyStore keyStore = null;
		String pin = new String(p);
		try {

			LogUtil.logInfo("Before calls to configure JDK"); //$NON-NLS-1$
			// keyStore = (javaVersion.startsWith("1."))?configurejdk8():configurejdk9();
			keyStore = configure();
			try {
				/*
				 * Only load the store if the pin is a valuye other than the default setting of
				 * "pin" Otherwise the store will be preloaded by the default loading of the
				 * keystore, dynamically
				 */
				if (!(pin.equalsIgnoreCase("pin"))) { //$NON-NLS-1$
					keyStore.load(null, pp.getPassword());
					AuthenticationBase.INSTANCE.setSSLContext(keyStore);
					System.out.println("AuthenticationBase SSL context PROTOCOL:" + sslContext.getProtocol()); //$NON-NLS-1$
				}

			} catch (Exception e) {
				/*
				 * An incorrect PiN could have been entered. AND thats OK, they can try again.
				 */
				// TODO Auto-generated catch block
//				IStatus status = new Status (IStatus.ERROR, AuthenticationPlugin.getPluginId(),"Did you enter an invalid PiN? ");
//				AuthenticationPlugin.getDefault().getLog().log(status);
				// e.printStackTrace();
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
		String cfgDirectory = "TBD"; //$NON-NLS-1$
		KeyStore keyStore = null;
		is9 = true;

		// System.out.println("In configure CFG STORED FILE LOC:" +
		// AuthenticationPlugin.getDefault()
		// .getPreferenceStore().getString(AuthenticationPreferences.PKCS11_CONFIGURE_FILE_LOCATION));

		// Pkcs11Location location = new Pkcs11Location();
		// location.getPkcs11LocationInstance();
		// String cfgDirectory = location.getDirectory();

		// String cfgDirectory = AuthenticationPlugin.getDefault().getPreferenceStore()
		// .getString(AuthenticationPreferences.PKCS11_CONFIGURE_FILE_LOCATION);

		configurationDirectory = Optional.ofNullable(System.getProperty("javax.net.ssl.cfgFileLocation")); //$NON-NLS-1$
		if (configurationDirectory.isEmpty()) {
			cfgDirectory = new String("/etc/opensc"); //$NON-NLS-1$
		} else {
			cfgDirectory = configurationDirectory.get().toString();
		}

		if (Files.exists(Paths.get(cfgDirectory))) {
			LogUtil.logInfo("AuthenticationBase - PKCS11 configure  DIR:" + cfgDirectory); //$NON-NLS-1$
			providerContainer=Optional.ofNullable(
							System.getProperty("javax.net.ssl.keyStoreProvider")); //$NON-NLS-1$

			if (providerContainer.isEmpty() ) {
				securityProvider = PKI_PROVIDER;
			} else {
				securityProvider = providerContainer.get().toString();
			}
			prototype = Security.getProvider(securityProvider);
			if (prototype == null) {
				LogUtil.logInfo("In configure  PROVIDER NOT FOUND"); //$NON-NLS-1$
			}

			try {
				Provider provider = prototype.configure(cfgDirectory);

				Security.addProvider(provider);
				keyStore = KeyStore.getInstance("pkcs11"); //$NON-NLS-1$
			} catch (KeyStoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvalidParameterException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnsupportedOperationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NullPointerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		// listProviders();

		return keyStore;
	}

	public boolean isPkcs11Setup() {

		/*
		 * if (AuthenticationPlugin.getDefault().getPreferenceStore()
		 * .getString(AuthenticationPreferences.PKCS11_CFG_FILE_LOCATION) != null) {
		 *
		 * if (!(AuthenticationPlugin.getDefault().getPreferenceStore()
		 * .getString(AuthenticationPreferences.PKCS11_CFG_FILE_LOCATION).isEmpty())) {
		 * Path path = Paths.get(AuthenticationPlugin.getDefault().getPreferenceStore()
		 * .getString(AuthenticationPreferences.PKCS11_CFG_FILE_LOCATION)); if
		 * (Files.notExists(path)) { System.out.println("AuthenticationBase CFG FILE:" +
		 * AuthenticationPlugin.getDefault()
		 * .getPreferenceStore().getString(AuthenticationPreferences.
		 * PKCS11_CFG_FILE_LOCATION)); return true; } } }
		 */
		return false;

	}

	public SSLContext setSSLContext(KeyStore keyStore) {
		CustomKeyManager manager = null;
		KeyManager[] keyManagers = new KeyManager[1];
		TrustManager[] trustManagers = new TrustManager[1];
		try {
			System.out.println("In setSSLContext initialize TLS"); //$NON-NLS-1$
			// sslContext = SSLContext.getInstance("TLS");
			sslContext = SSLContext.getInstance("TLSv1.3"); //$NON-NLS-1$
			Optional<X509TrustManager> PKIXtrust = ConfigureTrust.MANAGER.setUp();
			if (PKIXtrust.isEmpty()) {
				manager = new CustomKeyManager(keyStore, "".toCharArray(), null); //$NON-NLS-1$
			} else {
				manager = (CustomKeyManager) PKIXtrust.get();
			}

			manager.setSelectedFingerprint(getFingerprint());
			keyManagers[0] = manager;
			trustManagers[0] = new CustomTrustManager(keyStore);
			sslContext.init(keyManagers, trustManagers, new SecureRandom());
			SSLContext.setDefault(sslContext);
			HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return sslContext;
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

	public boolean isJavaModulesBased() {
		try {
			Class.forName("java.lang.Module"); //$NON-NLS-1$
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
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
