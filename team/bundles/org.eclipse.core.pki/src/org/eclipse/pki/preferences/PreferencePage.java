package org.eclipse.pki.preferences;

import java.io.PrintStream;
import java.security.KeyStore;


import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

import org.eclipse.pki.auth.AuthenticationPlugin;
import org.eclipse.pki.pkcs.VendorImplementation;
import org.eclipse.pki.pkiselection.PKCSpick;
import org.eclipse.pki.pkiselection.PKI;
import org.eclipse.pki.pkiselection.PKIProperties;
import org.eclipse.pki.util.ChangedPressedFieldEditorStatus;
import org.eclipse.pki.util.PKISecureStorage;
import org.eclipse.pki.util.TrustStoreSecureStorage;


/**
 * @since 1.3
 */
public class PreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	
	private StringFieldEditor pkcs11Certificate;
	private StringFieldEditor pkiCertificate;
	private StringFieldEditor trustStoreJKS;
	private PKI previousPKI;
	private Composite yourSibling=null;
	private String pkiType="NONE";
	private boolean isGoodConfig=false;
	public boolean exitView=false;
	private Group groups = null;
	private String pkcs11Label="Smartcard location Configuration";
	private String pkcs12Label="PKCS12 Certificate Installation location";
	PKISecureStorage pkiSecureStorage = null;
