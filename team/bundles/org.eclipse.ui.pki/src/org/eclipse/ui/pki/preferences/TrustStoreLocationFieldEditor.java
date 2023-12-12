package org.eclipse.ui.pki.preferences;

import java.security.KeyStore;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringButtonFieldEditor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.pki.preferences.ChangedPressedFieldEditorStatus;
import org.eclipse.ui.pki.AuthenticationPlugin;
import org.eclipse.pki.exception.UserCanceledException;

/**
 * This class can be used in an {@link FieldEditorPreferencePage} to represent a trust store
 * location. Validation is performed as the user enters a new value.
 * @since 1.3
 *
 */
public class TrustStoreLocationFieldEditor extends StringButtonFieldEditor {

	public TrustStoreLocationFieldEditor(String name, String labelText,
			Composite parent) {		
		init(name, labelText);
		setEmptyStringAllowed(false);
        setErrorMessage(JFaceResources
                .getString("DirectoryFieldEditor.errorMessage"));//$NON-NLS-1$
        setChangeButtonText(JFaceResources.getString("openChange"/*"openBrowse"*/));//$NON-NLS-1$
        setValidateStrategy(VALIDATE_ON_FOCUS_LOST);        
        createControl(parent);
	}


	@Override
	protected String changePressed(){
		ChangedPressedFieldEditorStatus.setJksChangedPressed(true);
        String fileName;
        KeyStore existingJKSTrustStore = null;
    	try{
    		
    		existingJKSTrustStore = AuthenticationPlugin.getDefault().getExistingTrustStore();
    		    		
    		ChangedPressedFieldEditorStatus.setJksTrustStore(existingJKSTrustStore);
    		
    		if(AuthenticationPlugin.getDefault().getExistingTrustStore() != null){
    			AuthenticationPlugin.getDefault().setTrustStore(null);
    		}
    		
        	AuthenticationPlugin.getDefault().getJKSTrustStore(); 
        	
    	}  catch (UserCanceledException e) {
    		
    		ChangedPressedFieldEditorStatus.setJksChangedPressed(false);
    		ChangedPressedFieldEditorStatus.setJKSSaveTrustStoreChecked(false);
    		ChangedPressedFieldEditorStatus.setJksTrustStore(null);
    		
    		AuthenticationPlugin.getDefault().setTrustStore(existingJKSTrustStore);
    		AuthenticationPlugin.getDefault().getPreferenceStore().setValue(
					AuthenticationPreferences.TRUST_STORE_LOCATION, AuthenticationPlugin.getDefault().obtainSystemPropertyJKSPath());
    		AuthenticationPlugin.getDefault().setTrustStorePassPhrase(AuthenticationPlugin.getDefault().obtainSystemPropertyJKSPass());
    		
			//System.out.println("Unable to set javax.net.ssl properties!");
		}        
        
        fileName = AuthenticationPlugin.getDefault().getPreferenceStore().getString(AuthenticationPreferences.TRUST_STORE_LOCATION);
        
        if ( null != fileName ) {
        	return fileName;
        }
		        
        return null;
	}
}