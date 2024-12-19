/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Atsuhiko Yamanaka, JCraft,Inc. - initial API and implementation.
 *     IBM Corporation - ongoing maintenance
 *     Atsuhiko Yamanaka, JCraft,Inc. - re-implement the public key transfer by using IJSchLocation.
 *     Atsuhiko Yamanaka, JCraft,Inc. - Bug 351094 SSH2 Key Management Comment Field stuck on RSA-1024
 *******************************************************************************/
package org.eclipse.jsch.internal.ui.preference;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jsch.core.IJSchLocation;
import org.eclipse.jsch.core.IJSchService;
import org.eclipse.jsch.internal.core.IConstants;
import org.eclipse.jsch.internal.core.JSchCorePlugin;
import org.eclipse.jsch.internal.core.Utils;
import org.eclipse.jsch.internal.ui.JSchUIPlugin;
import org.eclipse.jsch.internal.ui.Messages;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.HostKeyRepository;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.KeyPair;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;

public class PreferencePage extends org.eclipse.jface.preference.PreferencePage
		implements IWorkbenchPreferencePage{

	private static final String SSH2_PREFERENCE_PAGE_CONTEXT="org.eclipse.jsch.ui.ssh2_preference_page_context"; //$NON-NLS-1$
	private static final int RSA_MAX_KEY_SIZE = 15360;
	private static final int DSA_MAX_KEY_SIZE = 3072;
	private static final int INITIAL_KEY_SIZE = 4096;
	private static final int MIN_KEY_SIZE = 2048;
	private static final int KEY_SIZE_INCREMENT = 1024;

	private Label ssh2HomeLabel;
	private Label privateKeyLabel;
	Text ssh2HomeText;
	Text privateKeyText;
	private Button privateKeyAdd;

	private Button ssh2HomeBrowse;
	Button keyGenerateDSA;
	Button keyGenerateRSA;
	private Spinner keySizeValue;
	private Button keyLoad;
	private Button keyExport;
	Button saveKeyPair;
	private Label keyCommentLabel;
	Text keyCommentText;
	private Label keyFingerPrintLabel;
	Text keyFingerPrintText;
	private Label keyPassphrase1Label;
	Text keyPassphrase1Text;
	private Label keyPassphrase2Label;
	Text keyPassphrase2Text;
	private Label publicKeylabel;
	Text publicKeyText;
	KeyPair kpair=null;
	String kpairComment;

	public static final String AUTH_SCHEME="";//$NON-NLS-1$

	public PreferencePage(){
		setDescription(Messages.CVSSSH2PreferencePage_18);
	}

	@Override
protected Control createContents(Composite parent){
		Composite container=new Composite(parent, SWT.NULL);
		GridLayout layout=new GridLayout();
		container.setLayout(layout);
		initializeDialogUnits(container);

		CTabFolder tabFolder=new CTabFolder(container, SWT.NONE);
		tabFolder.setLayoutData(new GridData(GridData.FILL_BOTH));

		CTabItem tabItem=new CTabItem(tabFolder, SWT.NONE);
		tabItem.setText(Messages.CVSSSH2PreferencePage_19);
		tabItem.setControl(createGeneralPage(tabFolder));

		tabItem=new CTabItem(tabFolder, SWT.NONE);
		tabItem.setText(Messages.CVSSSH2PreferencePage_21);
		tabItem.setControl(createKeyManagementPage(tabFolder));

		tabItem=new CTabItem(tabFolder, SWT.NONE);
		tabItem.setText(Messages.CVSSSH2PreferencePage_133);
		tabItem.setControl(createHostKeyManagementPage(tabFolder));

		tabItem=new CTabItem(tabFolder, SWT.NONE);
		tabItem.setText(Messages.CVSSSH2PreferencePage_137);
		tabItem.setControl(createPreferredAuthenticationPage(tabFolder));

		tabItem=new CTabItem(tabFolder, SWT.NONE);
		tabItem.setText(Messages.CVSSSH2PreferencePage_144);
		tabItem.setControl(createPreferredKeyExchangePage(tabFolder));

		tabItem=new CTabItem(tabFolder, SWT.NONE);
		tabItem.setText(Messages.CVSSSH2PreferencePage_145);
		tabItem.setControl(createPreferredMACPage(tabFolder));

		tabItem=new CTabItem(tabFolder, SWT.NONE);
		tabItem.setText(Messages.CVSSSH2PreferencePage_146);
		tabItem.setControl(createPreferredSSHAgentPage(tabFolder));
		tabFolder.setSelection(0);
		initControls();

		Dialog.applyDialogFont(parent);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(),
				SSH2_PREFERENCE_PAGE_CONTEXT);
		return container;
	}

	private Control createGeneralPage(Composite parent){
		Composite group=new Composite(parent, SWT.NULL);
		GridLayout layout=new GridLayout();
		layout.numColumns=3;
		layout.marginHeight=convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
		layout.marginWidth=convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
		layout.verticalSpacing=convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		layout.horizontalSpacing=convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		group.setLayout(layout);
		GridData data=new GridData();
		data.horizontalAlignment=GridData.FILL;
		group.setLayoutData(data);

		ssh2HomeLabel=new Label(group, SWT.NONE);
		ssh2HomeLabel.setText(Messages.CVSSSH2PreferencePage_23);

		ssh2HomeText=new Text(group, SWT.SINGLE|SWT.BORDER);
		ssh2HomeText.setFont(group.getFont());
		GridData gd=new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan=1;
		ssh2HomeText.setLayoutData(gd);

		ssh2HomeBrowse=new Button(group, SWT.NULL);
		ssh2HomeBrowse.setText(Messages.CVSSSH2PreferencePage_24);
		gd=new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan=1;
		ssh2HomeBrowse.setLayoutData(gd);

		createSpacer(group, 3);

		privateKeyLabel=new Label(group, SWT.NONE);
		privateKeyLabel.setText(Messages.CVSSSH2PreferencePage_25);

		privateKeyText=new Text(group, SWT.SINGLE|SWT.BORDER);
		privateKeyText.setFont(group.getFont());
		gd=new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan=1;
		privateKeyText.setLayoutData(gd);

		privateKeyAdd=new Button(group, SWT.NULL);
		privateKeyAdd.setText(Messages.CVSSSH2PreferencePage_26);
		gd=new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan=1;
		privateKeyAdd.setLayoutData(gd);

		ssh2HomeBrowse.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				String home=ssh2HomeText.getText();

				if(!new File(home).exists()){
					while(true){
						int foo=home.lastIndexOf(java.io.File.separator, home.length());
						if(foo==-1)
							break;
						home=home.substring(0, foo);
						if(new File(home).exists())
							break;
					}
				}

				DirectoryDialog dd=new DirectoryDialog(getShell());
				dd.setFilterPath(home);
				dd.setMessage(Messages.CVSSSH2PreferencePage_27);
				String dir=dd.open();
				if(dir==null){ // cancel
					return;
				}
				ssh2HomeText.setText(dir);
			}
		});

		privateKeyAdd.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				String home=ssh2HomeText.getText();

				FileDialog fd=new FileDialog(getShell(), SWT.OPEN|SWT.MULTI);
				fd.setFilterPath(home);
				Object o=fd.open();
				if(o==null){ // cancel
					return;
				}
				String[] files=fd.getFileNames();
				String keys=privateKeyText.getText();
				String dir=fd.getFilterPath();
				if(dir.equals(home)){
					dir="";} //$NON-NLS-1$
				else{
					dir+=java.io.File.separator;
				}

				for (String foo : files) {
					if(keys.length()!=0)
						keys=keys+","; //$NON-NLS-1$
					keys=keys+dir+foo;
				}
				privateKeyText.setText(keys);
			}
		});

		return group;
	}

	private Control createKeyManagementPage(Composite parent){
		int columnSpan=3;
		Composite group=new Composite(parent, SWT.NULL);
		GridLayout layout=new GridLayout();
		layout.numColumns=3;
		layout.marginHeight=convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
		layout.marginWidth=convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
		layout.verticalSpacing=convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		layout.horizontalSpacing=convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		group.setLayout(layout);
		GridData gd=new GridData();
		gd.horizontalAlignment=GridData.FILL;
		group.setLayoutData(gd);

		keyGenerateDSA=new Button(group, SWT.NULL);
		keyGenerateDSA.setText(Messages.CVSSSH2PreferencePage_131);
		gd=new GridData();
		gd.horizontalSpan=1;
		keyGenerateDSA.setLayoutData(gd);

		keyGenerateRSA=new Button(group, SWT.NULL);
		keyGenerateRSA.setText(Messages.CVSSSH2PreferencePage_132);
		gd=new GridData();
		gd.horizontalSpan=1;
		keyGenerateRSA.setLayoutData(gd);

		keyLoad=new Button(group, SWT.NULL);
		keyLoad.setText(Messages.CVSSSH2PreferencePage_128);
		gd=new GridData();
		gd.horizontalSpan=1;
		keyLoad.setLayoutData(gd);

		final Label keySizeValueLabel = new Label(group, SWT.NONE);
		keySizeValueLabel.setText(Messages.CVSSSH2PreferencePage_148);
		keySizeValue = new Spinner(group, SWT.BORDER);
		int maxKeySize = Math.max(DSA_MAX_KEY_SIZE, RSA_MAX_KEY_SIZE);
		keySizeValue.setValues(INITIAL_KEY_SIZE, MIN_KEY_SIZE, maxKeySize, 0, KEY_SIZE_INCREMENT, KEY_SIZE_INCREMENT);
		keySizeValue.addKeyListener(KeyListener.keyPressedAdapter(e -> e.doit = false));

		publicKeylabel=new Label(group, SWT.NONE);
		publicKeylabel.setText(Messages.CVSSSH2PreferencePage_39);
		gd=new GridData();
		gd.horizontalSpan=columnSpan;
		publicKeylabel.setLayoutData(gd);

		publicKeyText=new Text(group, SWT.MULTI|SWT.BORDER|SWT.V_SCROLL|SWT.WRAP|SWT.LEFT_TO_RIGHT);
		publicKeyText.setText(""); //$NON-NLS-1$
		publicKeyText.setEditable(false);
		gd=new GridData();
		gd.horizontalSpan=columnSpan;
		gd.horizontalAlignment=GridData.FILL;
		gd.verticalAlignment=GridData.FILL;
		gd.grabExcessHorizontalSpace=true;
		gd.grabExcessVerticalSpace=true;
		publicKeyText.setLayoutData(gd);

		keyFingerPrintLabel=new Label(group, SWT.NONE);
		keyFingerPrintLabel.setText(Messages.CVSSSH2PreferencePage_41);
		keyFingerPrintText=new Text(group, SWT.SINGLE|SWT.BORDER|SWT.LEFT_TO_RIGHT);
		keyFingerPrintText.setFont(group.getFont());
		keyFingerPrintText.setEditable(false);
		gd=new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan=2;
		keyFingerPrintText.setLayoutData(gd);

		keyCommentLabel=new Label(group, SWT.NONE);
		keyCommentLabel.setText(Messages.CVSSSH2PreferencePage_42);
		keyCommentText=new Text(group, SWT.SINGLE|SWT.BORDER);
		keyCommentText.setFont(group.getFont());
		gd=new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan=2;
		keyCommentText.setLayoutData(gd);

		keyCommentText.addModifyListener(e -> {
			if(kpair==null)
				return;
			try{
				ByteArrayOutputStream out=new ByteArrayOutputStream();
				kpairComment = keyCommentText.getText();
				kpair.writePublicKey(out, kpairComment);
				out.close();
				publicKeyText.setText(out.toString());
			}
			catch(IOException ee){
				// Ignore
			}
		});

		keyPassphrase1Label=new Label(group, SWT.NONE);
		keyPassphrase1Label.setText(Messages.CVSSSH2PreferencePage_43);
		keyPassphrase1Text=new Text(group, SWT.SINGLE|SWT.BORDER);
		keyPassphrase1Text.setFont(group.getFont());
		keyPassphrase1Text.setEchoChar('*');
		gd=new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan=2;
		keyPassphrase1Text.setLayoutData(gd);

		keyPassphrase2Label=new Label(group, SWT.NONE);
		keyPassphrase2Label.setText(Messages.CVSSSH2PreferencePage_44);
		keyPassphrase2Text=new Text(group, SWT.SINGLE|SWT.BORDER);
		keyPassphrase2Text.setFont(group.getFont());
		keyPassphrase2Text.setEchoChar('*');
		gd=new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan=2;
		keyPassphrase2Text.setLayoutData(gd);

		keyPassphrase1Text.addModifyListener(e -> {
			String pass1=keyPassphrase1Text.getText();
			String pass2=keyPassphrase2Text.getText();
			if(kpair!=null&&pass1.equals(pass2)){
				saveKeyPair.setEnabled(true);
			}
			else{
				saveKeyPair.setEnabled(false);
			}
			if(pass2.length()==0){
				setErrorMessage(null);
				return;
			}
			if(pass1.equals(pass2)){
				setErrorMessage(null);
			}
			else{
				setErrorMessage(Messages.CVSSSH2PreferencePage_48);
			}
		});

		keyPassphrase2Text.addModifyListener(e -> {
			String pass1=keyPassphrase1Text.getText();
			String pass2=keyPassphrase2Text.getText();
			if(kpair!=null&&pass1.equals(pass2)){
				saveKeyPair.setEnabled(true);
			}
			else{
				saveKeyPair.setEnabled(false);
			}
			if(pass2.length()<pass1.length()){
				if(pass1.startsWith(pass2)){
					setErrorMessage(null);
				}
				else{
					setErrorMessage(Messages.CVSSSH2PreferencePage_48);
				}
				return;
			}
			if(pass1.equals(pass2)){
				setErrorMessage(null);
			}
			else{
				setErrorMessage(Messages.CVSSSH2PreferencePage_48);
			}
		});

		keyPassphrase2Text.addFocusListener(new FocusListener(){
			@Override
			public void focusGained(FocusEvent e){
				String pass1=keyPassphrase1Text.getText();
				String pass2=keyPassphrase2Text.getText();
				if(pass2.length()<pass1.length()){
					if(pass1.startsWith(pass2)){
						setErrorMessage(null);
					}
					else{
						setErrorMessage(Messages.CVSSSH2PreferencePage_48);
					}
					return;
				}
				if(pass1.equals(pass2)){
					setErrorMessage(null);
				}
				else{
					setErrorMessage(Messages.CVSSSH2PreferencePage_48);
				}
			}

			@Override
			public void focusLost(FocusEvent e){
				String pass1=keyPassphrase1Text.getText();
				String pass2=keyPassphrase2Text.getText();
				if(pass1.equals(pass2)){
					setErrorMessage(null);
				}
				else{
					setErrorMessage(Messages.CVSSSH2PreferencePage_48);
				}
			}
		});

		Composite buttons=new Composite(group, SWT.NONE);
		layout=new GridLayout(2, true);
		layout.marginWidth=0;
		layout.marginHeight=0;
		layout.horizontalSpacing=convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		buttons.setLayout(layout);
		gd=new GridData(GridData.HORIZONTAL_ALIGN_END);
		gd.horizontalSpan=columnSpan;
		buttons.setLayoutData(gd);

		keyExport=new Button(buttons, SWT.NULL);
		keyExport.setText(Messages.CVSSSH2PreferencePage_105);
		gd=new GridData(GridData.FILL_BOTH);
		keyExport.setLayoutData(gd);

		saveKeyPair=new Button(buttons, SWT.NULL);
		saveKeyPair.setText(Messages.CVSSSH2PreferencePage_45);
		gd=new GridData(GridData.FILL_BOTH);
		saveKeyPair.setLayoutData(gd);

		SelectionAdapter keygenadapter=new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				boolean ok=true;
				String _type=""; //$NON-NLS-1$

				try{
					int type=0;
					if(e.widget==keyGenerateDSA){
						type=KeyPair.DSA;
						_type=IConstants.DSA;
						if (keySizeValue.getSelection() > DSA_MAX_KEY_SIZE) {
							keySizeValue.setSelection(DSA_MAX_KEY_SIZE);
						}
					}
					else if(e.widget==keyGenerateRSA){
						type=KeyPair.RSA;
						_type=IConstants.RSA;
					}
					// TODO: ECDSA
					else{
						return;
					}

					final KeyPair[] _kpair=new KeyPair[1];
					final int __type=type;
					int keySize = keySizeValue.getSelection();
					final JSchException[] _e=new JSchException[1];
					BusyIndicator.showWhile(getShell().getDisplay(), () -> {
						try {
							_kpair[0] = KeyPair.genKeyPair(getJSch(), __type, keySize);
						} catch (JSchException e1) {
							_e[0] = e1;
						}
					});
					if(_e[0]!=null){
						throw _e[0];
					}
					kpair=_kpair[0];

					ByteArrayOutputStream out=new ByteArrayOutputStream();
					kpairComment = _type + "-" + keySize; //$NON-NLS-1$
					kpair.writePublicKey(out, kpairComment);
					out.close();
					publicKeyText.setText(out.toString());
					keyFingerPrintText.setText(kpair.getFingerPrint());
					keyCommentText.setText(kpairComment);
					keyPassphrase1Text.setText(""); //$NON-NLS-1$
					keyPassphrase2Text.setText(""); //$NON-NLS-1$
					updateControls();
				}
				catch(IOException ee){
					ok=false;
				}
				catch(JSchException ee){
					ok=false;
				}
				if(!ok){
					MessageDialog.openError(getShell(),
							Messages.CVSSSH2PreferencePage_error,
							Messages.CVSSSH2PreferencePage_47);
				}
			}
		};
		keyGenerateDSA.addSelectionListener(keygenadapter);
		keyGenerateRSA.addSelectionListener(keygenadapter);

		keyLoad.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				boolean ok=true;
				String home=ssh2HomeText.getText();
				FileDialog fd=new FileDialog(getShell(), SWT.OPEN);
				fd.setFilterPath(home);
				Object o=fd.open();
				if(o==null){ // cancel
					return;
				}
				String pkey=fd.getFileName();
				String pkeyab=(new File(fd.getFilterPath(), pkey)).getAbsolutePath();
				try{
					KeyPair _kpair=KeyPair.load(getJSch(), pkeyab);
					PassphrasePrompt prompt=null;
					while(_kpair.isEncrypted()){
						if(prompt==null){
							prompt=new PassphrasePrompt(NLS.bind(
									Messages.CVSSSH2PreferencePage_126, new String[] {pkey}));
						}
						Display.getDefault().syncExec(prompt);
						String passphrase=prompt.getPassphrase();
						if(passphrase==null)
							break;
						if(_kpair.decrypt(passphrase)){
							break;
						}
						MessageDialog.openError(getShell(),
								Messages.CVSSSH2PreferencePage_error, NLS.bind(
										Messages.CVSSSH2PreferencePage_129, new String[] {pkey}));
					}
					if(_kpair.isEncrypted()){
						return;
					}
					kpair=_kpair;
					String _type=(kpair.getKeyType()==KeyPair.DSA) ? IConstants.DSA
							: IConstants.RSA;
					kpairComment=_type+"-1024"; //$NON-NLS-1$

					// TODO Bug 351094 The comment should be from kpair object,
					// but the JSch API does not provided such a method.
					// In the version 0.1.45, JSch will support such a method,
					// and the following code should be replaced with it at that time.
					java.io.File f = new java.io.File(pkeyab + ".pub"); //$NON-NLS-1$
					int i = 0;
					try (java.io.FileInputStream fis = new java.io.FileInputStream(f)) {
						byte[] buf=new byte[(int)f.length()];
						while(i<buf.length){
							int j=fis.read(buf, i, buf.length-i);
							if(j<=0)
								break;
							i+=j;
						}
						String pubkey=new String(buf);
						if(pubkey.indexOf(' ')>0
								&&pubkey.indexOf(' ', pubkey.indexOf(' ')+1)>0){
							int j=pubkey.indexOf(' ', pubkey.indexOf(' ')+1)+1;
							kpairComment=pubkey.substring(j);
							if(kpairComment.indexOf('\n')>0){
								kpairComment=kpairComment.substring(0,
										kpairComment.indexOf('\n'));
							}
						}
					}
					catch(IOException ioe){
						// ignore if public-key does not exist.
					}

					ByteArrayOutputStream out=new ByteArrayOutputStream();

					kpair.writePublicKey(out, kpairComment);
					out.close();
					publicKeyText.setText(out.toString());
					keyFingerPrintText.setText(kpair.getFingerPrint());
					keyCommentText.setText(kpairComment);
					keyPassphrase1Text.setText(""); //$NON-NLS-1$
					keyPassphrase2Text.setText(""); //$NON-NLS-1$
					updateControls();
				}
				catch(IOException ee){
					ok=false;
				}
				catch(JSchException ee){
					ok=false;
				}
				if(!ok){
					MessageDialog.openError(getShell(),
							Messages.CVSSSH2PreferencePage_error,
							Messages.CVSSSH2PreferencePage_130);
				}
			}
		});

		keyExport.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				if(kpair==null)
					return;

				setErrorMessage(null);

				final String[] target=new String[1];
				final String title=Messages.CVSSSH2PreferencePage_106;
				final String message = Messages.CVSSSH2PreferencePage_107;
				Display.getDefault().syncExec(() -> {
					Display display = Display.getCurrent();
					Shell shell = new Shell(display);
					ExportDialog dialog = new ExportDialog(shell, title, message);
					dialog.open();
					shell.dispose();
					target[0] = dialog.getTarget();
				});
				if (target[0] == null) {
					return;
				}
				String user=""; //$NON-NLS-1$
				String host=""; //$NON-NLS-1$
				int port=22;

				if(target[0].indexOf('@')>0){
					user=target[0].substring(0, target[0].indexOf('@'));
					host=target[0].substring(target[0].indexOf('@')+1);
				}
				if(host.indexOf(':')>0){
					try{
						port=Integer.parseInt(host.substring(host.indexOf(':')+1));
					}
					catch(NumberFormatException ee){
						port=-1;
					}
					host=host.substring(0, host.indexOf(':'));
				}

				if(user.length()==0||host.length()==0||port==-1){
					setErrorMessage(NLS.bind(Messages.CVSSSH2PreferencePage_108,
							new String[] {target[0]}));
					return;
				}

				String options=""; //$NON-NLS-1$
				try{
					ByteArrayOutputStream bos=new ByteArrayOutputStream();
					if(options.length()!=0){
						try{
							bos.write((options+" ").getBytes());} //$NON-NLS-1$
						catch(IOException eeee){
							// Ignore
						}
					}
					kpair.writePublicKey(bos, kpairComment);
					bos.close();
					export_via_sftp(user, host, port, /* ".ssh/authorized_keys", //$NON-NLS-1$ */
							bos.toByteArray());
				}
				catch(IOException ee){
					// Ignore
				}
				catch(JSchException ee){
					setErrorMessage(Messages.CVSSSH2PreferencePage_111);
				}
			}
		});

		saveKeyPair.addSelectionListener(new SelectionAdapter(){
			@Override
	public void widgetSelected(SelectionEvent e){
				if(kpair==null)
					return;

				String pass=keyPassphrase1Text.getText();
				/*
				 * if(!pass.equals(keyPassphrase2Text.getText())){
				 * setErrorMessage(Policy.bind("CVSSSH2PreferencePage.48"));
				 * //$NON-NLS-1$ return; }
				 */
				if(pass.length()==0){
					if(!MessageDialog.openConfirm(getShell(),
							Messages.CVSSSH2PreferencePage_confirmation,
							Messages.CVSSSH2PreferencePage_49)){
						return;
					}
				}

				kpair.setPassphrase(pass);

				String home=ssh2HomeText.getText();

				File _home=new File(home);

				if(!_home.exists()){
					if(!MessageDialog.openConfirm(getShell(),
							Messages.CVSSSH2PreferencePage_confirmation, NLS.bind(
									Messages.CVSSSH2PreferencePage_50, new String[] {home}))){
						return;
					}
					if(!_home.mkdirs()){
						setErrorMessage(Messages.CVSSSH2PreferencePage_100+home);
						return;
					}
				}

				FileDialog fd=new FileDialog(getShell(), SWT.SAVE);
				fd.setFilterPath(home);
				String file=(kpair.getKeyType()==KeyPair.RSA) ? "id_rsa" : "id_dsa"; //$NON-NLS-1$ //$NON-NLS-2$
				fd.setFileName(file);
				file=fd.open();
				if(file==null){ // cancel
					return;
				}

				if(new File(file).exists()){
					if(!MessageDialog.openConfirm(getShell(),
							Messages.CVSSSH2PreferencePage_confirmation, //
							NLS.bind(Messages.CVSSSH2PreferencePage_53, new String[] {file}))){
						return;
					}
				}

				boolean ok=true;
				try{
					kpair.writePrivateKey(file);
					kpair.writePublicKey(file+".pub", kpairComment); //$NON-NLS-1$
				}
				catch(Exception ee){
					ok=false;
				}

				if(ok){
					MessageDialog.openInformation(getShell(),
							Messages.CVSSSH2PreferencePage_information,
							Messages.CVSSSH2PreferencePage_55+"\n"+ //$NON-NLS-1$
									Messages.CVSSSH2PreferencePage_57+file+"\n"+ //$NON-NLS-1$
									Messages.CVSSSH2PreferencePage_59+file+".pub"); //$NON-NLS-1$
				}
				else{
					return;
				}

				// The generated key should be added to privateKeyText.

				String dir=fd.getFilterPath();
				File mypkey=new java.io.File(dir, fd.getFileName());
				String pkeys=privateKeyText.getText();

				// Check if the generated key has been included in pkeys?
				String[] pkeysa=pkeys.split(","); //$NON-NLS-1$
				for (String p : pkeysa) {
					File pkey = new java.io.File(p);
					if (!pkey.isAbsolute()) {
						pkey = new java.io.File(home, p);
					}
					if(pkey.equals(mypkey))
						return;
				}

				if(dir.equals(home)){
					dir="";} //$NON-NLS-1$
				else{
					dir+=java.io.File.separator;
				}
				if(pkeys.length()>0)
					pkeys+=","; //$NON-NLS-1$
				pkeys=pkeys+dir+fd.getFileName();
				privateKeyText.setText(pkeys);
			}
		});

		return group;
	}

	private TableViewer viewer;
	private Button removeHostKeyButton;

	Table preferedAuthMethodTable;
	Table preferedSSHAgentTable;
	Table preferedKeyExchangeMethodTable;
	Table preferedMACMethodTable;

	Button up;
	Button down;
	Button kex_up;
	Button kex_down;
	Button mac_up;
	Button mac_down;

	class TableLabelProvider extends LabelProvider implements ITableLabelProvider{
		@Override
		public String getColumnText(Object element, int columnIndex){
			HostKey entry=(HostKey)element;
			switch(columnIndex){
				case 0:
					return entry.getHost();
				case 1:
					return entry.getType();
				case 2:
					return entry.getFingerPrint(getJSch());
				default:
					return null;
			}
		}

		@Override
		public Image getColumnImage(Object element, int columnIndex){
			return null;
		}
	}

	private Control createHostKeyManagementPage(Composite parent){
		Composite group=new Composite(parent, SWT.NULL);
		GridLayout layout=new GridLayout();
		layout.marginHeight=convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
		layout.marginWidth=convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
		layout.verticalSpacing=convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		layout.horizontalSpacing=convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		layout.numColumns=2;
		group.setLayout(layout);
		GridData gd=new GridData();
		gd.horizontalAlignment=GridData.FILL;
		gd.verticalAlignment=GridData.FILL;
		group.setLayoutData(gd);

		Label label=new Label(group, SWT.NONE);
		label.setText(Messages.CVSSSH2PreferencePage_139);
		gd=new GridData();
		gd.horizontalSpan=2;
		label.setLayoutData(gd);

		viewer=new TableViewer(group, SWT.MULTI|SWT.FULL_SELECTION|SWT.H_SCROLL
				|SWT.V_SCROLL|SWT.BORDER);
		Table table=viewer.getTable();
		new TableEditor(table);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		gd=new GridData(GridData.FILL_BOTH);
		gd.widthHint=convertWidthInCharsToPixels(30);
		/*
		 * The hardcoded hint does not look elegant, but in reality it does not make
		 * anything bound to this 100-pixel value, because in any case the tree on
		 * the left is taller and that's what really determines the height.
		 */
		gd.heightHint=100;
		table.setLayoutData(gd);
		table.addListener(SWT.Selection, e -> handleSelection());
		// Create the table columns
		new TableColumn(table, SWT.NULL);
		new TableColumn(table, SWT.NULL);
		new TableColumn(table, SWT.NULL);
		TableColumn[] columns=table.getColumns();
		columns[0].setText(Messages.CVSSSH2PreferencePage_134);
		columns[1].setText(Messages.CVSSSH2PreferencePage_135);
		columns[2].setText(Messages.CVSSSH2PreferencePage_136);
		viewer.setColumnProperties(new String[] {
				Messages.CVSSSH2PreferencePage_134, //
				Messages.CVSSSH2PreferencePage_135, //
				Messages.CVSSSH2PreferencePage_136});
		viewer.setLabelProvider(new TableLabelProvider());
		viewer.setContentProvider(new IStructuredContentProvider(){
			@Override
			public void dispose(){
				// nothing to do
			}

			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput){
				// nothing to do
			}

			@Override
			public Object[] getElements(Object inputElement){
				if(inputElement==null)
					return null;
				return (Object[])inputElement;
			}
		});
		TableLayout tl=new TableLayout();
		tl.addColumnData(new ColumnWeightData(30));
		tl.addColumnData(new ColumnWeightData(20));
		tl.addColumnData(new ColumnWeightData(70));
		table.setLayout(tl);

		Composite buttons=new Composite(group, SWT.NULL);
		buttons.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
		layout=new GridLayout();
		layout.marginHeight=0;
		layout.marginWidth=0;
		buttons.setLayout(layout);

		removeHostKeyButton=new Button(buttons, SWT.PUSH);
		removeHostKeyButton.setText(Messages.CVSSSH2PreferencePage_138);
		int buttonWidth=SWTUtils
				.calculateControlSize(SWTUtils.createDialogPixelConverter(parent),
						new Button[] {removeHostKeyButton});
		removeHostKeyButton.setLayoutData(SWTUtils.createGridData(buttonWidth,
				SWT.DEFAULT, SWT.END, SWT.CENTER, false, false));
		removeHostKeyButton.setEnabled(false);
		removeHostKeyButton.addListener(SWT.Selection, e -> removeHostKey());

		Dialog.applyDialogFont(parent);

		// JSchSession.loadKnownHosts(JSchContext.getDefaultContext().getJSch());
		JSchCorePlugin.getPlugin().loadKnownHosts();
		HostKeyRepository hkr=getJSch().getHostKeyRepository();
		viewer.setInput(hkr.getHostKey());
		handleSelection();

		return group;
	}

	private Control createPreferredAuthenticationPage(Composite parent){
		Composite root = new Composite(parent, SWT.NONE);
		GridLayout layout=new GridLayout();
		layout.marginHeight=convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
		layout.marginWidth=convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
		layout.verticalSpacing=convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		layout.horizontalSpacing=convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		layout.numColumns = 2;
		root.setLayout(layout);

		Label label=new Label(root, SWT.NONE);
		GridData textLayoutData=new GridData(SWT.BEGINNING, SWT.BEGINNING, true, false);
		textLayoutData.horizontalSpan = 2;
		label.setLayoutData(textLayoutData);
		label.setText(Messages.CVSSSH2PreferencePage_4);

		preferedAuthMethodTable=new Table(root, SWT.CHECK | SWT.BORDER);
		GridData layoutData=new GridData(SWT.FILL, SWT.BEGINNING, true, true);
		layoutData.verticalSpan = 3;
		preferedAuthMethodTable.setLayoutData(layoutData);
		layoutData.minimumHeight = 150;
		layoutData.minimumWidth = 200;
		populateAuthMethods();

		up=new Button(root, SWT.PUSH);
		up.setText(Messages.CVSSSH2PreferencePage_2);
		up.setEnabled(false);
		setButtonLayoutData(up);

		down=new Button(root, SWT.PUSH);
		down.setText(Messages.CVSSSH2PreferencePage_3);
		down.setEnabled(false);
		setButtonLayoutData(down);

		preferedAuthMethodTable.addSelectionListener(new SelectionAdapter(){

			@Override
			public void widgetSelected(SelectionEvent e){
				boolean anySelected = false;
				for(int i = 0; i < preferedAuthMethodTable.getItemCount(); i++){
					anySelected |= preferedAuthMethodTable.getItem(i).getChecked();
				}

				if(anySelected){
					setErrorMessage(null);
					setValid(true);
				}
				else{
					setErrorMessage(Messages.CVSSSH2PreferencePage_5);
					setValid(false);
				}
				up.setEnabled(preferedAuthMethodTable.getSelectionIndex()>0);
				down
						.setEnabled(preferedAuthMethodTable.getSelectionIndex()<preferedAuthMethodTable
								.getItemCount()-1);
			}

		});
		up.addSelectionListener(new SelectionAdapter(){

			@Override
			public void widgetSelected(SelectionEvent e){
				int selectedIndex=preferedAuthMethodTable.getSelectionIndex();
				if(selectedIndex == 1){ //this is the last possible swap
					up.setEnabled(false);
				}
				down.setEnabled(true);
				TableItem sourceItem = preferedAuthMethodTable.getItem(selectedIndex);
				TableItem targetItem = preferedAuthMethodTable.getItem(selectedIndex-1);

				//switch text
				String stemp = targetItem.getText();
				targetItem.setText(sourceItem.getText());
				sourceItem.setText(stemp);

				//switch selection
				boolean btemp = targetItem.getChecked();
				targetItem.setChecked(sourceItem.getChecked());
				sourceItem.setChecked(btemp);

				preferedAuthMethodTable.setSelection(targetItem);
			}
		});

		down.addSelectionListener(new SelectionAdapter(){

			@Override
			public void widgetSelected(SelectionEvent e){
				int selectedIndex=preferedAuthMethodTable.getSelectionIndex();
				if(selectedIndex == preferedAuthMethodTable.getItemCount()-2){ //this is the last possible swap
					down.setEnabled(false);
				}
				up.setEnabled(true);
				TableItem sourceItem = preferedAuthMethodTable.getItem(selectedIndex);
				TableItem targetItem = preferedAuthMethodTable.getItem(selectedIndex+1);

				//switch text
				String stemp = targetItem.getText();
				targetItem.setText(sourceItem.getText());
				sourceItem.setText(stemp);

				//switch selection
				boolean btemp = targetItem.getChecked();
				targetItem.setChecked(sourceItem.getChecked());
				sourceItem.setChecked(btemp);

				preferedAuthMethodTable.setSelection(targetItem);
			}
		});

		return root;
	}

	private void populateAuthMethods(){
		preferedAuthMethodTable.removeAll();
		String[] methods = Utils.getEnabledPreferredAuthMethods().split(","); //$NON-NLS-1$
		Set<String> smethods = new HashSet<>(Arrays.asList(methods));

		String[] order = Utils.getMethodsOrder().split(","); //$NON-NLS-1$
		for (String o : order) {
			TableItem tableItem= new TableItem(preferedAuthMethodTable, SWT.NONE);
			tableItem.setText(0, o);
			if (smethods.contains(o)) {
				tableItem.setChecked(true);
			}
		}
	}

	private Control createPreferredKeyExchangePage(Composite parent){
		Composite root = new Composite(parent, SWT.NONE);
		GridLayout layout=new GridLayout();
		layout.marginHeight=convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
		layout.marginWidth=convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
		layout.verticalSpacing=convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		layout.horizontalSpacing=convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		layout.numColumns = 2;
		root.setLayout(layout);

		Label label=new Label(root, SWT.NONE);
		GridData textLayoutData=new GridData(SWT.BEGINNING, SWT.BEGINNING, true, false);
		textLayoutData.horizontalSpan = 2;
		label.setLayoutData(textLayoutData);
		label.setText(Messages.CVSSSH2PreferencePage_140);

		preferedKeyExchangeMethodTable=new Table(root, SWT.CHECK | SWT.BORDER);
		GridData layoutData=new GridData(SWT.FILL, SWT.BEGINNING, true, true);
		layoutData.verticalSpan = 3;
		preferedKeyExchangeMethodTable.setLayoutData(layoutData);
		layoutData.minimumHeight = 150;
		layoutData.minimumWidth = 200;
		populateAuthMethods();

		kex_up=new Button(root, SWT.PUSH);
		kex_up.setText(Messages.CVSSSH2PreferencePage_2);
		kex_up.setEnabled(false);
		setButtonLayoutData(kex_up);

		kex_down=new Button(root, SWT.PUSH);
		kex_down.setText(Messages.CVSSSH2PreferencePage_3);
		kex_down.setEnabled(false);
		setButtonLayoutData(kex_down);

		preferedKeyExchangeMethodTable.addSelectionListener(new SelectionAdapter(){

			@Override
			public void widgetSelected(SelectionEvent e){
				boolean anySelected = false;
				for(int i = 0; i < preferedKeyExchangeMethodTable.getItemCount(); i++){
					anySelected |= preferedKeyExchangeMethodTable.getItem(i).getChecked();
				}

				if(anySelected){
					setErrorMessage(null);
					setValid(true);
				}
				else{
					setErrorMessage(Messages.CVSSSH2PreferencePage_142);
					setValid(false);
				}
				kex_up.setEnabled(preferedKeyExchangeMethodTable.getSelectionIndex()>0);
				kex_down
						.setEnabled(preferedKeyExchangeMethodTable.getSelectionIndex()<preferedKeyExchangeMethodTable
								.getItemCount()-1);
			}

		});
		kex_up.addSelectionListener(new SelectionAdapter(){

			@Override
			public void widgetSelected(SelectionEvent e){
				int selectedIndex=preferedKeyExchangeMethodTable.getSelectionIndex();
				if(selectedIndex == 1){ //this is the last possible swap
					kex_up.setEnabled(false);
				}
				kex_down.setEnabled(true);
				TableItem sourceItem = preferedKeyExchangeMethodTable.getItem(selectedIndex);
				TableItem targetItem = preferedKeyExchangeMethodTable.getItem(selectedIndex-1);

				//switch text
				String stemp = targetItem.getText();
				targetItem.setText(sourceItem.getText());
				sourceItem.setText(stemp);

				//switch selection
				boolean btemp = targetItem.getChecked();
				targetItem.setChecked(sourceItem.getChecked());
				sourceItem.setChecked(btemp);

				preferedKeyExchangeMethodTable.setSelection(targetItem);
			}
		});

		kex_down.addSelectionListener(new SelectionAdapter(){

			@Override
			public void widgetSelected(SelectionEvent e){
				int selectedIndex=preferedKeyExchangeMethodTable.getSelectionIndex();
				if(selectedIndex == preferedKeyExchangeMethodTable.getItemCount()-2){ //this is the last possible swap
					kex_down.setEnabled(false);
				}
				kex_up.setEnabled(true);
				TableItem sourceItem = preferedKeyExchangeMethodTable.getItem(selectedIndex);
				TableItem targetItem = preferedKeyExchangeMethodTable.getItem(selectedIndex+1);

				//switch text
				String stemp = targetItem.getText();
				targetItem.setText(sourceItem.getText());
				sourceItem.setText(stemp);

				//switch selection
				boolean btemp = targetItem.getChecked();
				targetItem.setChecked(sourceItem.getChecked());
				sourceItem.setChecked(btemp);

				preferedKeyExchangeMethodTable.setSelection(targetItem);
			}
		});

		return root;
	}

	private void populateKeyExchangeMethods(){
		preferedKeyExchangeMethodTable.removeAll();
		String[] methods = Utils.getEnabledPreferredKEXMethods().split(","); //$NON-NLS-1$
		Set<String> smethods = new HashSet<>(Arrays.asList(methods));

		String[] order = Utils.getKEXMethodsOrder().split(","); //$NON-NLS-1$
		for (String o : order) {
			TableItem tableItem= new TableItem(preferedKeyExchangeMethodTable, SWT.NONE);
			tableItem.setText(0, o);
			if (smethods.contains(o)) {
				tableItem.setChecked(true);
			}
		}
	}

	private Control createPreferredMACPage(Composite parent){
		Composite root = new Composite(parent, SWT.NONE);
		GridLayout layout=new GridLayout();
		layout.marginHeight=convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
		layout.marginWidth=convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
		layout.verticalSpacing=convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		layout.horizontalSpacing=convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		layout.numColumns = 2;
		root.setLayout(layout);

		Label label=new Label(root, SWT.NONE);
		GridData textLayoutData=new GridData(SWT.BEGINNING, SWT.BEGINNING, true, false);
		textLayoutData.horizontalSpan = 2;
		label.setLayoutData(textLayoutData);
		label.setText(Messages.CVSSSH2PreferencePage_141);

		preferedMACMethodTable=new Table(root, SWT.CHECK | SWT.BORDER);
		GridData layoutData=new GridData(SWT.FILL, SWT.BEGINNING, true, true);
		layoutData.verticalSpan = 3;
		preferedMACMethodTable.setLayoutData(layoutData);
		layoutData.minimumHeight = 150;
		layoutData.minimumWidth = 200;
		populateMACMethods();

		mac_up=new Button(root, SWT.PUSH);
		mac_up.setText(Messages.CVSSSH2PreferencePage_2);
		mac_up.setEnabled(false);
		setButtonLayoutData(mac_up);

		mac_down=new Button(root, SWT.PUSH);
		mac_down.setText(Messages.CVSSSH2PreferencePage_3);
		mac_down.setEnabled(false);
		setButtonLayoutData(mac_down);

		preferedMACMethodTable.addSelectionListener(new SelectionAdapter(){

			@Override
			public void widgetSelected(SelectionEvent e){
				boolean anySelected = false;
				for(int i = 0; i < preferedMACMethodTable.getItemCount(); i++){
					anySelected |= preferedMACMethodTable.getItem(i).getChecked();
				}

				if(anySelected){
					setErrorMessage(null);
					setValid(true);
				}
				else{
					setErrorMessage(Messages.CVSSSH2PreferencePage_143);
					setValid(false);
				}
				mac_up.setEnabled(preferedMACMethodTable.getSelectionIndex()>0);
				mac_down
						.setEnabled(preferedMACMethodTable.getSelectionIndex()<preferedMACMethodTable
								.getItemCount()-1);
			}

		});
		mac_up.addSelectionListener(new SelectionAdapter(){

			@Override
			public void widgetSelected(SelectionEvent e){
				int selectedIndex=preferedMACMethodTable.getSelectionIndex();
				if(selectedIndex == 1){ //this is the last possible swap
					mac_up.setEnabled(false);
				}
				mac_down.setEnabled(true);
				TableItem sourceItem = preferedMACMethodTable.getItem(selectedIndex);
				TableItem targetItem = preferedMACMethodTable.getItem(selectedIndex-1);

				//switch text
				String stemp = targetItem.getText();
				targetItem.setText(sourceItem.getText());
				sourceItem.setText(stemp);

				//switch selection
				boolean btemp = targetItem.getChecked();
				targetItem.setChecked(sourceItem.getChecked());
				sourceItem.setChecked(btemp);

				preferedMACMethodTable.setSelection(targetItem);
			}
		});

		mac_down.addSelectionListener(new SelectionAdapter(){

			@Override
			public void widgetSelected(SelectionEvent e){
				int selectedIndex=preferedMACMethodTable.getSelectionIndex();
				if(selectedIndex == preferedMACMethodTable.getItemCount()-2){ //this is the last possible swap
					mac_down.setEnabled(false);
				}
				mac_up.setEnabled(true);
				TableItem sourceItem = preferedMACMethodTable.getItem(selectedIndex);
				TableItem targetItem = preferedMACMethodTable.getItem(selectedIndex+1);

				//switch text
				String stemp = targetItem.getText();
				targetItem.setText(sourceItem.getText());
				sourceItem.setText(stemp);

				//switch selection
				boolean btemp = targetItem.getChecked();
				targetItem.setChecked(sourceItem.getChecked());
				sourceItem.setChecked(btemp);

				preferedMACMethodTable.setSelection(targetItem);
			}
		});

		return root;
	}

	private Control createPreferredSSHAgentPage(Composite parent){
		Composite root = new Composite(parent, SWT.NONE);
		GridLayout layout=new GridLayout();
		layout.marginHeight=convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
		layout.marginWidth=convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
		layout.verticalSpacing=convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		layout.horizontalSpacing=convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		layout.numColumns = 2;
		root.setLayout(layout);

		Label label=new Label(root, SWT.NONE);
		GridData textLayoutData=new GridData(SWT.BEGINNING, SWT.BEGINNING, true, false);
		textLayoutData.horizontalSpan = 2;
		label.setLayoutData(textLayoutData);
		label.setText(Messages.CVSSSH2PreferencePage_147);

		preferedSSHAgentTable=new Table(root, SWT.CHECK | SWT.BORDER);
		GridData layoutData=new GridData(SWT.FILL, SWT.BEGINNING, true, true);
		layoutData.verticalSpan = 3;
		preferedSSHAgentTable.setLayoutData(layoutData);
		layoutData.minimumHeight = 150;
		layoutData.minimumWidth = 200;
		populateSSHAgents();
		return root;
	}

	private void populateSSHAgents(){
		preferedSSHAgentTable.removeAll();
		String[] methods = Utils.getAvailableSSHAgents().split(","); //$NON-NLS-1$
		String[] selected = Utils.getSelectedSSHAgent().split(","); //$NON-NLS-1$
		for (String method : methods) {
			if (method.length() == 0) {
				continue;
			}
			TableItem tableItem= new TableItem(preferedSSHAgentTable, SWT.NONE);
			tableItem.setText(0, method);
			for (String s : selected) {
				if (s.equals(method)) {
					tableItem.setChecked(true);
					break;
				}
			}
		}
	}

	private void populateMACMethods(){
		preferedMACMethodTable.removeAll();
		String[] methods = Utils.getEnabledPreferredMACMethods().split(","); //$NON-NLS-1$
		Set<String> smethods = new HashSet<>(Arrays.asList(methods));

		String[] order = Utils.getMACMethodsOrder().split(","); //$NON-NLS-1$
		for (String o : order) {
			TableItem tableItem= new TableItem(preferedMACMethodTable, SWT.NONE);
			tableItem.setText(0, o);
			if (smethods.contains(o)) {
				tableItem.setChecked(true);
			}
		}
	}

	void handleSelection(){
		boolean empty=viewer.getSelection().isEmpty();
		removeHostKeyButton.setEnabled(!empty);
	}

	void removeHostKey(){
		IStructuredSelection selection=viewer.getStructuredSelection();
		HostKeyRepository hkr=getJSch().getHostKeyRepository();
		for(Iterator<?> iterator=selection.iterator(); iterator.hasNext();){
			HostKey hostkey=(HostKey)iterator.next();
			hkr.remove(hostkey.getHost(), hostkey.getType());
			viewer.remove(hostkey);
		}
	}

	void export_via_sftp(String user, String host, int port, byte[] pkey) throws JSchException{
		try{

			int timeout = 60000;
			IJSchService service = JSchUIPlugin.getPlugin().getJSchService();
			if (service == null) {
				MessageDialog.openInformation(getShell(), Messages.PreferencePage_0, Messages.PreferencePage_1);
				return;
			}

			IJSchLocation location=service.getLocation(user, host, port);
			// We hope that prompts for jsch are given by IJSchService, so "null" should be passed.
			Session session = service.createSession(location, null);
			session.setTimeout(timeout);
			try {
				service.connect(session, timeout, new NullProgressMonitor());
				if(!session.getServerVersion().contains("OpenSSH")){ //$NON-NLS-1$
					setErrorMessage(Messages.CVSSSH2PreferencePage_110);
					return;
				}
				Channel channel=session.openChannel("sftp"); //$NON-NLS-1$
				channel.connect();
				ChannelSftp c=(ChannelSftp)channel;

				SftpATTRS attr=null;

				try{
					attr=c.stat(".ssh");} //$NON-NLS-1$
				catch(SftpException ee){
					// Ignore
				}
				if(attr==null){
					try{
						c.mkdir(".ssh");} //$NON-NLS-1$
					catch(SftpException ee){
						setErrorMessage(ee.getMessage());
						return;
					}
				}
				try{
					c.cd(".ssh");} //$NON-NLS-1$
				catch(SftpException ee){
					setErrorMessage(ee.getMessage());
					return;
				}

				try{
					try (ByteArrayInputStream bis = new ByteArrayInputStream(pkey)) {
						c.put(bis, "authorized_keys", null, ChannelSftp.APPEND); //$NON-NLS-1$
					}
					checkPermission(c, "authorized_keys"); //$NON-NLS-1$
					checkPermission(c, "."); // .ssh //$NON-NLS-1$
					c.cd(".."); //$NON-NLS-1$
					checkPermission(c, "."); // home directory //$NON-NLS-1$
				}
				catch(SftpException ee){
					// setErrorMessage(debug+ee.message);
				}

				MessageDialog.openInformation(getShell(),
						Messages.CVSSSH2PreferencePage_information,
						NLS.bind(Messages.CVSSSH2PreferencePage_109, (user
								+"@"+host+(port==22 ? "" : ":"+port)+":~/.ssh/authorized_keys"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

				c.quit();
				c.disconnect();
			} finally {
				session.disconnect();
			}
		} catch(IOException eee){
			setErrorMessage(eee.toString());
		}
	}

	private void checkPermission(ChannelSftp c, String path) throws SftpException{
		SftpATTRS attr=c.stat(path);
		int permissions=attr.getPermissions();
		if((permissions&00022)!=0){
			permissions&=~00022;
			c.chmod(permissions, path);
		}
	}

	void updateControls(){
		boolean enable=(kpair!=null);
		publicKeylabel.setEnabled(enable);
		publicKeyText.setEnabled(enable);
		keyFingerPrintLabel.setEnabled(enable);
		keyFingerPrintText.setEnabled(enable);
		keyCommentLabel.setEnabled(enable);
		keyCommentText.setEnabled(enable);
		keyPassphrase1Label.setEnabled(enable);
		keyPassphrase1Text.setEnabled(enable);
		keyPassphrase2Label.setEnabled(enable);
		keyPassphrase2Text.setEnabled(enable);
		keyExport.setEnabled(enable);
		saveKeyPair.setEnabled(enable);
		populateAuthMethods();
		populateKeyExchangeMethods();
		populateMACMethods();
		up.setEnabled(false);
		down.setEnabled(false);
		kex_up.setEnabled(false);
		kex_down.setEnabled(false);
		mac_up.setEnabled(false);
		mac_down.setEnabled(false);
	}

	@Override
public void init(IWorkbench workbench){
		// super.init(workbench);
		// initControls();
	}

	public void initialize(){
		initControls();
	}

	private void initControls(){
		Preferences preferences=JSchCorePlugin.getPlugin().getPluginPreferences();
		ssh2HomeText.setText(preferences
				.getString(org.eclipse.jsch.internal.core.IConstants.KEY_SSH2HOME));
		privateKeyText.setText(preferences
				.getString(org.eclipse.jsch.internal.core.IConstants.KEY_PRIVATEKEY));
		updateControls();
	}

	@Override
public boolean performOk(){
		boolean result=super.performOk();
		storeAuthenticationMethodSettings();
		storeSSHAgentSettings();
		storeKeyExchangeMethodSettings();
		storeMACMethodSettings();

		if(result){
			setErrorMessage(null);
			String home=ssh2HomeText.getText();
			File _home=new File(home);
			if(!_home.exists()){
				if(MessageDialog.openQuestion(getShell(),
						Messages.CVSSSH2PreferencePage_question, NLS.bind(
								Messages.CVSSSH2PreferencePage_99, new String[] {home}))){
					if(!(_home.mkdirs())){
						setErrorMessage(Messages.CVSSSH2PreferencePage_100+home);
						return false;
					}
				}
			}

			Preferences preferences=JSchCorePlugin.getPlugin().getPluginPreferences();
			preferences.setValue(
					org.eclipse.jsch.internal.core.IConstants.KEY_SSH2HOME, home);
			preferences.setValue(
					org.eclipse.jsch.internal.core.IConstants.KEY_PRIVATEKEY,
					privateKeyText.getText());
		}
		JSchCorePlugin.getPlugin().setNeedToLoadKnownHosts(true);
		JSchCorePlugin.getPlugin().setNeedToLoadKeys(true);
		JSchCorePlugin.getPlugin().setIdentityRepository();
		JSchCorePlugin.getPlugin().savePluginPreferences();
		return result;
	}

	private void storeAuthenticationMethodSettings(){
		String selected = null;
		String order = null;
		for(int i = 0; i < preferedAuthMethodTable.getItemCount(); i++){
			TableItem item=preferedAuthMethodTable.getItem(i);
			if(item.getChecked()){
				if(selected==null){
					selected=item.getText();
				}
				else{
					selected+=","+item.getText(); //$NON-NLS-1$
				}
			}
			if(order == null){
				order = item.getText();
			} else {
				order += "," + item.getText(); //$NON-NLS-1$
			}
		}
		Utils.setEnabledPreferredAuthMethods(selected, order);
	}

	private void storeSSHAgentSettings(){
		String selected = ""; //$NON-NLS-1$
		for(int i = 0; i < preferedSSHAgentTable.getItemCount(); i++){
			TableItem item=preferedSSHAgentTable.getItem(i);
			if(item.getChecked()){
				if(selected.length()==0){
					selected=item.getText();
				}
				else{
					selected+=","+item.getText(); //$NON-NLS-1$
				}
			}
		}
		Utils.setSelectedSSHAgents(selected);
	}

	private void storeKeyExchangeMethodSettings(){
		String selected = null;
		String order = null;
		for(int i = 0; i < preferedKeyExchangeMethodTable.getItemCount(); i++){
			TableItem item=preferedKeyExchangeMethodTable.getItem(i);
			if(item.getChecked()){
				if(selected==null){
					selected=item.getText();
				}
				else{
					selected+=","+item.getText(); //$NON-NLS-1$
				}
			}
			if(order == null){
				order = item.getText();
			} else {
				order += "," + item.getText(); //$NON-NLS-1$
			}
		}
		Utils.setEnabledPreferredKEXMethods(selected, order);
	}

	private void storeMACMethodSettings(){
		String selected = null;
		String order = null;
		for(int i = 0; i < preferedMACMethodTable.getItemCount(); i++){
			TableItem item=preferedMACMethodTable.getItem(i);
			if(item.getChecked()){
				if(selected==null){
					selected=item.getText();
				}
				else{
					selected+=","+item.getText(); //$NON-NLS-1$
				}
			}
			if(order == null){
				order = item.getText();
			} else {
				order += "," + item.getText(); //$NON-NLS-1$
			}
		}
		Utils.setEnabledPreferredMACMethods(selected, order);
	}

	@Override
public void performApply(){
		performOk();
	}

	@Override
protected void performDefaults(){
		super.performDefaults();
		Utils.setEnabledPreferredAuthMethods(Utils.getDefaultAuthMethods(), Utils
				.getDefaultAuthMethods());
		Preferences preferences=JSchCorePlugin.getPlugin().getPluginPreferences();
		ssh2HomeText
				.setText(preferences
						.getDefaultString(org.eclipse.jsch.internal.core.IConstants.KEY_SSH2HOME));
		privateKeyText
				.setText(preferences
						.getDefaultString(org.eclipse.jsch.internal.core.IConstants.KEY_PRIVATEKEY));
		updateControls();
	}

	protected void createSpacer(Composite composite, int columnSpan){
		Label label=new Label(composite, SWT.NONE);
		GridData gd=new GridData();
		gd.horizontalSpan=columnSpan;
		label.setLayoutData(gd);
	}

	JSch getJSch(){
		return JSchCorePlugin.getPlugin().getJSch();
	}

	class PassphrasePrompt implements Runnable{
		private final String message;
		private String passphrase;

		PassphrasePrompt(String message){
			this.message=message;
		}

		@Override
		public void run(){
			Display display=Display.getCurrent();
			Shell shell=new Shell(display);
			PassphraseDialog dialog=new PassphraseDialog(shell, message);
			dialog.open();
			shell.dispose();
			passphrase=dialog.getPassphrase();
		}

		public String getPassphrase(){
			return passphrase;
		}
	}
}
