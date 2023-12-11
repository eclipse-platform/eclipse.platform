package org.eclipse.ui.pki.wizard;

import java.security.KeyStore;
import org.eclipse.jface.wizard.Wizard;

import org.eclipse.ui.pki.AuthenticationPlugin;
import org.eclipse.ui.pki.preferences.AuthenticationPreferences;
import org.eclipse.ui.pki.preferences.ChangedPressedFieldEditorStatus;
import org.eclipse.ui.pki.wizard.TrustStoreSecureStorage;

public class TrustStoreLoginWizard extends Wizard {
	
	private TrustStoreSelectionPage truststorePage;

	public TrustStoreLoginWizard() {
		super();
        this.setWindowTitle("Trust Store Selection");
        this.setHelpAvailable(false);
        this.setNeedsProgressMonitor(true);
	}

	@Override
	public boolean performFinish() {
		/*
		 * check if changes made and to store new info into secure storage or not and to remove secure storage if not.
		 */
    	KeyStore truststorekeystore = truststorePage.getTrustStore();

    	if ( null != truststorekeystore ) {
    		
    		//Set trust store to user entered values.
    		AuthenticationPlugin.getDefault().setTrustStore(truststorekeystore);
    		AuthenticationPlugin.getDefault().setTrustStorePassPhrase(truststorePage.getPasswordText());
    		AuthenticationPlugin.getDefault().getPreferenceStore().setValue(AuthenticationPreferences.TRUST_STORE_LOCATION, 
    				truststorePage.getTruststoreJKSPath());    		
			
    		TrustStoreSecureStorage truststoreSecureStorage = new TrustStoreSecureStorage();
    		
    		//if the Change button is not pressed in the Preference window, then automatically save to 
    		//secure storage. If it is pressed, then wait until OK or Apply button is clicked in the 
    		//Preference window.
    		if(!ChangedPressedFieldEditorStatus.isJksChangedPressed()){
        		if(truststorePage.isPasswordSaveChecked()){    			
        			truststoreSecureStorage.storeJKS(AuthenticationPlugin.getDefault());
        		} else {
        			truststoreSecureStorage.getNode().removeNode();
        		} 
    		}   		

    		// Tell the page to finish its own business
    		truststorePage.performFinish();

    		return true;
    	}
		return false;
	}
	
    @Override
    public void addPages() {
    	truststorePage = new TrustStoreSelectionPage();
    	this.addPage(truststorePage);
    }
    
    @Override
    public boolean canFinish() {
    	if(truststorePage != null && 
    			truststorePage.isPageComplete())
    		return true;
    	
    	return false;
    }

}
