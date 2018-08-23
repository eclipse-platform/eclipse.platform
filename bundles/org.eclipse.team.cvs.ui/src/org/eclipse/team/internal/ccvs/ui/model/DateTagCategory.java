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
package org.eclipse.team.internal.ccvs.ui.model;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.ICVSUIConstants;

/**
 * The DateTagCategory is the parent of all the date tags in the repositories view.
 */
public class DateTagCategory extends TagCategory {

	public DateTagCategory(ICVSRepositoryLocation repository) {
		super(repository);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.model.TagCategory#getTags(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected CVSTag[] getTags(IProgressMonitor monitor) throws CVSException {
		return CVSUIPlugin.getPlugin().getRepositoryManager().getRepositoryRootFor(repository).getDateTags();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.model.IWorkbenchAdapter#getImageDescriptor(java.lang.Object)
	 */
	public ImageDescriptor getImageDescriptor(Object object) {
		return CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_DATES_CATEGORY);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.model.IWorkbenchAdapter#getLabel(java.lang.Object)
	 */
	public String getLabel(Object o) {
		return CVSUIMessages.DateTagCategory_0; 
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.model.TagCategory#getAdapter(java.lang.Class)
	 */
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter.equals(ICVSRepositoryLocation.class)) {
			return adapter.cast(getRepository(null));
		}
		return super.getAdapter(adapter);
	}

}
