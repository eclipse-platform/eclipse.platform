/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.core.mapping.ISynchronizationScopeManager;
import org.eclipse.team.core.mapping.provider.SynchronizationContext;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.mappings.WorkspaceSubscriberContext;
import org.eclipse.team.internal.ui.SWTUtils;
import org.eclipse.team.internal.ui.dialogs.DetailsDialog;
import org.eclipse.team.ui.synchronize.*;

public class OutgoingChangesDialog extends DetailsDialog {

	private final String message;
	private ParticipantPagePane pane;
	private ModelSynchronizeParticipant participant;
	private final String title;
	private final String detailsMessage;
	private final ISynchronizationScopeManager manager;
	private String helpContextId;

	public OutgoingChangesDialog(Shell parentShell, ISynchronizationScopeManager manager, String title, String message, String detailsMessage) {
		super(parentShell, title);
		this.manager = manager;
		this.title = title;
		this.message = message;
		this.detailsMessage = detailsMessage;
	}

	@Override
	protected void createMainDialogArea(Composite parent) {
		Composite composite = SWTUtils.createHVFillComposite(parent, SWTUtils.MARGINS_NONE);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		createWrappingLabel(composite, message);
		Dialog.applyDialogFont(parent);
	}

	@Override
	protected Label createWrappingLabel(Composite parent, String text) {
		Label label = new Label(parent, SWT.LEFT | SWT.WRAP);
		label.setText(text);
		GridData data = new GridData();
		data.horizontalSpan = 1;
		data.horizontalAlignment = GridData.FILL;
		data.horizontalIndent = 0;
		data.grabExcessHorizontalSpace = true;
		data.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH);
		label.setLayoutData(data);
		label.setFont(parent.getFont());
		return label;
	}
	
	@Override
	protected Composite createDropDownDialogArea(Composite parent) {
		Composite composite = SWTUtils.createHVFillComposite(parent, SWTUtils.MARGINS_DIALOG);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.heightHint = 200;
		data.widthHint = 200;
		composite.setLayoutData(data);
		composite.addDisposeListener(e -> {
			if (pane != null)
				pane.dispose();
			if (participant != null)
				participant.dispose();
		});
		
		createWrappingLabel(composite, detailsMessage);
		
		try {
			participant = createParticipant();
			ISynchronizePageConfiguration configuration = participant.createPageConfiguration();
			configuration.setSupportedModes(ISynchronizePageConfiguration.OUTGOING_MODE);
			configuration.setMode(ISynchronizePageConfiguration.OUTGOING_MODE);
			configuration.setMenuGroups(ISynchronizePageConfiguration.P_TOOLBAR_MENU, new String[] { ISynchronizePageConfiguration.NAVIGATE_GROUP, ISynchronizePageConfiguration.LAYOUT_GROUP });
			pane = new ParticipantPagePane(getShell(), true, configuration, participant);
			Control control = pane.createPartControl(composite);
			control.setLayoutData(SWTUtils.createHVFillGridData());
		} catch (InvocationTargetException e) {
			CVSUIPlugin.openError(getShell(), null, null, e);
			SWTUtils.createLabel(parent, CVSUIMessages.OutgoingChangesDialog_0);
		} catch (InterruptedException e) {
			SWTUtils.createLabel(parent, CVSUIMessages.OutgoingChangesDialog_1);
		}
		
		return composite;
	}

	private ModelSynchronizeParticipant createParticipant() throws InvocationTargetException, InterruptedException {
		SynchronizationContext context = createSynchronizationContext(manager);
		ModelSynchronizeParticipant participant = ModelSynchronizeParticipant.createParticipant(context, title);
		participant.setMergingEnabled(false);
		return participant;
	}

	private SynchronizationContext createSynchronizationContext(final ISynchronizationScopeManager manager) throws InvocationTargetException, InterruptedException {
		final SynchronizationContext[] context = new SynchronizationContext[] { null };
		context[0] = WorkspaceSubscriberContext.createContext(manager, ISynchronizationContext.THREE_WAY);
		return context[0];
	}
	
	@Override
	protected boolean isMainGrabVertical() {
		return false;
	}
	
	@Override
	protected void updateEnablements() {
		// Can always finish
		setPageComplete(true);
	}
	
	@Override
	protected boolean includeErrorMessage() {
		return false;
	}

	public void setHelpContextId(String helpContextId) {
		this.helpContextId = helpContextId;	
	}
	
	@Override
	protected String getHelpContextId() {
		return helpContextId;
	}
}