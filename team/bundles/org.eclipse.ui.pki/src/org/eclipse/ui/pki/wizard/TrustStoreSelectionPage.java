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

import java.io.File;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import org.eclipse.core.pki.util.LogUtil;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.pki.preferences.ChangedPressedFieldEditorStatus;
import org.eclipse.core.pki.util.KeyStoreFormat;
import org.eclipse.ui.pki.util.KeyStoreUtil;
import org.eclipse.ui.pki.AuthenticationPlugin;
import org.eclipse.ui.pki.PKIController;
import org.eclipse.ui.pki.preferences.AuthenticationPreferences;

public class TrustStoreSelectionPage extends WizardPage {
	
	private final static String PAGENAME = "Default Authentication - Trust Store Selection Page";
	private final static String TITLE = "Trust Store Authentication";
	private final static String DESCRIPTION = "Select Trust Store";
	private final static String EMPTY_STRING = "";
	private final static String OPENING_INSTRUCTIONS =
            "This application allows users to select the Java keystore file. "
            + "If the default trust store in your home directory is used, the default password will automatically appear in the password field.";
    private final static String SELECTION_INSTRUCTIONS =
            "Please select the Java keystore by clicking on the Browse... button. If using a different "
            + "trust store (jks file), delete the default password and enter the password for the new trust store.";
   	
	private Text truststorePathText = null;
	private Button selecttruststore = null;
	private Text passwordText = null;
	private String truststoreJKSPath = null;
	
	private boolean isPasswordSaveChecked = false;
	
	protected Button passwordSavedCheckbox;
	
	

	public TrustStoreSelectionPage() {
		super(PAGENAME);
		setTitle(TITLE);
		setDescription(DESCRIPTION);
	}

	public void createControl(Composite parent) {
		// Create a main composite for the page
        final Composite main = new Composite( parent, SWT.NONE );
        main.setLayoutData( new GridData( GridData.FILL_BOTH ) );
        main.setLayout( new GridLayout() );
        
        // Add section for instructions
        addInstructionsSection( main );        
        
        // Add section for trust store details, including file and password
        addTrustStoreDetailSection(main);
        
        
        // set focus on password entry if trust store path already set
        if (truststorePathText != null && truststorePathText.getText() != null &&
        		!"".equals(truststorePathText.getText().trim()) &&
        		passwordText != null) {
        	passwordText.setFocus();
        }
        
        // Finishing touches
        this.setPageComplete( true );
        this.setControl( main );

	}
	
	private Composite addInstructionsSection(final Composite parent) {
        // Create composite for the section
        Composite sectionComposite = new Composite( parent, SWT.NONE );
        GridLayout subsectionCompositeLayout = new GridLayout();
        subsectionCompositeLayout.horizontalSpacing = 10;
        subsectionCompositeLayout.verticalSpacing = 10;
        sectionComposite.setLayout( subsectionCompositeLayout );
        sectionComposite.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );

        // Calculate initial width
        int widthHint = computeWidthHint( this.getContainer().getShell() );        

        // Create labels for each set of instructions
        Label openingInstructionsLabel = new Label( sectionComposite, SWT.WRAP );
        GridData labelGridData = new GridData( GridData.FILL_BOTH );
        labelGridData.widthHint = widthHint;
        openingInstructionsLabel.setLayoutData( labelGridData );
        openingInstructionsLabel.setText( OPENING_INSTRUCTIONS );
        
        Label selectionInstructionsLabel = new Label( sectionComposite, SWT.WRAP );
        labelGridData = new GridData( GridData.FILL_BOTH );
        labelGridData.widthHint = widthHint;
        selectionInstructionsLabel.setLayoutData( labelGridData );
        selectionInstructionsLabel.setText( SELECTION_INSTRUCTIONS );
        
