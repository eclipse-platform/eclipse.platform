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
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.core.pki.FingerprintX509;
import org.eclipse.core.pki.auth.PKIState;
import org.eclipse.core.pki.pkiselection.PKI;
import org.eclipse.core.pki.pkiselection.PKIProperties;
import org.eclipse.core.pki.util.LogUtil;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
//import org.eclipse.jface.dialogs.MessageDialog;
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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.pki.buttons.PKCSButtons;
import org.eclipse.ui.pki.pkcs.VendorImplementation;
import org.eclipse.ui.pki.pkiselection.PKCSSelected;
//import org.eclipse.ui.pki.pkiselection.PKCSpick;
import org.eclipse.ui.pki.preferences.ChangedPressedFieldEditorStatus;
import org.eclipse.core.pki.util.KeyStoreFormat;
import org.eclipse.ui.pki.util.KeyStoreUtil;
import org.eclipse.ui.pki.util.PKISecureStorage;
import org.eclipse.ui.pki.AuthenticationPlugin;
import org.eclipse.ui.pki.preferences.AuthenticationPreferences;

 
 
 
public class CertificateSelectionPage extends WizardPage  {
    
    private final static String NAME = "Default Authentication - Certificate Selection Page"; 
    private final static String DESCRIPTION = "Choose your method of authentication, PKCS11 or PKCS12"; 
//SWM: 20200305    private final static String TITLE = "PKI Authentication"; 
    private final static String TITLE = "PKI Certificate Selection"; 
    private final static String EMPTY_STRING = "";
    private final static String[] INITIAL_SLOT = {"There has been no certificate selected from store, please select your DS slot."} ;
    private final static String OPENING_INSTRUCTIONS =
        "This application authenticates users using their X509 corporate-issued " +
        "PKI certificates and certificate passwords.  For more information " +
        "about PKI certificates, type \"go pki\" in your web browser, " +
    	"or to find out about the PKCS11 software product  type \"pkcs11\" in your web browser.";
    private final static String PKCS12_SELECTION_INSTRUCTIONS =
        "Please select the PKI Digital Signature certificate provided to " +
        "you by the Public Key Infrastructure Group.  This X509 " +
        "certificate will originally include a \"D\" or \"DS\" in its " +
        "name and is normally located in " + 
        AuthenticationPreferences.DEFAULT_PKI_CERTIFICATE_DIR + ".";
    private final static String PKCS11_SELECTION_INSTRUCTIONS =
    		 "Select the credential you would like to use during this session. ";
    PKISecureStorage pkiSecureStorage = null;
    protected PKIProperties lastPropertyValues=null;
    protected static String[] slots=null;
    protected static int certificateIndex=0;
    protected static String selectedAlias=null;
    private Text certPathText = null;
    private Button pkcs12selectCert = null;
    private Button checkPasswordButton = null;
    private Button checkPinButton = null;
    private Text passwordText = null;
    private Label passwordLabel;
    private Text pinText = null;
    private ProgressBar progressBar = null;
    private Label pkcs12certificateLabel = null; 
    private boolean finishOK=false;
    private boolean validPasswd=false;
    private Label pkcs11PasswordLabel = null;
    private Composite buttonGroup = null;
    public Button pkcs11Button = null;
   
     
    private Label pkcs12PasswordLabel = null;
     
    public Button pkcs12Button = null;
    public Button fakeButton =  null;
    private Text pkcs12PasswordTextBox = null;
     
    private boolean isSaveCertificateChecked = false;
    private boolean isButtonSelected = false;
    
    protected Button saveCertificateCheckBox;
    
    protected PKCSButtons pkcsbuttons = null;
    
    protected Combo combo = null;
        
