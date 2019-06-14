/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.core.IConnectionMethod;
import org.eclipse.team.internal.ccvs.core.connection.CVSRepositoryLocation;
import org.eclipse.team.internal.ui.SWTUtils;
import org.eclipse.ui.*;
import org.eclipse.ui.dialogs.PreferenceLinkArea;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;

public class ExtMethodPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	Text cvsRsh;
	Text cvsServer;	
	Text cvsRshParameters;
	private Button useExternal;
	private Button useInternal;
	private Combo methodType;
	private Control internal, external;

	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
		
		SelectionAdapter selectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateEnablements();
			}
		};
		
		// create radio buttons in a consecutive manner (see bug 282032)
		useExternal = createRadioButton(composite, CVSUIMessages.ExtMethodPreferencePage_0, 1); 
		useExternal.addSelectionListener(selectionListener);
		useInternal = createRadioButton(composite, CVSUIMessages.ExtMethodPreferencePage_1, 1); 
		useInternal.addSelectionListener(selectionListener);

		external = createExternalArea(composite);
		internal = createInternalArea(composite);
		
		PreferenceLinkArea proxyLink = SWTUtils.createPreferenceLink((IWorkbenchPreferenceContainer) getContainer(), composite, CVSUIMessages.CVSPreferencesPage_52, CVSUIMessages.CVSPreferencesPage_53);

		PreferenceLinkArea ssh2Link = SWTUtils.createPreferenceLink((IWorkbenchPreferenceContainer) getContainer(), composite, CVSUIMessages.CVSPreferencesPage_54, CVSUIMessages.CVSPreferencesPage_55);

		// lay out controls in a different order than they were created in
		FormLayout formLayout = new FormLayout();
		composite.setLayout(formLayout);

		FormData data = new FormData();
		data.top = new FormAttachment(0, SWT.DEFAULT);
		data.left = new FormAttachment(0, 0);
		data.right = new FormAttachment(100, 0);
		useExternal.setLayoutData(data);

		data = new FormData();
		data.top = new FormAttachment(useExternal, 5, SWT.DEFAULT);
		data.left = new FormAttachment(0, 0);
		data.right = new FormAttachment(100, 0);
		external.setLayoutData(data);

		data = new FormData();
		data.top = new FormAttachment(external, 5, SWT.DEFAULT);
		data.left = new FormAttachment(0, 0);
		data.right = new FormAttachment(100, 0);		
		useInternal.setLayoutData(data);

		data = new FormData();
		data.top = new FormAttachment(useInternal, 5, SWT.DEFAULT);
		data.left = new FormAttachment(0, 0);
		data.right = new FormAttachment(100, 0);		
		internal.setLayoutData(data);

		data = new FormData();
		data.top = new FormAttachment(internal, 5, SWT.DEFAULT);
		data.left = new FormAttachment(0, 0);
		data.right = new FormAttachment(100, 0);		
		proxyLink.getControl().setLayoutData(data);
		
		data = new FormData();
		data.top = new FormAttachment(proxyLink.getControl(), 5, SWT.DEFAULT);
		data.left = new FormAttachment(0, 0);
		data.right = new FormAttachment(100, 0);		
		ssh2Link.getControl().setLayoutData(data);

		initializeDefaults();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.EXT_PREFERENCE_PAGE);
		Dialog.applyDialogFont(parent);
		return composite;
	}
	
	private void updateEnablements() {
		external.setEnabled(useExternal.getSelection());
		cvsRsh.setEnabled(useExternal.getSelection());
		cvsRshParameters.setEnabled(useExternal.getSelection());
		cvsServer.setEnabled(useExternal.getSelection());
		internal.setEnabled(!useExternal.getSelection());
		methodType.setEnabled(!useExternal.getSelection());
	}

	private Control createInternalArea(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 5;
		layout.marginHeight = 5;
		layout.numColumns = 2;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Label intro = new Label(composite, SWT.LEFT | SWT.WRAP);
		intro.setText(CVSUIMessages.ExtMethodPreferencePage_2); 
		GridData data = new GridData();
		data.horizontalSpan = 2;
		data.horizontalAlignment = GridData.FILL;
		data.widthHint = 300;
		intro.setLayoutData(data);
		
		createLabel(composite, CVSUIMessages.CVSPropertiesPage_connectionType, 1); 
		methodType = createCombo(composite);
		
		return composite;
	}
	protected Control createExternalArea(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 5;
		layout.marginHeight = 5;
		layout.numColumns = 3;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Label intro = new Label(composite, SWT.LEFT | SWT.WRAP);
		intro.setText(CVSUIMessages.ExtMethodPreferencePage_message); 
		GridData data = new GridData();
		data.horizontalSpan = 3;
		data.horizontalAlignment = GridData.FILL;
		data.widthHint = 300;
		intro.setLayoutData(data);
		
		new Label(composite, SWT.LEFT).setText(CVSUIMessages.ExtMethodPreferencePage_CVS_RSH); 
		cvsRsh = new Text(composite, SWT.BORDER);
		cvsRsh.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		final Button b = new Button(composite, SWT.NONE);
		b.setText(CVSUIMessages.ExtMethodPreferencePage_Browse); 
		data = new GridData();
		data.horizontalAlignment = GridData.FILL;
		int widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
		data.widthHint = Math.max(widthHint, b.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
		b.setLayoutData(data);
		b.addListener(SWT.MouseDown, event -> {
			FileDialog d = new FileDialog(getShell());
			d.setText(CVSUIMessages.ExtMethodPreferencePage_Details);
			String file = d.open();
			if (file != null) {
				setCvsRshText(file);
			}
		});
		
		new Label(composite, SWT.LEFT).setText(CVSUIMessages.ExtMethodPreferencePage_CVS_RSH_Parameters); 
		cvsRshParameters = new Text(composite, SWT.BORDER);
		data = new GridData();
		data.horizontalSpan = 2;
		data.horizontalAlignment = GridData.FILL;
		cvsRshParameters.setLayoutData(data);

		new Label(composite, SWT.LEFT).setText(CVSUIMessages.ExtMethodPreferencePage_CVS_SERVER__7); 
		cvsServer = new Text(composite, SWT.BORDER);
		data = new GridData();
		data.horizontalSpan = 2;
		data.horizontalAlignment = GridData.FILL;
		cvsServer.setLayoutData(data);
		
		PlatformUI.getWorkbench().getHelpSystem().setHelp(cvsRsh, IHelpContextIds.EXT_PREFERENCE_RSH);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(cvsRshParameters, IHelpContextIds.EXT_PREFERENCE_PARAM);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(cvsServer, IHelpContextIds.EXT_PREFERENCE_SERVER);
		return composite;
	}
	
	private void initializeDefaults() {
		IPreferenceStore store = getPreferenceStore();
		String rsh = store.getString(ICVSUIConstants.PREF_CVS_RSH);
		String parameter = store.getString(ICVSUIConstants.PREF_CVS_RSH_PARAMETERS);
		String server = store.getString(ICVSUIConstants.PREF_CVS_SERVER);
		String method = store.getString(ICVSUIConstants.PREF_EXT_CONNECTION_METHOD_PROXY);
		initializeDefaults(rsh, parameter, server, method);
	}
	
	/*
	 * Set CVS_RSH program
	 */
	protected void setCvsRshText(String s) {
		cvsRsh.setText(s);
	}
	
	@Override
	public void init(IWorkbench workbench) {
	}
	
	@Override
	public boolean performOk() {
		IPreferenceStore store = getPreferenceStore();
		String method;
		if (useExternal.getSelection()) {
			method = "ext"; //$NON-NLS-1$
			store.setValue(ICVSUIConstants.PREF_CVS_RSH, cvsRsh.getText());
			store.setValue(ICVSUIConstants.PREF_CVS_RSH_PARAMETERS, cvsRshParameters.getText());
			store.setValue(ICVSUIConstants.PREF_CVS_SERVER, cvsServer.getText());
			CVSProviderPlugin.getPlugin().setCvsRshCommand(cvsRsh.getText());
			CVSProviderPlugin.getPlugin().setCvsRshParameters(cvsRshParameters.getText());
			CVSProviderPlugin.getPlugin().setCvsServer(cvsServer.getText());
		} else {
			method = methodType.getText();
		}
		store.setValue(ICVSUIConstants.PREF_EXT_CONNECTION_METHOD_PROXY, method);
		CVSRepositoryLocation.setExtConnectionMethodProxy(method);
		CVSUIPlugin.getPlugin().savePluginPreferences();
		return super.performOk();
	}
	
	@Override
	protected void performDefaults() {
		IPreferenceStore store = getPreferenceStore();
		String rsh = store.getDefaultString(ICVSUIConstants.PREF_CVS_RSH);
		String parameter = store.getDefaultString(ICVSUIConstants.PREF_CVS_RSH_PARAMETERS);
		String server = store.getDefaultString(ICVSUIConstants.PREF_CVS_SERVER);
		String method = store.getDefaultString(ICVSUIConstants.PREF_EXT_CONNECTION_METHOD_PROXY);
		initializeDefaults(rsh, parameter, server, method);
		super.performDefaults();
	}
	
	private void initializeDefaults(String rsh, String parameters, String server, String method) {
		cvsRsh.setText(rsh);
		cvsRshParameters.setText(parameters);
		cvsServer.setText(server);
		IConnectionMethod[] methods = CVSRepositoryLocation.getPluggedInConnectionMethods();
		for (IConnectionMethod m : methods) {
			String name = m.getName();
			if (!name.equals("ext")) { //$NON-NLS-1$
				methodType.add(name);
			}
		}
		if (method.equals("ext")) { //$NON-NLS-1$
			methodType.select(0);
		} else {
			methodType.select(methodType.indexOf(method));
		}
		useExternal.setSelection(method.equals("ext")); //$NON-NLS-1$
		useInternal.setSelection(!method.equals("ext")); //$NON-NLS-1$
		updateEnablements();
	}
	
	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return CVSUIPlugin.getPlugin().getPreferenceStore();
	}
	
	protected Button createRadioButton(Composite parent, String label, int span) {
		Button button = new Button(parent, SWT.RADIO);
		button.setText(label);
		GridData data = new GridData();
		data.horizontalSpan = span;
		button.setLayoutData(data);
		return button;
	}
	
	protected Label createLabel(Composite parent, String text, int span) {
		Label label = new Label(parent, SWT.LEFT);
		label.setText(text);
		GridData data = new GridData();
		data.horizontalSpan = span;
		data.horizontalAlignment = GridData.FILL;
		label.setLayoutData(data);
		return label;
	}
	protected Combo createCombo(Composite parent) {
		Combo combo = new Combo(parent, SWT.READ_ONLY);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		data.horizontalSpan = 1;
		combo.setLayoutData(data);
		return combo;
	}
}
