/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Corporation - initial API and implementation
 * yyyymmdd bug      Email and other contact information
 * -------- -------- -----------------------------------------------------------
 * 20231101   00000 joe@schiavone.org PKI implementation
 *******************************************************************************/
package org.eclipse.ui.pki;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.security.auth.callback.CallbackHandler;
import org.eclipse.core.pki.FingerprintX509;
import org.eclipse.core.pki.auth.PKIState;
import org.eclipse.core.pki.auth.SecurityFileSnapshot;
import org.eclipse.core.pki.pkiselection.PKIProperties;
import org.eclipse.core.pki.util.LogUtil;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.osgi.framework.BundleContext;
import org.eclipse.ui.pki.dialog.PassphraseDialog;
import org.eclipse.ui.pki.jsse.CdeX509TrustManager;
import org.eclipse.ui.pki.pkcs.VendorImplementation;
import org.eclipse.ui.pki.pkiselection.PKCSSelected;
import org.eclipse.ui.pki.pkiselection.PKCSpick;
import org.eclipse.pki.exception.UserCanceledException;
import org.eclipse.ui.pki.preferences.AuthenticationPreferences;
import org.eclipse.core.pki.util.KeyStoreFormat;
import org.eclipse.ui.pki.util.KeyStoreUtil;
import org.eclipse.ui.pki.util.PKIAuthenticator;
import org.eclipse.ui.pki.wizard.PKILoginWizard;
import org.eclipse.ui.pki.wizard.TrustStoreLoginWizard;

/**
 * The activator class controls the plug-in life cycle
 */
public class AuthenticationPlugin extends AbstractUIPlugin {

    // The plug-in ID
	
	private KeyStore userKeyStore = null;
	private transient String certPassPhrase;
	private KeyStore trustStore;
	private transient String trustStorePassPhrase;
	PKIProperties snapshotProperties = null;
	private Provider provider;
	
	//
	// Cert defines
	//
	private static final int DIGITAL_SIGNATURE = 0;
	private static final int KEY_CERT_SIGN = 5;
	private static final int CRL_SIGN = 6;
	
	private static final TrustManager trustManager = new CdeX509TrustManager();
	
    // The shared instance
    private static AuthenticationPlugin plugin;
    
    protected static final String DEFAULT_TRUST_STORE = "cacerts";
    protected static final String CONFIGURATION_DIR = "configuration";
    
    private static final String DEFAULT_TRUST_STORE_PASSWORD = "changeit";
    private static final String JAVA_SSL_CFG_PATH_KEY = "java.cfg.file";
    private static final String JAVA_SSL_TRUST_STORE_PATH_KEY = "javax.net.ssl.trustStore";
    private static final String JAVA_SSL_TRUST_STORE_TYPE_KEY = "javax.net.ssl.trustStoreType";
    private static final String JAVA_SSL_TRUST_STORE_PASS_KEY = "javax.net.ssl.trustStorePassword";
    private static final String JAVA_SSL_USER_KEY_STORE_PATH_KEY = "javax.net.ssl.keyStore";
    private static final String JAVA_SSL_USER_KEY_STORE_TYPE_KEY = "javax.net.ssl.keyStoreType";
    private static final String JAVA_SSL_USER_KEY_STORE_PASS_KEY = "javax.net.ssl.keyStorePassword";
    private static final String JAVA_SSL_USER_KEY_STORE_PROVIDER_KEY = "javax.net.ssl.keyStoreProvider";
    
    /**
     * The constructor
     */
    public AuthenticationPlugin() {}
    
    /**
     * Returns the shared instance
     *
     * @return the shared instance
     */
    public static AuthenticationPlugin getDefault() {
    	System.out.println("AuthenticationPlugin ---getDefault plugin");
    	if (plugin == null) {
    		AuthenticationPlugin auth = new AuthenticationPlugin();
			/*
			 * try { auth.startup();
			 * System.out.println("AuthenticationPlugin JUST RAN START UP MANNUALLY"); }
			 * catch (CoreException e) { // TODO Auto-generated catch block
			 * e.printStackTrace(); }
			 */
    	}
    	return plugin; 
    }

    public static String getPluginId() {
        return getDefault().getBundle().getSymbolicName();
    }
   
    public ILog getLogger() {
    	
    	return  AuthenticationPlugin.getDefault().getLog();
    }
   
    /*
     * (non-Javadoc)
     * @see org.eclipse.core.runtime.Plugins#start(org.osgi.framework.BundleContext)
     */
    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
        
