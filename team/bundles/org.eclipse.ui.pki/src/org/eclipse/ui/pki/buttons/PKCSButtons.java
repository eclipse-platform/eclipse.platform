package org.eclipse.ui.pki.buttons;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.pki.AuthenticationPlugin;
import org.eclipse.pki.pkiselection.PKCS11WidgetSelectedActions;
import org.eclipse.pki.pkiselection.PKCS12WidgetSelectedActions;
import org.eclipse.pki.preferences.AuthenticationPreferences;


public class PKCSButtons {
	
	protected Button pkcs12Button;
    protected Button pkcs11Button;
    
    public PKCSButtons(final Composite sectionComposite){
		
    	final Group buttonGroup = new Group(sectionComposite, SWT.NONE);
    	buttonGroup.setText("Select PKI Type to Use");
    	GridLayout layout = new GridLayout();
    	buttonGroup.setLayout(layout);
    	
    	//Create layout and controls
		Composite subsectionComposite = new Composite(buttonGroup, SWT.NONE);
		GridLayout subsectionCompositeLayout = new GridLayout(1, false);
		subsectionComposite.setLayout(subsectionCompositeLayout);
		subsectionComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		pkcs11Button = new Button(subsectionComposite, SWT.RADIO);
		pkcs11Button.setLayoutData(new GridData(SWT.BEGINNING, SWT.BOTTOM, true, true));

		pkcs11Button.setText("(PKCS11)");
		
		pkcs12Button = new Button(subsectionComposite, SWT.RADIO);
		pkcs12Button.setLayoutData(new GridData(SWT.BEGINNING, SWT.TOP, true, true));				
		pkcs12Button.setText("(PKCS12)");
	}
	
	public void buttonSelected(final Text certpathtext, final Label pkiLabel, final Button browseButton, final Text passwordText){
		//PKCS11 is always initially selected by default.
		if(pkcs11Button.getSelection()){
			pkcs11Actions(certpathtext, pkiLabel, browseButton);			
		}
		
		pkcs11Button.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				
				if(pkcs11Button.getSelection()){
					pkcs11Actions(certpathtext, pkiLabel, browseButton);
					passwordText.setText("");
				}
			}
		});
		
		pkcs12Button.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				
				if(pkcs12Button.getSelection()){
					PKCS12WidgetSelectedActions.setKeyStoreFormat();
					certpathtext.setText(AuthenticationPlugin.getDefault().getPreferenceStore().getString(AuthenticationPreferences.PKI_CERTIFICATE_LOCATION));
					PKCS12WidgetSelectedActions.userInterfaceDisplay(certpathtext, pkiLabel, null, null, browseButton, true);
					passwordText.setText("");
				}
			}
		});
	}
	
	void pkcs11Actions(Text certpathtext, Label pkiLabel, Button browseButton){
		PKCS11WidgetSelectedActions.setKeyStoreFormat();
		PKCS11WidgetSelectedActions.userInterfaceDisplay(certpathtext, pkiLabel, true);		
	}
}
