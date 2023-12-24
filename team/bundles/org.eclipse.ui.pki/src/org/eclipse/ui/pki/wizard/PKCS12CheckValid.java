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
package org.eclipse.ui.pki.wizard;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;



public enum PKCS12CheckValid {
	INSTANCE;
	String expirationDate = null;
	public boolean isExpired( KeyStore keyStore, char[] c) {
		boolean isExpired=false;
		
		try {
			Enumeration<String> aliasesEnum = keyStore.aliases();
			while (aliasesEnum.hasMoreElements()) {
				String alias = (String) aliasesEnum.nextElement();	
				//System.out.println("ALIAS:"+ alias);
				X509Certificate certificate = (X509Certificate) keyStore.getCertificate(alias);
				Date goodUntil = certificate.getNotAfter();
				DateFormat sdf = SimpleDateFormat.getDateInstance();
				expirationDate = sdf.format(goodUntil);
				//System.out.println("EXPIRATION:"+ expirationDate);
				
				try {
					certificate.checkValidity();
				} catch (CertificateExpiredException e) {	
					// TODO Auto-generated catch block
					//e.printStackTrace();
					isExpired=true;
					
				} catch (CertificateNotYetValidException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}	
			}
			
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return isExpired;
		//return false;
		
	}
}