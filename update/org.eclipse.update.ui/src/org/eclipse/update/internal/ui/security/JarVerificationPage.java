package org.eclipse.update.internal.ui.security;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.update.core.IVerificationResult;
import org.eclipse.update.internal.ui.UpdateUIPlugin;

/**
 * 
 */
public class JarVerificationPage extends BannerPage {

	private IVerificationResult _VerificationResult = null;
	private String _fileName = null;
	private String _strFeatureName = null;
	private String _strId = null;
	private String _strProviderName = null;
	private TitleAreaDialog _Dialog;
	private boolean okToInstall = false;
	private String componentVerified;

	/*
	 * Constructor for JarVerificationPage.
	 */
	public JarVerificationPage(IVerificationResult verificationResult) {
		super(UpdateUIPlugin.getResourceString("JarVerificationDialog.Verification"));
		_fileName = verificationResult.getContentReference().getIdentifier();
		_VerificationResult = verificationResult;
		_strId = verificationResult.getFeature().getVersionedIdentifier().toString();
		_strFeatureName = verificationResult.getFeature().getLabel();
		_strProviderName = verificationResult.getFeature().getProvider();
		componentVerified =	(verificationResult.isFeatureVerification()) ? ".Feature" : ".File";
		okToInstall = false;
	}

	/* (non-Javadoc)
	 * Method declared on Dialog.
	 */
	protected Control createContents(Composite compositeParent) {
		WorkbenchHelp.setHelp(compositeParent, "org.eclipse.update.ui.JarVerificationPage");
		// Composite: Client
		//------------------
		Composite compositeClient = new Composite(compositeParent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		compositeClient.setLayout(layout);
		compositeClient.setLayoutData(new GridData(GridData.FILL_BOTH));

		// Text Information
		//------------------		
		createTextArea(compositeClient);

		// Certificate Area
		//------------------
		createCertificateArea(compositeClient);

		// File and Feature Information
		//-----------------------------		
		createInformationArea(compositeClient);

		// Choice Area
		//------------		
		//createChoiceArea(compositeClient);

		return compositeClient;

	}

	/*
	 * Continue install or cancel install
	 */
	private void createChoiceArea(Composite compositeClient) {
		if (_VerificationResult.getVerificationCode()
			!= IVerificationResult.TYPE_ENTRY_CORRUPTED) {

			// Label: Instruction
			//------------------
			Label labelInstruction = new Label(compositeClient, SWT.NULL);
			labelInstruction.setLayoutData(
				new GridData(
					GridData.VERTICAL_ALIGN_BEGINNING
						| GridData.GRAB_VERTICAL
						| GridData.FILL_HORIZONTAL));
			if (_VerificationResult.isFeatureVerification()) {
				labelInstruction.setText(
					UpdateUIPlugin.getResourceString("JarVerificationDialog.MayChooseToInstall"));
				//$NON-NLS-1$
			} else {
				labelInstruction.setText(
					UpdateUIPlugin.getResourceString("JarVerificationDialog.MayChooseToContinue"));
				//$NON-NLS-1$ 					
			}
			//$NON-NLS-1$
		}
	}

	/*
	 * Creates the Information text
	 */
	private void createTextArea(Composite compositeClient) {

		// Label: Information
		//------------------
		Label labelInformation =
			new Label(compositeClient, SWT.WRAP);
		labelInformation.setLayoutData(
			new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL));

		StringBuffer strb = new StringBuffer();
		switch (_VerificationResult.getVerificationCode()) {

			case IVerificationResult.TYPE_ENTRY_NOT_SIGNED :
				String msg =
					UpdateUIPlugin.getResourceString(
						"JarVerificationDialog.AboutToInstall"+
						componentVerified);
				setMessage(msg, WARNING);
				strb.append(
					UpdateUIPlugin.getResourceString(
						"JarVerificationDialog.NotDigitallySigned"+
						componentVerified));
				//$NON-NLS-1$
				strb.append("\r\n"); //$NON-NLS-1$
				strb.append(
					UpdateUIPlugin.getResourceString(
						"JarVerificationDialog.CannotVerifyProvider"+
						componentVerified));
				//$NON-NLS-1$
/*				strb.append("\r\n"); //$NON-NLS-1$
				if (_VerificationResult.isFeatureVerification()) {
					strb.append(
						UpdateUIPlugin.getResourceString("JarVerificationDialog.InstallMayCorrupt"));
					//$NON-NLS-1$
				} else {
					strb.append(
						UpdateUIPlugin.getResourceString("JarVerificationDialog.ContinueMayCorrupt"));
					//$NON-NLS-1$ 					
				}
				*/
				labelInformation.setText(strb.toString());
				break;

			case IVerificationResult.TYPE_ENTRY_CORRUPTED :
				msg =
					UpdateUIPlugin.getResourceString(
						"JarVerificationDialog.CorruptedContent"+
						componentVerified);
				setMessage(msg, ERROR);
				//$NON-NLS-1$
				strb.append(
					UpdateUIPlugin.getResourceString(
						"JarVerificationDialog.ComponentNotInstalled"));
				//$NON-NLS-1$
				labelInformation.setText(strb.toString());
				break;

			case IVerificationResult.TYPE_ENTRY_SIGNED_UNRECOGNIZED :
				msg =
					UpdateUIPlugin.getResourceString(
						"JarVerificationDialog.SignedComponent"+
						componentVerified);
				//$NON-NLS-1$
				setMessage(msg, WARNING);
				strb.append(
					UpdateUIPlugin.getResourceString(
						"JarVerificationDialog.UnknownCertificate"+
						componentVerified));
				//$NON-NLS-1$
				strb.append("\r\n"); //$NON-NLS-1$
				strb.append(
					UpdateUIPlugin.getResourceString(
						"JarVerificationDialog.UnableToVerifyProvider"+
						componentVerified));
				//$NON-NLS-1$
/*				strb.append("\r\n"); //$NON-NLS-1$
				if (_VerificationResult.isFeatureVerification()) {
					strb.append(
						UpdateUIPlugin.getResourceString("JarVerificationDialog.InstallMayCorrupt"));
					//$NON-NLS-1$
				} else {
					strb.append(
						UpdateUIPlugin.getResourceString("JarVerificationDialog.ContinueMayCorrupt"));
					//$NON-NLS-1$ 					
				}
				*/
				labelInformation.setText(strb.toString());
				break;

			case IVerificationResult.TYPE_ENTRY_SIGNED_RECOGNIZED :
				msg =
					UpdateUIPlugin.getResourceString(
						"JarVerificationDialog.SignedComponent"+
						componentVerified);
				//$NON-NLS-1$
				setMessage(msg, WARNING);
				strb.append(
					UpdateUIPlugin.getResourceString(
						"JarVerificationDialog.KnownCertificate"+
						componentVerified));
				//$NON-NLS-1$
				strb.append("\r\n"); //$NON-NLS-1$
				strb.append(
					UpdateUIPlugin.getResourceString(
						"JarVerificationDialog.ProviderKnown"+
						componentVerified));
				//$NON-NLS-1$
				strb.append("\r\n"); //$NON-NLS-1$

				labelInformation.setText(strb.toString());

//				createCautionArea(compositeClient);
				break;
		}
	}
	
