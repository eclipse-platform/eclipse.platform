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
package org.eclipse.ui.pki.util;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.security.auth.login.FailedLoginException;

import org.eclipse.core.pki.util.KeyStoreFormat;

//import sun.security.pkcs11.wrapper.PKCS11Exception;

public class KeyStoreUtil {


	/**
	 * Returns a KeyStore object loaded from provided file location and decrypted with given password
	 * @param fileLocation
	 * @param password
	 * @param format "JKS", "PKCS12"
	 * @return
	 * @throws KeyStoreException 
	 * @throws IOException 
	 * @throws CertificateException 
	 * @throws NoSuchAlgorithmException 
	 * @throws SecurityException 
	 * @throws NoSuchMethodException 
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws ClassNotFoundException 
	 */
	protected static boolean isKeyStoreInitialized=false;
	protected final static int DIGITAL_SIGNATURE=0;
	protected static KeyStore keystore=null;
	public static KeyStore getKeyStore(String fileLocation, String password, KeyStoreFormat format) 
	throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException  {
		InputStream in = null;
		try {
			
			//if selection is pkcs11 then pkcs11 provider.
			//Put code here instead of in pkcs11 widget action so that an error thrown can be 
			//displayed on the user interface.
			/*
			 *    IF we are pkcs11, this method SHOULD NEVER be called.
			 */
//			if (KeyStoreFormat.PKCS11.getValue().equals(format.getValue())){
//				Pkcs11Provider provider = new Pkcs11Provider();	
//				provider.setSecurityProvider(fileLocation);							
//			}
			
			in = new FileInputStream(fileLocation);
			in = new BufferedInputStream(in);
			return getKeyStore(in, password, format);
		} finally {
			try {
				in.close();
			} catch (Throwable t) {}
		}	
	}
	
	/**
	 * Returns a KeyStore object loaded from provided {@link InputStream} and decrypted with given password
	 * @param in
	 * @param password
	 * @param format "JKS", "PKCS12", "PKCS11"
	 * @return
	 * @throws KeyStoreException 
	 * @throws NoSuchAlgorithmException 
	 * @throws CertificateException 
	 * @throws IOException 
	 */
	public static KeyStore getKeyStore(InputStream in, String password, KeyStoreFormat format) 
	throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
				
		keystore = KeyStore.getInstance(format.getValue());
		char pwd[] = null;
		if(password != null)
			pwd = password.toCharArray();
		
		keystore.load(in, pwd);
		return keystore;
		 
	}
	public static KeyStore getKeyStore(KeyStoreFormat format) 
			throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, NoSuchProviderException {
		
			String pin="";
			KeyStore.PasswordProtection pp = new KeyStore.PasswordProtection(pin.toCharArray());	
			keystore = KeyStore.getInstance("pkcs11", "SunPKCS11" );
			try {
				keystore.load(null, pp.getPassword());
				isKeyStoreInitialized=true;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				//System.out.println("KeyStoreUtil ------ The user elected to press cancel, KEYSOTRE is NOT initialized.");
				keystore=null;
			}
			return keystore;
	}
	public static KeyStore getKeyStore() { return keystore; }
	public static Hashtable<X509Certificate, PrivateKey> getCertificates(KeyStore keyStore) {
		
		Hashtable<X509Certificate, PrivateKey> table = new Hashtable<X509Certificate, PrivateKey>();
		PrivateKey privateKey=null;
		 
		try {
			if ( isKeyStoreInitialized ) {
				Enumeration<String> aliasesEnum = keyStore.aliases();
				while (aliasesEnum.hasMoreElements())
				{
				      String alias = (String) aliasesEnum.nextElement();
				      X509Certificate certificate = (X509Certificate) keyStore.getCertificate(alias);
					  try {
						if ( isDigitalSignature(certificate.getKeyUsage()) ) {
			    	  		privateKey = (PrivateKey) keyStore.getKey(alias, null);
							if ( privateKey != null) {
								table.put(certificate, privateKey);
							}
				    	 }   
					  } catch (UnrecoverableKeyException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return table;
		
	}
	public static ArrayList<String>getAliases(KeyStore keyStore) {
		
		ArrayList<String>aliasList = new ArrayList<String>();
		PrivateKey privateKey=null;
		try {
			Enumeration<String> aliasesEnum = keyStore.aliases();
			while (aliasesEnum.hasMoreElements())
			{ 
			      String alias = (String) aliasesEnum.nextElement();
			      X509Certificate certificate = (X509Certificate) keyStore.getCertificate(alias);
				  try {
					if ( isDigitalSignature(certificate.getKeyUsage()) ) {
//		    	  		privateKey = (PrivateKey) keyStore.getKey(alias, null);
//						if ( privateKey != null) {
//							aliasList.add(alias);
//						}
						aliasList.add(alias);
			    	 }   
					
				  } catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				//System.out.println("END OF WHILE STATUEM");  
			}
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return aliasList;
		
	}
	private static boolean isDigitalSignature(boolean[] ba) {
		if ( ba != null) {
			return ba[DIGITAL_SIGNATURE];
		} else {
			return false;
		}
	}
	
	public static boolean checkUserKeystorePass(String certPath, String password, String certType)
    {    			
		StringBuilder message = new StringBuilder();
	    message.append( "Problem reading your certificate.  \n\r \n\r" );
    	
	    try {

				KeyStore keyStore = getKeyStore(certPath, password, KeyStoreFormat.valueOf(certType) );
				getAliases( keyStore);
                return true;
    	} 
	    
	    catch (KeyStoreException e) {
			message.append( "The selected file does not appear " );
			message.append( "to be a valid PKCS file.  Please " );
			message.append( "select a different file and/or " );
			message.append( "check the logs for more information." ); 
			System.err.printf("%s\n", message.toString());
    	}
    	
    	catch (NoSuchAlgorithmException e) {
			message.append( "An unexpected error '" );
			message.append( e.getClass().getName() );
			message.append( "' occurred: " );
			message.append( e.getMessage() );
			message.append( " Please select a different file and/or " );
			message.append( "check the logs for more information." );
			System.err.printf("%s\n", message.toString());
    	}
    	
    	catch (CertificateException e) {
			message.append( "Either your password was incorrect or the " );
			message.append( "the selected file is corrupt. Please try " );
			message.append( "a different password or PKCS file." ); 
			System.err.printf("%s CertificateException: %s\n", message.toString(), e.getMessage());
    	}
    	
    	catch (IOException e) {
    		if ( e.getCause().toString().contains("FailedLoginException")) {
    			message.append( "\tYou entered an incorrect password. \n\r" );
    			message.append( "\tPlease check your password and re-enter it. \n\r  \n\r" );
    			System.err.printf("%s IOException: %s\n", message.toString(), e.getMessage());
    		} else {
	    		
	    		message.append( "Either your password was incorrect or the " );
			    message.append( "selected file is corrupt. Please try " );
			    message.append( "a different password or PKCS file." ); 
			    System.err.printf("%s IOException: %s\n", message.toString(), e.getMessage());
    		}
    	}
 
	    //System.out.println("KeyStoreUtil ----------checkUserKeystorePass   NEEDS TO RETURN FALSE and its NOT");
    	return false;
    }	
}