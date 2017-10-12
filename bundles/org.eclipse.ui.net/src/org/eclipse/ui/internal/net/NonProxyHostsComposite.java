/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 * yyyymmdd bug      Email and other contact information
 * -------- -------- -----------------------------------------------------------
 * 20060217   127138 pmoogk@ca.ibm.com - Peter Moogk
 * 20070201   154100 pmoogk@ca.ibm.com - Peter Moogk, Port internet code from WTP to Eclipse base.
 *******************************************************************************/
package org.eclipse.ui.internal.net;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.internal.net.ProxySelector;
import org.eclipse.core.internal.net.StringUtil;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;

/**
 * This class is the Composite that consists of controls for proxy bypass hosts
 * and is used by ProxyPreferencesPage.
 */
public class NonProxyHostsComposite extends Composite {

	private Label hostsLabel;
	CheckboxTableViewer hostsViewer;
	private Button addButton;
	private Button editButton;
	private Button removeButton;

	protected String currentProvider;
	private ArrayList bypassHosts = new ArrayList();

	NonProxyHostsComposite(Composite parent, int style) {
		super(parent, style);
		createWidgets();
	}

	protected void createWidgets() {
		setLayout(new GridLayout(2, false));

		hostsLabel = new Label(this, SWT.NONE);
		hostsLabel.setText(NetUIMessages.ProxyPreferencePage_12);
		hostsLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false,
				2, 1));

		Table hostsTable = new Table(this, SWT.BORDER | SWT.V_SCROLL
				| SWT.H_SCROLL | SWT.MULTI | SWT.FULL_SELECTION | SWT.CHECK);
		hostsTable.setHeaderVisible(true);
		hostsTable.setLinesVisible(true);
		hostsTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true,
				1, 3));

		hostsViewer = new CheckboxTableViewer(hostsTable);
		NonProxyHostsLabelProvider labelProvider = new NonProxyHostsLabelProvider();
		NonProxyHostsContentProvider contentProvider = new NonProxyHostsContentProvider();
		labelProvider.createColumns(hostsViewer);
		hostsViewer.setContentProvider(contentProvider);
		hostsViewer.setLabelProvider(labelProvider);

		TableLayout tableLayout = new TableLayout();
		tableLayout.addColumnData(new ColumnPixelData(24));
		tableLayout.addColumnData(new ColumnWeightData(50, 50, true));
		tableLayout.addColumnData(new ColumnWeightData(50, 50, true));
		hostsTable.setLayout(tableLayout);

		addButton = createButton(NetUIMessages.ProxyPreferencePage_15);
		editButton = createButton(NetUIMessages.ProxyPreferencePage_16);
		removeButton = createButton(NetUIMessages.ProxyPreferencePage_17);

		hostsViewer
				.addSelectionChangedListener(new ISelectionChangedListener() {
					public void selectionChanged(SelectionChangedEvent event) {
						enableButtons();
					}
				});
		hostsViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				setProvider(currentProvider);
			}
		});
		hostsViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				editSelection();
			}
		});
		addButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				addHost();
			}
		});
		editButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				editSelection();
			}
		});
		removeButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				removeSelection();
			}
		});

		initializeValues();
		enableButtons();
	}

	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		hostsViewer.getTable().setEnabled(enabled);
		enableButtons();
	}

	protected void enableButtons() {
		boolean enabled = getEnabled();
		if (enabled) {
			boolean editable = isSelectionEditable();
			addButton.setEnabled(true);
			editButton.setEnabled(editable);
			removeButton.setEnabled(editable);
		} else {
			addButton.setEnabled(false);
			editButton.setEnabled(false);
			removeButton.setEnabled(false);
		}
	}

	private boolean isSelectionEditable() {
		IStructuredSelection selection = (IStructuredSelection) hostsViewer
				.getSelection();
		Iterator iterator = selection.iterator();
		boolean editable = iterator.hasNext();
		while (iterator.hasNext()) {
			String provider = ((ProxyBypassData) iterator.next()).getSource();
			if (!ProxySelector.canSetBypassHosts(provider)) {
				editable = false;
			}
		}
		return editable;
	}

	protected void addHost() {
		String hosts[] = promptForHost(null);
		if (hosts != null) {
			for (int i = 0; i < hosts.length; i++) {
				bypassHosts.add(0, new ProxyBypassData(hosts[i],
						getEditableProvider()));
			}
			hostsViewer.refresh();
			setProvider(currentProvider);
		}
	}

	private String getEditableProvider() {
		String providers[] = ProxySelector.getProviders();
		for (int i = 0; i < providers.length; i++) {
			if (ProxySelector.canSetBypassHosts(providers[i])) {
				return providers[i];
			}
		}
		return null;
	}

	protected void removeSelection() {
		IStructuredSelection selection = (IStructuredSelection) hostsViewer
				.getSelection();
		Iterator it = selection.iterator();
		while (it.hasNext()) {
			ProxyBypassData data = (ProxyBypassData) it.next();
			bypassHosts.remove(data);
		}
		hostsViewer.refresh();
	}

	protected void editSelection() {
		if (!isSelectionEditable()) {
			return;
		}
		IStructuredSelection selection = (IStructuredSelection) hostsViewer
				.getSelection();
		String selectedHosts = getStringList(selection.iterator());
		String hosts[] = promptForHost(selectedHosts);
		if (hosts != null) {
			Object[] selectedItems = selection.toArray();
			for (int i = 0; i < selectedItems.length; i++) {
				ProxyBypassData data = (ProxyBypassData) selectedItems[i];
				data.setHost(hosts[i]);
			}
			hostsViewer.refresh();
		}
	}

	String getStringList(Iterator iterator) {
		StringBuilder buffer = new StringBuilder();
		if (iterator.hasNext()) {
			ProxyBypassData data = (ProxyBypassData) iterator.next();
			buffer.append(data.getHost());
		}
		while (iterator.hasNext()) {
			buffer.append(';');
			ProxyBypassData data = (ProxyBypassData) iterator.next();
			buffer.append(data.getHost());
		}
		return buffer.toString();
	}

	private String[] promptForHost(String selectedHosts) {
		InputDialog dialog = new InputDialog(getShell(),
				NetUIMessages.ProxyBypassDialog_0,
				NetUIMessages.ProxyBypassDialog_1, selectedHosts, null) {
			private ControlDecoration decorator;

			protected Control createDialogArea(Composite parent) {
				Control createDialogArea = super.createDialogArea(parent);
				decorator = new ControlDecoration(getText(), SWT.TOP | SWT.LEFT);
				decorator.setDescriptionText(NetUIMessages.ProxyBypassDialog_2);
				decorator.setImage(FieldDecorationRegistry.getDefault()
						.getFieldDecoration(
								FieldDecorationRegistry.DEC_INFORMATION)
						.getImage());
				return createDialogArea;
			}

			public boolean close() {
				decorator.dispose();
				return super.close();
			}
		};
		int result = dialog.open();
		if (result != Window.CANCEL) {
			String value = dialog.getValue();
			String hosts[] = StringUtil.split(value, new String[] { ";", "|" }); //$NON-NLS-1$ //$NON-NLS-2$
			ArrayList filtered = new ArrayList();
			for (int i = 0; i < hosts.length; i++) {
				if (hosts[i].length() != 0) {
					filtered.add(hosts[i]);
				}
			}
			return (String[]) filtered.toArray(new String[0]);
		}
		return null;
	}

	private Button createButton(String message) {
		Button button = new Button(this, SWT.PUSH);
		button.setText(message);
		button.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		return button;
	}

	public void initializeValues() {
		String providers[] = ProxySelector.getProviders();
		for (int i = 0; i < providers.length; i++) {
			bypassHosts.addAll(getProxyBypassData(providers[i]));
		}
		hostsViewer.setInput(bypassHosts);
		setProvider(ProxySelector.getDefaultProvider());
	}

	public void setProvider(String item) {
		if (item == null) {
			item = currentProvider;
		} else {
			currentProvider = item;
		}
		ArrayList selected = new ArrayList();
		Iterator it = bypassHosts.iterator();
		while (it.hasNext()) {
			ProxyBypassData data = (ProxyBypassData) it.next();
			if (data.getSource().equalsIgnoreCase(item)) {
				selected.add(data);
			}
		}
		hostsViewer
				.setCheckedElements(selected.toArray(new ProxyBypassData[0]));
	}

	public void performApply() {
		String provider = getEditableProvider();
		Iterator it = bypassHosts.iterator();
		ArrayList hosts = new ArrayList();
		while (it.hasNext()) {
			ProxyBypassData data = (ProxyBypassData) it.next();
			if (data.getSource().equals(provider)) {
				hosts.add(data.getHost());
			}
		}
		String data[] = (String[]) hosts.toArray(new String[0]);
		ProxySelector.setBypassHosts(provider, data);
	}

	public void refresh() {
		ArrayList natives = new ArrayList();
		String provider = getEditableProvider();
		Iterator it = bypassHosts.iterator();
		while (it.hasNext()) {
			ProxyBypassData data = (ProxyBypassData) it.next();
			if (!data.getSource().equals(provider)) {
				natives.add(data);
			}
		}
		bypassHosts.removeAll(natives);
		String providers[] = ProxySelector.getProviders();
		for (int i = 0; i < providers.length; i++) {
			if (!providers[i].equals(provider)) {
				bypassHosts.addAll(getProxyBypassData(providers[i]));
			}
		}
		hostsViewer.refresh();
		setProvider(currentProvider);
	}

	private List getProxyBypassData(String provider) {
		List bypassProxyData = new ArrayList();
		String[] hosts = ProxySelector.getBypassHosts(provider);
		for (int j = 0; hosts != null && j < hosts.length; j++) {
			ProxyBypassData data = new ProxyBypassData(hosts[j], provider);
			bypassProxyData.add(data);
		}
		return bypassProxyData;
	}

}
