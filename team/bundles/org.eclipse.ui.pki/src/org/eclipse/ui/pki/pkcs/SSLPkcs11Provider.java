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
package org.eclipse.ui.pki.pkcs;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.ProviderException;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

import javax.net.ssl.SSLContext;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.pki.pkiselection.PKCSSelected;
import org.eclipse.ui.pki.pkiselection.PKIProperties;
import org.eclipse.core.pki.AuthenticationBase;
import org.eclipse.ui.pki.AuthenticationPlugin; 


public class SSLPkcs11Provider {
	private static final SSLPkcs11Provider iprovider = new SSLPkcs11Provider(); 
	private static String alias=null;
	private static SSLContext sslContext;
	private static Object sunPkcs11Instance=null;
	private transient static String pin="pin";
	private static List<String> list = new ArrayList<String>();
	private static boolean isPKCS11on=false;
	private static boolean isInstalled=false;
	private static KeyStore keyStore;
	private static KeyStore targetKeyStore;
	private static KeyStore trustStore;
	private static Provider provider = null;
	private static KeyStore.PasswordProtection pp = new KeyStore.PasswordProtection(pin.toCharArray());;
	private SSLPkcs11Provider(){}
	protected final static int DIGITAL_SIGNATURE=0;
	private static final int KEY_ENCIPHERMENT = 2;
	protected static PKIProperties auth=PKIProperties.getInstance();;
	public boolean isPKCS11Enabled() {return isPKCS11on;}
	public boolean isPKCS11Installed() {return isInstalled;}
	public void setPKCS11on(boolean isPKCS11on) {SSLPkcs11Provider.isPKCS11on = isPKCS11on;}
	public static SSLPkcs11Provider getInstance() {	if (!isPKCS11on) getProvider(); return iprovider;}
	public static Provider getProvider() {
		String tmpAlias=null;
		String logMessage="logger message";
		try {
			
			System.out.println("SSLPkcs11Provider   STARTING PROCESSING");

			/*
			 *  PKCS11 can be dynamic.  So if its already been enabled no PiN is needed and
			 *  the Software will let you hook into it dynamically. 
			 */
			char[] tmpPassword=null;
			if ( !(pin.equalsIgnoreCase("pin"))) {
				// WITH A PIM
				tmpPassword=Arrays.copyOf(pp.getPassword(), pp.getPassword().length);
			} else {
				// WITHOUT  A PIN
				tmpPassword=Arrays.copyOf(pin.toCharArray(), pin.toCharArray().length );	
			}
			
			/*
			 *  Set up the PKI keystore for PKCS11
			 */
			System.out.println("SSLPkcs11Provide init KEYSTORE  FIX THIS  ");
			// @see AUTHENTICATION
			targetKeyStore = AuthenticationBase.INSTANCE.initialize(tmpPassword);
			
			if ( targetKeyStore != null ) {
				
				try {
					/*
					 *   The correct pin must be entered for this to work properly or the pkcs11 manager
					 *   must already be enabled by some other applcation outside of eclipse.
					 */
					//System.out.println("SSLPkcs11Provide init KEYSTORE with pin:"+pin);
					targetKeyStore.load(null, pin.toCharArray());
					
					keyStore =  KeyStore.getInstance("JKS");
					keyStore.load(null,"".toCharArray());
					Enumeration<String> aliasesEnum = targetKeyStore.aliases();
					list = new ArrayList<String>();
					while (aliasesEnum.hasMoreElements()) {
					  tmpAlias = (String) aliasesEnum.nextElement();
					  try {
						  	X509Certificate certificate = (X509Certificate) targetKeyStore.getCertificate(tmpAlias);
						  	/*
						  	 * See if this credential is Digital Signature usage, we only want those, NO KE!
						  	 */
						  	try {
								certificate.checkValidity();
								if ( isDigitalSignature(certificate.getKeyUsage()) ) {
									PrivateKey privateKey = (PrivateKey) targetKeyStore.getKey(tmpAlias, null);
									if ( privateKey != null) {
										alias = tmpAlias;
										list.add( alias );
										isPKCS11on = true;
										isInstalled=true;
										auth.setKeyStorePassword( getPin() );
										keyStore.setCertificateEntry(tmpAlias, certificate);
										//System.out.println("SSLPkcs11Provider  - SunPKCS11  DS ALIAS:"+alias);	
										IStatus status = new Status (IStatus.OK, AuthenticationPlugin.getPluginId(),"      SunPKCS11 provider loaded.");
										AuthenticationPlugin.getDefault().getLog().log(status);
										if ( pin.equals("pin")) {
											System.out.println("SSLPkcs11Provide KEYSTORE  PIN CHECK FIX THIS  ");
											// @see AUTHENTICATION
											AuthenticationBase.INSTANCE.setSSLContext(targetKeyStore);
										}
									}
								 } else {
									 if ( !(isKeyEncipherment(certificate.getKeyUsage())) ) {
										 //System.out.println("SSLPkcs11Provider  -  NOT KE alias:"+tmpAlias );
										 keyStore.setCertificateEntry(tmpAlias, certificate);
									 }
								 }
							} catch (CertificateExpiredException e) {
								// TODO Auto-generated catch block
								//e.printStackTrace();
								//System.out.println("SSLPkcs11Provider  - SunPKCS11 EXPIRED  DS ALIAS:"+tmpAlias);
								//keyStore.setCertificateEntry(tmpAlias+" EXPIRED", certificate);
							}
						  	catch (CertificateNotYetValidException e) {
								// TODO Auto-generated catch block
								//e.printStackTrace();
						  		//System.out.println("SSLPkcs11Provider  - SunPKCS11 INVALID  DS ALIAS:"+tmpAlias);
						  		//keyStore.setCertificateEntry(tmpAlias+" INVALID", certificate);
							}
						} catch (UnrecoverableKeyException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					//sslContext = AuthenticationBase.INSTANCE.setSSLContext(keyStore);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
					
					/*
					 * Keystore is not loaded yet, no password provided and we require pin!
					 */
					keyStore=null;
					//
					//Pkcs11Provider.setConfigurePath("");
					//
					isInstalled=true;
					//System.out.println("SSLPkcs11Provider  ----------- PRESSED CANCEL when pin was requested..");
					PKCSSelected.setPkcs11Selected(false);
					PKCSSelected.setPkcs12Selected(false);
					isPKCS11on = false;
				}
				
			} else {
				logMessage = "Unable to locate a valid pkcs11 provider, searching for SunPKCS11";
				AuthenticationPlugin.getDefault().getLog().log(new Status(IStatus.OK, AuthenticationPlugin.getPluginId()+":SSLPkcs11Provider",logMessage));
				System.out.println("SSLPkcs11Provider    --------------------------   NO SunPKCS11 provider found.");
			}
			

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			// TODO Auto-generated catch block
			//e.printStackTrace();
			/*
			 * 
			 * THE USER PRESSED CANCEL when their pin was being requested
			 * 
			 */
			keyStore=null;
			//
			//Pkcs11Provider.setConfigurePath("");
			//
			isInstalled=true;
			System.out.println("SSLPkcs11Provider  ----------- PRESSED CANCEL when pin was requested..");
			PKCSSelected.setPkcs11Selected(false);
			PKCSSelected.setPkcs12Selected(false);
			isPKCS11on = false;
		}
		return provider;
	}
	
	public String getAlias() {
		return this.alias;
	}
	public List getList() {
		return this.list;
	}
	public static String getPin() {
		return pin;
	}
	public void setPin(String pin) {
		this.pin = pin;
		pp = new KeyStore.PasswordProtection(pin.toCharArray());
	}
	public boolean login() {
		Method logout=null;
		Class<?> noparams[]={};
		//System.out.println("SSLPkcs11Provider LOGIN");
		if ( provider != null) { 
			
			try {
				if ( sunPkcs11Instance != null ) {		
				    logout = sunPkcs11Instance.getClass().getMethod("logout", noparams);
					try {
						logout.invoke(sunPkcs11Instance);
					} catch (IllegalArgumentException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			} catch (NoSuchMethodException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		getProvider();
		return isPKCS11on;
	}
	public void logoff() {
		try {
			//System.out.println("SSLPkcs11Provider   LOGOFF  INVOKATION:");
			//provider.clear();
			isPKCS11on = false;
			isInstalled=false;
			//System.out.println("SSLPkcs11Provider   LOGOFF   DONE");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	public KeyStore getKeyStore() {
//		if ( keyStore == null ) {
//			System.out.println("SSLPkcs11Provider   getting new PROVIDER ");
//			getProvider();
//		}
		//return keyStore;
		return targetKeyStore;
	}
	public static void disable() {
		isPKCS11on=false;
	}
	
	/*
	 * 
	 *  Eclipse software looks for the digitalSignature ONLY, could be multiple in the target keystore 
	 * 
	 *  BIT SET is as follow;
	 *  
	 *  0 = digitalSignature
	 *  1 = nonRepudiations - (modified in subsequent definitions)
	 *  2 = keyEncipherment
	 *  3 = dataEncipherment
	 *  4 = keyAgreement
	 *  5 = keyCertSign
	 *  6 = cRLsign
	 *  7 = decipherOnly
	 */
	private static boolean isDigitalSignature(boolean[] ba) {
		if ( ba != null) {
			
			return ba[DIGITAL_SIGNATURE];
		} else {
			return false;
		}
	}
	private static boolean isKeyEncipherment(boolean[] ba) {
		if ( ba != null) {
			
			return ba[KEY_ENCIPHERMENT];
		} else {
			return false;
		}
	}
	
	public SSLContext getSSLContext() {
		return sslContext;
	}
	public static void setSSLContext(SSLContext sslContext) {
		SSLPkcs11Provider.sslContext = sslContext;
	}
	public static void listProviders() {
		for ( Provider provider : Security.getProviders() ) {
	    	//System.out.println("BEFORE ADDING ANY Provider NAME:"+ provider.getName() );
	    }
	}
	public static void listProviderAlgs(Provider provider ) {
		
		Enumeration<Object> keys = provider.keys();
		while ( keys.hasMoreElements()) {
			//System.out.println( "KEY:"+keys.nextElement());
		}
		
	}
}
