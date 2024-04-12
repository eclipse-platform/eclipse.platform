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
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.help.ui.internal.views;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.help.ui.internal.HelpUIResources;
import org.eclipse.help.ui.internal.IHelpUIConstants;
import org.eclipse.help.ui.internal.Messages;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.PlatformUI;

/**
 * Scope dialog for federated search.
 */
public class ScopeSetDialog extends TrayDialog  {


	public static class NonDefaultFilter extends ViewerFilter {

		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (element instanceof ScopeSet && ((ScopeSet)element).isDefault()) {
				return false;
			}
			return true;
		}

	}

	public class ShowAllListener extends SelectionAdapter {

		@Override
		public void widgetSelected(SelectionEvent e) {
			enableTable();
		}
	}

	private final ScopeSetManager manager;
	private final EngineDescriptorManager descManager;
	private static final int NEW_ID = IDialogConstants.CLIENT_ID + 1;
	private static final int EDIT_ID = IDialogConstants.CLIENT_ID + 2;
	private static final int RENAME_ID = IDialogConstants.CLIENT_ID +3;
	private static final int REMOVE_ID = IDialogConstants.CLIENT_ID + 4;
	private Button editButton;
	private Button renameButton;
	private Button removeButton;
	private final ArrayList<ScopeSet> sets;
	private ArrayList<PendingOperation> operations;
	private final IStructuredContentProvider contentProvider;

	private Button showAllRadio;
	private Button showSelectedRadio;

	private final ILabelProvider labelProvider;
	private Object input;
	private TableViewer viewer;
	private final int widthInChars = 55;
	private final int heightInChars = 15;
	private ScopeSet initialSelection;
	private Object[] result;
	private final boolean localOnly;

	private abstract static class PendingOperation {
		ScopeSet set;
		public PendingOperation(ScopeSet set) {
			this.set = set;
		}
		public abstract void commit();
		public abstract void cancel();
	}

	private class AddOperation extends PendingOperation {
		public AddOperation(ScopeSet set) {
			super(set);
		}

		@Override
		public void commit() {
			manager.add(set);
		}

		@Override
		public void cancel() {
			set.dispose();
		}
	}

	private static class RenameOperation extends PendingOperation {
		private String newName;
		public RenameOperation(ScopeSet set, String newName) {
			super(set);
			this.newName = newName;
		}

		@Override
		public void commit() {
			this.set.setName(newName);
		}

		@Override
		public void cancel() {
		}
	}

