/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
 *     George Suaridze <suag@1c.ru> (1C-Soft LLC) - Bug 560168
 *******************************************************************************/
package org.eclipse.help.ui.internal.preferences;

import java.util.Iterator;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.help.internal.base.HelpBasePlugin;
import org.eclipse.help.internal.base.IHelpBaseConstants;
import org.eclipse.help.internal.browser.BrowserManager;
import org.eclipse.help.ui.internal.IHelpUIConstants;
import org.eclipse.help.ui.internal.Messages;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.IPreferenceNode;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferenceLinkArea;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Preference page for selecting default web browser.
 */
public class HelpPreferencePage extends PreferencePage implements
		IWorkbenchPreferencePage {

	private static final String WBROWSER_PAGE_ID = "org.eclipse.ui.browser.preferencePage";//$NON-NLS-1$

	private Combo useExternalCombo;

	private Combo searchLocationCombo;

	private Combo openModeCombo;

	private Combo dialogHelpCombo;

	private Combo windowHelpCombo;

	/**
	 * Creates preference page controls on demand.
	 *
	 * @param parent
	 *            the parent for the preference page
	 */
	@Override
	protected Control createContents(Composite parent) {
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent,
				IHelpUIConstants.PREF_PAGE_HELP);
		Composite mainComposite = new Composite(parent, SWT.NONE);
		GridData data = new GridData();
		data.verticalAlignment = GridData.FILL;
		data.horizontalAlignment = GridData.FILL;
		mainComposite.setLayoutData(data);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		mainComposite.setLayout(layout);
		Label description = new Label(mainComposite, SWT.NONE);
		description.setText(Messages.select_browser);

		createOpenModesPrefs(mainComposite);
		createDynamicHelpArea(mainComposite);
		createSpacer(mainComposite);
		Dialog.applyDialogFont(mainComposite);
		return mainComposite;
	}

	private void createOpenModesPrefs(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		GridLayout layout = new GridLayout();		group.setLayout(layout);
		group.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		group.setText(Messages.HelpPreferencePage_openModeGroup);
		createSearchLocation(group);
		createHelpViewOpenPrefs(group);
		createOpenContents(group);
		createLinkArea(group);
	}

	private void createDynamicHelpArea(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		GridLayout layout = new GridLayout();		group.setLayout(layout);
		group.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		group.setText(Messages.HelpPreferencePage_contextHelpGroup);

		createWindowContextPrefs(group);
		createDialogContextPrefs(group);
	}

	private void createSearchLocation(Composite mainComposite) {
		Label searchLocationLabel = new Label(mainComposite, SWT.NONE);
		searchLocationLabel.setText(Messages.HelpPreferencePage_searchLocation);
		searchLocationLabel.setLayoutData(createLabelData());
		searchLocationCombo = new Combo(mainComposite, SWT.READ_ONLY);
		searchLocationCombo.add(Messages.HelpPreferencePage_view);
		searchLocationCombo.add(Messages.HelpPreferencePage_openInBrowser);
		searchLocationCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		boolean searchFromBrowser = Platform.getPreferencesService().getBoolean
			(HelpBasePlugin.PLUGIN_ID, IHelpBaseConstants.P_KEY_SEARCH_FROM_BROWSER, false, null);
		searchLocationCombo.setText(searchFromBrowser ? Messages.HelpPreferencePage_openInBrowser : Messages.HelpPreferencePage_view);
	}

	private void createOpenContents(Composite mainComposite) {
		if (BrowserManager.getInstance().isEmbeddedBrowserPresent()) {
			Label isExternalLabel = new Label(mainComposite, SWT.NONE);
			isExternalLabel.setText(Messages.HelpPreferencePage_openContents);
			isExternalLabel.setLayoutData(createLabelData());
			useExternalCombo = new Combo(mainComposite, SWT.READ_ONLY);
			useExternalCombo.add(Messages.HelpPreferencePage_helpBrowser);
			useExternalCombo.add(Messages.HelpPreferencePage_externalBrowser);
			useExternalCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			boolean useExternal = Platform.getPreferencesService().getBoolean
				(HelpBasePlugin.PLUGIN_ID, IHelpBaseConstants.P_KEY_ALWAYS_EXTERNAL_BROWSER, false, null);
			useExternalCombo.setText(useExternal ? Messages.HelpPreferencePage_externalBrowser : Messages.HelpPreferencePage_helpBrowser);
		}
	}

	private void createLinkArea(Composite parent) {
		IPreferenceNode node = getPreferenceNode(WBROWSER_PAGE_ID);
		if (node != null) {
			PreferenceLinkArea linkArea = new PreferenceLinkArea(parent,
					SWT.WRAP, WBROWSER_PAGE_ID,
					Messages.HelpPreferencePage_message,
					(IWorkbenchPreferenceContainer) getContainer(), null);
			GridData data = new GridData(GridData.FILL_HORIZONTAL
					| GridData.GRAB_HORIZONTAL);
			linkArea.getControl().setLayoutData(data);
		}
	}


	private void createHelpViewOpenPrefs(Group group) {
		if (PlatformUI.getWorkbench().getBrowserSupport()
				.isInternalWebBrowserAvailable()) {
			Label ohelpDescription = new Label(group, SWT.NONE);
			ohelpDescription.setText(Messages.HelpPreferencePage_olabel);
			ohelpDescription.setLayoutData(createLabelData());
			openModeCombo = new Combo(group, SWT.READ_ONLY);
			openModeCombo.add(Messages.HelpPreferencePage_openInPlace);
			openModeCombo.add(Messages.HelpPreferencePage_openInEditor);
			openModeCombo.add(Messages.HelpPreferencePage_openInBrowser);
			openModeCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			String openMode = Platform.getPreferencesService().getString
			     (HelpBasePlugin.PLUGIN_ID, IHelpBaseConstants.P_KEY_HELP_VIEW_OPEN_MODE,
			      IHelpBaseConstants.P_IN_PLACE, null);
			openModeCombo.setText(openModeToString(openMode));
		}
	}

	private void createDialogContextPrefs(Group group) {
		Label dhelpDescription = new Label(group, SWT.NONE);
		dhelpDescription.setText(Messages.HelpPreferencePage_dlabel);
		dhelpDescription.setLayoutData(createLabelData());
		dialogHelpCombo = new Combo(group, SWT.READ_ONLY);
		dialogHelpCombo.add(Messages.HelpPreferencePage_tray);
		dialogHelpCombo.add(Messages.HelpPreferencePage_infopop);
		dialogHelpCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		boolean dinfopop = Platform.getPreferencesService().getBoolean
			(HelpBasePlugin.PLUGIN_ID, IHelpBaseConstants.P_KEY_DIALOG_INFOPOP, false, null);
		dialogHelpCombo.setText(dinfopop ? Messages.HelpPreferencePage_infopop : Messages.HelpPreferencePage_tray);
	}

	private void createWindowContextPrefs(Group group) {
		Label whelpDescription = new Label(group, SWT.NONE);
		whelpDescription.setText(Messages.HelpPreferencePage_wlabel);
		whelpDescription.setLayoutData(createLabelData());

		windowHelpCombo = new Combo(group, SWT.READ_ONLY);
		windowHelpCombo.add(Messages.HelpPreferencePage_view);
		windowHelpCombo.add(Messages.HelpPreferencePage_infopop);
		windowHelpCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		boolean winfopop = Platform.getPreferencesService().getBoolean
			(HelpBasePlugin.PLUGIN_ID, IHelpBaseConstants.P_KEY_WINDOW_INFOPOP, false, null);
		windowHelpCombo.setText(winfopop ? Messages.HelpPreferencePage_infopop : Messages.HelpPreferencePage_view);
	}

	private GridData createLabelData () {
		GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		data.verticalIndent = 5;
		return data;
	}

	private IPreferenceNode getPreferenceNode(String pageId) {
		Iterator<IPreferenceNode> iterator = PlatformUI.getWorkbench().getPreferenceManager()
				.getElements(PreferenceManager.PRE_ORDER).iterator();
		while (iterator.hasNext()) {
			IPreferenceNode next = iterator.next();
			if (next.getId().equals(pageId))
				return next;
		}
		return null;
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	/**
	 * Performs special processing when this page's Defaults button has been
	 * pressed.
	 * <p>
	 * This is a framework hook method for sublcasses to do special things when
	 * the Defaults button has been pressed. Subclasses may override, but should
	 * call <code>super.performDefaults</code>.
	 * </p>
	 */
	@Override
	protected void performDefaults() {
		IEclipsePreferences defaults = DefaultScope.INSTANCE.getNode(HelpBasePlugin.PLUGIN_ID);
		if (useExternalCombo != null) {
			boolean useExternal = defaults.getBoolean(
							IHelpBaseConstants.P_KEY_ALWAYS_EXTERNAL_BROWSER, false);
			useExternalCombo.setText(useExternal ? Messages.HelpPreferencePage_externalBrowser : Messages.HelpPreferencePage_helpBrowser);
		}

		boolean searchFromBrowser = defaults.getBoolean(IHelpBaseConstants.P_KEY_SEARCH_FROM_BROWSER, false);
		searchLocationCombo.setText(searchFromBrowser ? Messages.HelpPreferencePage_openInBrowser : Messages.HelpPreferencePage_view);

		boolean winfopop = defaults.getBoolean(IHelpBaseConstants.P_KEY_WINDOW_INFOPOP, false);
		windowHelpCombo.setText(winfopop ? Messages.HelpPreferencePage_infopop : Messages.HelpPreferencePage_view);

		boolean dinfopop = defaults.getBoolean(IHelpBaseConstants.P_KEY_DIALOG_INFOPOP, false);
		dialogHelpCombo.setText(dinfopop ? Messages.HelpPreferencePage_infopop : Messages.HelpPreferencePage_tray);

		if (openModeCombo !=null) {
			String openMode = defaults.get(
						IHelpBaseConstants.P_KEY_HELP_VIEW_OPEN_MODE, IHelpBaseConstants.P_IN_PLACE);
			openModeCombo.setText(openModeToString(openMode));
		}

		super.performDefaults();
	}

	@Override
	public boolean performOk() {
		IEclipsePreferences pref = InstanceScope.INSTANCE.getNode(HelpBasePlugin.PLUGIN_ID);
		if (useExternalCombo != null) {
			boolean isExternalBrowser = useExternalCombo.getText().equals(Messages.HelpPreferencePage_externalBrowser);
			pref.putBoolean(IHelpBaseConstants.P_KEY_ALWAYS_EXTERNAL_BROWSER,
					isExternalBrowser);
			BrowserManager.getInstance().setAlwaysUseExternal(
					isExternalBrowser);
		}
		pref.putBoolean(IHelpBaseConstants.P_KEY_SEARCH_FROM_BROWSER,
				searchLocationCombo.getText().equals(Messages.HelpPreferencePage_openInBrowser));

		pref.putBoolean(IHelpBaseConstants.P_KEY_WINDOW_INFOPOP,
				windowHelpCombo.getText().equals(Messages.HelpPreferencePage_infopop));

		pref.putBoolean(IHelpBaseConstants.P_KEY_DIALOG_INFOPOP,
				dialogHelpCombo.getText().equals(Messages.HelpPreferencePage_infopop));
		if (openModeCombo!=null) {
			pref.put(IHelpBaseConstants.P_KEY_HELP_VIEW_OPEN_MODE, openModeFromString(openModeCombo.getText()));
		}

		try {
			pref.flush();
		} catch (BackingStoreException e) {
			ILog.of(getClass()).error("Failed to save preferences", e); //$NON-NLS-1$
		}

		return true;
	}

	private String openModeToString(String openMode) {
		if (IHelpBaseConstants.P_IN_BROWSER.equals(openMode)) {
			return Messages.HelpPreferencePage_openInBrowser;
		} else if (IHelpBaseConstants.P_IN_EDITOR.equals(openMode)) {
			return Messages.HelpPreferencePage_openInEditor;
		} else {
			return Messages.HelpPreferencePage_openInPlace;
		}
	}

	private String openModeFromString(String openMode) {
		if (Messages.HelpPreferencePage_openInBrowser.equals(openMode)) {
			return IHelpBaseConstants.P_IN_BROWSER;
		} else if (Messages.HelpPreferencePage_openInEditor.equals(openMode)) {
			return IHelpBaseConstants.P_IN_EDITOR;
		} else {
			return IHelpBaseConstants.P_IN_PLACE;
		}
	}

	/**
	 * Creates a horizontal spacer line that fills the width of its container.
	 *
	 * @param parent
	 *            the parent control
	 */
	private void createSpacer(Composite parent) {
		Label spacer = new Label(parent, SWT.NONE);
		GridData data = new GridData();
		data.horizontalAlignment = GridData.FILL;
		data.verticalAlignment = GridData.BEGINNING;
		spacer.setLayoutData(data);
	}
}
