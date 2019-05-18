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
package org.eclipse.team.internal.ccvs.ui;


import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.ICVSFolder;
import org.eclipse.team.internal.ccvs.core.ICVSResource;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;
import org.eclipse.ui.dialogs.PropertyPage;

/**
 * A property page which displays the CVS-specific properties for the
 * selected resource.
 */
public class ResourcePropertiesPage extends PropertyPage {
	// The resource to show properties for
	IResource resource;

	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = layout.marginWidth = 0;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		try {
			IResource resource = getSelectedElement();
			if (resource != null) {
				ICVSResource cvsResource = CVSWorkspaceRoot.getCVSResourceFor(resource);
				if (!cvsResource.isManaged()) {
					createPair(composite, CVSUIMessages.ResourcePropertiesPage_status, CVSUIMessages.ResourcePropertiesPage_notManaged); // 
				} else {
					boolean hasRemote = false;
					if(cvsResource.isFolder()) {
						hasRemote = ((ICVSFolder)cvsResource).isCVSFolder();
					} else {
						ResourceSyncInfo info = cvsResource.getSyncInfo();
						if(info!=null && !info.isAdded()) {
							hasRemote = true;
						}
					}
					createPair(composite, CVSUIMessages.ResourcePropertiesPage_status, hasRemote ? CVSUIMessages.ResourcePropertiesPage_versioned : CVSUIMessages.ResourcePropertiesPage_notVersioned); //  
				}
			}
		} catch (TeamException e) {
			createPair(composite, CVSUIMessages.ResourcePropertiesPage_error, e.getMessage()); 
		}
		Dialog.applyDialogFont(parent);
		return composite;
	}

	/**
	 * Creates a key-value property pair in the given parent.
	 * 
	 * @param parent  the parent for the labels
	 * @param left  the string for the left label
	 * @param right  the string for the right label
	 */
	protected void createPair(Composite parent, String left, String right) {
		Label label = new Label(parent, SWT.NONE);
		label.setText(left);
	
		label = new Label(parent, SWT.NONE);
		label.setText(right);
		label.setToolTipText(right);
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	}

	/**
	 * Returns the element selected when the properties was run
	 * 
	 * @return the selected element
	 */	
	protected IResource getSelectedElement() {
		// get the resource that is the source of this property page
		IResource resource = null;
		IAdaptable element = getElement();
		if (element instanceof IResource) {
			resource = (IResource)element;
		} else {
			Object adapter = element.getAdapter(IResource.class);
			if (adapter instanceof IResource) {
				resource = (IResource)adapter;
			}
		}
		return resource;
	}
}
