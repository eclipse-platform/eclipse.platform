package org.eclipse.ui.pki.util;

import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

/**
 * This class can be used to create an SSLSocketFactory with provided keystore and trustore.
 * You might use this class in the following way:
 * 
 * KeyStore keystore = KeyStoreUtil.getKeyStore("/path/to/cert/bob.p12", "password", KeyStoreFormat.PKCS12)
 * KeyStore truststore = KeyStoreUtil.getKeyStore("/path/to/trustcert/trust.jks", null, KeyStoreFormat.JKS)
 * SSLSocketFactory sslSocketFactory = getSSLSocketFactory(getKeyManagers(keystore, "password", getTrustManagers(truststore))
 *
 */
public abstract class SSLHelper {
	
	public final static String X509_ALGORITHM = "SunX509";
	public final static String SSL_PROTOCOL = "SSL";
	

	public static SSLSocketFactory getSSLSocketFactory(KeyManager[] keyManagers, TrustManager[] trustManagers) {
		return getSSLContext(keyManagers, trustManagers).getSocketFactory();
	}
	
	private static SSLContext getSSLContext(KeyManager[] keyManagers, TrustManager[] trustManagers) {
		SSLContext sslContext = null;
		try {
			sslContext = SSLContext.getInstance(SSL_PROTOCOL);
			
			
			sslContext.init(keyManagers, trustManagers, null);
			return sslContext;
		} catch (KeyManagementException e) {
			LogUtil.logError(e.getMessage(), e);
		} catch (NoSuchAlgorithmException e) {
			LogUtil.logError(e.getMessage(), e);
		}
		return null;
	}
	
	public static TrustManager[] getTrustManagers(KeyStore trustStore) {
		
		TrustManagerFactory trustManagerFactory;
		try {
			
			trustManagerFactory = TrustManagerFactory.getInstance( X509_ALGORITHM );
			trustManagerFactory.init( trustStore );
			return trustManagerFactory.getTrustManagers();
		
		} catch (NoSuchAlgorithmException e) {
			LogUtil.logError(e.getMessage(), e);
		} catch (KeyStoreException e) {
			LogUtil.logError(e.getMessage(), e);
		}
		
		return null;
	}

	public static KeyManager[] getKeyManagers(KeyStore keyStore, char[] password) {
		try {
			KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(X509_ALGORITHM);
			keyManagerFactory.init(keyStore, password);
			return keyManagerFactory.getKeyManagers();
		} catch (Exception e) {
			LogUtil.logError(e.getMessage(), e);
		}
		return null;
	}
}