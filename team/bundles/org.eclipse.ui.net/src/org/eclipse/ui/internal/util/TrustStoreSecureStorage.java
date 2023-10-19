package org.eclipse.ui.internal.util;

import java.security.KeyStore;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.ui.internal.pkiselection.PKCSpick;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class TrustStoreSecureStorage extends AbstractUIPlugin {

	// private ISecurePreferences securePreference = null;
	// private ISecurePreferences node = null;
	private IEclipsePreferences securePreference = null;
	private IEclipsePreferences node = null;

	private String jksLocation;
	private transient String jksPassPhrase;

	private static final String JAVA_SSL_TRUST_STORE_PATH_KEY = "javax.net.ssl.trustStore"; //$NON-NLS-1$
	private static final String JAVA_SSL_TRUST_STORE_TYPE_KEY = "javax.net.ssl.trustStoreType"; //$NON-NLS-1$
	private static final String JAVA_SSL_TRUST_STORE_PASS_KEY = "javax.net.ssl.trustStorePassword"; //$NON-NLS-1$
	private static final String TRUST_STORE_SECURE_STORAGE_NODE = "sigint.eclipse.pki.util/secureTrustJKSStore"; //$NON-NLS-1$
	private static final String SVNSaveAuthorizationInfo = "Save Authorization Info"; //$NON-NLS-1$

	private final String JKS_LOCATION = "jks_location"; //$NON-NLS-1$
	private final String JKS_PASSPHRASE = "jks_passphrase"; //$NON-NLS-1$
	private final String JKS_SAVED = "jks_saved"; //$NON-NLS-1$


    public TrustStoreSecureStorage() {
		IPreferencesService service = Platform.getPreferencesService();

		this.securePreference = (IEclipsePreferences) service.getRootNode().node(InstanceScope.SCOPE);
		this.node = (IEclipsePreferences) securePreference.node(TRUST_STORE_SECURE_STORAGE_NODE);

		// this.securePreference = SecurePreferencesFactory.getDefault();
		// this.node = securePreference.node(TRUST_STORE_SECURE_STORAGE_NODE);
	}

	/**
	 * Stores the trust store information in secure storage.
	 * @param authenticationInfo the authentication plugin containing the user input.
	 */
	public void storeJKS(PkcsConfigurationIfc authenticationInfo) {
		try {
			String truststoreLocation = authenticationInfo.getTrustStoreLocationDir();
			node.put(JKS_LOCATION, truststoreLocation);
			String passPhrase = authenticationInfo.getTrustStorePassPhrase().trim();
			node.put(JKS_PASSPHRASE, passPhrase);
			node.put(JKS_SAVED, "true"); //$NON-NLS-1$
		} catch (Exception e) {
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
			if ("true".equals(node.get(JKS_SAVED, "false"))) { //$NON-NLS-1$ //$NON-NLS-2$
				saved = true;
			}
		} catch (Exception e) {
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
			jksLocation = node.get(JKS_LOCATION, "none"); //$NON-NLS-1$
			jksPassPhrase = node.get(JKS_PASSPHRASE, "none"); //$NON-NLS-1$

			if ( PKCSpick.getInstance().isPKCS12on() ) {
				keyStore = KeyStoreUtil.getKeyStore(jksLocation, jksPassPhrase, KeyStoreFormat.JKS);
			}
		} catch (Exception e) {
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
			if ("none".equals(node.get(JKS_LOCATION, "none")) && //$NON-NLS-1$ //$NON-NLS-2$
					"none".equals(node.get(JKS_PASSPHRASE, "none"))) { //$NON-NLS-1$ //$NON-NLS-2$
				cleared = true;
			}
		} catch (Exception e) {
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
			}
		}
	}

	/**
	 * The node in secure storage containing the trust store information.
	 * @return the trust store secure storage node.
	 */
	public IEclipsePreferences getNode() {
		return node;
	}

	/**
	 * @return the jks trust store password.
	 */
	public String getJksPassPhrase() {
		try {
			jksPassPhrase = node.get(JKS_PASSPHRASE, "none"); //$NON-NLS-1$
		} catch (Exception e) {
			LogUtil.logError(SVNSaveAuthorizationInfo, e);
		}
		return jksPassPhrase;
	}

}