/*
	private class EditOperation extends PendingOperation {
		public EditOperation(ScopeSet set) {
			super(set);
		}
		public void commit() {
		}
		public void cancel() {
		}
	}
*/

	private class RemoveOperation extends PendingOperation {
		public RemoveOperation(ScopeSet set) {
			super(set);
		}

		@Override
		public void commit() {
			manager.remove(set);
		}

		@Override
		public void cancel() {
		}
	}

	private class ScopeContentProvider implements IStructuredContentProvider {

		@Override
		public Object[] getElements(Object inputElement) {
			return sets.toArray();
		}

		@Override
		public void dispose() {
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
	}

	private class ScopeLabelProvider extends LabelProvider {

		@Override
		public String getText(Object obj) {
			String name = findNewName((ScopeSet)obj);
			if (name!=null)
				return name;
			return ((ScopeSet)obj).getName();
		}
		private String findNewName(ScopeSet set) {
			PendingOperation op = findOperation(set, RenameOperation.class);
			if (op!=null) {
				RenameOperation rop = (RenameOperation)op;
				return rop.newName;
			}
			return null;
		}

		@Override
		public Image getImage(Object obj) {
			return HelpUIResources.getImage(IHelpUIConstants.IMAGE_SCOPE_SET);
		}
	}

	public ScopeSetDialog(Shell parent, ScopeSetManager manager, EngineDescriptorManager descManager, boolean localOnly) {
		super(parent);
		this.manager = manager;
		this.descManager = descManager;
		this.sets = extractSets(manager.getScopeSets(false));
		this.localOnly = localOnly;
		contentProvider = new ScopeContentProvider();
		labelProvider = new ScopeLabelProvider();
		setInitialSelections( manager.getActiveSet());
	}

	private void setInitialSelections(ScopeSet scopeSet) {
		initialSelection = scopeSet;
	}

	private ArrayList<ScopeSet> extractSets(ScopeSet[] array) {
		ArrayList<ScopeSet> list = new ArrayList<>();
		Collections.addAll(list, array);
		return list;
	}

	@Override
	protected Control createDialogArea(Composite container) {
		Composite innerContainer = (Composite)super.createDialogArea(container);
		createRadioButtons(innerContainer);
		createTable(innerContainer);
		enableTable();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(innerContainer,
		     "org.eclipse.help.ui.searchScope"); //$NON-NLS-1$
		createEditingButtons(innerContainer);
		viewer.addSelectionChangedListener(event -> updateButtons());
		ViewerFilter[] filters = { new NonDefaultFilter() };
		viewer.setFilters(filters );
		return innerContainer;
	}

	private void createRadioButtons(Composite parent) {
		boolean showAll = initialSelection != null  && initialSelection.isDefault();
		showAllRadio = new Button(parent, SWT.RADIO);
		showAllRadio.setText(Messages.ScopeSet_selectAll);

		showSelectedRadio = new Button(parent, SWT.RADIO);
		showSelectedRadio.setText(Messages.ScopeSet_selectWorkingSet);
		showAllRadio.addSelectionListener(new ShowAllListener());
		showAllRadio.setSelection(showAll);
		showSelectedRadio.setSelection(!showAll);
	}

	private void createTable(Composite parent) {
		viewer = new TableViewer(parent, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		viewer.setContentProvider(contentProvider);
		viewer.setLabelProvider(labelProvider);
		viewer.setInput(input);
		viewer.addDoubleClickListener(event -> okPressed());
		if (initialSelection != null) {
			viewer.setSelection(new StructuredSelection(initialSelection));
		}
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = convertHeightInCharsToPixels(heightInChars);
		gd.widthHint = convertWidthInCharsToPixels(widthInChars);
		Table table = viewer.getTable();
		table.setLayoutData(gd);
		table.setFont(parent.getFont());
	}

	private void enableTable() {
		if (viewer != null) {
			boolean showSelected = showSelectedRadio.getSelection();
			viewer.getTable().setEnabled(showSelected);
			viewer.refresh();
			// Ensure that a scope is selected unless there are no
			// user defined scopes
			if (showSelected && viewer.getSelection().isEmpty()) {
				Object firstElement = viewer.getElementAt(0);
				if ( firstElement != null ) {
					viewer.setSelection(new StructuredSelection(firstElement));
				}
			}
		}
	}

	private void createEditingButtons(Composite composite) {
		Composite buttonComposite= new Composite(composite, SWT.RIGHT);
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		buttonComposite.setLayout(layout);
		GridData data= new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.GRAB_HORIZONTAL);
		data.grabExcessHorizontalSpace= true;
		composite.setData(data);
		createButton(buttonComposite, NEW_ID, Messages.ScopeSetDialog_new, false);
		renameButton = createButton(buttonComposite, RENAME_ID, Messages.ScopeSetDialog_rename, false);
		editButton = createButton(buttonComposite, EDIT_ID, Messages.ScopeSetDialog_edit, false);
		removeButton = createButton(buttonComposite, REMOVE_ID, Messages.ScopeSetDialog_remove, false);
		updateButtons();
	}

	public ScopeSet getActiveSet() {
		if (result!=null && result.length>0)
			return (ScopeSet)result[0];
		return null;
	}

	@Override
	protected void okPressed() {
		if (operations!=null) {
			for (int i=0; i<operations.size(); i++) {
				PendingOperation operation = operations.get(i);
				operation.commit();
			}
			operations = null;
		}
		if (showAllRadio.getSelection()) {
			setResult(manager.getDefaultScope());
		} else {
			// Build a list of selected children.
			IStructuredSelection selection = viewer.getStructuredSelection();
			setResult(selection.toList());
		}
		super.okPressed();
	}

	private void setResult(ScopeSet scope) {
		result = new Object[] { scope };
	}

	private void setResult(List<?> newResult) {
		if (newResult == null) {
			result = null;
		} else {
			result = new Object[newResult.size()];
			newResult.toArray(result);
		}
	}

	@Override
	protected void cancelPressed() {
		if (operations!=null) {
			for (int i=0; i<operations.size(); i++) {
				PendingOperation operation = operations.get(i);
				operation.cancel();
			}
			operations = null;
		}
		super.cancelPressed();
	}

	@Override
	protected void buttonPressed(int buttonId) {
		switch (buttonId) {
		case NEW_ID:
			doNew();
			doEdit();
			break;
		case EDIT_ID:
			doEdit();
			break;
		case RENAME_ID:
			doRename();
			break;
		case REMOVE_ID:
			doRemove();
			break;
		}
		super.buttonPressed(buttonId);
	}

	private void doNew() {
		ScopeSet newSet = new ScopeSet(getDefaultName());
		String name = getNewName(newSet.getName(), false);
		if (name!=null) {
			newSet.setName(name);
			scheduleOperation(new AddOperation(newSet));
			sets.add(newSet);
			viewer.refresh();
			viewer.setSelection(new StructuredSelection(newSet));
			updateButtons();
		}
	}

	private String getDefaultName() {
		Set<String> namesInUse = new HashSet<>();
		for (int i=0; i<sets.size(); i++) {
			ScopeSet set = sets.get(i);
			namesInUse.add(set.getName().toLowerCase());
		}
		for (int i = 1; i < 1000; i++) {
			String name = Messages.ScopeSetDialog_defaultName + i;
			if (!namesInUse.contains(name.toLowerCase())) {
				return name;
			}
		}
		return ""; //$NON-NLS-1$
	}


	private void doEdit() {
		IStructuredSelection ssel = viewer.getStructuredSelection();
		ScopeSet set = (ScopeSet)ssel.getFirstElement();
		if (set==null) {
			return;
		}
		PreferenceManager manager = new ScopePreferenceManager(descManager, set);

		if (!localOnly) {
			PreferenceDialog dialog = new ScopePreferenceDialog(getShell(), manager, descManager, set.isEditable());
			dialog.setPreferenceStore(set.getPreferenceStore());
			dialog.create();
			dialog.getShell().setText(NLS.bind(Messages.ScopePreferenceDialog_wtitle, set.getName()));
			dialog.open();
		} else {
			LocalScopeDialog localDialog = new LocalScopeDialog(getShell(), manager, descManager, set);
			localDialog.create();
			localDialog.getShell().setText(NLS.bind(Messages.ScopePreferenceDialog_wtitle, set.getName()));
			localDialog.open();
		}
	}

	private void doRename() {
		IStructuredSelection ssel = viewer.getStructuredSelection();
		ScopeSet set = (ScopeSet)ssel.getFirstElement();
		if (set!=null) {
			RenameOperation rop = (RenameOperation)findOperation(set, RenameOperation.class);
			String oldName = rop!=null?rop.newName:set.getName();
			String newName = getNewName(oldName, true);
			if (newName!=null) {
				if (rop!=null)
					rop.newName = newName;
				else
					scheduleOperation(new RenameOperation(set, newName));
				viewer.update(set, null);
				updateButtons();
			}
		}
	}

	private String getNewName(String oldName, boolean isRename) {
		RenameDialog dialog = new RenameDialog(getShell(), oldName);
		for (int i=0; i<sets.size(); i++) {
			ScopeSet set = sets.get(i);
			dialog.addOldName(set.getName());
		}
		dialog.create();
		String dialogTitle = isRename ?
		  Messages.RenameDialog_wtitle : Messages.NewDialog_wtitle;
		dialog.getShell().setText(dialogTitle);
		if (dialog.open()==Window.OK) {
			return dialog.getNewName();
		}
		return null;
	}

	private void doRemove() {
		IStructuredSelection ssel = viewer.getStructuredSelection();
		ScopeSet set = (ScopeSet)ssel.getFirstElement();
		if (set!=null) {
			scheduleOperation(new RemoveOperation(set));
			sets.remove(set);
			viewer.refresh();
			// Set the selection to the first remaining element
			Object element = viewer.getElementAt(0);
			if (element != null) {
				viewer.setSelection(new StructuredSelection(element));
			}
			updateButtons();
		}
	}

	private void scheduleOperation(PendingOperation op) {
		if (operations==null)
			operations = new ArrayList<>();
		operations.add(op);
	}

	private void updateButtons() {
		IStructuredSelection ssel = viewer.getStructuredSelection();
		editButton.setEnabled(!ssel.isEmpty());
		ScopeSet set = (ScopeSet)ssel.getFirstElement();
		boolean editableSet = set!=null && set.isEditable() && !set.isImplicit();
		removeButton.setEnabled(editableSet);
		renameButton.setEnabled(editableSet);
	}

	private PendingOperation findOperation(ScopeSet set, Class<?> type) {
		if (operations!=null) {
			for (int i=0; i<operations.size(); i++) {
				PendingOperation op = operations.get(i);
				if (op.getClass().equals(type)) {
					if (op.set.equals(set))
						return op;
				}
			}
		}
		return null;
	}

	public void setInput(ScopeSetManager scopeSetManager) {
		input = scopeSetManager;
	}
}
