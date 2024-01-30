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
import javax.net.ssl.X509TrustManager;


public class CustomTrustManager extends X509ExtendedTrustManager implements TrustManager {
	private KeyStore trustStore;
	private Collection<X509Certificate>trustedCerts;
	protected X509TrustManager trustManager;
	protected CustomTrustManager() {}

	public CustomTrustManager(KeyStore trustStore) {
		super();
		this.trustStore=trustStore;
		DebugLogger.printDebug("CustomTrustManager -- CONSTRUCTOR  ALG:" + TrustManagerFactory.getDefaultAlgorithm()); //$NON-NLS-1$
		try {
			Security.getAlgorithms("PKCS11"); //$NON-NLS-1$
			TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509"); //$NON-NLS-1$
			tmf.init(trustStore);
			TrustManager tms[] = tmf.getTrustManagers();
			for (TrustManager tm : tms) {
				if (tm instanceof X509TrustManager) {
					trustManager = (X509TrustManager) tm;
					break;
				}
			}
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		// TODO Auto-generated method stub
		trustManager.checkClientTrusted(chain, authType);
	}

	@Override
	public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		// TODO Auto-generated method stub
		trustManager.checkClientTrusted(chain, authType);
	}

	@Override
	public X509Certificate[] getAcceptedIssuers() {
		// TODO Auto-generated method stub
		X509Certificate X509cert=null;
		X509Certificate[] X509certs=null;

		this.trustedCerts = new ArrayList<>();

		String alias=null;
		try {
			Enumeration<String> aliases=this.trustStore.aliases();
			while ( aliases.hasMoreElements() ) {
				alias = aliases.nextElement();
				if (alias.startsWith("IC")) { //$NON-NLS-1$
					X509cert = (X509Certificate) this.trustStore.getCertificate(alias);
					trustedCerts.add(X509cert);
					DebugLogger.printDebug("CustomTrustManager-FOUND TRUSTORE FOR IC"); //$NON-NLS-1$
				}
			}
			DebugLogger.printDebug("CustomTrustManager-COMPLETED TRUSTSTORE SEARCH"); //$NON-NLS-1$
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
	public void checkClientTrusted(X509Certificate[] chain, String authType, Socket arg2) throws CertificateException {
		// TODO Auto-generated method stub
		trustManager.checkClientTrusted(chain, authType);
	}

	@Override
	public void checkClientTrusted(X509Certificate[] arg0, String arg1, SSLEngine arg2) throws CertificateException {
		// TODO Auto-generated method stub
		DebugLogger.printDebug("CustomTrustManager -- checkClientTrusted"); //$NON-NLS-1$
	}

	@Override
	public void checkServerTrusted(X509Certificate[] x509incoming, String arg1, Socket socket) throws CertificateException {
		DebugLogger.printDebug("CustomTrustManager checkServerTrusted  based on socket"); //$NON-NLS-1$
		if (x509incoming != null) {
			DebugLogger.printDebug("CustomTrustManager checkServerTrusted  INCOMING SIZE:" + x509incoming.length); //$NON-NLS-1$
			for(X509Certificate x509 :  x509incoming) {
				DebugLogger
						.printDebug("CustomTrustManager checkServerTrusted  INCOMING:" + x509.getSubjectDN().getName()); //$NON-NLS-1$
				x509.checkValidity();

			}
			return;
		}

	}

	@Override
	public void checkServerTrusted(X509Certificate[] arg0, String arg1, SSLEngine arg2) throws CertificateException {
		// TODO Auto-generated method stub
		DebugLogger.printDebug("CustomTrustManager checkServerTrusted  with SSLEngine"); //$NON-NLS-1$
	}

}
