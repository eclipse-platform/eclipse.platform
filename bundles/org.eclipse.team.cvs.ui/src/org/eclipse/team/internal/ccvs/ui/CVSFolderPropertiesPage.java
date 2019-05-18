/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.ui;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.syncinfo.FolderSyncInfo;
import org.eclipse.team.internal.ccvs.core.util.Util;
import org.eclipse.ui.PlatformUI;

public class CVSFolderPropertiesPage extends CVSPropertiesPage {

	IFolder folder;

	private Text root;

	private Text repository;
	
	@Override
	protected Control createContents(Composite parent) {
		initialize();
		noDefaultAndApplyButton();
		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		composite.setLayout(layout);
		
		try {
			ICVSFolder cvsResource = CVSWorkspaceRoot.getCVSFolderFor(folder);
			if (!cvsResource.isManaged()) {
				if (cvsResource.isIgnored()) {
					createLabel(composite, CVSUIMessages.CVSFolderPropertiesPage_ignored, 2); 
				} else {
					createLabel(composite, CVSUIMessages.CVSFolderPropertiesPage_notManaged, 2); 
				}
			} else if (!cvsResource.isCVSFolder()) {
				// We have a managed folder which is not a cvs folder. 
				// This is really an invalid state but it does happen once in a while
				createLabel(composite, CVSUIMessages.CVSFolderPropertiesPage_notCVSFolder, 2); 
			} else {
				FolderSyncInfo syncInfo = cvsResource.getFolderSyncInfo();
				createLabel(composite, CVSUIMessages.CVSFolderPropertiesPage_root); 
				root= createReadOnlyText(composite, syncInfo.getRoot());
				createLabel(composite, CVSUIMessages.CVSFolderPropertiesPage_repository); 
				repository= createReadOnlyText(composite, syncInfo.getRepository());
			
				// Tag
				createLabel(composite, CVSUIMessages.CVSFilePropertiesPage_tag); 
				CVSTag tag = syncInfo.getTag();

				if (tag != null && tag.getType() == CVSTag.BRANCH) {
					tag = Util.getAccurateFolderTag(folder, tag);				
				}
			
				createReadOnlyText(composite, getTagLabel(tag));
				
				// Static-ness
				if (syncInfo.getIsStatic()) {
					createLabel(composite, CVSUIMessages.CVSFolderPropertiesPage_static); 
					createReadOnlyText(composite, syncInfo.getIsStatic() ? CVSUIMessages.yes : CVSUIMessages.no); // 
				}
				
				createLabel(composite, "", 2); // spacer //$NON-NLS-1$
				
				// Allow the folder to be disconnected from CVS control
				final Button disconnect = new Button(composite, SWT.NONE);
				disconnect.setText(CVSUIMessages.CVSFolderPropertiesPage_disconnect); 
				GridData data = new GridData(GridData.HORIZONTAL_ALIGN_END);
				int widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
				data.widthHint = Math.max(widthHint, disconnect.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
				disconnect.setLayoutData(data);
				disconnect.addListener(SWT.Selection, event -> {
					// perform a disconnect
					if (disconnectFolder()) {
						root.setText(CVSUIMessages.CVSFilePropertiesPage_none);
						repository.setText(CVSUIMessages.CVSFilePropertiesPage_none);
						disconnect.setEnabled(false);
					}
				});
			}
		} catch (TeamException e) {
			// Display error text
			createLabel(composite, CVSUIMessages.CVSFilePropertiesPage_error, 2); 
		}
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.FOLDER_PROPERTY_PAGE);
		Dialog.applyDialogFont(parent);
		return composite;
	}

	/**
	 * Initializes the page
	 */
	private void initialize() {
		// Get the file that is the source of this property page
		folder = null;
		IAdaptable element = getElement();
		if (element instanceof IFolder) {
			folder = (IFolder)element;
		} else {
			Object adapter = element.getAdapter(IFolder.class);
			if (adapter instanceof IFolder) {
				folder = (IFolder)adapter;
			}
		}
	}

	private boolean disconnectFolder() {
		if (MessageDialog.openQuestion(getShell(), CVSUIMessages.CVSFolderPropertiesPage_disconnectTitle, CVSUIMessages.CVSFolderPropertiesPage_disconnectQuestion)) { // 
			final ICVSFolder cvsFolder = CVSWorkspaceRoot.getCVSFolderFor(folder);
			try {
				PlatformUI.getWorkbench().getProgressService().run(true, false, monitor -> {
					try {
						cvsFolder.unmanage(null);
					} catch (CVSException e) {
						throw new InvocationTargetException(e);
					}
				});
			} catch (InvocationTargetException e) {
				CVSUIPlugin.openError(getShell(), null, null, e);
				return false;
			} catch (InterruptedException e) {
				// Ignore
			}
			return true;
		} else {
			return false;
		}
	}			
}