	/*
	 * Caution Label and text
	 */
	private void createCautionArea(Composite compositeClient) {
		// Composite: Caution
		//------------------------------
		Composite compositeCaution = new Composite(compositeClient, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = layout.marginHeight = 0;
		compositeCaution.setLayout(layout);
		compositeCaution.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		// Icon
		//-----
		Label label = new Label(compositeCaution,SWT.LEFT);
		label.setImage(JFaceResources.getImage(_Dialog.DLG_IMG_MESSAGE_WARNING));
		
		// Text
		//-----
		Label labelInformationCaution =
			new Label(compositeCaution, SWT.WRAP);
		labelInformationCaution.setText(
			UpdateUIPlugin.getFormattedMessage(
				"JarVerificationDialog.Caution",
				_strProviderName));
		//$NON-NLS-1$
	}

	/*
	 * Presents File & Feature information
	 */
	private void createInformationArea(Composite compositeClient) {

		// Composite: Information labels
		//------------------------------
		Composite compositeInformation = new Composite(compositeClient, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = layout.marginHeight = 0;
		compositeInformation.setLayout(layout);
		compositeInformation.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		// Feature name
		//---------------
		Label keyLabel = null;
		CLabel valueLabel = null;
		if (_strFeatureName != null && _strFeatureName.length() > 0) {
			keyLabel = new Label(compositeInformation, SWT.NULL);
			keyLabel.setText(
				UpdateUIPlugin.getResourceString("JarVerificationDialog.FeatureName"));
			//$NON-NLS-1$
			valueLabel = new CLabel(compositeInformation, SWT.NULL);
			valueLabel.setFont(JFaceResources.getBannerFont());
			valueLabel.setText(_strFeatureName);
			valueLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		}
		// Feature identifier
		//---------------------
		if (_strId != null && _strId.length() > 0) {
			keyLabel = new Label(compositeInformation, SWT.NULL);
			keyLabel.setText(
				UpdateUIPlugin.getResourceString("JarVerificationDialog.FeatureIdentifier"));
			//$NON-NLS-1$
			valueLabel = new CLabel(compositeInformation, SWT.NULL);
			valueLabel.setFont(JFaceResources.getBannerFont());
			valueLabel.setText(_strId);
			valueLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		}
		// Provider name
		//--------------
		if (_strProviderName != null && _strProviderName.length() > 0) {
			keyLabel = new Label(compositeInformation, SWT.NULL);
			keyLabel.setText(
				UpdateUIPlugin.getResourceString("JarVerificationDialog.Provider"));
			//$NON-NLS-1$
			valueLabel = new CLabel(compositeInformation, SWT.NULL);
			valueLabel.setFont(JFaceResources.getBannerFont());
			valueLabel.setText(_strProviderName);
			valueLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		}
		// Label: File name
		//-----------------
		keyLabel = new Label(compositeInformation, SWT.NULL);
		keyLabel.setText(
			UpdateUIPlugin.getResourceString("JarVerificationDialog.FileName"));
		//$NON-NLS-1$
		valueLabel = new CLabel(compositeInformation, SWT.NULL);
		valueLabel.setFont(JFaceResources.getBannerFont());
		valueLabel.setText(_fileName);
		valueLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	}

	/*
	 * Show certificate information
	 */
	private void createCertificateArea(Composite compositeClient) {

		if (_VerificationResult.getVerificationCode()
			== IVerificationResult.TYPE_ENTRY_SIGNED_UNRECOGNIZED
			|| _VerificationResult.getVerificationCode()
				== IVerificationResult.TYPE_ENTRY_SIGNED_RECOGNIZED) {
			// Group box
			//----------
			Group group = new Group(compositeClient, SWT.SHADOW_ETCHED_IN);
			GridLayout layout = new GridLayout();
			layout.numColumns = 2;
			layout.marginWidth = layout.marginHeight = 0;
			group.setLayout(layout);
			group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			group.setText(UpdateUIPlugin.getResourceString("JarVerificationDialog.CertificateInfo"));

			// Signer
			//-------------------
			Label keyLabel = null;
			Text valueText = null;
			//data = new GridData(GridData.FILL_HORIZONTAL);
			//data.horizontalIndent = 0;
			//textInformation.setLayoutData(data);			
			if (_VerificationResult.getSignerInfo() != null) {
				keyLabel = new Label(group, SWT.NULL);
				keyLabel.setText(UpdateUIPlugin.getResourceString("JarVerificationDialog.SubjectCA"));
				keyLabel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
				//$NON-NLS-1$
				valueText = new Text(group, SWT.MULTI|SWT.BORDER|SWT.WRAP|SWT.V_SCROLL);
				valueText.setText(_VerificationResult.getSignerInfo());
				valueText.setEditable(false);
				valueText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			}
			
			// Authenticator
			//---------------------
			if (_VerificationResult.getVerifierInfo() != null) {
				keyLabel = new Label(group, SWT.NULL);
				keyLabel.setText(UpdateUIPlugin.getResourceString("JarVerificationDialog.RootCA"));
				keyLabel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));				
				//$NON-NLS-1$
				valueText = new Text(group, SWT.MULTI|SWT.BORDER|SWT.WRAP|SWT.V_SCROLL);
				valueText.setText(_VerificationResult.getVerifierInfo());
				valueText.setEditable(false);
				valueText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			}
		}
	}

	/*
	 * Sets the Dialog
	 */
	public void setTitleAreaDialog(TitleAreaDialog dialog) {
		_Dialog = dialog;
	};

	/*
	 * 
	 */
	public void setMessage(String newMessage, int newType) {
		super.setMessage(newMessage, newType);
		if (_Dialog != null) {
			_Dialog.setMessage(newMessage, newType);
		}
	}

}