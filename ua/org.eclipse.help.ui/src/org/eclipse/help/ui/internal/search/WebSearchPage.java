/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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

package org.eclipse.help.ui.internal.search;

import org.eclipse.help.ui.RootScopePage;
import org.eclipse.help.ui.internal.Messages;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * Web serch participant in the federated search.
 */
public class WebSearchPage extends RootScopePage {
	private Text urlText;

	/**
	 * Default constructor.
	 */
	public WebSearchPage() {
	}

	@Override
	protected int createScopeContents(Composite parent) {
		//Font font = parent.getFont();
		initializeDialogUnits(parent);

		Label label = new Label(parent, SWT.NULL);
		label.setText(Messages.WebSearchPage_label);
		GridData gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
		label.setLayoutData(gd);
		urlText = new Text(parent, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL
				| SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.heightHint = 64;
		gd.widthHint = 200;
		urlText.setLayoutData(gd);
		urlText.addModifyListener(e -> validate());
		urlText.setEditable(getEngineDescriptor().isUserDefined());
		new Label(parent, SWT.NULL);
		label = new Label(parent, SWT.WRAP);
		label.setText(Messages.WebSearchPage_info);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 100;
		label.setLayoutData(gd);
		applyDialogFont(parent);
		updateControls();
		return 2;
	}

	@Override
	protected void initializeDefaults(IPreferenceStore store) {
		super.initializeDefaults(store);
		String template = (String) getEngineDescriptor().getParameters().get(
				WebSearchScopeFactory.P_URL);
		if (template != null)
			store
					.setDefault(getStoreKey(WebSearchScopeFactory.P_URL),
							template);
	}

	@Override
	protected void performDefaults() {
		getPreferenceStore().setToDefault(
				getStoreKey(WebSearchScopeFactory.P_URL));
		updateControls();
		super.performDefaults();
	}

	private void updateControls() {
		String template = getPreferenceStore().getString(
				getStoreKey(WebSearchScopeFactory.P_URL));
		urlText.setText(template != null ? template : "http://"); //$NON-NLS-1$
		validate();
	}

	private void validate() {
		String text = urlText.getText();
		setValid(text.length() > 0);
	}

	@Override
	public boolean performOk() {
		String urlTemplate = urlText.getText();
		getPreferenceStore().setValue(getStoreKey(WebSearchScopeFactory.P_URL),
				urlTemplate);
		return super.performOk();
	}

	private String getStoreKey(String key) {
		return getEngineDescriptor().getId() + "." + key; //$NON-NLS-1$
	}
}
