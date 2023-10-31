package org.eclipse.pki.preferences;

import java.io.File;
import java.nio.file.FileSystems;
import java.security.KeyStore;
import java.security.KeyStoreException;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringButtonFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;

import org.eclipse.pki.auth.AuthenticationPlugin;
import org.eclipse.pki.exception.UserCanceledException;
import org.eclipse.pki.pkiselection.PKCSpick;
import org.eclipse.pki.pkiselection.PKIProperties;
import org.eclipse.pki.util.ChangedPressedFieldEditorStatus;

/**
 * This class can be used in an {@link FieldEditorPreferencePage} to represent an
 * pki certificate location..
 * @since 1.3
 *
 */
public class PKICertLocationFieldEditor extends StringButtonFieldEditor {
	private String type = null;
	String myName=null;
	private boolean validInput=true;
	private Composite father=null;
	boolean inFocus = true;
	protected KeyStore updatedKeystore=null;
	protected PKIProperties lastPropertyValues=null;
	PreferencePage preferencePage=null;
	protected final static String FOCUS_LOST="LOST_FOCUS";
	protected final static String FOCUS_GAINED="GAINED_FOCUS";
	private FocusAdapter focusAdapter=null;
	private Button changeButton = null;
	private static final String UPDATE_PREFERENCE = "Update";
	private static final String CSPext="java_pkcs11.cfg";
	public PKICertLocationFieldEditor(String name, String labelText,
			Composite parent, String pkiType, PreferencePage preferencePage) {
		
;		//System.out.println("PKICertLocationFieldEditor  CONSTRUCTOR open ");
		this.preferencePage = preferencePage;
		init(name, labelText);
		myName=name;
		type = pkiType;
		father=parent;
        setErrorMessage(JFaceResources
                .getString("DirectoryFieldEditor.errorMessage"));//$NON-NLS-1$
        
        setChangeButtonText(JFaceResources.getString("openChange"/*"openBrowse"*/));//$NON-NLS-1$
        //setValidateStrategy(VALIDATE_ON_FOCUS_LOST);
        //setValidateStrategy(VALIDATE_ON_KEY_STROKE | VALIDATE_ON_FOCUS_LOST);
        //setValidateStrategy(VALIDATE_ON_KEY_STROKE);
        //setValidateStrategy(UNLIMITED);
        
        if ( pkiType.equalsIgnoreCase("PKCS11")) {
        	setChangeButtonText("Update");
        }
        createControl(parent);
        //setPropertyChangeListener( listener() );
        getTextControl().addFocusListener( focus(oldValue) );
        lastPropertyValues = PKIProperties.getInstance();
        lastPropertyValues.load();
        
        if ( this.getChangeControl(parent) != null ) {
    		changeButton = this.getChangeControl(parent);
    	}
	}
	
	
	@Override
	protected boolean checkState() {
		//getPage().getControl().addFocusListener( focus() );
		//System.out.println("PKICertLocationFieldEditor  checkState is occring");
		checkValue();
		return validInput;
	}

	@Override
	public void store() {
		//System.out.println("PKICertLocationFieldEditor  store values is occring");
	}
	
	@Override
	protected void refreshValidState() {
		//System.out.println("PKICertLocationFieldEditor  refreshValidState");
		
		isValid();
	}
	@Override
	public boolean isValid() {
		
		//System.out.println("PKICertLocationFieldEditor  isValid check");		
		boolean isGood = false;
		
		try {
			if ( inFocus ) {
				isGood = checkValue();
			} else {
				isGood = true;
				super.isValid();
			}
				
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			isGood=false;
		}
		//System.out.println("PKICertLocationFieldEditor  isValid check   isVALID:"+isGood);
		return isGood;	
	}
	
	protected boolean checkValue() {
		//System.out.println("PKICertLocationFieldEditor  in checkValue");
		if ( type.equalsIgnoreCase("pkcs12")) {
			return true;
		}
		/*
		 * NOTE:   Turn off modifiable field and allow "update" button to be enabled.
		 */
		if ( type.equalsIgnoreCase("pkcs11")) {
			return true;
		}
		
		//System.out.println("PKICertLocationFieldEditor  --- You should not see this");
		//   NOTE:   after change above, the rest of code in this method wont run.
		String value = getStringValue();
		if ( isPathGood( value ) ) {
			validInput=true;
			changeButton.setEnabled(true);
			if ( isPathConfigGood(value)) {
				//System.out.println("PKICertLocationFieldEditor  --- turn on apply, but not yest");
				//this.preferencePage.pageChangeListener( myName, "VALIDATE", oldValue, "TURN_ON_APPLY");
			}
		} else {
			changeButton.setEnabled(false);
			validInput=false;
		}
		
		getTextControl(father);
		return validInput;
		
	}
//	@Override
//	public Text getTextControl(Composite parent) {
//		
//		Text text= super.getTextControl(parent);
//		if ( validInput ) {
//			text.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
//			System.out.println("PKICertLocationFieldEditor -- Set text color BLACK");
//		} else {
//			text.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
//			System.out.println("PKICertLocationFieldEditor -- Set text color RED");
//		}
//		return text;
//	}

