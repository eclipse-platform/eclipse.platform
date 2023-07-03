/*******************************************************************************
 * Copyright (c) 2006, 2019 IBM Corporation and others.
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
package org.eclipse.help.ui.internal.preferences;

import java.util.List;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.help.internal.base.HelpBasePlugin;
import org.eclipse.help.internal.base.IHelpBaseConstants;
import org.eclipse.help.internal.base.remote.DefaultPreferenceFileHandler;
import org.eclipse.help.internal.base.remote.RemoteHelp;
import org.eclipse.help.ui.internal.IHelpUIConstants;
import org.eclipse.help.ui.internal.Messages;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

/**
 * Preference page to set remote infocenters
 */
public class HelpContentPreferencePage extends PreferencePage implements
		IWorkbenchPreferencePage {

	private ICTable table;
	private ICButtons buttons;

	private Button searchLocalHelpOnly;
	private Button searchLocalHelpFirst;
	private Button searchLocalHelpLast;
	private Label descLabel;

	/**
	 * Creates the preference page
	 */
	public HelpContentPreferencePage() {
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	public ICTable getTable()
	{
		return table;
	}

	@Override
	protected Control createContents(Composite parent) {
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent,
				IHelpUIConstants.PREF_PAGE_HELP_CONTENT);

		initializeDialogUnits(parent);

		descLabel = new Label(parent, SWT.NONE);
		descLabel.setText(Messages.HelpContentPage_title);
		Dialog.applyDialogFont(descLabel);

		createSearchLocalHelpOnly(parent);
		createSearchLocalHelpFirst(parent);
		createSearchLocalHelpLast(parent);

/*		remoteICPage = new InfocenterDisplay(this);
		remoteICPage.createContents(parent);
*/
		initializeTableEnablement(parent,searchLocalHelpOnly.getSelection());

		return parent;
	}

	@Override
	protected void performDefaults() {
		super.performDefaults();

		List<IC> ics = ICPreferences.getDefaultICs();
		table.setICs(ics);

		// Restore Defaults functionality here
/*		HelpContentBlock currentBlock=remoteICPage.getHelpContentBlock();
		currentBlock.getRemoteICviewer().getRemoteICList().removeAllRemoteICs(currentBlock.getRemoteICList());
		currentBlock.getRemoteICviewer().getRemoteICList().loadDefaultPreferences();
		currentBlock.restoreDefaultButtons();
*/
		boolean remoteHelpOn = new DefaultPreferenceFileHandler().isRemoteHelpOn();
		boolean remoteHelpPreferred = new DefaultPreferenceFileHandler().isRemoteHelpPreferred();
		searchLocalHelpOnly.setSelection(!remoteHelpOn);
		searchLocalHelpFirst.setSelection(remoteHelpOn && !remoteHelpPreferred);
		searchLocalHelpLast.setSelection(remoteHelpOn && remoteHelpPreferred);
		changeListener.handleEvent(null);
	}

	@Override
	public boolean performOk() {

		IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(HelpBasePlugin.PLUGIN_ID);
		prefs.putBoolean(IHelpBaseConstants.P_KEY_REMOTE_HELP_ON, !(searchLocalHelpOnly.getSelection()));
		prefs.putBoolean(IHelpBaseConstants.P_KEY_REMOTE_HELP_PREFERRED, searchLocalHelpLast.getSelection());


		List<IC> ics = table.getICs();
		ICPreferences.setICs(ics);

		RemoteHelp.notifyPreferenceChange();
		return super.performOk();
	}

	@Override
	protected GridData setButtonLayoutData(Button button) {
		return super.setButtonLayoutData(button);
	}

	private void createSearchLocalHelpOnly(Composite parent) {
		searchLocalHelpOnly = new Button(parent, SWT.RADIO);
		searchLocalHelpOnly.setText(Messages.SearchEmbeddedHelpOnly);
		searchLocalHelpOnly.addListener(SWT.Selection, changeListener);

		boolean isRemoteOn = Platform.getPreferencesService().getBoolean
			(HelpBasePlugin.PLUGIN_ID, IHelpBaseConstants.P_KEY_REMOTE_HELP_ON, false, null);

		searchLocalHelpOnly.setSelection(!isRemoteOn);
		Dialog.applyDialogFont(searchLocalHelpOnly);
	}

	private void createSearchLocalHelpFirst(Composite parent) {
		searchLocalHelpFirst = new Button(parent, SWT.RADIO);
		searchLocalHelpFirst.setText(Messages.SearchEmbeddedHelpFirst);
		searchLocalHelpFirst.addListener(SWT.Selection, changeListener);

		boolean isRemoteOn = Platform.getPreferencesService().getBoolean
			(HelpBasePlugin.PLUGIN_ID, IHelpBaseConstants.P_KEY_REMOTE_HELP_ON, false, null);
		boolean isRemotePreferred = Platform.getPreferencesService().getBoolean
			(HelpBasePlugin.PLUGIN_ID, IHelpBaseConstants.P_KEY_REMOTE_HELP_PREFERRED, false, null);

		searchLocalHelpFirst.setSelection(isRemoteOn && !isRemotePreferred);
		Dialog.applyDialogFont(searchLocalHelpFirst);
	}

	private void createSearchLocalHelpLast(Composite parent) {
		searchLocalHelpLast = new Button(parent, SWT.RADIO);
		searchLocalHelpLast.setText(Messages.SearchEmbeddedHelpLast);
		searchLocalHelpLast.addListener(SWT.Selection, changeListener);

		boolean isRemoteOn = Platform.getPreferencesService().getBoolean
			(HelpBasePlugin.PLUGIN_ID, IHelpBaseConstants.P_KEY_REMOTE_HELP_ON, false, null);
		boolean isRemotePreferred = Platform.getPreferencesService().getBoolean
			(HelpBasePlugin.PLUGIN_ID, IHelpBaseConstants.P_KEY_REMOTE_HELP_PREFERRED, false, null);

		searchLocalHelpLast.setSelection(isRemoteOn && isRemotePreferred);
		Dialog.applyDialogFont(searchLocalHelpLast);
	}

	/*
	 * Initialize the table enablement with the current checkbox selection
	 */

	private void initializeTableEnablement(Composite parent, boolean isRemoteHelpDisabled)
	{
		Composite top = new Composite(parent, SWT.NONE);

		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 2;
		layout.marginWidth = 2;
		top.setLayout(layout);

		top.setLayoutData(new GridData(GridData.FILL_BOTH));

		table = new ICTable(top);
		buttons = new ICButtons(top,this);

		changeListener.handleEvent(null);
	}

	/*
	 * Listens for any change in the UI and checks for valid input and correct
	 * enablement.
	 */
	private Listener changeListener = event -> {

		boolean isRemoteHelpEnabled = !(searchLocalHelpOnly.getSelection());

		// Disable/Enable table
		table.getTable().setEnabled(isRemoteHelpEnabled);
		buttons.setEnabled(isRemoteHelpEnabled);
	};

}
