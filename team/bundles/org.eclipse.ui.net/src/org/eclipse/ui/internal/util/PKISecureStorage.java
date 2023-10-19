/**
 *
 */
package org.eclipse.ui.internal.util;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.ui.internal.pkiselection.PKCSSelected;
import org.eclipse.ui.internal.pkiselection.PKCSpick;
import org.eclipse.ui.internal.pkiselection.PKIProperties;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 *
 *
 */
public class PKISecureStorage extends AbstractUIPlugin {

	private IEclipsePreferences securePreference = null;
	private IEclipsePreferences node = null;

	private transient String certPassPhrase;
	private String certificateLocation;
	protected PKIProperties auth=PKIProperties.getInstance();
	private static final String JAVA_SSL_USER_KEY_STORE_PATH_KEY = "javax.net.ssl.keyStore"; //$NON-NLS-1$
	private static final String JAVA_SSL_USER_KEY_STORE_TYPE_KEY = "javax.net.ssl.keyStoreType"; //$NON-NLS-1$
	private static final String JAVA_SSL_USER_KEY_STORE_PASS_KEY = "javax.net.ssl.keyStorePassword"; //$NON-NLS-1$

	protected final String PKI_LOCATION = "pki_location"; //$NON-NLS-1$
	protected final String PKCS11_LOCATION = "pkcs11_location"; //$NON-NLS-1$
	protected final String PKCS12_LOCATION = "pkcs12_location"; //$NON-NLS-1$
	protected final String PKI_TYPE = "pki_type"; //$NON-NLS-1$
	protected final String PKI_STATUS = "pki_status"; //$NON-NLS-1$
	protected final String PKI_PIN = "pki_pin"; //$NON-NLS-1$
	protected final String PKI_PASSPHRASE = "pki_passphrase"; //$NON-NLS-1$
	protected final String PKI_SAVED = "pki_saved"; //$NON-NLS-1$
	protected final String PKI_PROVIDER = "pki_provider"; //$NON-NLS-1$
	protected final String PKI_USER_STORE_NODE = "eclipse.pki.util/userKeyStore"; //$NON-NLS-1$
	protected final String SVNSaveAuthorizationInfo = "Save Authorization Info"; //$NON-NLS-1$

	public PKISecureStorage(){
		IPreferencesService service = Platform.getPreferencesService();

		this.securePreference = (IEclipsePreferences) service.getRootNode().node(InstanceScope.SCOPE);
		this.node = (IEclipsePreferences) securePreference.node(PKI_USER_STORE_NODE);
	}

	/**
	 * Stores the pki information in secure storage.
	 * @param authenticationInfo the authentication plugin containing the information input by user.
	 */
	//public void storePKI(AuthenticationPlugin authenticationInfo){
	public void storePKI(PkcsConfigurationIfc authenticationInfo) {
		try {

			String userKeyStoreLocation = authenticationInfo.getConfigurationLocationDir().trim();
			String passPhrase = authenticationInfo.getCertPassPhrase().trim();
			if (  PKCSpick.getInstance().isPKCS11on() ) {
				node.remove(PKCS12_LOCATION);
				node.remove(PKI_PASSPHRASE);
				node.put(PKI_TYPE, "PKCS11"); //$NON-NLS-1$

				userKeyStoreLocation = "CSPid"; //$NON-NLS-1$
				node.put(PKCS11_LOCATION, userKeyStoreLocation);
				node.put(PKI_PIN, passPhrase);
			}
			if (  PKCSpick.getInstance().isPKCS12on() ) {
				String status = ExpiredCertCheck.INSTANCE.getDate(userKeyStoreLocation, passPhrase.toCharArray());
				node.remove(PKCS11_LOCATION);
				node.remove(PKI_PIN);
				node.put(PKI_TYPE, "PKCS12"); //$NON-NLS-1$
				node.put(PKI_STATUS, status);
				node.put(PKCS12_LOCATION, userKeyStoreLocation);
				node.put(PKI_PASSPHRASE, passPhrase);
			}
			node.put(PKI_SAVED, "true"); //$NON-NLS-1$
			try {
				node.flush();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				LogUtil.logError(SVNSaveAuthorizationInfo, e);
			}
		} catch (Exception e) {
			LogUtil.logError(SVNSaveAuthorizationInfo, e);
		}
	}

	/**
	 * The secure storage node containing the information.
	 * @return the secure storage node.
	 */
	public IEclipsePreferences getNode() {
		return node;
	}

