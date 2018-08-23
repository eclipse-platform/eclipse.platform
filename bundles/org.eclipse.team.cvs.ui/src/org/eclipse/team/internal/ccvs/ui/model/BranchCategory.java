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
package org.eclipse.team.internal.ccvs.ui.model;

 
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.ICVSUIConstants;
/**
 * BranchCategory is the model element for the branches category
 * for a particular repository in the repositories view. Its children
 * are the array of all known branch tags, other than HEAD, for the
 * given repository.
 */
public class BranchCategory extends TagCategory {

	public BranchCategory(ICVSRepositoryLocation repository) {
		super(repository);
	}
	
	protected CVSTag[] getTags(IProgressMonitor monitor) throws CVSException {
		return CVSUIPlugin.getPlugin().getRepositoryManager().getKnownTags(repository, getWorkingSet(), CVSTag.BRANCH, monitor);
	}

	/**
	 * Returns an image descriptor to be used for displaying an object in the workbench.
	 * Returns null if there is no appropriate image.
	 *
	 * @param object The object to get an image descriptor for.
	 */
	public ImageDescriptor getImageDescriptor(Object object) {
		return CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_BRANCHES_CATEGORY);
	}
	
	/**
	 * Returns the name of this element.  This will typically
	 * be used to assign a label to this object when displayed
	 * in the UI.  Returns an empty string if there is no appropriate
	 * name for this object.
	 *
	 * @param object The object to get a label for.
	 */
	public String getLabel(Object o) {
		return CVSUIMessages.BranchCategory_Branches_1; 
	}
}
