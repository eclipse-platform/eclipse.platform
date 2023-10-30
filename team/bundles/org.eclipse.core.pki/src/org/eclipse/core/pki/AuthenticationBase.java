package org.eclipse.core.pki;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;


public enum AuthenticationBase implements AuthenticationService {
	INSTANCE;
	protected static SSLContext sslContext;
	static KeyStore.PasswordProtection pp = new KeyStore.PasswordProtection("".toCharArray());
	//private static final String javaVersion = System.getProperty("java.version");
	protected static boolean is9;
	protected static String PROVIDER="SunPKCS11"; // or could be FIPS provider :SunPKCS11-FIPS
	protected static String fingerprint;
	public KeyStore initialize(char[] p) {
		// TODO Auto-generated method stub
		pp = new KeyStore.PasswordProtection(p);
		KeyStore keyStore=null;
		String pin=new String(p);
		try {
			
			DebugLogger.printDebug("Before calls to configure JDK");
			//keyStore = (javaVersion.startsWith("1."))?configurejdk8():configurejdk9();
			keyStore = (isJavaModulesBased())?configurejdk9():configurejdk8();
			try {
				/*
				 *  Only load the store if the pin is a valuye other than the default setting of "pin"
				 *  Otherwise the store will be preloaded by the default loading of the keystore, dynamically
				 */
				if (!(pin.equalsIgnoreCase("pin"))) {
					keyStore.load(null, pp.getPassword());
					this.setSSLContext(keyStore);
					DebugLogger.printDebug("AuthenticationBase SSL context PROTOCOL:"+sslContext.getProtocol());
				}
			
			} catch (Exception e) {
				/*
				 *   An incorrect PiN could have been entered.  AND thats OK, they can try again.
				 */
				// TODO Auto-generated catch block
//				IStatus status = new Status (IStatus.ERROR, AuthenticationPlugin.getPluginId(),"Did you enter an invalid PiN? ");
//				AuthenticationPlugin.getDefault().getLog().log(status);
				//e.printStackTrace();
			}
			//System.setProperty("javax.net.ssl.keyStoreProvider", "SunPKCS11");
			System.setProperty("javax.net.ssl.keyStoreProvider", "SunPKCS11");
			System.setProperty("https.protocols","TLSv1.1,TLSv1.2,TLSv1.3");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		/*
		 * TDB:  TODO:    Set the context AFTER you set the keystore...
		 */
		
//		this.sslContext= setSSLContext(keyStore );
		return keyStore;
	}
	private static KeyStore configurejdk8() {
		KeyStore keyStore=null;
		Provider provider=null; 
		try {
			
			DebugLogger.printDebug("In configurejdk8");
			if ( Security.getProvider("SunPKCS11") != null ) {
				provider = Security.getProvider("SunPKCS11");
			} else {
				ClassLoaderPKCS11 p11 = new ClassLoaderPKCS11();
				try {
					provider = (Provider) p11.loadClass();
					Security.addProvider(provider);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
				}
			}
			keyStore = KeyStore.getInstance( "PKCS11", provider);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			//throw new KeyStoreException("You have specified an invalid location, Is pkcs11 installed?");
		} 
		
		return keyStore;
	}
	private static KeyStore configurejdk9() {
		
		KeyStore keyStore=null;
		is9=true;
		DebugLogger.printDebug("In configurejdk9");
		Pkcs11Location location = new Pkcs11Location();
		location.getPkcs11LocationInstance();
		String cfgDirectory = location.getDirectory();
		//listProviders();
		if ( isFips() ) {
			//PROVIDER="SunPKCS11";
			PROVIDER="SunPKCS11-NSS-FIPS";
			//cfgDirectory = "/etc/java/java-17-openjdk/java-17-openjdk-17.0.8.0.7-2.el8.x86_64/conf/nss.fips.cfg";
		}
		DebugLogger.printDebug("In configurejdk9  DIR:"+cfgDirectory);
		try {
			
			
			
			//Provider prototype = Security.getProvider("SunPKCS11");
			Provider prototype = Security.getProvider(PROVIDER);
			
			if (prototype == null) {
				DebugLogger.printDebug("In configurejdk9  PROVIDER NOT FOUND");
				Path path = Paths.get(cfgDirectory);
			}
			Provider provider = prototype.configure(cfgDirectory);
			Security.addProvider(provider);
			keyStore = KeyStore.getInstance("pkcs11");
			//listProviders();
			DebugLogger.printDebug("In configurejdk9 KEYSTORE LOADED" );
		} catch (Exception e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			DebugLogger.printDebug("In configurejdk9 EXCEPTION:"+e.getMessage());
		}
		return keyStore;
	}
	public static SSLContext setSSLContext(KeyStore keyStore) {
		KeyManager[] keyManagers=new KeyManager[1];
		TrustManager[] trustManagers=new TrustManager[1];
		try {
			DebugLogger.printDebug("In setSSLContext initialize TLS");
			//sslContext = SSLContext.getInstance("TLS");
			sslContext = SSLContext.getInstance("TLSv1.3");
			CustomKeyManager manager = new CustomKeyManager(keyStore, "".toCharArray(), null);
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
	public static String getFingerprint() {
		return fingerprint;
	}
	public static void setFingerprint(String fingerprint) {
		AuthenticationBase.fingerprint = fingerprint;
	}
	public static KeyManager getCustomKeyManager(KeyStore keyStore) {
		CustomKeyManager keyManager=null;	
		try {	
			keyManager = new CustomKeyManager(keyStore, "".toCharArray(), null);
			keyManager.setSelectedFingerprint( getFingerprint() );
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return keyManager;
	}
	public boolean isJavaModulesBased() {
		try {
			Class.forName("java.lang.Module");
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}
	@Override
	public String findPkcs11CfgLocation() {
		// TODO Auto-generated method stub
		Pkcs11Location location = new Pkcs11Location();
		location.getPkcs11LocationInstance();
		return location.getJavaPkcs11Cfg();
	}
	private static void listProviders() {
		
		Provider[] providers = Security.getProviders();
		for( Provider provider:providers) {
			DebugLogger.printDebug("In configurejdk9 PROVIDER:"+ provider.getName());
			DebugLogger.printDebug("In configurejdk9 PROVIDER INFO:"+ provider.getInfo());
		}
	}
	private static boolean isFips() {
		boolean enabled=false;
		Provider[] providers = Security.getProviders();
		for( Provider provider:providers) {
			if ( provider.getName().contains("FIPS")) {	
				/*
				 * for (Provider.Service service : provider.getServices() ) {
				 * DebugLogger.printDebug("FIPS Algorithm:"+ service.getAlgorithm()); }
				 */
				DebugLogger.printDebug("FIPS Provider:"+ provider.getName());
				enabled = true;
			}
			
		}
		return enabled;
	}
}
