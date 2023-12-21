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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import org.eclipse.core.pki.util.LogUtil;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.ui.pki.AuthenticationPlugin;
import org.eclipse.ui.pki.pkcs.VendorImplementation;
import org.eclipse.ui.pki.pkiselection.PKCSSelected;
import org.eclipse.ui.pki.pkiselection.PKCSpick;
import org.eclipse.ui.pki.pkiselection.PKIProperties;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * 
 *
 */
public class PKISecureStorage extends AbstractUIPlugin {
	
	private ISecurePreferences securePreference = null;
	private ISecurePreferences node = null;
	
	private transient String certPassPhrase;
	private String certificateLocation;
	protected PKIProperties auth=PKIProperties.getInstance();
    private static final String JAVA_SSL_USER_KEY_STORE_PATH_KEY = "javax.net.ssl.keyStore";
    private static final String JAVA_SSL_USER_KEY_STORE_TYPE_KEY = "javax.net.ssl.keyStoreType";
    private static final String JAVA_SSL_USER_KEY_STORE_PASS_KEY = "javax.net.ssl.keyStorePassword";
    
    protected final String PKI_LOCATION = "pki_location";
    protected final String PKCS11_LOCATION = "pkcs11_location";
    protected final String PKCS12_LOCATION = "pkcs12_location";
    protected final String PKI_TYPE = "pki_type";
    protected final String PKI_STATUS = "pki_status";
    protected final String PKI_PIN = "pki_pin";
    protected final String PKI_PASSPHRASE = "pki_passphrase";
    protected final String PKI_SAVED = "pki_saved";
    protected final String PKI_PROVIDER = "pki_provider";
    protected final String PKI_USER_STORE_NODE = "org.eclipse.pki.util/userKeyStore";
    protected final String SVNSaveAuthorizationInfo = "Save Authorization Info";
    	
	public PKISecureStorage(){
		this.securePreference = SecurePreferencesFactory.getDefault();
		this.node = securePreference.node(PKI_USER_STORE_NODE);
	}
	
	/**
	 * Stores the pki information in secure storage.
	 * @param authenticationInfo the authentication plugin containing the information input by user.
	 */
	public void storePKI(AuthenticationPlugin authenticationInfo){		
		try {
			
			String userKeyStoreLocation = authenticationInfo.getCertificatePath().trim();
			String passPhrase = authenticationInfo.getCertPassPhrase().trim();
			if (  PKCSpick.getInstance().isPKCS11on() ) {
				node.remove(PKCS12_LOCATION);
				node.remove(PKI_PASSPHRASE);
				node.put(PKI_TYPE, "PKCS11", false);
				
				userKeyStoreLocation = "pkcs11";
				node.put(PKCS11_LOCATION, userKeyStoreLocation, false);
				node.put(PKI_PIN, passPhrase, true);
			}
			if (  PKCSpick.getInstance().isPKCS12on() ) {
				String status = ExpiredCertCheck.INSTANCE.getDate(userKeyStoreLocation, passPhrase.toCharArray());
				node.remove(PKCS11_LOCATION);
				node.remove(PKI_PIN);
				node.put(PKI_TYPE, "PKCS12", false);
				node.put(PKI_STATUS, status, false);
				node.put(PKCS12_LOCATION, userKeyStoreLocation, false);
				node.put(PKI_PASSPHRASE, passPhrase, true);
			}
			node.put(PKI_SAVED, "true", false);
			try {
				node.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				LogUtil.logError(SVNSaveAuthorizationInfo, e);
			}
		} catch (StorageException e) {
			LogUtil.logError(SVNSaveAuthorizationInfo, e);
		} 
	}	

	/**
	 * The secure storage node containing the information.
	 * @return the secure storage node.
	 */
	public ISecurePreferences getNode(){
		return node;
	}
	