//	Display display = Display.getCurrent();
//	Color blue = display.getSystemColor(SWT.COLOR_BLUE);
//	Color red = display.getSystemColor(SWT.COLOR_RED);
//	Color green = display.getSystemColor(SWT.COLOR_GREEN);
//	Color yellow = display.getSystemColor(SWT.COLOR_YELLOW);
//	Color black = display.getSystemColor(SWT.COLOR_BLACK);

	public PreferencePage() {
		super(FieldEditorPreferencePage.GRID);
		noDefaultButton();
		//Activator.logInfo("SOOOOOOOOOOsosososososososososososososoosOOO", new Throwable());
		//setPreferenceStore(AuthenticationPlugin.getDefault().getPreferenceStore());
		setDescription("PKI Preferences:");
		//printoutStore();
		previousPKI = this.previousPKI();
		pkiSecureStorage = new PKISecureStorage();
	}
	@Override
	public void createControl(Composite parent) {
		super.createControl( parent );
		
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this.getShell(), 
	            "org.eclipse.pki.preferences");
	}
	@Override
    protected Control createContents(Composite parent) {
		
		// help to debug
		//yourParent.getChildren()[0].setBackground(yellow);
		
		yourSibling = new Composite(parent, SWT.NONE);
		
		//  HELP with debugging...
		//yourSibling.setBackground(red);
		
        yourSibling.setLayoutData( new GridData( GridData.FILL_BOTH ) );
        GridLayout pkiGrid = new GridLayout();
        yourSibling.setLayout( pkiGrid );
        groups =  addGroup( yourSibling );
        addFields( groups );
        
        
        /*
         * NOTE: 
         * When there has been NO pki selection made, then select PKCS11, but ONLY if its been configured by SA's.
         * Otherwise, just make the pkcs12 selection available.
         */
      
        if ((!(PKCSpick.getInstance().isPKCS11on())) && 
        	(!(PKCSpick.getInstance().isPKCS12on())) &&
        	(VendorImplementation.getInstance().isInstalled() )) {
        	//System.out.println("PreferencePage -------- AVAL:"+VendorImplementation.getInstance().isInstalled() );
        	//System.out.println("PreferencePage --------- TURNING ON DEFAULT pkcs11 is on");
        	setPkcs12InVisible();
        	setPkcs11Visible();
        	
        	setVisible(false);
        	
        } else if ((!(  PKCSpick.getInstance().isPKCS11on())) && 
            		(!(PKCSpick.getInstance().isPKCS12on())) &&
            		(!(VendorImplementation.getInstance().isInstalled() ))) {
        	//System.out.println("PreferencePage --------- THERE WAS NO DEFAULT  pkcs12 is on");
        	setPkcs11InVisible();
        	setPkcs12Visible();
        } else if ( PKCSpick.getInstance().isPKCS11on()) {
        	//System.out.println("PreferencePage ---------  pkcs11 is on");
        	setPkcs12InVisible();
        	setPkcs11Visible();
        } else if ( PKCSpick.getInstance().isPKCS12on()) {
        	//System.out.println("PreferencePage --------- pkcs12 is on");
        	setPkcs11InVisible();
        	setPkcs12Visible();
        }
        
        initialize();
        setEditors();
	    checkState();
		
		yourSibling.layout();
		
		parent.redraw();  
	    return yourSibling;
	}
	

	private Group addGroup(Composite top) {
		Group group = new Group(top, SWT.TOP);
		GridData data = new GridData(SWT.TOP, GridData.FILL_HORIZONTAL);
		data.horizontalSpan=500;
		data.verticalSpan=5;
		data.widthHint=600;
		data.heightHint = 125;
		group.setLayoutData(data); 
		return group;
	}
	private void addFields(Group group) {
		
		pkcs11Certificate = new PKICertLocationFieldEditor(AuthenticationPreferences.PKCS11_CONFIGURE_FILE_LOCATION,
                "Smartcard repository location", group, "pkcs11", this);
		
		pkiCertificate = new PKICertLocationFieldEditor(AuthenticationPreferences.PKI_CERTIFICATE_LOCATION,
                "Certificate path:", group, "pkcs12", this);
		
		trustStoreJKS = new TrustStoreLocationFieldEditor(AuthenticationPreferences.TRUST_STORE_LOCATION,
                "Trust Store Location:", group);
		
		pkcs11Certificate.getTextControl(group).setEnabled(false);
		pkiCertificate.getTextControl(group).setEnabled(false);
	    trustStoreJKS.getTextControl(group).setEnabled(false);
	    
	    pkcs11Certificate.loadDefault();
	    pkiCertificate.loadDefault();
	    
	    addField(pkcs11Certificate);
	    addField(pkiCertificate);
	    
	}
	

    /**
     * Initializes all field editors.
     * This has been overridden so that the preference store from the
     * GForgeClientPlugin can be retrieved and used for the MADForge URL.
     */
    protected void initialize() {
        super.initialize();
    }
    protected void setEditors() {
    	
        if (pkiCertificate != null) {
        	pkiCertificate.setPage(this);
        	pkiCertificate.setPreferenceStore(AuthenticationPlugin.getDefault().getPreferenceStore());
        	pkiCertificate.load(); 
        }
        if (pkcs11Certificate != null) {
        	pkcs11Certificate.setPage(this);
        	pkcs11Certificate.setPreferenceStore(AuthenticationPlugin.getDefault().getPreferenceStore());
        	pkcs11Certificate.load();
        }
        if (trustStoreJKS != null) {
        	trustStoreJKS.setPage(this);
        	trustStoreJKS.setPreferenceStore(AuthenticationPlugin.getDefault().getPreferenceStore());
        	trustStoreJKS.load();
        	
        }
    }
    protected void setPkcs11InVisible() {
    	
    	try {
    		//System.out.println("PreferencePage ---------  pkcs12 is on");
			groups.getChildren()[0].setVisible(false);
			groups.getChildren()[1].setVisible(false);
			groups.getChildren()[2].setVisible(false);
			groups.setText(pkcs12Label);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    protected void setPkcs12InVisible() {
    	
    	try {
    		//System.out.println("PreferencePage ---------  pkcs11 is on");
        	groups.getChildren()[3].setVisible(false);
        	groups.getChildren()[4].setVisible(false);
        	groups.getChildren()[5].setVisible(false);
        	groups.setText(pkcs11Label);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    protected void setPkcs11Visible() {
    	
    	try {
    		pkiType="pkcs11";
    		groups.setText(pkcs11Label);
			groups.getChildren()[0].setVisible(true);
        	groups.getChildren()[1].setVisible(true);
        	groups.getChildren()[2].setVisible(true);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    protected void setPkcs12Visible() {
    	
    	try {
    		pkiType="pkcs12";
    		groups.setText(pkcs12Label);
        	groups.getChildren()[3].setVisible(true);
        	groups.getChildren()[4].setVisible(true);
        	groups.getChildren()[5].setVisible(true);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

	@Override
	protected void performApply() {
		//CheckUpdatedKeystoreValue.isValid( pkiCertificate.getStringValue() );
		//super.performApply();
		
		//System.out.println("PreferencePage --------- APPLY PRESSED REQUEST   TBD:   FIX the stored values. TYPE:"+pkiType);
		try {
			if (!(exitView)) {
				if (( pkiType.equals("pkcs11") && (ChangedPressedFieldEditorStatus.isPkiChangedPressed()))) {
					//System.out.println("PreferencePage --------- APPLY PRESSED REQUEST  PKCS11 needs to be set");
					if ( pkcs11Certificate.isValid() ) {
						PKCSpick.getInstance().setPKCS11on(true);
						ChangedPressedFieldEditorStatus.setPkiChangedPressed(true);
						if ((ChangedPressedFieldEditorStatus.isPkiChangedPressed() ) ) {
							if ( CheckUpdatedKeystoreValue.isValid( pkcs11Certificate.getStringValue() )) {
								
								pkcs11Certificate.setStringValue( "pkcs11" );
								
						
								AuthenticationPlugin.getDefault().setCertificatePath("pkcs11");
								
								AuthenticationPlugin.getDefault().setUserKeyStore(VendorImplementation.getInstance().getKeyStore());
								AuthenticationPlugin.getDefault().setUserKeyStoreSystemProperties( VendorImplementation.getInstance().getKeyStore() );
								System.setProperty("javax.net.ssl.keyStoreProvider", "SunPKCS11");
								pkcs11Certificate.getTextControl(groups).setEnabled(false);
							}
						}
					} 
				}
				if ( (pkiType.equalsIgnoreCase("pkcs12") && (ChangedPressedFieldEditorStatus.isPkiChangedPressed()))) {
					if ( pkiCertificate.isValid() ) {
						PKCSpick.getInstance().setPKCS12on(true);
						if ((ChangedPressedFieldEditorStatus.isPkiChangedPressed() ) ) {
							if ( CheckUpdatedKeystoreValue.isValid( pkiCertificate.getStringValue() )) {
								AuthenticationPlugin.getDefault().setCertificatePath(pkiCertificate.getStringValue());
								pkiCertificate.getTextControl(groups).setEnabled(false);
							}
						}
					}
				}
				previousPKI = this.previousPKI();
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	protected void performDefaults() {
		//System.out.println("PreferencePage --------- perform defaults...----------------------------RE");
		
		//getPreferenceStore().removePropertyChangeListener( propertyChangeListener );
//		if ( pkcs11Composite == null) {
//			AuthenticationPlugin.getDefault().setUserKeyStore(null);
//			super.performDefaults();
//		}
	}
    
    public void init(IWorkbench workbench) {
        // TODO Auto-generated method stub  
    }

    @Override
    protected void createFieldEditors() {
        // NOT called b/c we override createContents()
    }    

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.FieldEditorPreferencePage#performOk()
	 */
	@Override
	public boolean performOk() {
		boolean isOK=true;
		//System.out.println("PreferencePage --------- OK PRESSED REQUEST   TBD:   FIX the stored values. TYPE:"+pkiType);
		
		if(ChangedPressedFieldEditorStatus.isJksChangedPressed()){
			//System.out.println("PreferencePage --------- OK PRESSED REQUEST changepresed trust");
			changeJKSTrustStoreSecureStorage();
			//The trust store path, trust store password and key store already set in AuthenticationPlugin when 
			//the user entered and clicked Finish in the trust store login wizard.
			AuthenticationPlugin.getDefault().setTrustStoreSystemProperties(AuthenticationPlugin.getDefault().getExistingTrustStore());
		}
		
		if ((ChangedPressedFieldEditorStatus.isPkiChangedPressed() ) ) {
			AuthenticationPlugin.getDefault().setUserKeyStore(ChangedPressedFieldEditorStatus.getPkiUserKeyStore());
			//System.out.println("PreferencePage --------- PROCESSING A CHANGE OK REQUEST");
			if (( PKCSpick.getInstance().isPKCS11on()) || ( pkiType.equals("pkcs11") )) {
				/*
				 * NOTE:
				 * The USER needs to be able to type in a location. and If its NOT valid, then FAIL this TEST.
				 */
				isOK = CheckUpdatedKeystoreValue.isValid( pkcs11Certificate.getStringValue() );
				
				pkcs11Certificate.setStringValue( "pkcs11" );
				PKIProperties.getInstance().setKeyStorePassword(AuthenticationPlugin.getDefault().getCertPassPhrase());
				PKIProperties.getInstance().restore();
			} else 	if (( PKCSpick.getInstance().isPKCS12on()) || ( pkiType.equals("pkcs12") )) {
				//System.out.println("PreferencePage --------- PROCESSING A CHANGE OK REQUEST   FOR PKCS12");
				if (  (pkiCertificate.getStringValue() != null ) || (!pkiCertificate.getStringValue().isEmpty() )){
					isOK  = CheckUpdatedKeystoreValue.isValid( pkiCertificate.getStringValue() );
				} else {
					//  The value was alaready set in authentication plugin so get it from there.
					isOK  = CheckUpdatedKeystoreValue.isValid( AuthenticationPlugin.getDefault().getCertificatePath() );
				}
			} else {
				System.out.println("PreferencePage --------- PROCESSING A CHANGE OK REQUESt NO SELECTION");
			}
		
			changePKISecureStorage();
			//System.out.println("PreferencePage -----------performOK  SETING TRUSTSTORE VALUE BACK to initial value:"+ AuthenticationPlugin.getDefault().obtainSystemPropertyJKSPath());
			//AuthenticationPlugin.getDefault().setTrustStoreSystemProperties(AuthenticationPlugin.getDefault().getExistingTrustStore());
			
			//trustStoreJKS.setStringValue(AuthenticationPlugin.getDefault().obtainSystemPropertyJKSPath());
			
			//createContents(yourParent);
			
			//The pki path, pki password and key store already set in AuthenticationPlugin when 
			//the user entered and clicked Finish in the pki login wizard.
			//AuthenticationPlugin.getDefault().setUserKeyStoreSystemProperties(AuthenticationPlugin.getDefault().getExistingUserKeyStore());
		} else {
			//System.out.println("PreferencePage --------- OK PRESSED REQUEST  AND DING A RESTORE OF OLD VALUES");
			previousPKI.reSetSystem();
		}
		
		ChangedPressedFieldEditorStatus.setPKISaveCertificateChecked(false);
		ChangedPressedFieldEditorStatus.setJKSSaveTrustStoreChecked(false);
		
		ChangedPressedFieldEditorStatus.setPkiChangedPressed(false);
		ChangedPressedFieldEditorStatus.setJksChangedPressed(false);
		
		//System.out.println("PreferencePage -----------performOK DONE   VALUE:"+isOK);
		if ( isOK ) {
			super.performOk();
		}
		
		//return super.performOk();
		return isOK;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performCancel()
	 */
	@Override
	public boolean performCancel() {
		
		String jksTrustStorePathInSystemProperties = AuthenticationPlugin.getDefault().obtainSystemPropertyJKSPath();
		
		if(ChangedPressedFieldEditorStatus.isPkiChangedPressed()){
			AuthenticationPlugin.getDefault().setUserKeyStore(ChangedPressedFieldEditorStatus.getPreviousUserKeyStore());
    		previousPKI.reSetSystem();
    		if ( previousPKI.getKeyStoreType().equalsIgnoreCase("PKCS11") ) {
    			PKCSpick.getInstance().setPKCS11on(true);
    			PKCSpick.getInstance().setPKCS12on(false);
    			AuthenticationPlugin.getDefault().setCertificatePath("pkcs11" );
    			pkcs11Certificate.setStringValue("pkcs11");
    		}
    		if ( previousPKI.getKeyStoreType().equalsIgnoreCase("PKCS12") ) {
    			PKCSpick.getInstance().setPKCS12on(true);
    			PKCSpick.getInstance().setPKCS11on(false);
    			String pkiCertificatePathInSystemProperties = AuthenticationPlugin.getDefault().obtainSystemPropertyPKICertificatePath();
    			AuthenticationPlugin.getDefault().setCertificatePath(pkiCertificatePathInSystemProperties);
    			pkiCertificate.setStringValue(pkiCertificatePathInSystemProperties);
    		}
    		PKIProperties.getInstance().load();
    		AuthenticationPlugin.getDefault().setCertPassPhrase(AuthenticationPlugin.getDefault().obtainSystemPropertyPKICertificatePass());
    		AuthenticationPlugin.getDefault().setTrustStoreSystemProperties(AuthenticationPlugin.getDefault().getTrustStore());
    		
    		if ( previousPKI.isSecureStorage()) {
    			ChangedPressedFieldEditorStatus.setPKISaveCertificateChecked( true );
    			changePKISecureStorage();
    		}
    		
		} else {
			//System.out.println("PreferencePage -----------performCANCEL THERE WAS NO CHANGE,,,..");
		}
		
		if(ChangedPressedFieldEditorStatus.isJksChangedPressed()){
			AuthenticationPlugin.getDefault().setTrustStore(ChangedPressedFieldEditorStatus.getJksTrustStore());
    		AuthenticationPlugin.getDefault().getPreferenceStore().setValue(AuthenticationPreferences.TRUST_STORE_LOCATION, jksTrustStorePathInSystemProperties);
    		AuthenticationPlugin.getDefault().setTrustStorePassPhrase(AuthenticationPlugin.getDefault().obtainSystemPropertyJKSPass());
		}
		
		ChangedPressedFieldEditorStatus.setPkiChangedPressed(false);
		ChangedPressedFieldEditorStatus.setJksChangedPressed(false);
		
		
		ChangedPressedFieldEditorStatus.setPKISaveCertificateChecked(false);
		ChangedPressedFieldEditorStatus.setJKSSaveTrustStoreChecked(false);
		
		ChangedPressedFieldEditorStatus.setPkiUserKeyStore(null);
		ChangedPressedFieldEditorStatus.setJksTrustStore(null);
		
		return super.performCancel();
		
	}
	@Override
	public boolean okToLeave() {
		//System.out.println("PreferencePage ----------------------------------------ok to leave");
		
		//initialize();
		//this.performApply();
		//this.performDefaults();
		
		/*
		*** dont do this because it causes problems
   		* this.getShell().setVisible(false);
   		*/
//		return false;	
		return true;
	}
//	@Override
//	public void setValid( boolean b ) {
//		isGoodConfig = b;
//	}
	
	@Override
	public boolean isValid() {
		//System.out.println("PreferencePage ------ isValid");
		boolean isGood=false;
		isGood=isGoodConfig;
		if ( PKCSpick.getInstance().isPKCS12on()) {
			isGood=true;
		}
		if ( PKCSpick.getInstance().isPKCS11on()) {
			if ( pkcs11Certificate.isValid() ) {
				isGood=true;
			}
		}
		 if ((!(PKCSpick.getInstance().isPKCS11on())) && 
		     (!(PKCSpick.getInstance().isPKCS12on())) ) {
		      isGood=true;
		 }
		 //System.out.println("PreferencePage --------------------------isValid-----------------VALID:"+isGoodConfig);
		 
		/*
		 * TODO
		 * Figure out how to see if focus is lost for this preference page.
		 * Till then the apply and OK buttons need to be ALEWAYS enabled.
		 */
		//isGood=true;
		return isGood;
	}
	
	/**
	 * Store changed PKI information to secure storage.
	 */
	private void changePKISecureStorage(){
		
		pkiSecureStorage = new PKISecureStorage();
		if(ChangedPressedFieldEditorStatus.isPKISaveCertificateChecked()){			
			pkiSecureStorage.storePKI(AuthenticationPlugin.getDefault());
		} else {
			pkiSecureStorage.getNode().removeNode();
		}				
	}
	
	/**
	 * Store changed jks trust store to secure storage.
	 */
	private void changeJKSTrustStoreSecureStorage(){
		AuthenticationPlugin.getDefault().getPreferenceStore().setValue(AuthenticationPreferences.TRUST_STORE_LOCATION, trustStoreJKS.getStringValue() );
		TrustStoreSecureStorage jksTrustStore = new TrustStoreSecureStorage();
		if(ChangedPressedFieldEditorStatus.isJKSSaveTrustStoreChecked()){			
			jksTrustStore.storeJKS(AuthenticationPlugin.getDefault());
		} else {
			jksTrustStore.getNode().removeNode();
		}
	}
	public boolean pageChangeListener(Object source, String property, String oldValue, String newValue ) {
		boolean pageUpdate=true;
		
		try {
			if ( "FOCUS".equals(property)) {
				if ( newValue.equals(PKICertLocationFieldEditor.FOCUS_LOST)  ) {
					isGoodConfig=true;
					exitView=true;
					setValid(true);
				}
				if ( newValue.equals(PKICertLocationFieldEditor.FOCUS_GAINED)  ) {
					isGoodConfig=false;
					exitView=false;
					setValid(false);
				}
			}
			if ( "VALIDATE".equals(property)) {
				if ( newValue.equals("TURN_ON_APPLY")  ) {
					//System.out.println("PreferencePage ------  APPLY EVENT TURN ON GOOD CONFIG  GOODCONIF:"+isGoodConfig);
					if (!( isGoodConfig)  ) {
						//System.out.println("PreferencePage ------  APPLY EVENT TURN ON GOOD CONFIG");
						isGoodConfig=true;
						setValid(true);
						setVisible(true);
					}
					updateApplyButton();
				}
				if ( newValue.equals("TURN_OFF_APPLY")  ) {
					//System.out.println("PreferencePage ------  APPLY EVENT TURN OFF APPLY --- GOOD CONFIG");
					isGoodConfig=false;
					setValid(false);
					setVisible(true);
				}
			}
			if ( AuthenticationPreferences.PKI_CERTIFICATE_LOCATION.equals(property)) {
				if ( newValue != null) {
					setPkcs11InVisible();
					setPkcs12Visible(); 	
		        	pkcs11Certificate.setStringValue((String) "");
		        	pkiCertificate.setStringValue((String) newValue);
		        	isGoodConfig=true;
		        	setValid(true);
				}
			}
			if ( AuthenticationPreferences.PKCS11_CONFIGURE_FILE_LOCATION.equals(property)) {
				if ( newValue != null) {
					setPkcs12InVisible();
					setPkcs11Visible();
		        	pkiCertificate.setStringValue((String) "");
		        	pkcs11Certificate.setStringValue((String) newValue);
		        	isGoodConfig=true;
		        	setValid(true);
				}
			}
			
		} catch( Exception pageProcessorError) {
			pageProcessorError.printStackTrace();
		}
		
		return pageUpdate;
	}
	
	protected PKI previousPKI() {
		PKI pki = new PKI();
		PKIProperties current =  PKIProperties.getInstance();
		pkiSecureStorage = new PKISecureStorage();
		if (pkiSecureStorage.isPKISaved()) {
			pki.setSecureStorage(true);
		}
		current.load();
		pki.setKeyStore(current.getKeyStore());
		pki.setKeyStorePassword(current.getKeyStorePassword());
		pki.setKeyStoreProvider(current.getKeyStoreProvider());
		pki.setKeyStoreType(current.getKeyStoreType());
		return pki;	
	}
	
	public void printoutStore() {
		PrintStream ps = new PrintStream( System.out);
		
//		PreferenceStore store = (PreferenceStore) this.getPreferenceStore();
//		store.list ( ps );
		
		
	}
}
