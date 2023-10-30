/**
 * 
 */
package org.eclipse.pki.jsse;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.eclipse.pki.auth.AuthenticationPlugin;
import org.eclipse.pki.util.LogUtil;


/**
 * A {@link TrustManager} which uses the user supplied (through preference page) trust store
 * or the default 5eyesTrustStore.
 */
public class CdeX509TrustManager implements X509TrustManager {

	//TODO You can enhance MyX509TrustManager to handle dynamic keystore updates. When a checkClientTrusted or checkServerTrusted test fails and does not establish a trusted certificate chain, you can add the required trusted certificate to the keystore. You need to create a new pkixTrustManager from the TrustManagerFactory initialized with the updated keystore. When you establish a new connection (using the previously initialized SSLContext), the newly added certificate will be called to make the trust decisions.
	
	/*
     * The default PKIX X509TrustManager9.  We'll delegate
     * decisions to it, and fall back to the logic in this class if the
     * default X509TrustManager doesn't trust it.
     */
    protected X509TrustManager pkixTrustManager = null;
    
    protected void init() {
    	
    	if (pkixTrustManager != null) {
    		return;
    	}
    	
    	synchronized (this) {
    		
    		if (pkixTrustManager != null) {
        		return;
        	}
    		
    		try {
    			// create a "default" JSSE X509TrustManager.

    			KeyStore ks = AuthenticationPlugin.getDefault().getTrustStore();

    			TrustManagerFactory tmf =
    			TrustManagerFactory.getInstance("PKIX");
    			tmf.init(ks);

    			TrustManager tms [] = tmf.getTrustManagers();

    			/*
    			 * Iterate over the returned trustmanagers, look
    			 * for an instance of X509TrustManager.  If found,
    			 * use that as our "default" trust manager.
    			 */
    			for (int i = 0; i < tms.length; i++) {
    			    if (tms[i] instanceof X509TrustManager) {
    			        pkixTrustManager = (X509TrustManager) tms[i];
    			        return;
    			    }
    			}
    		} catch (NoSuchAlgorithmException e) {
    			LogUtil.logError("Error while initializing " + CdeX509TrustManager.class.getName(), e);
    		} catch (KeyStoreException e) {
    			LogUtil.logError("Error while initializing " + CdeX509TrustManager.class.getName(), e);
    		}
		}
    	
    	
    	
    }
    

	/* (non-Javadoc)
	 * @see javax.net.ssl.X509TrustManager#checkClientTrusted(java.security.cert.X509Certificate[], java.lang.String)
	 */
	public void checkClientTrusted(X509Certificate[] arg0, String arg1)
			throws CertificateException {
		init();
		pkixTrustManager.checkClientTrusted(arg0, arg1);
	}

	/* (non-Javadoc)
	 * @see javax.net.ssl.X509TrustManager#checkServerTrusted(java.security.cert.X509Certificate[], java.lang.String)
	 */
	public void checkServerTrusted(X509Certificate[] arg0, String arg1)
			throws CertificateException {
		init();
		pkixTrustManager.checkServerTrusted(arg0, arg1);
	}

	/* (non-Javadoc)
	 * @see javax.net.ssl.X509TrustManager#getAcceptedIssuers()
	 */
	public X509Certificate[] getAcceptedIssuers() {
		init();
		return pkixTrustManager.getAcceptedIssuers();
	}

}
