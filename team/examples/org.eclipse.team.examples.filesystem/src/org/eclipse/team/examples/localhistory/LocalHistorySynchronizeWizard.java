/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
package org.eclipse.team.examples.localhistory;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.team.internal.ui.ITeamUIImages;
import org.eclipse.team.ui.TeamImages;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.*;
import org.eclipse.team.ui.synchronize.ISynchronizeManager;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;


public class LocalHistorySynchronizeWizard extends Wizard {

	private class MessagePage extends WizardPage {
		protected MessagePage(String pageName, String title, ImageDescriptor titleImage) {
			super(pageName, title, titleImage);
		}

		@Override
		public void createControl(Composite parent) {
			Composite top = new Composite(parent, SWT.NONE);
			top.setLayout(new GridLayout());
			top.setLayoutData(new GridData(GridData.FILL_BOTH));
			Label label = new Label(top, SWT.WRAP);
			label.setText("This will create a synchronization against the latest file state in local history."); //$NON-NLS-1$
			label.setLayoutData(new GridData(GridData.FILL_BOTH));
			setControl(top);
		}
	}

	public LocalHistorySynchronizeWizard() {
		super();
	}

	@Override
	public void addPages() {
		addPage(new MessagePage("Local History", "Create a local history synchronization", TeamImages.getImageDescriptor(ITeamUIImages.IMG_WIZBAN_SHARE)));  //$NON-NLS-1$//$NON-NLS-2$
	}

	@Override
	public boolean performFinish() {
		LocalHistoryParticipant participant = new LocalHistoryParticipant();
		ISynchronizeManager manager = TeamUI.getSynchronizeManager();
		manager.addSynchronizeParticipants(new ISynchronizeParticipant[] {participant});
		ISynchronizeView view = manager.showSynchronizeViewInActivePage();
		view.display(participant);
		return true;
	}
}
