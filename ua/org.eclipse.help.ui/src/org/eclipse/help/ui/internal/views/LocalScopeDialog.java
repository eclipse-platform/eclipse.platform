/*******************************************************************************
 * Copyright (c) 2010, 2016 IBM Corporation and others.
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

package org.eclipse.help.ui.internal.views;

import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;


public class LocalScopeDialog extends TrayDialog {

	private static final String ORG_ECLIPSE_HELP_UI_LOCAL_SEARCH = "org.eclipse.help.ui.localSearch"; //$NON-NLS-1$
	private EngineDescriptorManager descManager;
	private IPreferenceStore preferenceStore;
	private PreferencePage localHelpPage;
	private ScopeSet scopeSet;

	public LocalScopeDialog(Shell parentShell, PreferenceManager manager, EngineDescriptorManager descManager,
			ScopeSet set) {
		super(parentShell);
		this.descManager = descManager;
		this.preferenceStore = set.getPreferenceStore();
		this.scopeSet = set;
	}

	@Override
	protected boolean isResizable() {
		return true;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		EngineDescriptor localSearchDesc = null;
		for (int i = 0; localSearchDesc == null && i < descManager.getDescriptors().length; i++) {
			if (ORG_ECLIPSE_HELP_UI_LOCAL_SEARCH.equals(descManager.getDescriptors()[i].getEngineTypeId())) {
				localSearchDesc = descManager.getDescriptors()[i];
			}
		}
		String id = localSearchDesc.getEngineTypeId();
		localHelpPage = localSearchDesc.createRootPage(scopeSet.getName());
		localHelpPage.setTitle(localSearchDesc.getLabel() + '_' + id);
		localHelpPage.setImageDescriptor(localSearchDesc.getImageDescriptor());
		localHelpPage.setDescription(localSearchDesc.getDescription());
		localHelpPage.setPreferenceStore(preferenceStore);
		localHelpPage.createControl(parent);
		Control helpPageControl = localHelpPage.getControl();
		GridData layoutData = new GridData(GridData.FILL_BOTH);
		helpPageControl.setLayoutData(layoutData);
		return helpPageControl;
	}

	@Override
	protected void okPressed() {
		localHelpPage.performOk();
		super.okPressed();
	}

}