    public CertificateSelectionPage()  {
        super( NAME );
        setTitle( TITLE );
        setDescription( DESCRIPTION );
        try {
        	lastPropertyValues = PKIProperties.getInstance();
        	lastPropertyValues.setLastPkiValue(setLastInstance( lastPropertyValues ));
        	pkiSecureStorage = new PKISecureStorage();
        	if ((!(PKIState.CONTROL.isPKCS11on())) && ((!(PKIState.CONTROL.isPKCS12on())))) {
        		 PKCSSelected.setPkcs11Selected(false);
        		 PKCSSelected.setPkcs12Selected(false);
        		
        	}
        	if ( PKIState.CONTROL.isPKCS11on()) {
        		
        		AuthenticationPlugin.getDefault().getPreferenceStore().setValue(
     					AuthenticationPreferences.PKI_SELECTION_TYPE, "PKCS11");
        		slots = selectFromPkcs11Store();
                PKCSSelected.setKeystoreformat(KeyStoreFormat.PKCS11);
                PKCSSelected.setPkcs11Selected(true);
                isButtonSelected=true;
                
        	} else {
        		if ( PKIState.CONTROL.isPKCS12on()) {
        			PKCSSelected.setKeystoreformat(KeyStoreFormat.PKCS12);
        			AuthenticationPlugin.getDefault().getPreferenceStore().setValue(
     					AuthenticationPreferences.PKI_SELECTION_TYPE, "PKCS12");
        			PKCSSelected.setPkcs12Selected(true);
        			isButtonSelected=true;
        		
        		} else {
        			isButtonSelected=false;
        		}
        		
        	}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        //MessageDialog.openConfirm(Display.getCurrent().getActiveShell(), "OK", "HERE IS WHERE WE LOAD PKCS11.");
    }
    
    
    private Composite addCertificateDetailsSection( final Composite parent ) {       
        
        //This composite is used for the remaining methods.
        //final Composite sectionComposite = new Composite( parent, SWT.NONE );
        final Composite sectionComposite = new Composite( parent, SWT.NO_RADIO_GROUP );
        sectionComposite.setLayoutData( new GridData(SWT.FILL, SWT.FILL, true, true  ) );
        GridLayout sectionCompositeLayout = new GridLayout( 2, false );
        sectionCompositeLayout.verticalSpacing = 1;
        sectionComposite.setLayout( sectionCompositeLayout );               
               
        //This composite is used for the addPKITypeButtons
        //final Composite sectionComposite1 = new Composite( parent, SWT.NONE );
        final Composite sectionComposite1 = new Composite( parent, SWT.NO_RADIO_GROUP );
        sectionComposite1.setLayoutData( new GridData(SWT.FILL, SWT.FILL, true, true  ) );
        GridLayout sectionCompositeLayout1 = new GridLayout( 1, false );
        sectionCompositeLayout1.verticalSpacing = 1;
        sectionComposite1.setLayout( sectionCompositeLayout1 ); 
        
        addButtonGroup(parent);
        
        //Add pkcs12 section
        addPKCS12Section(sectionComposite1);
        
        // Add pkcs11 section
        addPKCS11Section(sectionComposite);
        
        //Add pkcs11 and pkcs12 widget selection.
        addPKCSWidgetSelection();
        
        if (PKIState.CONTROL.isPKCS12on()) {
        	//this.pkcs12Button.setSelection(true);
        	this.pkcs12Button.setFocus();
        	pkcs12Actions();
        } else {
        	if (PKIState.CONTROL.isPKCS11on() ) {
        		this.pkcs11Button.setSelection(true);
        		//Not sure if add this setFocus() to cspid.
        		//this.pkcs11Button.setFocus();
        		pkcs11Actions();
        	} else {
        	//  else NO buttons are selected!  Make everything GREY!
        		this.deSelectAll();
        		
        		pkcs11Button.setSelection(false);
            	pkcs12Button.setSelection(false);
            	fakeButton.setSelection(true);
        	}
        }
        
        return sectionComposite;
    }
    
    private void addPKCSWidgetSelection() {

        pkcs11Button.addSelectionListener(new SelectionAdapter(){
            @Override
            public void widgetSelected(SelectionEvent e) {
            	/*
            	 * TODO:
            	 * Figure out why WINDOZ does not set radio buttons correctly.  
            	 * The FAKE button allows the real button to be de-selected at STARTUP only, then
            	 * the buttons work correclty.
            	 */
            	if ( fakeButton.getSelection() ) {
            		//System.out.println("CertificateSelectionPage -  FAKE BUTTON IS SELECTED");
            		//pkcs11Button.setSelection(false);
            		fakeButton.setSelection(false);;
            		
            		//return;
            	}
            	/**************************************************************************************/
            	Event event = new Event();
            	//System.out.println("CertificateSelectionPage - 11-BUTTON:"+pkcs11Button.getSelection());
            	//System.out.println("CertificateSelectionPage - 11-BUTTON  INCOMING:"+e.toString());
                if ((pkcs11Button.getSelection()) && ((e.doit==true)) ) {
                	if ( VendorImplementation.getInstance().isInstalled() ) {	
                		CertificateSelectionPage.this.setMessage("Please enter your PKCS11 PiN.", WizardPage.INFORMATION);
                		pinText.setEnabled(true);
                        passwordLabel.setEnabled(true);
                        checkPinButton.setEnabled(true);
                        pkcs11PasswordLabel.setEnabled(false);
                        combo.setText("");
                        combo.setEnabled(false);
                        deSelectPkcs12();
	                	if ( VendorImplementation.getInstance().isEnabled() ) {
	                		slots = selectFromPkcs11Store();
	                        if ( passwordText != null ) {
	                        	passwordText.setText("");
	                        }
	                	} else {
	                		//System.out.println("CertificateSelectionPage --  pkcs11 is NOT enabled");
	                		//AuthenticationPlugin.getDefault().getLog().log(new Status(IStatus.OK, AuthenticationPlugin.getPluginId()+":CertificateSelectionPage","PKCS11 CHECK"));
	                		try {
								if ( VendorImplementation.getInstance().isInstalled() ) { 
									//AuthenticationPlugin.getDefault().getLog().log(new Status(IStatus.OK, AuthenticationPlugin.getPluginId()+":CertificateSelectionPage","PKCS11 IS ALIVE"));
								} else {
									// Disable the PKCS11 button, becuase its not configured on this machine.
									event = new Event();
			                		event.doit = false;
			                		event.stateMask = 80000;
			                		pkcs11Button.notifyListeners(SWT.Selection, event);
								}
							} catch (Exception e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
	                		isPageComplete();
	                	}
	                //
	                // if we get here someone selected PKCS11 on a machine that does not have PKCS11 installed.
	                //
                	} else {
                    	//System.out.println("CertificateSelectionPage ---------------FALSE-----------------  PKCS11 IS TURNED OFF");
                    	pkcs11Button.setSelection(false);
                    	pkcs12Button.setSelection(true);
                    	VendorImplementation.getInstance().off();
                    	PKIState.CONTROL.setPKCS11on(false);
                	}
                    //isPageComplete();
                } else {
                     
                	System.out.println("CertificateSelectionPage ---------CLEAR------FALSE-----------------  PKCS11 IS TURNED OFF");
                	pkcs11Button.setSelection(false);
                	pkcs12Button.setSelection(false);
                	VendorImplementation.getInstance().off();
                	deSelectAll();
					System.clearProperty("javax.net.ssl.keyStoreType");
					System.clearProperty("javax.net.ssl.keyStoreProvider");
                	PKIState.CONTROL.setPKCS11on(false);
                }
                
                if ( isPageComplete() ) {
                	//System.out.println("CertificateSelectionPage ---------------------  PKCS11 FINISH BUTTON IS GOOD");
                }
            }
        });
         
        pkcs12Button.addSelectionListener(new SelectionAdapter(){
            @Override
            public void widgetSelected(SelectionEvent e) {
            	//System.out.println("CertificateSelectionPage - 12-BUTTON:"+pkcs12Button.getSelection());
            	//System.out.println("CertificateSelectionPage - 12-BUTTON  INCOMING:"+e.toString());
                if ((pkcs12Button.getSelection()) && ((e.doit==true))) {
                	//System.out.println("CertificateSelectionPage ---------------------  PKCS12 IS TURNED ON");
                	PKIState.CONTROL.setPKCS12on(true);
                	pkcs12Actions();
                	deSelectPkcs11();

              	    setPageComplete(false);
              	    CertificateSelectionPage.this.setMessage("Please enter your valid PKCS12 password and P12 file location.", WizardPage.INFORMATION);
                } else {
                	//  UN-select selected stuff...
                	//System.out.println("CertificateSelectionPage ---------------------  PKCS12 IS TURNED OFF");
                	PKIState.CONTROL.setPKCS12on(false);
                	PKCSSelected.setPkcs12Selected(false);
                	deSelectAll();
                    isButtonSelected=true; 
                }
                
                if ( isPageComplete() ) {
                	//System.out.println("CertificateSelectionPage ---------------------  PKCS12 FINISH BUTTON IS GOOD");
                } else {
                	//System.out.println("CertificateSelectionPage ---------------------  PKCS12 FINISH BUTTON IS FALSE");
                }
            }
        });
        //System.out.println("CertificateSelectionPage - addPKCSWidgetSelection  MAKING BURTTONS AVAILABLE");
        //isButtonSelected=true; 
    }
    private void deSelectAll() {
    	this.deSelectPkcs11();
    	this.deSelectPkcs12();
    	setPageComplete(isPageComplete());
    	getWizard().canFinish();
    	//System.out.println("CertificateSelectionPage ----deSelectALL   probably need to grey off finish bitton too");
    }
    private void deSelectPkcs11() {
    	this.pkcs11Button.setSelection(false);
    	this.checkPinButton.setEnabled(false);
        this.pinText.setEnabled(false);
        this.passwordLabel.setEnabled(false);
    	this.pkcs11PasswordLabel.setEnabled(false);
        this.combo.setEnabled(false);
        PKIState.CONTROL.setPKCS11on(false);
    }
    private void deSelectPkcs12() {
    	this.pkcs12Button.setSelection(false);
    	this.pkcs12certificateLabel.setEnabled(false);
        this.certPathText.setEnabled(false);
        this.pkcs12PasswordLabel.setEnabled(false);
        this.pkcs12PasswordTextBox.setEnabled(false);
        //this.saveCertificateCheckBox.setEnabled(false);
        this.pkcs12selectCert.setEnabled(false);
        this.checkPasswordButton.setEnabled(false);
        //this.passwordText.setText("");
        if ( this.passwordText != null) {
        	this.passwordText.setText("");
        }
    }
    
    void pkcs11Actions(){
    	
        //  TURN OFF ALL PKCS12
        this.pkcs12certificateLabel.setEnabled(false);
        this.certPathText.setEnabled(false);
        this.pkcs12PasswordLabel.setEnabled(false);
        this.pkcs12PasswordTextBox.setEnabled(false);
        //this.saveCertificateCheckBox.setEnabled(false);
        this.pkcs12selectCert.setEnabled(false);
        this.pkcs12Button.setSelection(false);
        this.checkPasswordButton.setEnabled(false);
        
        
        //TURN ON ALL PKCS11
        this.checkPinButton.setEnabled(true);
        this.passwordLabel.setEnabled(true);
        this.pinText.setEnabled(true);
        this.pkcs11Button.setSelection(true);
        PKCSSelected.setPkcs11Selected(true);
       
       
    }
     
    private void pkcs12Actions(){
    	
    	//TURN OFF PKCS11
    	this.checkPinButton.setEnabled(false);
        this.pinText.setEnabled(false);
        this.passwordLabel.setEnabled(false);
        this.pkcs11PasswordLabel.setEnabled(false);
        this.pinText.setText("");
        //this.pkcs11PasswordTextBox.setEnabled(false);
        this.pkcs11Button.setSelection(false);
        this.combo.setEnabled(false);
        this.checkPasswordButton.setEnabled(false);
        
      //Enable PKCS12     
        this.passwordText = this.pkcs12PasswordTextBox;
        this.pkcs12certificateLabel.setEnabled(true);
        this.certPathText.setEnabled(true);
        //this.saveCertificateCheckBox.setEnabled(true);
        this.pkcs12PasswordLabel.setEnabled(true);
        this.pkcs12PasswordTextBox.setEnabled(true);
        this.pkcs12selectCert.setEnabled(true); 
        PKCSSelected.setPkcs12Selected(true);
        
//SWM: 20200312 - Added the next 2 lines to fix Win 10 lost focus problem
        this.pkcs12Button.setSelection(true);
   	 	this.pkcs12Button.forceFocus();

        //this.pkcs12Button.setSelection(true);
   	 	//PKCSSelected.setKeystoreformat(KeyStoreFormat.PKCS12);
   	 	//this.pkcs12Button.forceFocus();
        //this.pkcs11PasswordTextBox.setText("");
        if ( pkiSecureStorage.isPKISaved() ) {
        	//System.out.println("CertificateSelectionPage ---  pkcs12   IS  IN SECURE STORAGE");
	 			pkiSecureStorage.loadUpPKI();
	 			pkiSecureStorage.setPKISystemProperties();
	 			this.certPathText.setText( AuthenticationPlugin.getDefault().getCertificatePath() );
        } else {
        	//System.out.println("CertificateSelectionPage ---  pkcs12  IS NOT IN SECURE STORAGE");
        }
    }
 
 
    private void addPKCS12Section(Composite sectionComposite1) {
        Group group = new Group(sectionComposite1, SWT.NONE);
        group.setLayout(new GridLayout());
        group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
//SWM: 20200306        group.setText("PKCS12 (deprecating)");
        group.setText("PKCS12 (.p12)");
                 
        //Create layout and controls
        //Composite pkcs12ButtonComposite = new Composite(group, SWT.NONE);
        Composite pkcs12ButtonComposite = new Composite(group, SWT.NO_RADIO_GROUP);
        GridLayout pkcs12ButtonCompositeLayout = new GridLayout(1, false);
        pkcs12ButtonComposite.setLayout(pkcs12ButtonCompositeLayout);
        pkcs12ButtonComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        
       
        int widthHint = computeWidthHint( this.getContainer().getShell() ); 
        GridData labelGridData = new GridData( GridData.FILL_BOTH );
        Label selectionInstructionsLabel = new Label( pkcs12ButtonComposite, SWT.WRAP );
        labelGridData = new GridData( GridData.FILL_BOTH );
        labelGridData.widthHint = widthHint;
        selectionInstructionsLabel.setLayoutData( labelGridData );
        selectionInstructionsLabel.setText( PKCS12_SELECTION_INSTRUCTIONS );
        
        this.pkcs12Button = new Button(pkcs12ButtonComposite, SWT.RADIO);
        this.pkcs12Button.setLayoutData(new GridData(SWT.BEGINNING, SWT.TOP, true, true));                 
        this.pkcs12Button.setText("PKCS12");
                       
        // Create layout and controls for manually selecting certs
        Composite subsectionComposite = new Composite( group, SWT.NONE );
        GridLayout subsectionCompositeLayout = new GridLayout( 3, false );
        subsectionComposite.setLayout( subsectionCompositeLayout );
        subsectionComposite.setLayoutData( new GridData(SWT.FILL, SWT.FILL, true, true  ) );
        
        // Create label for selecting the certificate
        this.pkcs12certificateLabel = new Label( subsectionComposite, SWT.RIGHT );
        this.pkcs12certificateLabel.setText( "PKI Certificate Location: " );
 
        this.certPathText = new Text( subsectionComposite, SWT.LEFT | SWT.BORDER );
        this.certPathText.setLayoutData( new GridData(SWT.FILL, SWT.FILL, true, true  ) );
        
        this.pkcs12selectCert = new Button( subsectionComposite, SWT.PUSH );
        this.pkcs12selectCert.setText( "Browse..." );
        
        BrowseButtonListener listener = new BrowseButtonListener();
        pkcs12selectCert.addSelectionListener( listener );    
         
        //Create layout and controls
        Composite pkcs12PasswordComposite = new Composite(group, SWT.NONE);
        GridLayout pkcs12PasswordLayout = new GridLayout(3, false);
        //pkcs12PasswordLayout.horizontalSpacing = 10;
        //pkcs12PasswordLayout.verticalSpacing = 10;
        pkcs12PasswordComposite.setLayout(pkcs12PasswordLayout);
        pkcs12PasswordComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
         
        // Create controls for specifying the password
        this.pkcs12PasswordLabel = new Label( pkcs12PasswordComposite, SWT.RIGHT );
        this.pkcs12PasswordLabel.setText( "PKI Certificate Password: " );        
 
        this.pkcs12PasswordTextBox = new Text( pkcs12PasswordComposite, SWT.LEFT | SWT.PASSWORD | SWT.BORDER );
        this.pkcs12PasswordTextBox.setLayoutData( new GridData(SWT.FILL, SWT.FILL, true, true  ) );
        
        this.checkPasswordButton = new Button(pkcs12PasswordComposite, SWT.RIGHT);
        this.checkPasswordButton.setText("Validate");
        this.checkPasswordButton.addSelectionListener(new SelectionListener(){

			public void widgetSelected(SelectionEvent e) {
				// TODO Auto-generated method stub
				//System.out.println("CertificateSelectionPage --  bitton selected");
				CertificateSelectionPage.this.setErrorMessage( "CHECKING PASSWORD AND KEYSTORE");
				if ( checkPasswordAndKeystore( certPathText.getText().trim(), passwordText.getText()  ) ) {
					
					if (!(PKCS12CheckValid.INSTANCE.isExpired(getUserKeyStore(), passwordText.getText().toCharArray()))) {
						
						CertificateSelectionPage.this.setErrorMessage(null);
						CertificateSelectionPage.this.setMessage("Password matches your Keystore, PRESS the Finish button to continue.", WizardPage.INFORMATION);
			    		finishOK=true;
			    		setPageComplete(true);
			    		pkcs12selectCert.setEnabled(false);
			            certPathText.setEnabled(false);
			            pkcs12PasswordTextBox.setEnabled(false);
			            checkPasswordButton.setEnabled(false);
			    		passwordText.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
					} else {
						CertificateSelectionPage.this.setErrorMessage( "The keystore has expired certificate.");
						passwordText.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
					}
				} else {
					CertificateSelectionPage.this.setErrorMessage( "The Password does NOT match your keystore.");
					passwordText.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
				}
				isPageComplete();
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub
				//System.out.println("CertificateSelectionPage --  button default selected");
			} 
        	
        });
        
        final Composite sectionComposite2 = new Composite( group, SWT.NONE );
        sectionComposite2.setLayoutData( new GridData(SWT.FILL, SWT.FILL, true, true  ) );
        GridLayout sectionCompositeLayout2 = new GridLayout( 1, false );
        sectionCompositeLayout2.verticalSpacing = 1;
        sectionComposite2.setLayout( sectionCompositeLayout2 );
        
        //addSaveCertificateCheckbox(sectionComposite2);
        
        
        ModifyListener modifyListener = new ModifyListener() {
            public void modifyText( ModifyEvent e ) {
                CertificateSelectionPage.this.setErrorMessage( null );
                //CertificateSelectionPage.this.getContainer().updateButtons();
                //System.out.println("CertificateSelectionPage --- modifyText PASSWD LISTENER!");
                
                try {
                	if ( (!(passwordText.getText().isEmpty()) )) {
                		CertificateSelectionPage.this.setMessage("Please enter your valid PKCS12 password and P12 file location.", WizardPage.INFORMATION);
                		validPasswd=true;
            			//setPageComplete(true);
                		checkPasswordButton.setEnabled(true);
            			passwordText.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
            			if (finishOK ) {
            				// RESET the finish button back to 
            				getWizard().canFinish();
            				setPageComplete(false);
            				finishOK=false;
            				passwordText.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
            				CertificateSelectionPage.this.setErrorMessage( "Altered  Password does NOT match your keystore.");
            			}
                	}
                	isPageComplete();
                	CertificateSelectionPage.this.getContainer().updateButtons();
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					//e1.printStackTrace();
				}
            }
        };  
        pkcs12PasswordTextBox.addModifyListener( modifyListener );       
        if (PKIState.CONTROL.isPKCS12on()) {	
   	 		this.pkcs12Button.setSelection(true);
   	 		//PKCSSelected.setKeystoreformat(KeyStoreFormat.PKCS12);
   	 		this.pkcs12Button.forceFocus();
   	 		pkiSecureStorage = new PKISecureStorage();
   	 		
   	 		if ( pkiSecureStorage.isPKISaved() ) {
   	 		
   	 			pkiSecureStorage.loadUpPKI();
   	 			pkiSecureStorage.setPKISystemProperties();
   	 			this.certPathText.setText( AuthenticationPlugin.getDefault().getCertificatePath() );
   	 			
   	 			this.certPathText.setText(AuthenticationPlugin.getDefault().getPreferenceStore().getString(AuthenticationPreferences.PKI_CERTIFICATE_LOCATION));
   	 		} else {
   	 			this.certPathText.setText( AuthenticationPlugin.getDefault().getCertificatePath() );
   	 		}
   	 		
         } 
//        else {
//        	 System.out.println("CertificateSelectionPage ---  pkcs12 CERTPATHTEXT:  WRONG VALUE IS GETTING STORED INTO pkcs12 PREF----  FIX THIS");
//        	 System.out.println("CertificateSelectionPage ---  pkcs12 CERTPATHTEXT:"+AuthenticationPlugin.getDefault().getPreferenceStore().getString(AuthenticationPreferences.PKI_CERTIFICATE_LOCATION));
//             this.certPathText.setText(AuthenticationPlugin.getDefault().getPreferenceStore().getString(AuthenticationPreferences.PKI_CERTIFICATE_LOCATION));
//         }   
    }
 
 
    private void addPKCS11Section(Composite sectionComposite) {
        Group group = new Group(sectionComposite, SWT.NONE);
        group.setLayout(new GridLayout());
        group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        group.setText("PKCS11 (pki)");
                 
        //Create layout and controls
        //Composite pkcs11ButtonComposite = new Composite(group, SWT.NONE);
        Composite pkcs11ButtonComposite = new Composite(group, SWT.NO_RADIO_GROUP);
        
        GridLayout pkcs11ButtonCompositeLayout = new GridLayout(1, false);
        pkcs11ButtonComposite.setLayout(pkcs11ButtonCompositeLayout);
        pkcs11ButtonComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        
        int widthHint = computeWidthHint( this.getContainer().getShell() );  
        GridData labelGridData = new GridData( GridData.FILL_BOTH );
        Label selectionInstructionsLabel1 = new Label( pkcs11ButtonComposite, SWT.WRAP );
        labelGridData = new GridData( GridData.FILL_BOTH );
        labelGridData.widthHint = widthHint;
        selectionInstructionsLabel1.setLayoutData( labelGridData );
        selectionInstructionsLabel1.setText( PKCS11_SELECTION_INSTRUCTIONS );
        
         
        this.pkcs11Button = new Button(pkcs11ButtonComposite, SWT.RADIO);
        //this.pkcs11Button = new Button(buttonGroup, SWT.RADIO);
        fakeButton = new Button(pkcs11ButtonComposite, SWT.RADIO);
        fakeButton.setSelection(true);
        fakeButton.setVisible(false);
        
        
        this.pkcs11Button.setLayoutData(new GridData(SWT.BEGINNING, SWT.BOTTOM, true, true));
        this.pkcs11Button.setText("PKCS11");
        
        //this.pkcs11Button.forceFocus();
        
        addPasswordSubsection( pkcs11ButtonComposite );
        
        this.pkcs11PasswordLabel = new Label( pkcs11ButtonComposite, SWT.RIGHT );
        this.pkcs11PasswordLabel.setText( "Pkcs11 Selection list: " );    
        
        this.combo = new Combo(pkcs11ButtonComposite, SWT.READ_ONLY);
        this.combo.setItems(INITIAL_SLOT);
        this.combo.select(0);
        ModifyListener modifyListener = new ModifyListener() {
            public void modifyText( ModifyEvent e ) {
            	//System.out.println("CertificateSelectionPage ----addPKCS11Section  Modify Event"+e.getSource().toString());
            	//System.out.println("CertificateSelectionPage ----addPKCS11Section  SLOT"+combo.getSelectionIndex());
                CertificateSelectionPage.this.setErrorMessage( null );
                CertificateSelectionPage.this.getContainer().updateButtons();
                selectedAlias = combo.getText();
            }
        };
        combo.addModifyListener( modifyListener );
       
        combo.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected( SelectionEvent event) {
        		
        		//System.out.println("CertificateSelectionPage --MY SELECTION"+combo.getText());
        		selectedAlias = combo.getText();
        		certificateIndex = combo.getSelectionIndex();
        	}
        }); 
        if ( PKIState.CONTROL.isPKCS11on()) {
        	if ( pkiSecureStorage.isPKISaved() ) {
        		pkiSecureStorage.loadUpPKI();
        	}
        	//System.out.println("CertificateSelectionPage ----addPKCS11Section  SELECTING PKCS11 BUTTON");
        	 if ( this.loadPkcs11Slots() ) {
             	this.pkcs11Button.setEnabled(true);
             	this.pkcs11Button.forceFocus();
             }
        	pkcs11Actions();
        	this.pkcs11PasswordLabel.setEnabled(false);
            this.combo.setText("");
            this.slots=null;
            this.combo.setEnabled(false);
        	isPageComplete();
        }
        else {
        	pkcs12Actions();
        }
    }
    private Composite addButtonGroup(Composite parent) {
    	/*
    	 * TBD:
    	 * @TODO    CREATE a radio group without grouping buttons together.
    	 */
    	buttonGroup = new Composite(parent, SWT.NO_RADIO_GROUP);
    	
    	return buttonGroup;
    }
 
 
    private void addPKITypeButtons(final Composite sectionComposite) {
        pkcsbuttons = new PKCSButtons(sectionComposite);
         
        pkcsbuttons.buttonSelected(certPathText, pkcs12certificateLabel, pkcs12selectCert, passwordText);
    }
 
 
    private void addSaveCertificateCheckbox(final Composite sectionComposite) {
    	
        //Create layout and controls
        Composite subsectionComposite = new Composite(sectionComposite, SWT.NONE);
       
        GridLayout subsectionCompositeLayout = new GridLayout(2, false);
        subsectionComposite.setLayout(subsectionCompositeLayout);
        subsectionComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true  ));
        
        
        saveCertificateCheckBox = new Button(subsectionComposite, SWT.CHECK);
        saveCertificateCheckBox.setText("Save your selection into Eclipse Secure Store? ");
        saveCertificateCheckBox.addSelectionListener(new SelectionListener(){
 
            public void widgetSelected(SelectionEvent e) {
                isSaveCertificateChecked = saveCertificateCheckBox.getSelection();
                ChangedPressedFieldEditorStatus.setPKISaveCertificateChecked(isSaveCertificateChecked);
            }
 
            public void widgetDefaultSelected(SelectionEvent e) {
                // TODO Auto-generated method stub
                 
            }
             
        });
    }
 
    private void addFileSelectSubsection( final Composite sectionComposite ) {
        // Create label for selecting the certificate
        pkcs12certificateLabel = new Label( sectionComposite, SWT.RIGHT );
        pkcs12certificateLabel.setText( "PKI Certificate or Pkcs11 Location: " );
        
        // Create layout and controls for manually selecting certs
        Composite subsectionComposite = new Composite( sectionComposite, SWT.NONE );
        GridLayout subsectionCompositeLayout = new GridLayout( 2, false );
        subsectionComposite.setLayout( subsectionCompositeLayout );
        subsectionComposite.setLayoutData( new GridData(SWT.FILL, SWT.FILL, true, true  ) );
 
        certPathText = new Text( subsectionComposite, SWT.LEFT | SWT.BORDER );
        certPathText.setLayoutData( new GridData(SWT.FILL, SWT.FILL, true, true  ) );
        AuthenticationPlugin plugin = AuthenticationPlugin.getDefault();
        String preferencesKey = AuthenticationPreferences.PKI_CERTIFICATE_LOCATION;
        IPreferenceStore preferenceStore = plugin.getPreferenceStore();
        String certificatePath = preferenceStore.getString( preferencesKey );
        if ( null != certificatePath ) {
            certPathText.setText( certificatePath );
        }
 
        pkcs12selectCert = new Button( subsectionComposite, SWT.PUSH );
        pkcs12selectCert.setText( "Browse..." );
        BrowseButtonListener listener = new BrowseButtonListener();
        pkcs12selectCert.addSelectionListener( listener );        
        
    }
 
    private Composite addInstructionsSection( final Composite parent ) {
        // Create composite for the section
        Composite sectionComposite = new Composite( parent, SWT.NONE );
        GridLayout subsectionCompositeLayout = new GridLayout();
        //subsectionCompositeLayout.horizontalSpacing = 10;
        //subsectionCompositeLayout.verticalSpacing = 10;
        sectionComposite.setLayout( subsectionCompositeLayout );
        sectionComposite.setLayoutData( new GridData(SWT.FILL, SWT.FILL, true, true  ) );
 
        // Calculate initial width
        int widthHint = computeWidthHint( this.getContainer().getShell() );        
 
        // Create labels for each set of instructions
        Label openingInstructionsLabel = new Label( sectionComposite, SWT.WRAP );
        GridData labelGridData = new GridData( GridData.FILL_BOTH );
        labelGridData.widthHint = widthHint;
        openingInstructionsLabel.setLayoutData( labelGridData );
        openingInstructionsLabel.setText( OPENING_INSTRUCTIONS );
             
        return sectionComposite;
    }
    
    
 
    private void addPasswordSubsection( final Composite sectionComposite ) {
        // Create controls for specifying the password
       
        
        // Create layout and controls for manually selecting certs
        Composite subsectionComposite = new Composite( sectionComposite, SWT.NONE );
        GridLayout subsectionCompositeLayout = new GridLayout( 3, false );
        //subsectionCompositeLayout.horizontalSpacing = 1;
        //subsectionCompositeLayout.verticalSpacing = 1;
        
        subsectionComposite.setLayout( subsectionCompositeLayout );
        subsectionComposite.setLayoutData( new GridData(SWT.FILL, SWT.FILL, true, true  ) );
        passwordLabel = new Label( subsectionComposite, SWT.LEFT );
        passwordLabel.setText( "Enter your PiN: " );
        pinText = new Text( subsectionComposite, SWT.LEFT | SWT.PASSWORD | SWT.BORDER );
        pinText.setLayoutData( new GridData(SWT.FILL, SWT.FILL, true, true  ) );
 
        ModifyListener modifyListener = new ModifyListener() {
            public void modifyText( ModifyEvent e ) {
            	//System.out.println("CertificateSelectionPage - PKCS11 entering text");
            	if (( checkPinButton != null ) && (PKIState.CONTROL.isPKCS11on())) {
            		checkPinButton.setEnabled(true);
            	}
                CertificateSelectionPage.this.setErrorMessage( null );
                CertificateSelectionPage.this.getContainer().updateButtons();
            }
        };
        pinText.addModifyListener( modifyListener );
        if ( this.combo == null) {
        	 //this.slots=null;
        }
        this.checkPinButton = new Button(subsectionComposite, SWT.RIGHT);
        this.checkPinButton.setText("Log in");
        this.checkPinButton.addSelectionListener(new SelectionListener(){
			public void widgetSelected(SelectionEvent e) {
				// TODO Auto-generated method stub
				//System.out.println("CertificateSelectionPage --  bitton selected");
				CertificateSelectionPage.this.setErrorMessage( "CHECKING PiN AND KEYSTORE");
				
				if (!(pinText.getText().isEmpty())) {
					if ( VendorImplementation.getInstance().login(pinText.getText().trim() ) ) {
						CertificateSelectionPage.this.setErrorMessage(null);
						CertificateSelectionPage.this.setMessage("Password matches your Keystore, PRESS the Finish button to continue.", WizardPage.INFORMATION);
			    		finishOK=true;
			    		setPageComplete(true);
			    		PKIState.CONTROL.setPKCS11on(true);
	            		slots = selectFromPkcs11Store();
	            		combo.setEnabled(true);
	            		pkcs11PasswordLabel.setEnabled(true);
	            		loadPkcs11Slots();
	                    pkcs11Actions();
	                    PKCSSelected.setKeystoreformat(KeyStoreFormat.PKCS11);
	                    PKIState.CONTROL.setPKCS12on(false);
	                    pinText.setEnabled(false);
			    		pinText.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
			    		AuthenticationPlugin.getDefault().setCertPassPhrase(pinText.getText().trim());
			    		checkPinButton.setEnabled(false);
					} else {
						CertificateSelectionPage.this.setErrorMessage( "The Pin does NOT match your keystore.");
						pinText.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
						pkcs11PasswordLabel.setEnabled(false);
                        combo.setText("");
                        combo.setEnabled(false);
						slots=null;
						loadPkcs11Slots();
						if ( pinText.getText().trim().isEmpty()) {
							pinText.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
						}
						finishOK=false;
					}
				} else {
					CertificateSelectionPage.this.setErrorMessage( "The Pin does NOT match your keystore.");
				}
				isPageComplete();
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub
				//System.out.println("CertificateSelectionPage --  button default selected");
			} 
        	
        });
        
    }
 
    private void addProgressBarSection( final Composite parent ) {
        // Create progress bar
        Composite spacer = new Composite( parent, SWT.NONE );
        spacer.setLayoutData( new GridData( GridData.FILL_BOTH ) );
        spacer.setLayout( new GridLayout() );
        progressBar = new ProgressBar( parent, SWT.HORIZONTAL | SWT.INDETERMINATE );
        progressBar.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );
        progressBar.setVisible( false );
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
    
    public void createControl( final Composite parent ) {        
        // Create a main composite for the page
        //final Composite main = new Composite( parent, SWT.NONE );
        //final Composite main = new Composite( parent, SWT.BORDER );
        final Composite main = new Composite( parent, SWT.BORDER | SWT.NO_RADIO_GROUP );
        main.setSize(500, 100);
        main.pack(true);
        main.setLayoutData( new GridData( GridData.FILL_BOTH ) );
        main.setLayout( new GridLayout() );
        
        // Add section for instructions
        addInstructionsSection( main );
 
        // Add section for certificate details, including file and password
        addCertificateDetailsSection( main );
        
        // Add section for password
        addProgressBarSection( main );
        
        addSaveCertificateCheckbox(main);
        // set focus on password entry if cert path already set
        //for PKCS12
        
        if (certPathText != null && certPathText.getText() != null &&
                !"".equals(certPathText.getText().trim()) &&
                passwordText != null) {
            passwordText.setFocus();
        }
        //for PKCS11
//        if (PKCSSelected.isPkcs11Selected()){
//            passwordText.setFocus();
//        }
//        
        PlatformUI.getWorkbench().getHelpSystem().setHelp(this.getShell(), 
                "org.eclipse.pki.wizard");
        
       
         main.getSize();
         main.pack(true);
        
        // Finishing touches
        this.setPageComplete( true );
        this.setControl( main );
    }
    
    @Override
    public Control getControl() {
        return super.getControl();
    }
 
    protected KeyStore getUserKeyStore() {
    	String certPath = null;
        String password = null;
        KeyStore keyStore = null;
        try {
        	
            try {
				certPath = certPathText.getText();
				password = passwordText.getText();
			} catch (Exception e) {}  
            
           
            if (password != null ) {
            	System.setProperty("javax.net.ssl.keyStorePassword", password);
            } else {
            	System.setProperty("javax.net.ssl.keyStorePassword", "*******");
            }
            if (( certPath != null ) && (PKIState.CONTROL.isPKCS12on() ) ) {
            	System.setProperty("javax.net.ssl.keyStore", certPath );
            }
            if (( certPath != null ) && (PKIState.CONTROL.isPKCS11on() ) ) {
            	System.setProperty("javax.net.ssl.keyStore", "pkcs11" );
            }
            
            keyStore = KeyStoreUtil.getKeyStore();
            if ( keyStore == null ) {
            	keyStore = KeyStoreUtil.getKeyStore(certPath, password, PKCSSelected.getKeystoreformat()/*KeyStoreFormat.PKCS12*/);
            } 
            
       
        } catch ( Throwable exception ) {           
       
        	
            if ( null != exception ) {
                StringBuilder message = new StringBuilder();
                message.append( "Problem reading your certificate.  " );
                if(exception instanceof KeyStoreException){
                    message.append("The Java Key Store can not be loaded");
                } else if(exception instanceof NoSuchAlgorithmException){
                    message.append("The algorithm used to check the integrity of the pki file cannot be found.");
                } else if(exception instanceof CertificateException){
                    message.append("The pki file can not be loaded.");
                } else if(exception instanceof IOException){
                     message.append( "Either your password was incorrect or the " );
                     message.append( "the selected file is corrupt. Please try " );
                     message.append( "a different password or PKCS file." );
                } else {
                     message.append( "An unexpected error '" );
                     message.append( exception.getClass().getName() );
                     message.append( "' occurred: " );
                     message.append( exception.getMessage() );
                     message.append( " Please select a different file and/or " );
                     message.append( "check the logs for more information." );
                }
                this.setErrorMessage( message.toString() );
                
            }
        } 
            // Return the result
            return keyStore;
       
    }
    
    public String getCertificatePath()
    {
        return certPathText.getText();
    }
    
    public void setCertificatePath(String path){
        this.certPathText.setText(path);;
    }
    
    public String getCertPassPhrase()
    {
        return passwordText.getText();
    }
    
    public boolean isSaveCertificateChecked() {
        return isSaveCertificateChecked;
    }
    public void setSaveCertificateChecked( boolean b ) {
    	isSaveCertificateChecked=b;
    	ChangedPressedFieldEditorStatus.setPKISaveCertificateChecked(isSaveCertificateChecked);
    }
    /* (non-Javadoc)
     * @see org.eclipse.jface.wizard.WizardPage#isPageComplete()
     */
    @Override
    public boolean isPageComplete() {
    	boolean isComplete=false;
    	
    	//System.out.println("CertificateSelectionPage is PAGE COMPLETE?");
        try {
			// Get path
			String certPath = null;
			if ( null != certPathText ) {
			    certPath = certPathText.getText().trim();
			}
				
			if ( PKIState.CONTROL.isPKCS12on()) {	
				//System.out.println("CertificateSelectionPage is PAGE COMPLETE?   PKCS12");
				System.clearProperty("javax.net.ssl.keyStoreType");
			    if ( (!(certPath.isEmpty())) && (!(passwordText.getText().isEmpty() ))) {
			    	//System.out.println("CertificateSelectionPage YES PAGE IS  COMPLETE?   PKCS12");
			    	if ( finishOK )  {
			    		PKCSSelected.setKeystoreformat(KeyStoreFormat.PKCS12);
			    		isComplete=true;
			    	} 
			    } 
			}
			if (( PKIState.CONTROL.isPKCS11on()) && (pkcs11Button.getSelection()) &&
					(!(pinText.getText().isEmpty()) &&(finishOK))) {
				//System.out.println("CertificateSelectionPage -------------------- is PAGE COMPLETE?   PKCS11");
				//System.out.println("CertificateSelectionPage -------------------- PiN   PKCS11:"+ pinText.getText());
				isComplete=true;
			}
			if ((!(PKIState.CONTROL.isPKCS11on())) && ((!(PKIState.CONTROL.isPKCS12on())))) {
				//System.out.println("CertificateSelectionPage --------- NOTHING PICKED");
			}
		} catch (Exception e) {
			/*
			 *  This method may be called before page is displayed, so make sure its NOT complete..
			 */
			// TODO Auto-generated catch block
			//e.printStackTrace();
			isComplete=false;
		}
        return isComplete;
    }
    protected void exitOnCompletion() {
    	this.getShell().setVisible(false);
    }
  
    protected boolean finishButton()  {
    	//this.getWizard().getContainer().getButton(IDialogConstants.FINISH_ID);
    	//System.out.println("CertificateSelectionPage  PRESSED FINISH BUTTON  "+finishOK);
    	return finishOK;
    }
    public boolean isFinished() {
    	return finishOK;
    }
   
    @SuppressWarnings("static-access")
	protected boolean performFinish() {
    	boolean isDone=false;
    	String fingerprint=null;
    	//System.out.println("CertificateSelectionPage  PAGE COMPLETE   FINISH BUTTON  ");
    	
    	try {
    		AuthenticationPlugin plugin = AuthenticationPlugin.getDefault();
    		if ( PKIState.CONTROL.isPKCS12on()) {
    			String path = certPathText.getText().trim();
    			if ( !path.equals( "" ) ) { 
				    plugin.setCertificatePath( path );
				    lastPropertyValues.load();
			    }
    		}
            if ( PKIState.CONTROL.isPKCS11on() ) {
            	isDone=true;
             	plugin.setCertificatePath( "pkcs11" );
            	
				plugin.setCertPassPhrase(VendorImplementation.security.getPin());
				System.setProperty("javax.net.ssl.keyStoreProvider", "SunPKCS11");
				lastPropertyValues.load();
				KeyStore store = VendorImplementation.getInstance().getKeyStore();
				if ( store == null ) {
					System.out.println("CertificateSelectionPage performFinish()  CANT FIND KEYSTORE");
				}
				
				X509Certificate x509 = (X509Certificate) VendorImplementation.getInstance().getKeyStore().getCertificate(selectedAlias);
				if ( x509 != null) {
					//System.out.println("CertificateSelectionPage performFinish() CERTIFICATE ALIAS:"+ x509.getSubjectDN());
					fingerprint=FingerprintX509.INSTANCE.getFingerPrint(x509, "SHA-256");
					VendorImplementation.getInstance().setSelectedX509Fingerprint(fingerprint);
				} 
			}
            if (!( plugin.isSSLSystemPropertiesSet() )) {  	
            	AuthenticationPlugin.getDefault().obtainDefaultJKSTrustStore();
            }
            
            if(!ChangedPressedFieldEditorStatus.isPkiChangedPressed()){
    			PKISecureStorage pkiStorage = new PKISecureStorage();
        		if(isSaveCertificateChecked()){    			
                    pkiStorage.storePKI(AuthenticationPlugin.getDefault());                
        		} else {
        			pkiStorage.getNode().removeNode();
        		}
    		}   	
    	} catch ( Throwable t) {
    		LogUtil.logError(t.getMessage(), t);
    	}
        //System.out.println("CertificateSelectionPage ------------------performFinish   DONE   STATUS:"+isDone);
        return isDone;
    }
    boolean checkPasswordAndKeystore( String path, String password ) {
    	boolean isOK = false;
    	
		try {
			if ( KeyStoreUtil.checkUserKeystorePass( path, password, "PKCS12" )) {
				isOK=true;
				//System.out.println("CertificateSelectionPage  KeyStoreUtil SUCCESSFULLY VERIFIED cert path and password");
			} else {
				//System.out.println("CertificateSelectionPage  KeyStoreUtil SAYS ITS INVALID");
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			isOK=false;
		}
    	
    	return isOK;
    }
    private boolean isPKCS12Expired(char[] c) {
    	boolean isOK=false;
    	
    	PKCS12CheckValid.INSTANCE.isExpired(getUserKeyStore(), c);
    	return isOK;
    }
    private PKI setLastInstance( PKIProperties lastPki ) {
    	PKI pki = null;
    	
    		try {
				pki = new PKI();
				pki.setKeyStorePassword(lastPki.getKeyStorePassword());
				pki.setKeyStoreProvider(lastPki.getKeyStoreProvider());
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	
    	return pki;
    }
    private void restoreLastPKI() {
    	
    }
    private class BrowseButtonListener extends SelectionAdapter {
        @Override
        public void widgetSelected( SelectionEvent e ) {
            Shell shell = pkcs12selectCert.getShell();
            FileDialog dialog = new FileDialog( shell, SWT.SINGLE );
            String fileName = certPathText.getText();
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
            String[] filterNames = AuthenticationPreferences.FILTER_NAMES;
            String[] filterExts = AuthenticationPreferences.FILTER_EXTS;
            dialog.setFilterNames( filterNames );
            dialog.setFilterExtensions( filterExts );
            fileName = dialog.open();
            if ( null != fileName ) {
                certPathText.setText( fileName );
                CertificateSelectionPage.this.getContainer().updateButtons();
            }
        }
    }
    protected boolean loadPkcs11Slots() {
    	boolean isAnySlots=false;
		try {
			System.out.println("CertificateSelectionPage -- loadPkcs11Slots        load slots");
			if ( slots != null ) {
				if ( slots.length < 1 ) {
					slots = new String[1];
					slots[0] = "Eclipse didnt find any x509 digital signatures.  Did you enter your PIN? Please check your CAC.";
				} 
			} else {
				slots = new String[1];
				slots[0] = "Eclipse didnt find any x509 digital signatures.  Did you enter your PIN? Please check your CAC.";
			}
			combo.setItems(slots);
			combo.select(0);
			isAnySlots=true;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return isAnySlots;
    }
    
    protected String[] selectFromPkcs11Store() {
    	ArrayList<String> list = new ArrayList<String>();
    	String items[] = null;
    	items = new String[1];
    	try {
    		System.out.println("CertificateSelectionPage --  --selectFromPkcs11Store");
        	if ( VendorImplementation.getInstance().isInstalled()) {
        		System.out.println("CertificateSelectionPage --  --selectFromPkcs11Store installed");
        		list = new ArrayList<String>(VendorImplementation.getInstance().getList());
        		LogUtil.logInfo("CertificateSelectionPage - selectFromPkcs11Store list from cac:"+list.size());
        		Iterator<String>it = list.iterator();
        		int i=0;
        		if ( list.isEmpty() ) {
    				items = new String[1];
    				items[0] = "Eclipse didnt find any x509 digital signatures.  Did you enter your PIN? Please check your CAC.";
    			} else {
    				items = new String[list.size()];
    				while ( it.hasNext() ) {
    					items[i] = it.next();
    					i++;
    				}
    			}
        		System.out.println("CertificateSelectionPage --  --selectFromPkcs11Store installed DONE");
        	} else {
        		//  SET SELECTION TO PKCS12 here, because there is no PKCS11 installed
        		System.out.println("CertificateSelectionPage ----NOT able to load a PROVIDER");
        	}
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
    	return items;
    }
}