        // Has a headless config already been set up
        if ((PKIState.CONTROL.isPKCS11on()) || (PKIState.CONTROL.isPKCS12on())) {
        	LogUtil.logInfo("AuthenticationPluginA Headless system has already setup PKI");

        	snapshotProperties = PKIProperties.getInstance();
        	snapshotProperties.load();
        	//snapshotProperties.dump();
        	if (PKIState.CONTROL.isPKCS11on()) {
        		PKCSSelected.setKeystoreformat(KeyStoreFormat.PKCS11);
        	}
        	if (PKIState.CONTROL.isPKCS12on()) {
        		PKCSSelected.setKeystoreformat(KeyStoreFormat.PKCS12);
        		getDefault()
        			.getPreferenceStore()
        			.setValue(AuthenticationPreferences.PKCS11_CFG_FILE_LOCATION, null );
        	}
        	
        	LogUtil.logInfo("AuthenticationPlugin keystorePKI"+ snapshotProperties.getKeyStore());
        } else {
        	clearPKI();
        	initialize();
        	snapshotProperties = PKIProperties.getInstance();
        }  
    }
    
    /**
     * Tries to get trust store and user certificate information from the system, prompting the user
     * if necessary, and then sets javax.net.ssl system properties.
     */
    public void setSystemProperties() {
    	KeyStore keystore = null;
    	setTrustStoreSystemProperties(obtainDefaultJKSTrustStore());
    	keystore = obtainUserKeyStore();
    	if (keystore != null) {
    		setUserKeyStoreSystemProperties(keystore);
    	}
    	/*
    	 * NOTE:  when user hits cancel keysotre is null...  SO DONT set it.
    	 */
    	//setUserKeyStoreSystemProperties(obtainUserKeyStore());
    }
    
    /**
     * @return the jks path that is currently set in the system properties or empty string if not in the system property.
     */
    public String obtainSystemPropertyJKSPath(){
		String currentJKSPath="";
		try {
			currentJKSPath = System.getProperty(JAVA_SSL_TRUST_STORE_PATH_KEY);
			if(currentJKSPath == null){
				currentJKSPath ="";
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
		return currentJKSPath;
    }
    
    /**
     * @return the password currently in the system for the trust store.
     */
    public String obtainSystemPropertyJKSPass(){
    	String currentJKSPass = System.getProperty(JAVA_SSL_TRUST_STORE_PASS_KEY);
		if(currentJKSPass == null){
			currentJKSPass ="";
		}
		return currentJKSPass;
    }
    
    /**
     * @return trust store key store only.
     */
    public KeyStore obtainDefaultJKSTrustStore(){
    	
    	String trustStoreDirectory=null;
    	
    	try {
    		trustStoreDirectory=AuthenticationPlugin.getDefault().getPreferenceStore().getString(AuthenticationPreferences.TRUST_STORE_LOCATION);
    		if ((trustStoreDirectory == null ) || (trustStoreDirectory.isEmpty())) {
    			trustStoreDirectory=PKIController.PKI_ECLIPSE_DIR.getAbsolutePath() + File.separator + DEFAULT_TRUST_STORE;
    		}
    	} catch (Exception e1) {
			// TODO Auto-generated catch block
			trustStoreDirectory=PKIController.PKI_ECLIPSE_DIR.getAbsolutePath() + File.separator + DEFAULT_TRUST_STORE;
		}
    	
    	
    	final String defaultTrustStorePath = trustStoreDirectory;
    	
    	AuthenticationPlugin.getDefault().getPreferenceStore().setValue(AuthenticationPreferences.TRUST_STORE_LOCATION, defaultTrustStorePath);
    	this.trustStorePassPhrase = DEFAULT_TRUST_STORE_PASSWORD;
    	
    	//System.out.println("obtainDefaultJKSTrustStore -> " + defaultTrustStorePath + " " + this.trustStorePassPhrase);
   	
    	try {
			this.trustStore = KeyStoreUtil.getKeyStore(defaultTrustStorePath, this.trustStorePassPhrase, KeyStoreFormat.JKS);
			setTrustStoreSystemProperties(this.trustStore);
    	} catch (KeyStoreException e) {
			System.out.println("The Java key store can not be loaded.");
		} catch (NoSuchAlgorithmException e) {
			System.out.println("The algorithm used to check the integrity of the jks file cannot be found.");
		} catch (CertificateException e) {
			System.out.println("The jks file can not be loaded.");
		} catch (IOException e) {
			System.out.println("There is a problem with the password or problem with the jks file data. Please try a different password.");			
		} catch (Exception e){
			System.out.println("Unexpected error occurred.");
		}        
        
        // Return the result
        return this.trustStore;    	
    }
    
    /**
     * Tries to get trust store information from the system, prompting the user
     * if necessary, and then sets javax.net.ssl system properties.
     * @param keystore This parameter was set here because have to call the obtainJKSTrustStore() in order to create the trust store
     * before it can be used.
     * @return true if the trust store system properties are set.
     */
    public boolean setTrustStoreSystemProperties(KeyStore keystore){
    	boolean setTrustStoreProperties = false;
    	
    	String trustStoreLocation = getPreferenceStore().getString(AuthenticationPreferences.TRUST_STORE_LOCATION);
		if(trustStore != null && trustStore.getType() != null && trustStoreLocation != null && !trustStoreLocation.isEmpty()) {
			System.setProperty(JAVA_SSL_TRUST_STORE_PATH_KEY, trustStoreLocation);
			System.setProperty(JAVA_SSL_TRUST_STORE_TYPE_KEY, trustStore.getType());
			if(trustStorePassPhrase != null && !trustStorePassPhrase.isEmpty()) {
				System.setProperty(JAVA_SSL_TRUST_STORE_PASS_KEY, trustStorePassPhrase);
			} else {
				System.setProperty(JAVA_SSL_TRUST_STORE_PASS_KEY, DEFAULT_TRUST_STORE_PASSWORD);
			}
			setTrustStoreProperties = true;
		}
		
		//
		// set te HTTPS context this program will use. 
		//
		setHTTPSContext();
    	
		return setTrustStoreProperties;
    }
    
    /**
     * @return the certificate path that is currently set in the system properties or empty string if system property doesn't exist.
     */
    public String obtainSystemPropertyPKICertificatePath(){
    	
		String currentCertificatePath = "" ; 
		if (PKIState.CONTROL.isPKCS11on()) {
			currentCertificatePath = getPreferenceStore().getString(AuthenticationPreferences.PKCS11_CONFIGURE_FILE_LOCATION);
		}
		if (PKIState.CONTROL.isPKCS12on() ) {
			currentCertificatePath = System.getProperty(JAVA_SSL_USER_KEY_STORE_PATH_KEY);
		}
		
		if(currentCertificatePath == null){
			currentCertificatePath ="";
		}
		return currentCertificatePath;
    }
    
    /**
     * @return the pass phrase currently in the system for the pki.
     */
    public String obtainSystemPropertyPKICertificatePass(){
    	String currentCertificatePass = System.getProperty(JAVA_SSL_USER_KEY_STORE_PASS_KEY);
		if(currentCertificatePass == null){
			currentCertificatePass ="";
		}
		return currentCertificatePass;
    }

	/**
     * @return user key store
     */
    public KeyStore obtainUserKeyStore(){
    	KeyStore userKeyStore = null;
    	try {
    		
    		userKeyStore = getUserKeyStore("local");
			if(PKCSSelected.isPkcs12Selected()){
				
				AuthenticationPlugin.getDefault().getPreferenceStore().setValue(
						AuthenticationPreferences.PKI_CERTIFICATE_LOCATION, obtainSystemPropertyPKICertificatePath());		
			}
		} catch (UserCanceledException e) {
			//Removed initialize() to prevent the system properties to be set
			//in the configuration file when the PKI Certificate user interface is canceled.

		}
    	
    	return userKeyStore;
    }
 
    
    /**
     * Tries to get user certificate information from the system, prompting the user
     * if necessary, and then sets javax.net.ssl system properties.
     * @param userkeystore Must call the obtainUserKeyStore() to create the userKeyStore before setting the properties.
     * @return true if the key store system properties are set.
     */
    public boolean setUserKeyStoreSystemProperties(KeyStore userkeystore){
    	boolean setKeyStoreProperties = false;
		String userKeyStoreLocation = null;
		System.out.println("AuthenticationPlugin ----- setUserKeyStoreSystemProperties");
		if ( PKCSpick.getInstance().isPKCS11on()) {
			System.setProperty(JAVA_SSL_USER_KEY_STORE_TYPE_KEY, "PKCS11");
			System.setProperty(JAVA_SSL_USER_KEY_STORE_PASS_KEY, snapshotProperties.getPasswordAuthentication().getPassword().toString());
			//   COMMENTING OUT because this isnt used here.
			//userKeyStoreLocation = getPreferenceStore().getString(AuthenticationPreferences.CSPID_CONFIGURE_FILE_LOCATION);
			System.setProperty(JAVA_SSL_USER_KEY_STORE_PATH_KEY, "pkcs11" );
			Provider pkcs11Provider = Security.getProvider("SunPKCS11");
			if(pkcs11Provider != null) {
				System.setProperty(JAVA_SSL_USER_KEY_STORE_PROVIDER_KEY, pkcs11Provider.getName());
			}
			setKeyStoreProperties = true;
		} else if ( PKCSpick.getInstance().isPKCS12on()) {
			System.setProperty(JAVA_SSL_USER_KEY_STORE_TYPE_KEY, "PKCS12");
			userKeyStoreLocation = getPreferenceStore().getString(AuthenticationPreferences.PKI_CERTIFICATE_LOCATION);
			System.setProperty(JAVA_SSL_USER_KEY_STORE_PATH_KEY, userKeyStoreLocation);
			//Used by pkcs12.
			if(userKeyStore != null && userKeyStore.getType() != null && certPassPhrase != null && userKeyStoreLocation != null) {
				//System.setProperty(JAVA_SSL_USER_KEY_STORE_TYPE_KEY, userKeyStore.getType());
	        	System.setProperty(JAVA_SSL_USER_KEY_STORE_PASS_KEY, certPassPhrase);
	        	setKeyStoreProperties = true;
			}    	
		}
		
		//
		// set the HTTPS context this program will use. 
		//
		setHTTPSContext();
		
    	return setKeyStoreProperties;
    }
    
    /**
     * Checks if the javax.net.ssl system properties are set.
     */
    public static boolean isSSLSystemPropertiesSet() {
    	System.out.println("AuthenticationPlugin    isSSLSystemPropertiesSet   PKCS12 PATH:"+AuthenticationPlugin.getDefault().getPreferenceStore().getString(AuthenticationPreferences.PKI_CERTIFICATE_LOCATION));
    	String trustPath = System.getProperty(JAVA_SSL_TRUST_STORE_PATH_KEY);
    	String trustType = System.getProperty(JAVA_SSL_TRUST_STORE_TYPE_KEY);
    	
    	String userKeyPath = System.getProperty(JAVA_SSL_USER_KEY_STORE_PATH_KEY);
    	String userKeyType = System.getProperty(JAVA_SSL_USER_KEY_STORE_TYPE_KEY);
    	String userKeyPass = System.getProperty(JAVA_SSL_USER_KEY_STORE_PASS_KEY);
    	
    	if(isNullEmpty(trustPath) 
    			|| isNullEmpty(trustType) 
    			|| isNullEmpty(userKeyPath) 
    			|| isNullEmpty(userKeyType) 
    			|| isNullEmpty(userKeyPass)) {
    		return false;
    	}
    	
    	return true;
    }
    
    /**
     * @return true if javax.net.ssl properties need to be set.
     */
    protected static boolean isNeedSSLPropertiesSet() {
    	boolean flag = false;
    	
    	//
    	// only set them if the user didn't already set the properties somewhere else
    	//
    	if(!isSSLSystemPropertiesSet()) {
    	
        	flag = true;
       
    	}
    	
    	return flag;
    }
    
    /**
     * Null safe {@link String#isEmpty()}
     * @param property String
     */
    private static boolean isNullEmpty(String property) {
    	return (property == null || property.isEmpty());
    }
    
    /*
     * (non-Javadoc)
     * @see org.eclipse.core.runtime.Plugin#stop(org.osgi.framework.BundleContext)
     */
    @Override
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }


    /**
     * Returns an image descriptor for the image file at the given
     * plug-in relative path
     *
     * @param path the path
     * @return the image descriptor
     */
    public ImageDescriptor getImageDescriptor( String path ) {
        return imageDescriptorFromPlugin( getPluginId(), path);
    }

    /**
     * Returns an image descriptor for the image file at the given
     * plug-in relative path
     *
     * @param path the path
     * @return the image descriptor
     */
    public ImageDescriptor getImageDescriptor( String pluginId, String path ) {
        return imageDescriptorFromPlugin( pluginId, path);
    }
    

	public String getCertificatePath() {
		String path = null;
		if ( PKIState.CONTROL.isPKCS11on()) {
			System.out.println("AuthenticationPlugin ----getCertificatePath  PKCS11");
			System.setProperty(JAVA_SSL_USER_KEY_STORE_TYPE_KEY, "PKCS11");
			path = getPreferenceStore().getString(AuthenticationPreferences.PKCS11_CONFIGURE_FILE_LOCATION);
		} else if ( PKIState.CONTROL.isPKCS12on()) {
			System.out.println("AuthenticationPlugin ----getCertificatePath  PKCS12");
			System.setProperty(JAVA_SSL_USER_KEY_STORE_TYPE_KEY, "PKCS12");
			path = getPreferenceStore().getString(AuthenticationPreferences.PKI_CERTIFICATE_LOCATION);
		}
		//System.out.println("AuthenticationPlugin ----getCertificatePath   return PATH:"+ path);
		return path;
	}

	public void setCertificatePath(String path) {
		
		if ( PKIState.CONTROL.isPKCS11on()) {
			//System.out.println("AuthenticationPlugin ----setCertificatePath  PKCS11 PATH:"+ path);
			System.setProperty(JAVA_SSL_USER_KEY_STORE_TYPE_KEY, "PKCS11");
			//getPreferenceStore().setValue(AuthenticationPreferences.PKCS11_CONFIGURE_FILE_LOCATION, path);
		} else if ( PKIState.CONTROL.isPKCS12on()) {
			//System.out.println("AuthenticationPlugin ----setCertificatePath  PKCS12 PATH:"+ path);
			System.setProperty(JAVA_SSL_USER_KEY_STORE_TYPE_KEY, "PKCS12");
			System.clearProperty(JAVA_SSL_USER_KEY_STORE_PROVIDER_KEY);
			getPreferenceStore().setValue(AuthenticationPreferences.PKI_CERTIFICATE_LOCATION, path);
		}
	}

	public String getCertPassPhrase() {
		return certPassPhrase;
	}

	public void setCertPassPhrase(String passPhrase) {
		certPassPhrase = passPhrase;
	}
	
	
	public KeyStore getUserKeyStore() throws UserCanceledException {
		/*
		 *   TODO:  Find all of the CDE calls to getUserKeyStore and create Use case
		 *   to determine the best way to get pkcs11 integration included, see GForge plugin and more
		 *   insid.
		 */
		return getUserKeyStore("local");
	}
	
	
	public KeyStore getUserKeyStore(String operation ) throws UserCanceledException {
		
		/* If the pkcs11 has been already enabled by some other application,
		 * then grab that keystore and go,  DO NOT POP UP DIALOG BOX, unless operation is an update..
		 * 
		 * @PARAM: operation = { local or update }
		 * where;
		 * 	local = During an eclipse start or restart.
		 *  update = Request to update eclipse pki options
		 */
		userKeyStore = null;
		if (!( operation.equalsIgnoreCase("update".toString()))) {
			try {
			       VendorImplementation vendorPkcs11 = VendorImplementation.getInstance();
			       if (vendorPkcs11.isEnabled() ) {
				     userKeyStore = vendorPkcs11.getKeyStore();
				     System.out.println("AuthenticationPlugin - Dynamic vendorPkcs11 enabled.");
				     PKCSpick.getInstance().setPKCS11on(true);
				     if ( vendorPkcs11.getSelectedX509FingerPrint() == null ) {
				    	 /*
				    	  * Set fingerprint using the default DS certificate, because this is dynamic vendorPkcs11.
				    	  */
				    	 setUserKeyStoreSystemProperties(userKeyStore);
				    	 X509Certificate x509 = (X509Certificate) userKeyStore.getCertificate(vendorPkcs11.getAlias());
				    	 vendorPkcs11.setSelectedX509Fingerprint(FingerprintX509.INSTANCE.getFingerPrint(x509, "MD5"));
				     }
			       }
			       
			    } catch (Exception e) {
			    	PKIState.CONTROL.setPKCS11on(false);
			    	PKIState.CONTROL.setPKCS12on(true);
				    e.printStackTrace();
			    } catch (Throwable e) {
			    	PKIState.CONTROL.setPKCS11on(false);
			    	PKIState.CONTROL.setPKCS12on(true);
				    e.printStackTrace();
			    }
			
			if (userKeyStore != null ) {
				this.setCertificatePath("pkcs11");
				this.setUserKeyStoreSystemProperties(userKeyStore);
				this.setProvider(Security.getProvider("SunPKCS11"));
			} 
		}
		
		if(userKeyStore == null)
		{
			final List<UserCanceledException> exceptions = new ArrayList<UserCanceledException>();
			final String op = operation;
			
			Display.getDefault().syncExec(new Runnable() {

				public void run() {
					//System.out.println("AuthenticationPlugin    getUserKeystore   PKCS12 PATH:"+AuthenticationPlugin.getDefault().getPreferenceStore().getString(AuthenticationPreferences.PKI_CERTIFICATE_LOCATION));
					Wizard wizard = new PKILoginWizard();
					wizard.setWindowTitle(op);
					WizardDialog dialog = new WizardDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), wizard);			
					if(dialog.open() == WizardDialog.CANCEL) {
						//System.out.println("AuthenticationPlugin --- PKI LOGIN CANCELED.");
						// REMOVED EXCEPTION.  WHY are we generating a stack trace for a CANCELLATION???
						//exceptions.add(new UserCanceledException("User login cancelled"));
						if ( op.equalsIgnoreCase("Update")) {
							userKeyStore=null;
						}
						
						 if ((!( PKCSpick.getInstance().isPKCS11on())) &&  (!(PKCSpick.getInstance().isPKCS12on())) ) {
							 //System.out.println("AuthenticationPlugin --- LOGIN CANCELED  AND NO PKI IS SELECTED.");
							 userKeyStore=null;
							 if ( op.equalsIgnoreCase("Selection")) {
								 /*
								  * NOTE:
								  * Only initialize PKI properties during EarlyStartup when NO <i>Selection<i/> is made.
								  */
								 AuthenticationPlugin.getDefault().initialize();
							 }
						 }
						//Removed the AuthenticationPlugin.getDefault().initialize() to prevent the system properties to be set
						//in the configuration file when the PKI Certificate user interface is canceled.
						/*
						 * TODO:  When user presses cancel decide what steps should be taken;
						 * 1.  Does a cancel indicate that NO pki is desired?
						 * 2.  Should all of the SYSTEM properties for PKI be removed from eclipse ws?
						 * 3.  Why are we allowing a stack trace here?  
						 * 4.  We CAN detect if ANY varient of PKI was previously selected.  Should it be restored?
						 * 5.  Since more stringent oversight is in place, why even allow an eclipse to NOT be pki enabled?
						 */
					}
				}
				
			});
			
			if (exceptions.size() > 0) {
				throw exceptions.get(0);
			}	
		}
		SecurityFileSnapshot.INSTANCE.image();
		return userKeyStore;
	}

	public void setUserKeyStore(KeyStore keyStore)
    {
    	userKeyStore = keyStore;
    }
	
	/**
	 * @return the existing user key store.
	 */
	public KeyStore getExistingUserKeyStore(){
		return userKeyStore;
	}
	
	/**
	 * @return the existing trustStore.
	 */
	public KeyStore getExistingTrustStore() {		
		return trustStore;
	}	
	
    
	public Provider getProvider() {
		return provider;
	}

	public void setProvider(Provider provider) {
		this.provider = provider;
	}

	/**
	 * This method returns the trust {@link KeyStore} set by the user through the preference page, or
	 * the default cacert jks trust store.
	 * @return a valid {@link KeyStore}, or null on error
	 * @throws IOException 
	 * @throws CertificateException 
	 * @throws NoSuchAlgorithmException 
	 * @throws KeyStoreException 
	 * @throws SecurityException 
	 * @throws NoSuchMethodException 
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws ClassNotFoundException 
	 */
	@SuppressWarnings("deprecation")
	protected KeyPass loadTrustStore() throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		String trustStoreLocation = getPreferenceStore().getString(AuthenticationPreferences.TRUST_STORE_LOCATION);
		InputStream in = null;
		
		try {
			
			if (trustStoreLocation != null && trustStoreLocation.trim().length() > 0) {
				in = new FileInputStream(trustStoreLocation);
				in = new BufferedInputStream(in);
				//Mark the beginning index position of the buffer.
				in.mark(0);
			} else {
				//
				// original reading from bundle
				//
				//in = getBundle().getEntry(DEFAULT_TRUST_STORE).openStream();
			    
				//
				// try to read the file from the eclipse/configuration directory
				//
				File ConfigurationFile = new File(Platform.getInstallLocation().getURL().getPath() + File.separator + 
                        AuthenticationPlugin.CONFIGURATION_DIR + File.separator +
                        AuthenticationPlugin.DEFAULT_TRUST_STORE);
              
                in = new BufferedInputStream(new FileInputStream(ConfigurationFile)); 

				//Mark the beginning index position of the buffer.
				in.mark(0);
			}
			
			// try default password
			String password = DEFAULT_TRUST_STORE_PASSWORD;
			try {
				return new KeyPass(KeyStoreUtil.getKeyStore(in, password, KeyStoreFormat.JKS), password);
			} catch (Throwable t) {
				//Reset the index position to the beginning of the buffer stream.
				in.reset();				
			}
			
			// prompt for password
			PassphraseDialog dialog = new PassphraseDialog(null, "Enter TrustStore Password");
			if (dialog.open() == Dialog.OK) {
				password = dialog.getPassphrase();
				return new KeyPass(KeyStoreUtil.getKeyStore(in, password, KeyStoreFormat.JKS), password);
			}
		} finally {
			try {
				in.close();
			} catch (Throwable t) {}
		}
		
		return null;
		
	}
	
	
	/**
	 * @return the trust store
	 * @throws UserCanceledException
	 */
	public KeyStore getJKSTrustStore() throws UserCanceledException{
		if(trustStore == null)
		{
			final List<UserCanceledException> exceptions = new ArrayList<UserCanceledException>();
			
			Display.getDefault().syncExec(new Runnable() {

				public void run() {
					Wizard wizard = new TrustStoreLoginWizard();
					WizardDialog dialog = new WizardDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), wizard);
					
					if(dialog.open() == WizardDialog.CANCEL)
						exceptions.add(new UserCanceledException("User login cancelled"));
				}
				
			});
			
			if (exceptions.size() > 0) {
				throw exceptions.get(0);
			}
			
		}
		
		return trustStore;
	}
	

	/**
	 * @return the trustStore, or null on error
	 */
	public KeyStore getTrustStore() {
		if (trustStore == null) {
			synchronized(this) {
				if (trustStore != null) return trustStore;
				try {
					KeyPass kp = loadTrustStore();
					trustStore = kp.getKey();
					trustStorePassPhrase = kp.getPass();
				} catch (Throwable t) {
					LogUtil.logError("Error while loading trust store", t);
				}
			}
		}
		return trustStore;
	}

	/**
	 * @return the trustStorePassPhrase
	 */
	public String getTrustStorePassPhrase() {
		return trustStorePassPhrase;
	}

	/**
	 * @param trustStore the trustStore to set
	 */
	public void setTrustStore(KeyStore trustStore) {
		this.trustStore = trustStore;
	}

	/**
	 * @param trustStorePassPhrase the trustStorePassPhrase to set
	 */
	public void setTrustStorePassPhrase(String trustStorePassPhrase) {
		this.trustStorePassPhrase = trustStorePassPhrase;
	}
	
	
	
	/**
	 * @return the trustManager
	 */
	public static TrustManager getTrustManager() {
		return trustManager;
	}
	
	/**
	 * Returns the user's sid from their pki, or null if pki isn't loaded or other error
	 * @return the user's sid from their pki, or null if pki isn't loaded or other error
	 * @throws UserCanceledException 
	 * @throws KeyStoreException 
	 * @throws Exception if user cancels 
	 */
	public String getPkiSid() throws UserCanceledException, KeyStoreException  {
		KeyStore ks = getUserKeyStore("local");
		if (ks == null) {
			return null;
		}
		Enumeration<String> aliases = ks.aliases();
		while (aliases.hasMoreElements()) {
			try {
				String alias = aliases.nextElement();
				Certificate cert = ks.getCertificate(alias);
				if (cert instanceof X509Certificate) {
					X509Certificate pki = (X509Certificate)cert;
					String sid = PKIAuthenticator.getSid(pki);
					//String affilation = PKIAuthenticator.getAffiliation(pki);
					return sid;
				}
			} catch (Throwable t) {}
		}
		
		return null;
	}

	/**
	 * Returns the user's sid from their pki, or null if pki isn't loaded or other error
	 * This sid will have the affiliation code associated with it for example a CSE person will
	 * have "-cse" appended to the end of their sid.
	 * @return the user's sid from their pki, or null if pki isn't loaded or other error
	 * @throws UserCanceledException 
	 * @throws KeyStoreException 
	 * @throws Exception if user cancels 
	 */
	public String getAfilliatedPkiSid() throws UserCanceledException, KeyStoreException  
	{
		KeyStore ks = getUserKeyStore("local");
		if (ks == null) 
		  {
			return null;
		  }
		
		Enumeration<String> aliases = ks.aliases();
		while (aliases.hasMoreElements()) 
		  {
			try 
			  {
				String alias = aliases.nextElement();
				Certificate cert = ks.getCertificate(alias);
				if (cert instanceof X509Certificate) 
				  {
					X509Certificate pki = (X509Certificate)cert;
					String sid = PKIAuthenticator.getSid(pki);
					String affiliation = PKIAuthenticator.getAffiliation(pki);
					
					if (affiliation.equals("US")) {
						return sid + "-US";
					} else {
                        return sid + "-unknown";
					}
				  }
			  } 
			catch (Throwable t) {}
		  }
		return null;
	}


	public static String getDefaultTrustStorePassword() {
		return DEFAULT_TRUST_STORE_PASSWORD;
	}


	class KeyPass {
		KeyStore key;
		String pass;
		/**
		 * @param key
		 * @param pass
		 */
		public KeyPass(KeyStore key, String pass) {
			super();
			this.key = key;
			this.pass = pass;
		}
		/**
		 * @return the key
		 */
		public KeyStore getKey() {
			return key;
		}
		/**
		 * @return the pass
		 */
		public String getPass() {
			return pass;
		}
		/**
		 * @param key the key to set
		 */
		public void setKey(KeyStore key) {
			this.key = key;
		}
		/**
		 * @param pass the pass to set
		 */
		public void setPass(String pass) {
			this.pass = pass;
		}
	}
	
	public void initialize() {
		try {
			System.out.println("AuthenticationPlugin - initialize");
			
			 
			getPreferenceStore().setValue("JAVA_SSL_USER_KEY_STORE_PATH_KEY", "");
			getPreferenceStore().setValue(AuthenticationPreferences.PKI_CERTIFICATE_LOCATION, "");
			getPreferenceStore().setValue(AuthenticationPreferences.PKCS11_CONFIGURE_FILE_LOCATION, "");
			if ( getPreferenceStore().needsSaving() ) {
				/*
				 * TODO: save the store
				 */
				//System.out.println(" AuthenticationPlugin -- Please save the store!");
				try {
					((IPersistentPreferenceStore)getPreferenceStore()).save();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void clearPKI() {
		System.out.println("AuthenticationPlugin CLEARED PROPERTY PROVIDER TBD");
		//System.clearProperty(JAVA_SSL_USER_KEY_STORE_PROVIDER_KEY);
		System.clearProperty(JAVA_SSL_USER_KEY_STORE_PATH_KEY);
		System.clearProperty(JAVA_SSL_USER_KEY_STORE_TYPE_KEY);
		System.clearProperty(JAVA_SSL_USER_KEY_STORE_PASS_KEY);
		/*
		 * TODO:  Decide if we need to CLEAR all the trust store properties too. 
		 * 
		 */
		System.clearProperty(JAVA_SSL_TRUST_STORE_PATH_KEY);
		System.clearProperty(JAVA_SSL_TRUST_STORE_TYPE_KEY);
		System.clearProperty(JAVA_SSL_TRUST_STORE_PASS_KEY);
	}
	
	/**
	 * sets the certs on the connection if using the HTTPs protocol
	 */
	public SSLContext setHTTPSContext() {
		SSLContext ctx = null;
		if (isSSLSystemPropertiesSet()) {
			KeyManagerFactory kmf = null;
			TrustManagerFactory tmf = null;
			try {	
				if ( PKCSpick.getInstance().isPKCS11on()) {
					
					/*
					 *   SSLContext has already been set for PKCS11:
					 *   DO NOT OVERwrite it!  The consequence is that no certificate
					 *   negotiation will occur.
					 *   
					 */
					ctx = SSLContext.getDefault();
					//System.out.println("AuthenticationPlugin ---  setHTTPSContext");
					
				} else {
					kmf =  KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
					ctx = SSLContext.getInstance("TLS");
					/*
					 *   DEBUG TRUST defaultALG, change from PKIX to SunX509
					 */
					try {
						//String algo = TrustManagerFactory.getDefaultAlgorithm();
						//System.out.println("AuthenticationPlugin ----  setHTTPSContext   ALG:"+algo);
						//tmf = TrustManagerFactory.getInstance("SunX509", "SunJSSE");
						tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
						
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					tmf.init(trustStore);
					kmf.init(userKeyStore, certPassPhrase.toCharArray());
					KeyManager[] keyManagers = kmf.getKeyManagers();
					TrustManager[] trustManagers = tmf.getTrustManagers();
					ctx.init(keyManagers, trustManagers, null);
					SSLContext.setDefault(ctx);
					HttpsURLConnection.setDefaultSSLSocketFactory(ctx.getSocketFactory());
				}
				
				/*
				 *   TESTING TESTING 
				 */
//				System.out.println("AuthenticationPlugin  CALLING TEST CODE");
//				testEclipseServer();
//				System.out.println("AuthenticationPlugin  COMPLETED TEST CODE");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}    
		}
		return ctx;
	}
	private void testEclipseServer()  {
		String url="https://my.test.server/marketplace/oxygen/sites/update.zeroturnaround.com//update-site";
		SSLSocketFactory factory = null;
 
		try {
			System.out.println("AuthenticationPlugin testEclipseServer TESTING CONNECTION");
			URL urlconn = new URL(url);
			HttpsURLConnection conn = (HttpsURLConnection) urlconn.openConnection();
			SSLSocketFactory ssl=null;
			try {
				ssl = SSLContext.getDefault().getSocketFactory();
				
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			conn.setSSLSocketFactory(ssl);
			
			
			System.out.println("AuthenticationPlugin - opendConnection");
			conn.setDoOutput(true);
			conn.setUseCaches(false);
			int i = conn.getResponseCode();
		    if (i == 200)
		    {
		    	System.out.println("AuthenticationPlugin TEST  - 200 STATUS");
		      int iContentLen = conn.getContentLength();
		      if (iContentLen > 0)
		      {
		        InputStreamReader isr = new InputStreamReader(conn.getInputStream());
		        char[] response = new char[iContentLen];
		        isr.read(response);
		        isr.close();
		        
		      }
		      else
		      {
		    	  System.out.println("AuthenticationPlugin TEST  - BAD STATUS returned");
		      }
		    } else {
		    	System.out.println("AuthenticationPlugin TEST server returned:"+i);
		    }
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
 
	}
	private KeyStore.Builder createKeyStoreBuilder() {
		/*
		 * NOTE:  This code is used to create a Keystore when there is only a pre-initialized keystore
		 * already available, as in the case of a pkcs11 enabled application. The Password callback handler
		 * here allows the PKCS11 keystore to be utilized after it was already initialized
		 * elsewhere.  
		 */
		KeyStore.Builder builder=null;
		
		try {
			KeyStore.CallbackHandlerProtection cb = new KeyStore.CallbackHandlerProtection( (CallbackHandler) new PasswordCallbackHandler());
			builder = KeyStore.Builder.newInstance("PKCS11", Security.getProvider("SunPKCS11"), cb);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return builder;
	}
	
	/**
	 * returns true if the array of key usage sent in flags a DigitalSignature
	 * @param ba
	 * @return
	 */
	private static boolean isDigitalSignature(boolean[] ba) {
		if ( ba != null) {
			return ba[DIGITAL_SIGNATURE] && !ba[KEY_CERT_SIGN] && !ba[CRL_SIGN];
		} else {
			return false;
		}
	}
	
	/**
	 * Returns the users email from the certificate or null if it can't find it.
	 * @return
	 */
	public String getUserEmail()
	{
		String userEmail = null;

		if (isSSLSystemPropertiesSet()) {
			try {
				//
				// at this point we have loaded either a PKCS11 or PKCS12 cert
				//
				Enumeration<String> en = userKeyStore.aliases();
				while (en.hasMoreElements()) {
					String alias = en.nextElement();
					//System.out.println("      " + alias);
					Certificate cert = userKeyStore.getCertificate(alias);
					if (cert.getType().equalsIgnoreCase("X.509"))
					{
						X509Certificate X509 = (X509Certificate) cert;
						
						//
						// we need to make sure this is a digital certificate instead of a server 
						// cert or something
						//
						if (isDigitalSignature(X509.getKeyUsage())) {
							Collection<List<?>> altnames = X509.getSubjectAlternativeNames();
							if (altnames != null) {
								for (List item : altnames) {
									Integer type = (Integer) item.get(0);
									if (type == 1)
										try {
											userEmail = item.toArray()[1].toString();
										}
									catch (Exception e) {
										e.printStackTrace();
									}
								}
							}

						}

					}
				}

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		//System.out.println("Users email is " + userEmail);
		return userEmail;
	}
}