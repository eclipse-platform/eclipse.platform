package org.eclipse.pki.wizard;

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