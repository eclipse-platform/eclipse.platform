/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.tags;

import java.util.Date;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.ICVSUIConstants;
import org.eclipse.team.internal.ccvs.ui.model.CVSTagElement;
import org.eclipse.ui.model.IWorkbenchAdapter;

public class TagElement implements IWorkbenchAdapter, IAdaptable {
    Object parent;
	CVSTag tag;
	
	public static ImageDescriptor getImageDescriptor(CVSTag tag) {
        if (tag.getType() == CVSTag.BRANCH || tag.equals(CVSTag.DEFAULT)) {
			return CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_TAG);
		} else if (tag.getType() == CVSTag.DATE){
			return CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_DATE);
		}else {
			return CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_PROJECT_VERSION);
		}
    }
	
	/**
	 * @deprecated
	 * @param tag
	 */
	public TagElement(CVSTag tag) {
		this(null, tag);
	}
	public TagElement(Object parent, CVSTag tag) {
	    this.parent = parent;
		this.tag = tag;
	}
	public Object[] getChildren(Object o) {
		return new Object[0];
	}
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == IWorkbenchAdapter.class) return adapter.cast(this);
		return null;
	}
	public ImageDescriptor getImageDescriptor(Object object) {
		return getImageDescriptor(tag);
	}
    public String getLabel(Object o) {
		if(tag.getType() == CVSTag.DATE){
			Date date = tag.asDate();
			if (date != null){
				return CVSTagElement.toDisplayString(date);
			}
		}
		return tag.getName();
	}
	public Object getParent(Object o) {
		return parent;
	}
	public CVSTag getTag() {
		return tag;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return tag.hashCode();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (obj instanceof TagElement) {
			return tag.equals(((TagElement)obj).getTag());
		}
		return super.equals(obj);
	}
}
