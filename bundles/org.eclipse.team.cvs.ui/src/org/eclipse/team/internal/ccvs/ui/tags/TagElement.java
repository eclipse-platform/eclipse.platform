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
	@Deprecated
	public TagElement(CVSTag tag) {
		this(null, tag);
	}
	public TagElement(Object parent, CVSTag tag) {
		this.parent = parent;
		this.tag = tag;
	}
	@Override
	public Object[] getChildren(Object o) {
		return new Object[0];
	}
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == IWorkbenchAdapter.class) return adapter.cast(this);
		return null;
	}
	@Override
	public ImageDescriptor getImageDescriptor(Object object) {
		return getImageDescriptor(tag);
	}
	@Override
	public String getLabel(Object o) {
		if(tag.getType() == CVSTag.DATE){
			Date date = tag.asDate();
			if (date != null){
				return CVSTagElement.toDisplayString(date);
			}
		}
		return tag.getName();
	}
	@Override
	public Object getParent(Object o) {
		return parent;
	}
	public CVSTag getTag() {
		return tag;
	}
	
	@Override
	public int hashCode() {
		return tag.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof TagElement) {
			return tag.equals(((TagElement)obj).getTag());
		}
		return super.equals(obj);
	}
}