	@Override
	protected String changePressed(){
		StringBuilder message = new StringBuilder();
		message.append("Reading credentials:");
        String fileName;       
        KeyStore existingUserKeyStore = null;
        KeyStore newUserKeyStore = null;
        String path =  this.getStringValue();
        try {
        	
        	
        	if ( type.equalsIgnoreCase("PKCS11")) { 
        		
        	}
        	if ( (this.getStringValue().equals(oldValue) )) {
        		//System.out.println("PKICertLocationFieldEditor --- NO CHANGE IN VALUES");
        		ChangedPressedFieldEditorStatus.setPkiChangedPressed(false);
        		ChangedPressedFieldEditorStatus.setJksChangedPressed(false);
        	} else {
        		ChangedPressedFieldEditorStatus.setPkiChangedPressed(true);
        	}
    		
    		existingUserKeyStore = AuthenticationPlugin.getDefault().getExistingUserKeyStore();	
    		ChangedPressedFieldEditorStatus.setPkiUserKeyStore(existingUserKeyStore);
    		
    		if(AuthenticationPlugin.getDefault().getExistingUserKeyStore() != null){
    			AuthenticationPlugin.getDefault().setUserKeyStore(null);
    		}	
    		
    		//System.out.println("PKICertLocationFieldEditor -- changePressed THIS POPS UP THE DISPAY TO CHANGE THE FLAVOR and IT DOES BLOCK");
    		/*
    		 * 
    		 * NOTE: The following code will pop up an authentication request dialog.  IT BLOCKS until either user has pressed cancel button or
    		 * entered the requested security info and successfully pressed finish button.
    		 * 
    		 */
    		//System.out.println("PKICertLocationFieldEditor   BEFORE");
    		newUserKeyStore = AuthenticationPlugin.getDefault().getUserKeyStore(this.UPDATE_PREFERENCE);
    		//System.out.println("PKICertLocationFieldEditor   AFTER");
    		/*
    		 * 
    		 *  NOTE:  AFTER blocking, code continues here.
    		 *  
    		 */
    		if ( newUserKeyStore == null) {
    			/*
    			 *  NOTE: if the keystore is equal NULL, then the user probably pressed cancel.  SO restore back to las setting.
    			 */
    			//System.out.println("PKICertLocationFieldEditor --- NO KEYSORE FOUND");
    			preferencePage.exitView = true;
    			this.preferencePage.pageChangeListener(getPreferenceName(),"VALIDATE", oldValue, "TURN_OFF_APPLY");
    			ChangedPressedFieldEditorStatus.setPkiChangedPressed(false);
    			if ( existingUserKeyStore != null ) {
        			//System.out.println("PKICertLocationFieldEditor   ORIGINAL KEYSTORE TYPE= "+ existingUserKeyStore.getType() );
        			AuthenticationPlugin.getDefault().setUserKeyStore(existingUserKeyStore);
        			if ( "PKCS11".equalsIgnoreCase(existingUserKeyStore.getType()) ) {
        				PKCSpick.getInstance().setPKCS11on( true );
        				PKCSpick.getInstance().setPKCS12on( false );
        				lastPropertyValues.setKeyStoreType(existingUserKeyStore.getType());
        				lastPropertyValues.restore();
        				//AuthenticationPlugin.getDefault().setCertificatePath(Pkcs11Provider.getConfigurePath());
        				
        				
        				AuthenticationPlugin.getDefault().setCertificatePath("pkcs11");
        				
            			AuthenticationPlugin.getDefault().setUserKeyStoreSystemProperties(existingUserKeyStore);
            			AuthenticationPlugin.getDefault().setTrustStoreSystemProperties(AuthenticationPlugin.getDefault().obtainDefaultJKSTrustStore());
        			} else if ( "PKCS12".equalsIgnoreCase(existingUserKeyStore.getType()) ) {
        				PKCSpick.getInstance().setPKCS12on( true );
        				PKCSpick.getInstance().setPKCS11on( false );
        				lastPropertyValues.setKeyStoreType(existingUserKeyStore.getType());
        				//debugging
        				//lastPropertyValues.dump();
        				lastPropertyValues.setKeyStoreProvider(existingUserKeyStore.getProvider().getName());
        				lastPropertyValues.restore();
        				AuthenticationPlugin.getDefault().setCertificatePath(lastPropertyValues.getKeyStore());
            			AuthenticationPlugin.getDefault().setUserKeyStoreSystemProperties(existingUserKeyStore);
            			AuthenticationPlugin.getDefault().setTrustStoreSystemProperties(AuthenticationPlugin.getDefault().obtainDefaultJKSTrustStore());
        			} else {
        				PKCSpick.getInstance().setPKCS12on( false );
        				PKCSpick.getInstance().setPKCS11on( false );
        				AuthenticationPlugin.getDefault().initialize();
        			}
        	
        		} else {
        			//System.out.println("PKICertLocationFieldEditor --- RESET EVERYTHING   OH NO VALUES");
        			PKCSpick.getInstance().setPKCS12on( false );
    				PKCSpick.getInstance().setPKCS11on( false );
    				AuthenticationPlugin.getDefault().initialize();
        		}
    			
    		} else {
    			//System.out.println("PKICertLocationFieldEditor  - NEW KEYSTORE ENTERED");
    			ChangedPressedFieldEditorStatus.setPkiChangedPressed(true);
    			ChangedPressedFieldEditorStatus.setPkiUserKeyStore(newUserKeyStore);
    		}
    		//ChangedPressedFieldEditorStatus.setJksChangedPressed(false);
    	}  catch (UserCanceledException e) {
    		
        //} catch (Exception e) {
    		ChangedPressedFieldEditorStatus.setPkiChangedPressed(false);
    		ChangedPressedFieldEditorStatus.setPKISaveCertificateChecked(false);
    		ChangedPressedFieldEditorStatus.setPkiUserKeyStore(null);
    		
    		AuthenticationPlugin.getDefault().setUserKeyStore(existingUserKeyStore);
    		AuthenticationPlugin.getDefault().setCertificatePath(AuthenticationPlugin.getDefault().obtainSystemPropertyPKICertificatePath());
    		AuthenticationPlugin.getDefault().setCertPassPhrase(AuthenticationPlugin.getDefault().obtainSystemPropertyPKICertificatePass());
    		
    		System.out.println("PKICertLocationFieldEditor:Unable to set javax.net.ssl properties!");
		}
        
        fileName = AuthenticationPlugin.getDefault().getCertificatePath();
     
        if (( null != fileName ) && (!fileName.isEmpty())) {
            fileName = AuthenticationPlugin.getDefault().getCertificatePath();
        	if ( PKCSpick.getInstance().isPKCS11on()) {
        		//System.out.println("PKICertLocationFieldEditor -- EVENT FOR newVALUE:"+fileName+" oldVALUE:"+oldValue+" THIS PREFRENCE:"+getPreferenceName());
        		this.preferencePage.pageChangeListener(getPreferenceName(),AuthenticationPreferences.PKCS11_CONFIGURE_FILE_LOCATION, oldValue, fileName);
        	} else {
        		this.preferencePage.pageChangeListener(getPreferenceStore(),AuthenticationPreferences.PKI_CERTIFICATE_LOCATION, oldValue, fileName);
        	}
        	return fileName;
        } else {
        	if (((this.getStringValue() != null)) && (!this.getStringValue().isEmpty())) {
        		return this.getStringValue();
        	}
        }
		
        return null;
	}

