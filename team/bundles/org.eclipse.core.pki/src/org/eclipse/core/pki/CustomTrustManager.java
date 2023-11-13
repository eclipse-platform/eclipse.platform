package org.eclipse.core.pki;

import java.net.Socket;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;


public class CustomTrustManager extends X509ExtendedTrustManager implements TrustManager {
	private KeyStore trustStore;
	private Collection<X509Certificate>trustedCerts;
	protected CustomTrustManager() {}
	
	public CustomTrustManager(KeyStore trustStore) {
		super();
		this.trustStore=trustStore;
		DebugLogger.printDebug("CustomTrustManager -- CONSTRUCTOR  ALG:"+TrustManagerFactory.getDefaultAlgorithm());
		try {
			
			//Security.getAlgorithms("SunPKCS11-NSS-FIPS");
			//Security.getAlgorithms("SunPKCS11");
			Security.getAlgorithms("PKCS11");
			TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
			tmf.init(trustStore);
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
		// TODO Auto-generated method stub
		DebugLogger.printDebug("CustomTrustManager --  checkClientTrusted");
	}

	public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
		// TODO Auto-generated method stub
		DebugLogger.printDebug("CustomTrustManager --checkServerTrusted    STRING ASRG:"+arg1);
	}

	public X509Certificate[] getAcceptedIssuers() {
		// TODO Auto-generated method stub
		X509Certificate X509cert=null;
		X509Certificate[] X509certs=null;
		DebugLogger.printDebug("CustomTrustManager------------------------------ getAcceptedIssuers");
		
		this.trustedCerts = new ArrayList<X509Certificate>();
		
		String alias=null;
		try {
			Enumeration<String> aliases=this.trustStore.aliases();
			while ( aliases.hasMoreElements() ) {
				alias = aliases.nextElement();
				if ( alias.startsWith("IC")) {
					X509cert = (X509Certificate) this.trustStore.getCertificate(alias);
					trustedCerts.add(X509cert);
					DebugLogger.printDebug("CustomTrustManager-FOUND TRUSTORE FOR IC");
				}
			}
			DebugLogger.printDebug("CustomTrustManager-COMPLETED TRUSTSTORE SEARCH");	
			int i = 0;
			X509certs = new X509Certificate[ trustedCerts.size() ];
			Iterator<X509Certificate> it = trustedCerts.iterator();
			while ( it.hasNext() ) {
				X509certs[i]=it.next();
				i++;
			}
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return X509certs;
	}

	@Override
	public void checkClientTrusted(X509Certificate[] arg0, String arg1, Socket arg2) throws CertificateException {
		// TODO Auto-generated method stub
		DebugLogger.printDebug("CustomTrustManager -- checkClientTrusted");
	}

	@Override
	public void checkClientTrusted(X509Certificate[] arg0, String arg1, SSLEngine arg2) throws CertificateException {
		// TODO Auto-generated method stub
		DebugLogger.printDebug("CustomTrustManager -- checkClientTrusted");
	}

	@Override
	public void checkServerTrusted(X509Certificate[] x509incoming, String arg1, Socket socket) throws CertificateException {
		// TODO Auto-generated method stub
		boolean trusted=true;
		DebugLogger.printDebug("CustomTrustManager checkServerTrusted  based on socket");
		if (x509incoming != null) {
			DebugLogger.printDebug("CustomTrustManager checkServerTrusted  INCOMING SIZE:"+ x509incoming.length);
			for(X509Certificate x509 :  x509incoming) {
				DebugLogger.printDebug("CustomTrustManager checkServerTrusted  INCOMING:"+ x509.getSubjectDN().getName() );
				x509.checkValidity();
				
			}
			return;
		}
		
	}

	@Override
	public void checkServerTrusted(X509Certificate[] arg0, String arg1, SSLEngine arg2) throws CertificateException {
		// TODO Auto-generated method stub
		DebugLogger.printDebug("CustomTrustManager checkServerTrusted  with SSLEngine");
	}

}