	/**
	 * Test if pki is saved in secure storage.
	 * @return true if saved or false if not saved.
	 */
	public boolean isPKISaved(){
		boolean saved = false;
		try {
			if ("true".equals(node.get(PKI_SAVED, "false"))) { //$NON-NLS-1$ //$NON-NLS-2$
				saved = true;
			}
		} catch (Exception e) {
			LogUtil.logError(SVNSaveAuthorizationInfo, e);
		}
		return saved;
	}
	public String getPkiType() {
		String type = null;
		 try {
				type = node.get(PKI_TYPE, "none"); //$NON-NLS-1$
			} catch (Exception e) {
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
			if (!(node.get(PKI_TYPE, "none").equals("none"))) { //$NON-NLS-1$ //$NON-NLS-2$
				pkiType = node.get(PKI_TYPE, "none"); //$NON-NLS-1$
			} else {
				/*
				 * TODO: make backwards compatible, default to pkcs12, because pkiType came to the game late.
				 */
				pkiType = "PKCS12"; //$NON-NLS-1$
			}

			if ((PKCSpick.getInstance().isPKCS11on()) && ((pkiType.equals("PKCS11")))) { //$NON-NLS-1$


			} else if ((PKCSpick.getInstance().isPKCS12on()) && ((pkiType.equals("PKCS12")))) { //$NON-NLS-1$
				certLocation = node.get(PKCS12_LOCATION, "none"); //$NON-NLS-1$
				if (certLocation.equals("none")) { //$NON-NLS-1$
					certLocation = node.get(PKI_LOCATION, "none"); //$NON-NLS-1$
				}
				try {
					/*
					 *  NOTE:  The following code will pop up a prompt for secure storage password on UNIX only, but NOT windoz.
					 *  1.  Should we be requesting password on windows too?
					 *  2.  Perhaps, after getting password ONCE, then subsequently just load and go.
					 *  3.  There is a catch block below that logs when the cancel button is pressed on the password dialog.
					 */
					certPassPhrase = node.get(PKI_PASSPHRASE, "none"); //$NON-NLS-1$
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
					System.out.println("PKISecureStorage -  I PRESSED CANCEL When prompted for SS password...."); //$NON-NLS-1$


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
				this.certificateLocation = node.get(PKCS11_LOCATION, "none"); //$NON-NLS-1$
				this.certPassPhrase = node.get(PKI_PIN, "none"); //$NON-NLS-1$
				if (this.certificateLocation.equalsIgnoreCase("none")) { //$NON-NLS-1$

					this.certificateLocation = "MSCAPI"; //$NON-NLS-1$
				}
			}
			if (  PKCSpick.getInstance().isPKCS12on() ) {
				this.certificateLocation = node.get(PKCS12_LOCATION, "none"); //$NON-NLS-1$
				if (this.certificateLocation.equals("none")) { //$NON-NLS-1$
					this.certificateLocation = node.get(PKI_LOCATION, "none"); //$NON-NLS-1$
				}
				//  NOTE:  Is it still "none"  ????  then set it to something.
				if (this.certificateLocation.equalsIgnoreCase("none")) { //$NON-NLS-1$
					/*
					 * NOTE:  If you change it to anything other than "none" make sure you also start checking for that
					 * value ALL the code that uses th elocation.   FYI;  Best choice is "none"...
					 */
				}
			}

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
					(!certificateLocation.equals("none")) && (!certPassPhrase.equals("none"))) { //$NON-NLS-1$ //$NON-NLS-2$
				System.setProperty(JAVA_SSL_USER_KEY_STORE_PATH_KEY, "CSPid"); //$NON-NLS-1$
				System.setProperty(JAVA_SSL_USER_KEY_STORE_PASS_KEY, certPassPhrase);

			}
		} else if ( PKCSpick.getInstance().isPKCS12on() ) {
			if(userKeyStore != null && userKeyStore.getType() != null && certPassPhrase != null && userKeyStoreLocation != null
					&& !certificateLocation.equals("none") && !certPassPhrase.equals("none")) { //$NON-NLS-1$ //$NON-NLS-2$
				System.setProperty(JAVA_SSL_USER_KEY_STORE_PATH_KEY, userKeyStoreLocation);
				System.setProperty(JAVA_SSL_USER_KEY_STORE_TYPE_KEY, userKeyStore.getType());
				System.setProperty(JAVA_SSL_USER_KEY_STORE_PASS_KEY, certPassPhrase);

			}
		}
    }

	/**
	 * @return the certificate password
	 */
	public String getCertPassPhrase() {
		try {
			certPassPhrase = node.get(PKI_PASSPHRASE, "none"); //$NON-NLS-1$
		} catch (Exception e) {
			//LoggedOperation.reportError(SVNSaveAuthorizationInfo, e);

		}
		return certPassPhrase;
	}
}
