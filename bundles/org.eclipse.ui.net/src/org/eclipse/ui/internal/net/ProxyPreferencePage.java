/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 * yyyymmdd bug      Email and other contact information
 * -------- -------- -----------------------------------------------------------
 * 20070123   154100 eu@md.pp.ru - Eugene Kuleshov, Initial UI coding
 * 20070201   154100 pmoogk@ca.ibm.com - Peter Moogk, Port internet code from WTP to Eclipse base.
 * 20070219   174674 pmoogk@ca.ibm.com - Peter Moogk
 * 20070402   180622 brockj@tpg.com.au - Brock Janiczak, Inconsistent enablement states in network preference page
 * 20070416   177897 ymnk@jcraft.com - Atsuhiko Yamanaka, Improve UI of Proxy Preferences Page
 * 20070801   197977 holger.oehm@sap.com - Holger Oehm, [Proxy] non-proxy hosts not correctly updated
 *******************************************************************************/
package org.eclipse.ui.internal.net;

import org.eclipse.core.internal.net.ProxySelector;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

public class ProxyPreferencePage extends PreferencePage implements
		IWorkbenchPreferencePage {

	private static final String PROXY_PREFERENCE_PAGE_CONTEXT_ID = "org.eclipse.ui.net.proxy_preference_page_context"; //$NON-NLS-1$

	private Label providerLabel;
	protected Combo providerCombo;
	private ProxyEntriesComposite proxyEntriesComposite;
	private NonProxyHostsComposite nonProxyHostsComposite;

	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		composite.setLayout(layout);

		createProviderComposite(composite);
		createProxyEntriesComposite(composite);
		createNonProxiedHostsComposite(composite);

		// Adding help accessible by F1
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(),
				PROXY_PREFERENCE_PAGE_CONTEXT_ID);

		applyDialogFont(composite);
		initializeValues();

		return composite;
	}

	private void createProviderComposite(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		providerLabel = new Label(composite, SWT.NONE);
		providerLabel.setText(NetUIMessages.ProxyPreferencePage_0);
		providerCombo = new Combo(composite, SWT.READ_ONLY | SWT.DROP_DOWN);
		providerCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				setProvider(ProxySelector.unlocalizeProvider(providerCombo.getText()));
			}
		});
	}

	private void createProxyEntriesComposite(Composite parent) {
		proxyEntriesComposite = new ProxyEntriesComposite(parent, SWT.NONE);
		proxyEntriesComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL,
				true, true));
	}

	private void createNonProxiedHostsComposite(Composite parent) {
		nonProxyHostsComposite = new NonProxyHostsComposite(parent, SWT.NONE);
		nonProxyHostsComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL,
				true, true));
	}

	public void init(IWorkbench workbench) {
		// Nothing to do
	}

	protected void performApply() {
		refresh();
		int sel = providerCombo.getSelectionIndex();
		proxyEntriesComposite.performApply();
		nonProxyHostsComposite.performApply();
		ProxySelector.setActiveProvider(ProxySelector
				.unlocalizeProvider(providerCombo.getItem(sel)));
	}

	protected void performDefaults() {
		int index = 1;
		if (providerCombo.getItemCount() == 3) {
			index = 2;
		}
		providerCombo.select(index);
		setProvider(ProxySelector.unlocalizeProvider(providerCombo.getItem(index)));
	}

	public boolean performOk() {
		performApply();
		return super.performOk();
	}

	private void initializeValues() {
		String[] providers = ProxySelector.getProviders();
		String[] localizedProviders = new String[providers.length];
		for (int i = 0; i < localizedProviders.length; i++) {
			localizedProviders[i] = ProxySelector.localizeProvider(providers[i]);
		}
		providerCombo.setItems(localizedProviders);
		providerCombo.select(providerCombo.indexOf(ProxySelector
				.localizeProvider(ProxySelector.getDefaultProvider())));
	}

	protected void setProvider(String name) {
		proxyEntriesComposite.setProvider(name);
		nonProxyHostsComposite.setProvider(name);
		refresh();
	}

	private void refresh() {
		proxyEntriesComposite.refresh();
		nonProxyHostsComposite.refresh();
	}

}
