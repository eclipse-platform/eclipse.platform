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
package org.eclipse.team.internal.ccvs.ui.tags;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.ui.model.IWorkbenchAdapter;

/**
 * Workbench model element that contains a list of tags
 * of the same type (BRANCH, VERSION or DATE).
 */
public class TagRootElement implements IWorkbenchAdapter, IAdaptable {
	private TagSource tagSource;
	private int typeOfTagRoot;
	private final Object parent;
	
	public TagRootElement(Object parent, TagSource tagSource, int typeOfTagRoot) {
		this.parent = parent;
		this.typeOfTagRoot = typeOfTagRoot;
		this.tagSource = tagSource;
	}
	
	public Object[] getChildren(Object o) {
		CVSTag[] childTags = tagSource.getTags(typeOfTagRoot);
		TagElement[] result = new TagElement[childTags.length];
		for (int i = 0; i < childTags.length; i++) {
			result[i] = new TagElement(this, childTags[i]);
		}
		return result;
	}
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == IWorkbenchAdapter.class) return adapter.cast(this);
		return null;
	}
	public ImageDescriptor getImageDescriptor(Object object) {
		if(typeOfTagRoot==CVSTag.BRANCH) {
			return CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_BRANCHES_CATEGORY);
		} else if(typeOfTagRoot==CVSTag.DATE){
			return CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_DATES_CATEGORY);
		}else {
			return CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_VERSIONS_CATEGORY);
		}
	}
	public String getLabel(Object o) {
		if(typeOfTagRoot==CVSTag.BRANCH) {
			return CVSUIMessages.MergeWizardEndPage_branches; 
		} else if(typeOfTagRoot==CVSTag.DATE){
			return CVSUIMessages.TagRootElement_0; 
		}else {
			return CVSUIMessages.VersionsElement_versions; 
		}	
	}
	public Object getParent(Object o) {
		return parent;
	}
	/**
	 * Gets the typeOfTagRoot.
	 * @return Returns a int
	 */
	public int getTypeOfTagRoot() {
		return typeOfTagRoot;
	}

}
