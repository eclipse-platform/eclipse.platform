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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import org.eclipse.core.pki.util.LogUtil;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.ui.pki.AuthenticationPlugin;
import org.eclipse.ui.pki.pkiselection.PKCSpick;
import org.eclipse.core.pki.util.KeyStoreFormat;
import org.eclipse.ui.pki.util.KeyStoreUtil;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.pki.preferences.AuthenticationPreferences;

public class TrustStoreSecureStorage extends AbstractUIPlugin {
	
	private ISecurePreferences securePreference = null;
	private ISecurePreferences node = null;
	
	private String jksLocation;
	private transient String jksPassPhrase;    
    
    private static final String JAVA_SSL_TRUST_STORE_PATH_KEY = "javax.net.ssl.trustStore";
    private static final String JAVA_SSL_TRUST_STORE_TYPE_KEY = "javax.net.ssl.trustStoreType";
    private static final String JAVA_SSL_TRUST_STORE_PASS_KEY = "javax.net.ssl.trustStorePassword";
    private static final String TRUST_STORE_SECURE_STORAGE_NODE = "org.eclipse.pki.util/secureTrustJKSStore";
    private static final String SVNSaveAuthorizationInfo = "Save Authorization Info";
    
    private final String JKS_LOCATION = "jks_location";
    private final String JKS_PASSPHRASE = "jks_passphrase";
    private final String JKS_SAVED = "jks_saved";
    
    
    public TrustStoreSecureStorage() {
		this.securePreference = SecurePreferencesFactory.getDefault();
		this.node = securePreference.node(TRUST_STORE_SECURE_STORAGE_NODE);
	}
	
	/**
	 * Stores the trust store information in secure storage.
	 * @param authenticationInfo the authentication plugin containing the user input.
	 */
	public void storeJKS(AuthenticationPlugin authenticationInfo){		
		try {
			String truststoreLocation = authenticationInfo.getPreferenceStore().getString(AuthenticationPreferences.TRUST_STORE_LOCATION);
			node.put(JKS_LOCATION, truststoreLocation, false);
			String passPhrase = authenticationInfo.getTrustStorePassPhrase().trim();
			node.put(JKS_PASSPHRASE, passPhrase, true);
			node.put(JKS_SAVED, "true", false);
		} catch (StorageException e) {
			LogUtil.logError(SVNSaveAuthorizationInfo, e);
		} 
	}
	
	/**
	 * Test if the trust store is saved in secure storage.
	 * @return true if saved or false if not saved.
	 */
	public boolean isJKSSaved(){
		boolean saved = false;
		try {
			if("true".equals(node.get(JKS_SAVED, "false"))){
				saved = true;
			}
		} catch (StorageException e) {
			LogUtil.logError(SVNSaveAuthorizationInfo, e);
		}
		return saved;
	}
	
	/**
	 * Creates the trust store from secure storage
	 * @return the trust store.
	 */
	public KeyStore getTrustStore(){		
		KeyStore keyStore = null;
		try {
			jksLocation = node.get(JKS_LOCATION, "none");
			jksPassPhrase = node.get(JKS_PASSPHRASE, "none");
			if (jksLocation == null || jksLocation.isEmpty() ) {
				jksLocation=AuthenticationPlugin.getDefault().getPreferenceStore().getString(AuthenticationPreferences.TRUST_STORE_LOCATION);
			}
			if ( PKCSpick.getInstance().isPKCS12on() ) {
				keyStore = KeyStoreUtil.getKeyStore(jksLocation, jksPassPhrase, KeyStoreFormat.JKS);
			}
		} catch (StorageException e) {
			LogUtil.logError(SVNSaveAuthorizationInfo, e);
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
		return keyStore;
	}
	
	/**
	 * Clears the saved trust store in secure storage.
	 * @return true if clear was successful, else false if not cleared.
	 */
	public boolean clearSavedTrustStore(){
		boolean cleared = false;
		node.clear();
		try {
			if("none".equals(node.get(JKS_LOCATION, "none")) &&
					"none".equals(node.get(JKS_PASSPHRASE, "none"))){
				cleared = true;
			}
		} catch (StorageException e) {
			LogUtil.logError(SVNSaveAuthorizationInfo, e);
		}
		return cleared;
	}
	
	
	/**
	 * Sets the trust store system properties only. Does not set the trust store key in AuthenticationPlugin.
	 */
	public void setTrustStoreSystemProperties(){
    	KeyStore trustStore = getTrustStore();
    	String trustStoreLocation = jksLocation;
		if(trustStore != null && trustStore.getType() != null && trustStoreLocation != null && !trustStoreLocation.isEmpty()) {
			System.setProperty(JAVA_SSL_TRUST_STORE_PATH_KEY, trustStoreLocation);
			System.setProperty(JAVA_SSL_TRUST_STORE_TYPE_KEY, trustStore.getType());
			if(jksPassPhrase != null && !jksPassPhrase.isEmpty()) {
				System.setProperty(JAVA_SSL_TRUST_STORE_PASS_KEY, jksPassPhrase);
			} else {
				System.setProperty(JAVA_SSL_TRUST_STORE_PASS_KEY, AuthenticationPlugin.getDefaultTrustStorePassword());
			}
		}		
	}

	/**
	 * The node in secure storage containing the trust store information.
	 * @return the trust store secure storage node.
	 */
	public ISecurePreferences getNode() {
		return node;
	}

	/**
	 * @return the jks trust store password.
	 */
	public String getJksPassPhrase() {
		try {
			jksPassPhrase = node.get(JKS_PASSPHRASE, "none");
		} catch (StorageException e) {
			LogUtil.logError(SVNSaveAuthorizationInfo, e);
		}
		return jksPassPhrase;
	}
}