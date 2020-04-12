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
package org.eclipse.team.internal.ccvs.ui;

import java.text.DateFormat;
import java.util.Date;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.ICVSFile;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;
import org.eclipse.team.internal.ccvs.ui.actions.CVSAction;
import org.eclipse.ui.PlatformUI;

public class CVSFilePropertiesPage extends CVSPropertiesPage {
	IFile file;

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
			ICVSFile cvsResource = CVSWorkspaceRoot.getCVSFileFor(file);
			if (!cvsResource.isManaged()) {
				if (cvsResource.isIgnored()) {
					createLabel(composite, CVSUIMessages.CVSFilePropertiesPage_ignored); 
				} else {
					createLabel(composite, CVSUIMessages.CVSFilePropertiesPage_notManaged); 
				}
				createLabel(composite, ""); //$NON-NLS-1$
				return composite;
			}
			ResourceSyncInfo syncInfo = cvsResource.getSyncInfo();
			

			
			if (syncInfo.isAdded()) {
				createLabel(composite, CVSUIMessages.CVSFilePropertiesPage_isAdded, 2); 
			} else {
				// Base
				createLabel(composite, CVSUIMessages.CVSFilePropertiesPage_baseRevision); 
				createReadOnlyText(composite, syncInfo.getRevision());
				Date baseTime = syncInfo.getTimeStamp();
				if (baseTime != null) {
					createLabel(composite, CVSUIMessages.CVSFilePropertiesPage_baseTimestamp); 
					createReadOnlyText(composite, DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL).format(syncInfo.getTimeStamp()));
				}
				
				// Modified
				createLabel(composite, CVSUIMessages.CVSFilePropertiesPage_modified); 
				createReadOnlyText(composite, cvsResource.isModified(null) ? CVSUIMessages.yes : CVSUIMessages.no); // 
			}
			
			// Keyword Mode
			createLabel(composite, CVSUIMessages.CVSFilePropertiesPage_keywordMode); 
			createReadOnlyText(composite, syncInfo.getKeywordMode().getLongDisplayText());
			
			// Tag
			createLabel(composite, CVSUIMessages.CVSFilePropertiesPage_tag); 
			CVSTag tag = CVSAction.getAccurateFileTag(cvsResource);
			createReadOnlyText(composite, getTagLabel(tag));
		} catch (TeamException e) {
			// Display error text
			createLabel(composite, CVSUIMessages.CVSFilePropertiesPage_error); 
			createReadOnlyText(composite, ""); //$NON-NLS-1$
		}
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.FILE_PROPERTY_PAGE);
		Dialog.applyDialogFont(parent);
		return composite;
	}

	/**
	 * Initializes the page
	 */
	private void initialize() {
		// Get the file that is the source of this property page
		file = null;
		IAdaptable element = getElement();
		if (element instanceof IFile) {
			file = (IFile)element;
		} else {
			Object adapter = element.getAdapter(IFile.class);
			if (adapter instanceof IFile) {
				file = (IFile)adapter;
			}
		}
	}
}

