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


import java.security.KeyStore;

import org.eclipse.core.pki.auth.EventConstant;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.pki.EventProcessor;
import org.eclipse.ui.pki.pkcs.VendorImplementation;
import org.eclipse.ui.pki.pkiselection.PKCSSelected;
import org.eclipse.ui.pki.pkiselection.PKCSpick;
import org.eclipse.ui.pki.AuthenticationPlugin;

import org.eclipse.core.pki.auth.PublicKeySecurity;


public class PKILoginWizard extends Wizard {
    
    protected int PAGE_WIDTH = 550; 
    protected int PAGE_HEIGHT = 100;
    private CertificateSelectionPage certSelectionPage;
    private String operation=null;
    private String lastPkiPick=null;;

    public PKILoginWizard() {
        super();
        this.setWindowTitle( "PKI Certificate Selection" );
        this.setHelpAvailable( false );
        this.setNeedsProgressMonitor( true );
        /*
         * NOTE:
         * Grab up the current selection, if any.
         * This will enable the software to restore to last 
         * selection if a cancel is pressed.
         */
        if (PKCSpick.getInstance().isPKCS12on() ) {
        	this.lastPkiPick = "pkcs12";
        } else if (PKCSpick.getInstance().isPKCS11on() ) {
        	this.lastPkiPick = "pkcs11";
        } else {
        	this.lastPkiPick = "NONE";
        }
    }
    @Override
    public void setWindowTitle( String s ) {
    	StringBuilder sb = new StringBuilder();
    	this.operation = s;
    	sb.append( "PKI Certificate ");
    	sb.append( this.operation );
    	super.setWindowTitle( sb.toString() );
    }
  
    @Override
    public void addPages() {
    	certSelectionPage = new CertificateSelectionPage();
    	this.addPage( certSelectionPage );
    }


    @Override
    public boolean canFinish() {
    	boolean finishState = false;
    	//System.out.println("PKILoginWizard ---  canFinish");
    	if	((certSelectionPage != null) && 
    		 (certSelectionPage.isPageComplete() ) ) {
    		finishState=true;
    	}
    	
    	return finishState;
    }
    @Override
    public boolean performCancel() {
    	certSelectionPage.setSaveCertificateChecked(false);
    	
    	if ( this.operation.equals("Selection")) {
    		EventProcessor.getInstance().sendEvent(EventConstant.CANCEL.getValue() );
    	} else {
    		Event event = new Event();
			event.doit = true;
			event.stateMask = 80000;
    		if ( this.lastPkiPick.equals("pkcs11") ) {
    			certSelectionPage.pkcs11Button.setSelection(true);
    			certSelectionPage.pkcs11Button.notifyListeners(SWT.Selection, event);
    		} else if ( this.lastPkiPick.equals("pkcs12") ) {
    			certSelectionPage.pkcs12Button.setSelection(true);
    			certSelectionPage.pkcs12Button.notifyListeners(SWT.Selection, event);
    		}
    	}
    	certSelectionPage.lastPropertyValues.restore();
    	return true;
    }
    @Override
    public boolean performFinish() {
    	String certificatePath = null;
    	KeyStore userKeyStore = certSelectionPage.getUserKeyStore();
    	//System.out.println("PKILoginWizard -- FINISH");
    	
		try {
			if(PKCSSelected.isPkcs11Selected()) {
	    		//certSelectionPage.setCertificatePath(Pkcs11Provider.configPath("NONE"));
	    		System.setProperty("javax.net.ssl.keyStore", "PKCS11");
	    		System.setProperty("javax.net.ssl.keyStoreType", "PKCS11");
	    		PKCSpick.getInstance().setPKCS11on(true);
	    		PKCSpick.getInstance().setPKCS12on(false);
	    		userKeyStore = VendorImplementation.getInstance().getKeyStore();
	    		certificatePath = "pkcs11";
	    		
	    	} 
	    	if(PKCSSelected.isPkcs12Selected()) {
	    		PKCSpick.getInstance().setPKCS11on(false);
	    		PKCSpick.getInstance().setPKCS12on(true);
	    		//System.setProperty("javax.net.ssl.keyStoreType", "PKCS12");
	    		certificatePath = certSelectionPage.getCertificatePath();
	    	}

	    	if ( null != userKeyStore ) {
	    		
	    		AuthenticationPlugin.getDefault().setUserKeyStore(userKeyStore);
	    		if(PKCSSelected.isPkcs11Selected()) {
	        		//certSelectionPage.setCertificatePath(Pkcs11Provider.configPath("NONE"));
	        	} else {
	        		if ( certificatePath != null ) {
	        			AuthenticationPlugin.getDefault().setCertificatePath( certificatePath );	
	        		
	        			AuthenticationPlugin.getDefault().setCertPassPhrase(
	        				certSelectionPage.getCertPassPhrase());
	        		}
	        	}
	    		
	    		
	    		//if the Change button is not pressed in the Preference window, then automatically save to 
	    		//secure storage. If it is pressed, then wait until OK or Apply button is clicked in the 
	    		//Preference window.

	    		//  MOVED pkiSecure Storage call to CertificateSelectionPage so that the path could be saved first.
	
	    		// Tell the page to finish its own business
	    		certSelectionPage.performFinish();
	    		PublicKeySecurity.INSTANCE.setupPKIfile();
	    		return true;
	    	}
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	return false;
    }
}
