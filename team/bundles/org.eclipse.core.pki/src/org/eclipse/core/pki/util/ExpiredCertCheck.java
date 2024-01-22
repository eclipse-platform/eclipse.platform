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
package org.eclipse.core.pki.util;


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;

import javax.security.auth.x500.X500Principal;


public enum ExpiredCertCheck {
	INSTANCE;

	// private String expiredDN=null;
	public String getDate(String userKeyStoreLocation, char[] pin ) {
		KeyStore keyStore = getStore(userKeyStoreLocation, pin);
		ExpiredX509CertificateData expiredCert=null;
		ArrayList <ExpiredX509CertificateData>expiredCertList=new ArrayList<>();
		// Date today = Calendar.getInstance().getTime();
		String expirationDate = null;
		try {
			Enumeration<String> aliasesEnum = keyStore.aliases();
			while (aliasesEnum.hasMoreElements()) {
				String alias = aliasesEnum.nextElement();
				//System.out.println("ALIAS:"+ alias);
				X509Certificate certificate = (X509Certificate) keyStore.getCertificate(alias);
				Date goodUntil = certificate.getNotAfter();
				DateFormat sdf = DateFormat.getDateInstance();
				expirationDate = sdf.format(goodUntil);
				//System.out.println("EXPIRATION:"+ expirationDate);

				try {
					certificate.checkValidity();
				} catch (CertificateExpiredException e) {
					BigInteger sn = certificate.getSerialNumber();
					X500Principal subject = certificate.getSubjectX500Principal();
					X500Principal issuer = certificate.getIssuerX500Principal();
					expiredCert = new ExpiredX509CertificateData();
					expiredCert.setAlias(alias);
					expiredCert.setCertLocation(userKeyStoreLocation);
					expiredCert.setExpirationDate(expirationDate);
					expiredCert.setDistinguishedName(subject.getName());
					expiredCert.setIssuedBy(issuer.getName());
					expiredCert.setSerialNumber(sn.toString());

					// TODO Auto-generated catch block
					//e.printStackTrace();
					//System.out.println("ExpiredCertCheck found an Expired Cert");
					// IStatus status = new Status (IStatus.WARNING,
					// AuthenticationPlugin.getPluginId()," EXPIRED CERT..."+expirationDate);
					// ILog logger = Platform.getLog(this.getClass());
					// logger.log(status);
				} catch (CertificateNotYetValidException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				expiredCertList.add(expiredCert);

			}

		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		IWorkspace workspace = ResourcesPlugin.getWorkspace();
//		IResource resource = workspace.getRoot();
//
//		try {
//			IMarker[] markers = resource.findMarkers(IMarker.MARKER, true, IResource.SHALLOW);
//			for (IMarker m : markers) {
//				//System.out.println("ExpiredCertCheck - MARKER MSG:" + m.getAttribute(IMarker.MESSAGE));
//				if ( m.getAttribute(IMarker.MESSAGE).toString().contains("DN")) {
//					expiredDN = new String("EXPIRED");
//				}
//			}
//
//
//			if ( expiredDN == null ) {
//				IMarker marker = null;
//				ExpiredX509CertificateData data = null;
//				for (Object d : expiredCertList.toArray()) {
//					data = (ExpiredX509CertificateData) d;
//					marker = resource.createMarker(IMarker.PROBLEM);
//					marker.setAttribute(IMarker.MESSAGE, "Expired Certificate in your KeyStore;");
//					marker.setAttribute(IMarker.LOCATION, data.getCertLocation());
//					marker = resource.createMarker(IMarker.PROBLEM);
//					marker.setAttribute(IMarker.MESSAGE, "DN:" + data.getDistinguishedName());
//					marker = resource.createMarker(IMarker.PROBLEM);
//					marker.setAttribute(IMarker.MESSAGE, "EXPIRATION DATE:" + data.getExpirationDate());
//				}
//			}
//
//		} catch (CoreException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		return ("EXPIRATION:" + expirationDate); //$NON-NLS-1$
	}
	public KeyStore getStore(String userKeyStoreLocation, char[] pin) {
		KeyStore keyStore=null;
		try {
			FileInputStream fis = new FileInputStream(userKeyStoreLocation);
			keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
			keyStore.load(fis, pin);

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CertificateException e) {
			// TODO Auto-generated catch block
			System.out.println("ExpiredCertCheck found an Expired Cert"); //$NON-NLS-1$
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return keyStore;

	}

}
