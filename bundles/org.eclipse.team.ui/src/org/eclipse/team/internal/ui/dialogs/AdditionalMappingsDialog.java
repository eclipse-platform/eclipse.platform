/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.core.mapping.ISynchronizationScope;
import org.eclipse.team.internal.ui.SWTUtils;
import org.eclipse.team.internal.ui.TeamUIMessages;

public class AdditionalMappingsDialog extends DetailsDialog {

	private ResourceMappingHierarchyArea selectedMappingsArea;
	private ResourceMappingHierarchyArea allMappingsArea;
	private final ISynchronizationScope scope;
	private final ISynchronizationContext context;
	private String previewMessage;
	protected boolean forcePreview = true;

	public AdditionalMappingsDialog(Shell parentShell, String dialogTitle, ISynchronizationScope scope, ISynchronizationContext context) {
		super(parentShell, dialogTitle);
		this.scope = scope;
		this.context = context;
	}

	@Override
	protected void createMainDialogArea(Composite parent) {
		createWrappingLabel(parent, TeamUIMessages.AdditionalMappingsDialog_0);
		createSelectedMappingsArea(parent);
		createAllMappingsArea(parent);
		createPreviewOptionArea(parent);
	}

	/*
	 * Create a list that allows the selection of mappings via checkbox
	 */
	private void createSelectedMappingsArea(Composite parent) {
		Composite composite = createComposite(parent);
		GridLayout layout = new GridLayout(1, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		composite.setLayout(layout);
		selectedMappingsArea = ResourceMappingHierarchyArea.create(scope.asInputScope(), null /* no context */);
		selectedMappingsArea.setDescription(TeamUIMessages.AdditionalMappingsDialog_1);
		selectedMappingsArea.createArea(composite);
		// Create a separator between the two sets of buttons
		Label seperator = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
		seperator.setLayoutData(new GridData (GridData.FILL_HORIZONTAL));
	}

	/*
	 * Create a list that allows the selection of mappings via checkbox
	 */
	private void createAllMappingsArea(Composite parent) {
		Composite composite = createComposite(parent);
		GridLayout layout = new GridLayout(1, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		composite.setLayout(layout);
		allMappingsArea = ResourceMappingHierarchyArea.create(scope, context);
		allMappingsArea.setDescription(TeamUIMessages.AdditionalMappingsDialog_2);
		//allMappingsArea.addPropertyChangeListener(this);
		allMappingsArea.createArea(composite);
	}

	private void createPreviewOptionArea(Composite parent) {
		if (previewMessage != null) {
			final Button forcePreviewButton = SWTUtils.createCheckBox(parent, previewMessage);
			forcePreviewButton.setSelection(forcePreview);
			forcePreviewButton.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					// Ignore
				}
				@Override
				public void widgetSelected(SelectionEvent e) {
					forcePreview  = forcePreviewButton.getSelection();
				}
			});
		}
	}

	@Override
	protected Composite createDropDownDialogArea(Composite parent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void updateEnablements() {
		// TODO Auto-generated method stub

	}

	@Override
	protected boolean includeDetailsButton() {
		return false;
	}

	public String getPreviewMessage() {
		return previewMessage;
	}

	public void setPreviewMessage(String previewMessage) {
		this.previewMessage = previewMessage;
	}

	public boolean isForcePreview() {
		return forcePreview;
	}

}