	/**
	 * Test if pki is saved in secure storage.
	 * @return true if saved or false if not saved.
	 */
	public boolean isPKISaved(){
		boolean saved = false;
		try {
			if("true".equals(node.get(PKI_SAVED, "false"))){
				saved = true;
			}
		} catch (StorageException e) {
			LogUtil.logError(SVNSaveAuthorizationInfo, e);
		}
		return saved;
	}
	public String getPkiType() {
		String type = null;
		 try {
			type = node.get(PKI_TYPE, "none");
		} catch (StorageException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 return type;
	}
	
	/**
	 * Creates the user key store from secure storage.
	 * @return the user key store from secure storage.
	 */
	public KeyStore getUserKeyStore(){
		String certLocation = null;	
		KeyStore keyStore = null;
		String pkiType=null;
		
		try {
			if ( !(node.get(PKI_TYPE, "none").equals("none"))) {
				pkiType = node.get(PKI_TYPE, "none");
			} else {
				/*
				 * TODO: make backwards compatible, default to pkcs12, because pkiType came to the game late.
				 */
				pkiType = "PKCS12";
			}
			
			if ((  PKCSpick.getInstance().isPKCS11on() ) && (( pkiType.equals("PKCS11")))) {
				VendorImplementation vendorImplementation = VendorImplementation.getInstance(true);
				if ( vendorImplementation.isInstalled() ) {	
					vendorImplementation.login(node.get(PKI_PIN, "none"));
					keyStore = VendorImplementation.getInstance().getKeyStore();
					AuthenticationPlugin.getDefault().setUserKeyStoreSystemProperties(keyStore);
					auth.load();
				} else {
					System.out.println("PKISecureStorage --   PKCS11 NOT enabled");
				}
			} else if ((  PKCSpick.getInstance().isPKCS12on() ) && (( pkiType.equals("PKCS12")))) {
				certLocation = node.get(PKCS12_LOCATION, "none");
				if (certLocation.equals("none") ) {
					certLocation = node.get(PKI_LOCATION, "none");
				}
				try {
					/*
					 *  NOTE:  The following code will pop up a prompt for secure storage password on UNIX only, but NOT windoz.
					 *  1.  Should we be requesting password on windows too?
					 *  2.  Perhaps, after getting password ONCE, then subsequently just load and go.
					 *  3.  There is a catch block below that logs when the cancel button is pressed on the password dialog.
					 */
					certPassPhrase = node.get(PKI_PASSPHRASE, "none");
					certificateLocation = certLocation;
					
					try {
						keyStore = KeyStoreUtil.getKeyStore(certLocation, certPassPhrase, PKCSSelected.getKeystoreformat()); /*KeyStoreFormat.PKCS12*/
						//System.out.println("PKISecureStorage - GetUserkeystore after  where is password");
					} catch (KeyStoreException e){
						LogUtil.logError(SVNSaveAuthorizationInfo, e);
					} catch (NoSuchAlgorithmException e){
						LogUtil.logError(SVNSaveAuthorizationInfo, e);
					} catch (CertificateException e){
						LogUtil.logError(SVNSaveAuthorizationInfo, e);
					} catch (IOException e){
						LogUtil.logError(SVNSaveAuthorizationInfo, e);
					} catch (IllegalArgumentException e) {
						LogUtil.logError(SVNSaveAuthorizationInfo, e);
					} catch (SecurityException e) {
						LogUtil.logError(SVNSaveAuthorizationInfo, e);
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
					//System.out.println("PKISecureStorage -  I PRESSED CANCEL When prompted for SS password....");
					
					IStatus status = new Status (IStatus.OK, AuthenticationPlugin.getPluginId(),"   Canceled Secure Storage Loading...");
					AuthenticationPlugin.getDefault().getLog().log(status);
				}
			}
		} catch (Exception e) {
			LogUtil.logError(SVNSaveAuthorizationInfo, e);
		}
		return keyStore;
	}
	public void loadUpPKI( ) {
		
		try {
			if (  PKCSpick.getInstance().isPKCS11on() ) {
				this.certificateLocation=node.get(PKCS11_LOCATION, "none");
				this.certPassPhrase = node.get(PKI_PIN, "none");
				if ( this.certificateLocation.equalsIgnoreCase("none")) {
					this.certificateLocation = "pkcs11";
				}
			}
			if (  PKCSpick.getInstance().isPKCS12on() ) {
				this.certificateLocation=node.get(PKCS12_LOCATION, "none");
				if ( this.certificateLocation.equals("none") ) {
					this.certificateLocation=node.get(PKI_LOCATION, "none");
				}
				//  NOTE:  Is it still "none"  ????  then set it to something.
				if ( this.certificateLocation.equalsIgnoreCase("none")) {
					/*
					 * NOTE:  If you change it to anything other than "none" make sure you also start checking for that 
					 * value ALL the code that uses th elocation.   FYI;  Best choice is "none"...
					 */
				}
			}
			AuthenticationPlugin.getDefault().setCertificatePath( this.certificateLocation );
			AuthenticationPlugin.getDefault().setCertPassPhrase(getCertPassPhrase());
			AuthenticationPlugin.getDefault().setUserKeyStore(this.getUserKeyStore());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
    /**
     * Sets the system properties for the PKI only. Does not set the user key store in AuthenticationPlugin.
     */
    public void setPKISystemProperties(){
		KeyStore userKeyStore = getUserKeyStore();
		String userKeyStoreLocation = certificateLocation;
		
		if ( PKCSpick.getInstance().isPKCS11on() ) {
			if( (certPassPhrase != null) && (userKeyStoreLocation != null) &&
						(!certificateLocation.equals("none"))  && (!certPassPhrase.equals("none"))) {
				System.setProperty(JAVA_SSL_USER_KEY_STORE_PATH_KEY, "pkcs11");
				System.setProperty(JAVA_SSL_USER_KEY_STORE_PASS_KEY, certPassPhrase);
				AuthenticationPlugin.getDefault().setCertificatePath( userKeyStoreLocation );
			}
		} else if ( PKCSpick.getInstance().isPKCS12on() ) {
			if(userKeyStore != null && userKeyStore.getType() != null && certPassPhrase != null && userKeyStoreLocation != null 
				&& !certificateLocation.equals("none") && !certPassPhrase.equals("none")) {
				System.setProperty(JAVA_SSL_USER_KEY_STORE_PATH_KEY, userKeyStoreLocation);
				System.setProperty(JAVA_SSL_USER_KEY_STORE_TYPE_KEY, userKeyStore.getType());
				System.setProperty(JAVA_SSL_USER_KEY_STORE_PASS_KEY, certPassPhrase);
				AuthenticationPlugin.getDefault().setCertificatePath( userKeyStoreLocation );
			}
		}
    }

	/**
	 * @return the certificate password
	 */
	public String getCertPassPhrase() {
		try {
			certPassPhrase = node.get(PKI_PASSPHRASE, "none");
		} catch (StorageException e) {
			//LoggedOperation.reportError(SVNSaveAuthorizationInfo, e);
			IStatus status = new Status (IStatus.OK, AuthenticationPlugin.getPluginId(),"   Canceled Secure Storage Load..");
			AuthenticationPlugin.getDefault().getLog().log(status);
		}
		return certPassPhrase;
	}
	public boolean isPkcs11Enabled() {
		if (!(VendorImplementation.getInstance().isInstalled()) ){
			return false;
		} else {
			return true;
		}
		
	}
	private void clear() {
		node.clear();
	}
}
