package org.eclipse.ui.pki.preferences;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.pki.pkiselection.PKCSpick;
import org.eclipse.ui.pki.util.ChangedPressedFieldEditorStatus;
import org.eclipse.ui.pki.util.KeyStoreFormat;
import org.eclipse.ui.pki.util.KeyStoreUtil;
import org.eclipse.pki.auth.AuthenticationPlugin;
import org.eclipse.pki.exception.UserCanceledException;
import org.eclipse.pki.pkcs.VendorImplementation;


public class CheckUpdatedKeystoreValue  {
	
	public static boolean isValid(String userInputPath) {
		
		//System.out.println("CheckUpdatedKeystoreValue ----  isValid   ");
		//System.out.println("CheckUpdatedKeystoreValue ----  isValid   incoming value:  "+userInputPath );
		boolean isFound = false;
		StringBuilder message = new StringBuilder();
		message.append("Reading credentials:");
        //KeyStore existingUserKeyStore = null;
        KeyStore newUserKeyStore = null;
        String passwd=null;
        String userSuppliedLocation = null;
		
		try {
			if ( userInputPath == null ) {
				return false;
			}
			if ( PKCSpick.getInstance().isPKCS11on()) {
				
				try {
					
					/*
					 * TODO
					 * NOTE:  NEED to add a check here to make sure that the path that was entered is
					 * valid and that there is a valid CFG file..
					 * 
					 */
					System.out.println("CheckUpdatedKeystoreValue ----  ADD a check here to make sure path is good cfg ");
					VendorImplementation.refresh();
					newUserKeyStore = VendorImplementation.getInstance().getKeyStore();
					if ( newUserKeyStore != null ) {
						
						isFound=true;
					}
				} catch(Exception e) {
					System.out.println("CheckUpdatedKeystoreValue ----  isValid  bad path exception"); 
				}
			}
			 
			if  ( PKCSpick.getInstance().isPKCS12on()) {
				//System.out.println("CheckUpdatedKeystoreValue ----  isValid   PKCS12 ");
				passwd = AuthenticationPlugin.getDefault().getCertPassPhrase();
				if ( (passwd != null ) && (!( passwd.isEmpty() ))) {
					userSuppliedLocation = userInputPath;
					//System.out.println("CheckUpdatedKeystoreValue ----  isValid   PKCS12 passwd:"+passwd+"   incoming:"+userInputPath);
					//System.out.println("CheckUpdatedKeystoreValue ----  isValid   PKCS12 passwd:"+passwd+"   LOCATION:"+userSuppliedLocation);
					
					newUserKeyStore = KeyStoreUtil.getKeyStore(userInputPath, passwd, KeyStoreFormat.PKCS12);
					isFound=true;
				} else {
					//System.out.println("CheckUpdatedKeystoreValue ----  isValid   PKCS12    NO PASSWORD WAS FOUND");
					newUserKeyStore = AuthenticationPlugin.getDefault().getUserKeyStore("Update");
				}
			}
			System.out.println("CheckUpdatedKeystoreValue ----  TBD COMMENTED OUT,, DONT NEED?");
			/*
			 * if (userSuppliedLocation.isEmpty()) {
			 * 
			 * throw new IllegalArgumentException(); }
			 */
			//isFound=true;
			
		
		} catch (KeyStoreException e) {
			message.append( "Problem reading your certificate.  1111" );
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			message.append( "Problem reading your certificate.  1112" );
			e.printStackTrace();
		} catch (CertificateException e) {
			message.append( "Problem reading your certificate.  1113" );
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			message.append( "Problem reading your certificate.  Not found in specfified location." );
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			message.append( "Problem reading your certificate. 0000111000 " );
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			//System.out.println("PKICertLocation :  IOEXCEPTION");
			message.append( "Problem reading your certificate.  " );
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UserCanceledException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
		Status status=null;
		if (!(isFound)) {
			status = new Status(IStatus.ERROR, "plugin0", 0, message.toString(), null);
			ErrorDialog.openError(Display.getCurrent().getActiveShell(), "Error Display","Your entry cant be loaded,", status);	
		} else {
			ChangedPressedFieldEditorStatus.setPkiUserKeyStore( newUserKeyStore );
			AuthenticationPlugin.getDefault().setUserKeyStore( newUserKeyStore );
			AuthenticationPlugin.getDefault().setCertificatePath(userSuppliedLocation); 
		}
		return isFound;
	}
}