	private FocusAdapter focus(final String oldValue) {
		focusAdapter = new FocusAdapter() {
			public void focusGained(FocusEvent e) {
				//System.out.println("PKICertLocationFieldEditor focus gained");
				//getPreferenceStore().firePropertyChangeEvent("FOCUS", oldValue, FOCUS_GAINED);
				preferencePage.pageChangeListener(myName, "FOCUS", oldValue, FOCUS_GAINED);
			}
			public void focusLost(FocusEvent e) {
				//System.out.println("PKICertLocationFieldEditor focus LOST ");
				inFocus = false;
				preferencePage.pageChangeListener(myName,"FOCUS", oldValue, FOCUS_LOST);
				//getPreferenceStore().firePropertyChangeEvent("FOCUS", oldValue, FOCUS_LOST);
			}
		};
		return focusAdapter;
	}
	private boolean isPathGood( String s ) {
		boolean isGood=false;
		File f = null;
		try {
			isGood = new File( s ).exists();
		} catch( Exception e ){}
		return isGood;
	}
	private boolean isPathConfigGood( String s ) {
		boolean goodConfig=false;
		StringBuilder sb = new StringBuilder();
		try {
			sb.append(s);
			sb.append(File.separator);
			sb.append("java_pkcs11.cfg");
			goodConfig = new File( sb.toString() ).exists();
		} catch( Exception e ){}
		
		
		return goodConfig;
		
	}
}
