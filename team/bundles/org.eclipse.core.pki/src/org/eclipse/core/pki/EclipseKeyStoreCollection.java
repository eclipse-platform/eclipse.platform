package org.eclipse.core.pki;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import org.eclipse.core.pki.pkiselection.PKIProperties;
import org.eclipse.core.pki.util.LogUtil;


public enum EclipseKeyStoreCollection {
	PILE;
	protected final int DIGITAL_SIGNATURE=0;
	private final int KEY_ENCIPHERMENT = 2;
	protected static PKIProperties pkiProperties=PKIProperties.getInstance();;
	public ArrayList getList(KeyStore keyStore) {
		ArrayList list = new ArrayList<String>();
		try {
			String alias=null;
			Enumeration aliases = keyStore.aliases();
			while (aliases.hasMoreElements()) {
				alias = (String) aliases.nextElement();
				X509Certificate certificate = (X509Certificate) keyStore.getCertificate(alias);
				certificate.checkValidity();
				if ( isDigitalSignature(certificate.getKeyUsage()) ) {
					PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, null);
					if ( privateKey != null) {
						list.add( alias );
					}
				}
			}
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CertificateExpiredException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CertificateNotYetValidException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnrecoverableKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		LogUtil.logInfo("EclipseKeyStoreCollection list COUNT:"+list.size() );
		if (!( list.isEmpty())) {
			LogUtil.logInfo("EclipseKeyStoreCollection list item:"+list.get(0));
		}
		return list;
		
	}
	private boolean isDigitalSignature(boolean[] ba) {
		if ( ba != null) {
			
			return ba[DIGITAL_SIGNATURE];
		} else {
			return false;
		}
	}
	private boolean isKeyEncipherment(boolean[] ba) {
		if ( ba != null) {
			
			return ba[KEY_ENCIPHERMENT];
		} else {
			return false;
		}
	}
}
