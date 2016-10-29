/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.internal.net;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.internal.net.ProxyData;
import org.eclipse.core.internal.net.ProxySelector;
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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;

/**
 * This class is the Composite that consists of the controls for proxy entries
 * and is used by ProxyPreferencesPage.
 */
public class ProxyEntriesComposite extends Composite {

	private Label entriesLabel;
	private CheckboxTableViewer entriesViewer;
	// private Button addButton;
	private Button editButton;
	private Button removeButton;

	protected String currentProvider;
	private ArrayList proxyEntries = new ArrayList();

	ProxyEntriesComposite(Composite parent, int style) {
		super(parent, style);
		createWidgets();
	}

	protected void createWidgets() {
		setLayout(new GridLayout(2, false));

		entriesLabel = new Label(this, SWT.NONE);
		entriesLabel.setText(NetUIMessages.ProxyPreferencePage_1);
		entriesLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER,
				false, false, 2, 1));

		Table entriesTable = new Table(this, SWT.BORDER | SWT.V_SCROLL
				| SWT.H_SCROLL | SWT.MULTI | SWT.FULL_SELECTION | SWT.CHECK);
		entriesTable.setHeaderVisible(true);
		entriesTable.setLinesVisible(true);
		entriesTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true,
				1, 3));

		entriesViewer = new CheckboxTableViewer(entriesTable);
		ProxyEntriesLabelProvider labelProvider = new ProxyEntriesLabelProvider();
		ProxyEntriesContentProvider contentProvider = new ProxyEntriesContentProvider();
		labelProvider.createColumns(entriesViewer);
		entriesViewer.setContentProvider(contentProvider);
		entriesViewer.setLabelProvider(labelProvider);

		TableLayout tableLayout = new TableLayout();
		tableLayout.addColumnData(new ColumnPixelData(24));
		tableLayout.addColumnData(new ColumnWeightData(20, 50, true));
		tableLayout.addColumnData(new ColumnWeightData(50, 50, true));
		tableLayout.addColumnData(new ColumnWeightData(20, 50, true));
		tableLayout.addColumnData(new ColumnWeightData(20, 50, true));
		tableLayout.addColumnData(new ColumnWeightData(20, 50, true));
		tableLayout.addColumnData(new ColumnWeightData(50, 50, true));
		tableLayout.addColumnData(new ColumnWeightData(50, 50, true));

		entriesTable.setLayout(tableLayout);

		// addButton = createButton(NetUIMessages.ProxyPreferencePage_9);
		editButton = createButton(NetUIMessages.ProxyPreferencePage_10);
		removeButton = createButton(NetUIMessages.ProxyPreferencePage_11);

		entriesViewer
				.addSelectionChangedListener(new ISelectionChangedListener() {
					public void selectionChanged(SelectionChangedEvent event) {
						enableButtons();
					}
				});
		entriesViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				setProvider(currentProvider);
			}
		});
		entriesViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				editSelection();
			}
		});
		// addButton.addSelectionListener(new SelectionAdapter() {
		// public void widgetSelected(SelectionEvent e) {
		// addEntry();
		// }
		// });
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

	protected void enableButtons() {
		boolean enabled = getEnabled();
		if (enabled) {
			// addButton.setEnabled(true);
			editButton.setEnabled(isSelectionEditable());
			removeButton.setEnabled(isSelectionRemovable());
		} else {
			// addButton.setEnabled(false);
			editButton.setEnabled(false);
			removeButton.setEnabled(false);
		}
	}

	private boolean isSelectionEditable() {
		IStructuredSelection selection = (IStructuredSelection) entriesViewer
				.getSelection();
		return isSelectionRemovable() && selection.size() == 1;
	}

	private boolean isSelectionRemovable() {
		IStructuredSelection selection = (IStructuredSelection) entriesViewer
				.getSelection();
		Iterator iterator = selection.iterator();
		boolean editable = iterator.hasNext();
		while (iterator.hasNext()) {
			String provider = ((ProxyData) iterator.next()).getSource();
			if (!ProxySelector.canSetProxyData(provider)) {
				editable = false;
			}
		}
		return editable;
	}

	protected void addEntry() {
		Iterator it = proxyEntries.iterator();
		ArrayList added = new ArrayList();
		String editableProvider = getEditableProvider();
		while (it.hasNext()) {
			ProxyData data = (ProxyData) it.next();
			if (data.getSource().equalsIgnoreCase(editableProvider)) {
				added.add(data.getType());
			}
		}
		String addedArray[] = (String[]) added.toArray(new String[0]);
		ProxyData data = promptForEntry(null, addedArray,
				NetUIMessages.ProxyEntryDialog_0);
		if (data != null) {
			data.setSource(editableProvider);
			proxyEntries.add(0, data);
			entriesViewer.refresh();
		}
	}

	private String getEditableProvider() {
		String providers[] = ProxySelector.getProviders();
		for (int i = 0; i < providers.length; i++) {
			if (ProxySelector.canSetProxyData(providers[i])) {
				return providers[i];
			}
		}
		return null;
	}

	private ProxyData promptForEntry(ProxyData entry, String[] addedArray,
			String title) {
		ProxyEntryDialog dialog = new ProxyEntryDialog(getShell(), entry,
				addedArray, title);
		int result = dialog.open();
		if (result != Window.CANCEL) {
			return dialog.getValue();
		}
		return null;
	}

	protected void editSelection() {
		if (!isSelectionRemovable()) {
			return;
		}
		Iterator itsel = ((IStructuredSelection) entriesViewer.getSelection()).iterator();
		ProxyData toEdit = null;
		if (itsel.hasNext()) {
			toEdit = ((ProxyData) itsel.next());
		} else {
			return;
		}
		Iterator it = proxyEntries.iterator();
		ArrayList added = new ArrayList();
		String editableProvider = getEditableProvider();
		while (it.hasNext()) {
			ProxyData data = (ProxyData) it.next();
			if (data.getSource().equalsIgnoreCase(editableProvider)) {
				if (data.getType() != toEdit.getType()) {
					added.add(data.getType());
				}
			}
		}
		String addedArray[] = (String[]) added.toArray(new String[0]);
		ProxyData data = promptForEntry(toEdit, addedArray,
				NetUIMessages.ProxyEntryDialog_1);
		if (data != null) {
			entriesViewer.refresh();
		}
	}

	protected void removeSelection() {
		IStructuredSelection selection = (IStructuredSelection) entriesViewer
				.getSelection();
		Iterator it = selection.iterator();
		while (it.hasNext()) {
			ProxyData data = (ProxyData) it.next();
			data.setHost(""); //$NON-NLS-1$
			data.setPort(-1);
			data.setUserid(null);
			data.setPassword(null);
		}
		entriesViewer.refresh();
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
			proxyEntries.addAll(getProxyData(providers[i]));
		}
		entriesViewer.setInput(proxyEntries);
		setProvider(ProxySelector.getDefaultProvider());
	}

	public void setProvider(String item) {
		if (item == null) {
			item = currentProvider;
		} else {
			currentProvider = item;
		}
		ArrayList checked = new ArrayList();
		Iterator it = proxyEntries.iterator();
		while (it.hasNext()) {
			ProxyData data = (ProxyData) it.next();
			if (data.getSource().equalsIgnoreCase(item)) {
				checked.add(data);
			}
		}
		entriesViewer.setCheckedElements(checked.toArray(new ProxyData[0]));
	}

	public void performApply() {
		String provider = getEditableProvider();
		Iterator it = proxyEntries.iterator();
		ArrayList proxies = new ArrayList();
		while (it.hasNext()) {
			ProxyData data = (ProxyData) it.next();
			if (data.getSource().equals(provider)) {
				proxies.add(data);
			}
		}
		ProxyData data[] = (ProxyData[]) proxies.toArray(new ProxyData[0]);
		ProxySelector.setProxyData(provider, data);
	}

	public void refresh() {
		String provider = getEditableProvider();
		Iterator it = proxyEntries.iterator();
		ArrayList natives = new ArrayList();
		while (it.hasNext()) {
			ProxyData data = (ProxyData) it.next();
			if (!data.getSource().equals(provider)) {
				natives.add(data);
			}
		}
		proxyEntries.removeAll(natives);
		String[] providers = ProxySelector.getProviders();
		for (int i = 0; i < providers.length; i++) {
			if (!providers[i].equals(provider)) {
				proxyEntries.addAll(getProxyData(providers[i]));
			}
		}
		entriesViewer.refresh();
		setProvider(currentProvider);
	}

	private List getProxyData(String provider) {
		List proxyDatas = new ArrayList();
		ProxyData[] entries = ProxySelector.getProxyData(provider);
		for (int j = 0; j < entries.length; j++) {
			entries[j].setSource(provider);
			proxyDatas.add(entries[j]);
		}
		return proxyDatas;
	}
}