        return sectionComposite;
		
	}

    private int computeWidthHint( Shell shell ) {
        double ratio = .50d;
        double boundsWidth = ( double ) shell.getBounds().width;
        int widthHint = ( int ) ( boundsWidth * ratio );
        if ( widthHint < 400 ) {
            widthHint = 400;
        }

        return widthHint;
    }

	private Composite addTrustStoreDetailSection(final Composite parent){
		final Composite sectionComposite = new Composite( parent, SWT.NONE );
        sectionComposite.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );
        GridLayout sectionCompositeLayout = new GridLayout( 2, false );
        sectionCompositeLayout.verticalSpacing = 1;
        sectionComposite.setLayout( sectionCompositeLayout );
        
        //Add section for the trust store selection
        addTrustStoreSelection(sectionComposite);
        
        // Add section for password
        addPasswordSubsection( sectionComposite );
        
        //Add section for saving certificate checkbox
        addSavePasswordCheckbox(sectionComposite);
        
        return sectionComposite;
	}

	
	private void addSavePasswordCheckbox(Composite sectionComposite) {
		//Create label for saving the certificate
		Label saveCertificateLabel = new Label(sectionComposite, SWT.RIGHT);
		saveCertificateLabel.setText("Save Trust Store Information: ");
		
		//Create layout and controls
		Composite subsectionComposite = new Composite(sectionComposite, SWT.NONE);
		GridLayout subsectionCompositeLayout = new GridLayout(2, false);
		subsectionComposite.setLayout(subsectionCompositeLayout);
		subsectionComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
					
		passwordSavedCheckbox = new Button(subsectionComposite, SWT.CHECK);
		passwordSavedCheckbox.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		
		passwordSavedCheckbox.addSelectionListener(new SelectionListener(){

			public void widgetSelected(SelectionEvent e) {
				isPasswordSaveChecked = passwordSavedCheckbox.getSelection();
				ChangedPressedFieldEditorStatus.setJKSSaveTrustStoreChecked(isPasswordSaveChecked);
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub
				
			}
			
		});
		
	}

	private void addPasswordSubsection(Composite sectionComposite) {
        // Create controls for specifying the password
        Label passwordLabel = new Label( sectionComposite, SWT.RIGHT );
        passwordLabel.setText( "Trust Store Password: " );
        
        // Create layout and controls for manually selecting certs
        Composite subsectionComposite = new Composite( sectionComposite, SWT.NONE );
        GridLayout subsectionCompositeLayout = new GridLayout( 2, false );
        subsectionCompositeLayout.horizontalSpacing = 10;
        subsectionCompositeLayout.verticalSpacing = 10;
        subsectionComposite.setLayout( subsectionCompositeLayout );
        subsectionComposite.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );

        passwordText = new Text( subsectionComposite, SWT.LEFT | SWT.PASSWORD | SWT.BORDER );
        passwordText.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );
        if(isUsingDefaultTrustStore(truststorePathText.getText().trim())){
        	passwordText.setText(AuthenticationPlugin.getDefaultTrustStorePassword());
        }

        ModifyListener modifyListener = new ModifyListener() {
            public void modifyText( ModifyEvent e ) {
            	TrustStoreSelectionPage.this.setErrorMessage( null );
            	TrustStoreSelectionPage.this.getContainer().updateButtons();
            }
        };
        passwordText.addModifyListener( modifyListener );
        Composite spacer = new Composite( subsectionComposite, SWT.NONE );
        spacer.setLayoutData( new GridData( GridData.FILL_BOTH ) );
        spacer.setLayout( new GridLayout() );
		
	}

	private void addTrustStoreSelection(final Composite sectionComposite) {
		 // Create label for selecting the trust store
        Label certificateLabel = new Label( sectionComposite, SWT.RIGHT );
        certificateLabel.setText( "Trust Store Location: " );
        
        // Create layout and controls for manually selecting trust store
        Composite subsectionComposite = new Composite( sectionComposite, SWT.NONE );
        GridLayout subsectionCompositeLayout = new GridLayout( 2, false );
        subsectionComposite.setLayout( subsectionCompositeLayout );
        subsectionComposite.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );

        truststorePathText = new Text( subsectionComposite, SWT.LEFT | SWT.BORDER );
        truststorePathText.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );
        AuthenticationPlugin plugin = AuthenticationPlugin.getDefault();
        String preferencesKey = AuthenticationPreferences.TRUST_STORE_LOCATION;
        IPreferenceStore preferenceStore = plugin.getPreferenceStore();
        String truststorePath = preferenceStore.getString( preferencesKey );
        if ( null != truststorePath ) {
        	truststorePathText.setText( truststorePath );
        }

        selecttruststore = new Button( subsectionComposite, SWT.PUSH );
        selecttruststore.setText( "Browse..." );
        BrowseButtonListener listener = new BrowseButtonListener();
        selecttruststore.addSelectionListener( listener );
	}
	
	private boolean isUsingDefaultTrustStore(String trustStorePath){
		boolean usingDefaultTrustStore = false;
		
		final String defaultTrustStorePath = new File(PKIController.PKI_ECLIPSE_DIR, "cacerts").getPath();
		 
		if(trustStorePath.equals(defaultTrustStorePath)){
			usingDefaultTrustStore = true;
		}
		
		return usingDefaultTrustStore;
	}
	
	/* (non-Javadoc)
     * @see org.eclipse.jface.wizard.WizardPage#isPageComplete()
     */
    @Override
    public boolean isPageComplete() {
    	
        // Get path
        String truststorePath = null;
        if ( null != truststorePathText ) {
            truststorePath = truststorePathText.getText().trim();
        }
        
        // Test path
        if ( null != truststorePath  && !truststorePath.equals(EMPTY_STRING))
             	return true;
        
        return false;
    }

    
    protected void performFinish() {
        try {
            // Persist the selected path, if specified
            if ( ( null != truststorePathText ) &&
                 ( null != truststorePathText.getText() ) ) {
                String path = truststorePathText.getText().trim();
                truststoreJKSPath = path;
                if ( !path.equals( "" ) ) {
                    AuthenticationPlugin plugin = AuthenticationPlugin.getDefault();
                    String key = AuthenticationPreferences.TRUST_STORE_LOCATION;
                    IPreferenceStore preferenceStore = 
                        plugin.getPreferenceStore();
                    preferenceStore.setValue( key, path );                                      
                }
            }
            
        } catch ( Throwable t ) {
        	LogUtil.logError(t.getMessage(), t);
        }
    }
    
    
    protected KeyStore getTrustStore(){
    	String errormsg = null;
    	KeyStore keyStore = null;
        String truststorePath = truststorePathText.getText();
        truststoreJKSPath = truststorePath;
        String password = passwordText.getText();
        try {
			keyStore = KeyStoreUtil.getKeyStore(truststorePath, password, KeyStoreFormat.JKS);
		} catch (KeyStoreException e) {
			errormsg = "The Java key store can not be loaded.";
		} catch (NoSuchAlgorithmException e) {
			errormsg = "The algorithm used to check the integrity of the jks file cannot be found.";
		} catch (CertificateException e) {
			errormsg = "The jks file can not be loaded.";
		} catch (IOException e) {
			errormsg = "There is a problem with the password or problem with the jks file data. Please try a different password.";			
		} catch (Exception e){
			errormsg = "Unexpected error occurred.";
		}
        
        if(errormsg != null){
        	this.setErrorMessage(errormsg);
        }        
        
        // Return the result
        return keyStore;
    }
	
	
	
    public Text getTruststorePathText() {
		return truststorePathText;
	}



	public Button getSelecttruststore() {
		return selecttruststore;
	}



	public boolean isPasswordSaveChecked() {
		return isPasswordSaveChecked;
	}

	public void setPasswordSaved(boolean isPasswordSaved) {
		this.isPasswordSaveChecked = isPasswordSaved;
	}
	
    @Override
    public Control getControl() {
        return super.getControl();
    }



	public String getTruststoreJKSPath() {
		return truststoreJKSPath;
	}



	public String getPasswordText() {
		return passwordText.getText();
	}



	private class BrowseButtonListener extends SelectionAdapter {
        @Override
        public void widgetSelected( SelectionEvent e ) {
            Shell shell = selecttruststore.getShell();
            FileDialog dialog = new FileDialog( shell, SWT.SINGLE );
            String fileName = truststorePathText.getText();
            if ( ( null != fileName ) && 
                 ( !fileName.trim().equals( "" ) ) ) {
                File path = new File( fileName );  
                if ( path.exists() ) {
                    if ( path.isDirectory() ) {
                        dialog.setFilterPath( path.getAbsolutePath() );
                    } else {
                        dialog.setFileName( path.getAbsolutePath() );
                    }
                }
            }
            String[] filterNames = {"*"};
            String[] filterExts = {"*.jks"};
            dialog.setFilterNames( filterNames );
            dialog.setFilterExtensions( filterExts );
            fileName = dialog.open();
            if ( null != fileName ) {
            	truststorePathText.setText( fileName );
            	if(isUsingDefaultTrustStore(truststorePathText.getText().trim())){
                	passwordText.setText(AuthenticationPlugin.getDefaultTrustStorePassword());
                } else {
                	passwordText.setText("");
                }
            	TrustStoreSelectionPage.this.getContainer().updateButtons();
            }
        }
    }
}