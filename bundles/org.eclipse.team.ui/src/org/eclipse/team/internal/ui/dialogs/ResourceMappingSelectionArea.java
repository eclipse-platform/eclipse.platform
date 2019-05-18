/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.dialogs;

import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.mapping.ResourceMappingLabelProvider;
import org.eclipse.ui.model.AdaptableList;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;

/**
 * Dialog area that displays a checkbox list of mappings.
 */
public class ResourceMappingSelectionArea extends DialogArea {

	/**
	 * Property constant used to indicate that the selected mapping has changed.
	 * The object associated with the property is a <code>ResourceMapping</code>.
	 */
	public static final String SELECTED_MAPPING = "SelectedMapping"; //$NON-NLS-1$

	/**
	 * Property constant used to indicate that the checked mappings have changed.
	 * The object associated with the property is a <code>ResourceMapping[]</code>
	 * (i.e. an array of mappings).
	 */
	public static final String CHECKED_MAPPINGS = "CheckedMappings"; //$NON-NLS-1$

	private ResourceMapping[] mappings;
	private TableViewer viewer;
	private ResourceMapping[] checkedMappings;
	private ResourceMapping selectedMapping;
	private String description;
	private boolean supportsChecking;
	private boolean supportsSelection;

	public ResourceMappingSelectionArea(ResourceMapping[] mappings, boolean supportSelection, boolean supportChecking) {
		this.mappings = mappings;
		this.supportsChecking = supportChecking;
		this.supportsSelection = supportSelection;
	}

	@Override
	public void createArea(Composite parent) {
		Composite composite = createComposite(parent, 1, true);
		GridLayout layout = new GridLayout(1, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.verticalSpacing = 0;
		layout.horizontalSpacing = 0;
		composite.setLayout(layout);

		if (description != null)
			createWrappingLabel(composite, description, 1);

		createViewer(composite);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.heightHint = 100;
		data.widthHint = 300;
		viewer.getControl().setLayoutData(data);
		viewer.setContentProvider(new BaseWorkbenchContentProvider());
		viewer.setLabelProvider(new ResourceMappingLabelProvider());
		viewer.setInput(new AdaptableList(mappings));
		if (isSupportsSelection()) {
			viewer.addSelectionChangedListener(event -> {
				ResourceMapping oldSelection = selectedMapping;
				selectedMapping = internalGetSelectedMapping();
				if (oldSelection != selectedMapping)
					firePropertyChangeChange(SELECTED_MAPPING, oldSelection, selectedMapping);
			});
		}
		if (isSupportsChecking())
			initializeCheckboxViewer(composite);
	}

	private void initializeCheckboxViewer(Composite composite) {
		final CheckboxTableViewer checkboxViewer = getCheckboxTableViewer();
		checkboxViewer.addCheckStateListener(event -> {
			ResourceMapping[] oldMappings = checkedMappings;
			checkedMappings = internalGetCheckedMappings();
			if (oldMappings != checkedMappings)
				firePropertyChangeChange(CHECKED_MAPPINGS, oldMappings, checkedMappings);
		});
		checkboxViewer.setCheckedElements(mappings);
		checkedMappings = mappings;

		Composite buttons = createEmbeddedButtonComposite(composite);

		Button selectAll = new Button(buttons, SWT.PUSH);
		selectAll.setText(TeamUIMessages.ResourceMappingSelectionArea_0);
		selectAll.setLayoutData(new GridData(GridData.FILL_BOTH));
		selectAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				checkboxViewer.setAllChecked(true);
			}
		});

		Button deselectAll = new Button(buttons, SWT.PUSH);
		deselectAll.setText(TeamUIMessages.ResourceMappingSelectionArea_1);
		deselectAll.setLayoutData(new GridData(GridData.FILL_BOTH));
		deselectAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				checkboxViewer.setAllChecked(false);
			}
		});
	}

	private void createViewer(Composite composite) {
		if (isSupportsChecking())
			viewer = CheckboxTableViewer.newCheckList(composite, getViewerStyle());
		else
			viewer = new TableViewer(new Table(composite, getViewerStyle()));
	}

	private int getViewerStyle() {
		int style = SWT.BORDER;
		if (isSupportsSelection())
			style |= SWT.SINGLE;
		return style;
	}

	/* private */ ResourceMapping[] internalGetCheckedMappings() {
		Object[] checked = getCheckboxTableViewer().getCheckedElements();
		ResourceMapping[] mappings = new ResourceMapping[checked.length];
		for (int i = 0; i < checked.length; i++) {
			Object object = checked[i];
			mappings[i] = (ResourceMapping)object;
		}
		return mappings;
	}

	private Composite createEmbeddedButtonComposite(Composite composite) {
		GridData data;
		Composite buttons = new Composite(composite, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2; // this is incremented by createButton
		layout.makeColumnsEqualWidth = true;
		layout.marginWidth = 0;
		buttons.setLayout(layout);
		data = new GridData(GridData.HORIZONTAL_ALIGN_END
				| GridData.VERTICAL_ALIGN_CENTER);
		buttons.setLayoutData(data);
		return buttons;
	}

	/* private */ ResourceMapping internalGetSelectedMapping() {
		ISelection selection = viewer.getSelection();
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ss = (IStructuredSelection) selection;
			Object firstElement = ss.getFirstElement();
			if (firstElement instanceof ResourceMapping)
				return (ResourceMapping)firstElement;
		}
		return null;
	}

	public void setDescription(String description) {
		this.description = description;
	}
	public ResourceMapping[] getCheckedMappings() {
		return checkedMappings;
	}
	public ResourceMapping getSelectedMapping() {
		return selectedMapping;
	}

	private CheckboxTableViewer getCheckboxTableViewer() {
		return (CheckboxTableViewer)viewer;
	}

	public boolean isSupportsChecking() {
		return supportsChecking;
	}

	public boolean isSupportsSelection() {
		return supportsSelection;
	}
}
