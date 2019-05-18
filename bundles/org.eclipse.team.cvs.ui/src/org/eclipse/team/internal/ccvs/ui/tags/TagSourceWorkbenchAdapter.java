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

import java.util.ArrayList;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.ui.model.IWorkbenchAdapter;

/**
 * A workbench adapter for a tag source that creates a model
 * for displaying the tags from a tag source in a tree or table 
 * viewer. The workbench adapter is not a singleton since it needs
 * to be configured to display certain types of tags.
 */
public class TagSourceWorkbenchAdapter implements IAdaptable, IWorkbenchAdapter {

	/**
	 * Constants for configuring which types of tags should be displayed.
	 */
	public static final int INCLUDE_HEAD_TAG = 1;
	public static final int INCLUDE_BASE_TAG = 2;
	public static final int INCLUDE_BRANCHES = 4;
	public static final int INCLUDE_VERSIONS = 8;
	public static final int INCLUDE_DATES = 16;
	public static final int INCLUDE_ALL_TAGS = INCLUDE_HEAD_TAG | INCLUDE_BASE_TAG | INCLUDE_BRANCHES | INCLUDE_VERSIONS | INCLUDE_DATES;
	
	TagRootElement branches;
	TagRootElement versions;
	TagRootElement dates;
	int includeFlags;
	
	public static class ProjectElementComparator extends ViewerComparator {
	
		/*
		 * The order in the diaog should be HEAD, Branches, Versions, Dates, BASE
		 */
		@Override
		public int category(Object element) {
			if (element instanceof TagElement) {
				CVSTag tag = ((TagElement)element).getTag();
				if (tag == CVSTag.DEFAULT) return 1;
				if (tag == CVSTag.BASE) return 5;
				if (tag.getType() == CVSTag.BRANCH) return 2;
				if (tag.getType() == CVSTag.VERSION) return 3;
				if (tag.getType() == CVSTag.DATE) return 4;
			} else if (element instanceof TagRootElement) {
				if(((TagRootElement)element).getTypeOfTagRoot() == CVSTag.BRANCH) return 2;
				if(((TagRootElement)element).getTypeOfTagRoot() == CVSTag.VERSION) return 3;
				if(((TagRootElement)element).getTypeOfTagRoot() == CVSTag.DATE) return 4;
			}
			return 0;
		}
		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			int cat1 = category(e1);
			int cat2 = category(e2);
			if (cat1 != cat2) return cat1 - cat2;
			// Sort version tags in reverse order
			if (e1 instanceof TagElement){
				CVSTag tag1 = ((TagElement)e1).getTag();
				int type = tag1.getType();
				if(type == CVSTag.VERSION) {
					return -1 * super.compare(viewer, e1, e2);
				}else if(type == CVSTag.DATE){
					return -1*tag1.compareTo(((TagElement)e2).getTag());
				}
			}
			return super.compare(viewer, e1, e2);
		}
	}

	
	/**
	 * Create a viewer input for the tag source
	 * @param tagSource the tag source
	 * @param includeFlags the types of tags to include
	 * @return a tree viewer input
	 */
	public static Object createInput(TagSource tagSource, int includeFlags) {
		if (includeFlags == INCLUDE_VERSIONS) {
			// Versions only is requested by the merge start page.
			// Only need to show version tags
			return new TagRootElement(null, tagSource, CVSTag.VERSION);
		}
		return new TagSourceWorkbenchAdapter(tagSource, includeFlags);
	}
	
	public TagSourceWorkbenchAdapter(TagSource tagSource, int includeFlags) {
		this.includeFlags = includeFlags;
		if (this.includeFlags == 0) this.includeFlags = INCLUDE_ALL_TAGS;
		if ((includeFlags & INCLUDE_BRANCHES) > 0) {	
			branches = new TagRootElement(this, tagSource, CVSTag.BRANCH);
		}
		if ((includeFlags & INCLUDE_VERSIONS) > 0) {
			versions = new TagRootElement(this, tagSource, CVSTag.VERSION);
		}
		if ((includeFlags & INCLUDE_DATES) > 0) {
			dates = new TagRootElement(this, tagSource, CVSTag.DATE);
		}
	}
	
	@Override
	public Object[] getChildren(Object o) {
		ArrayList<IAdaptable> children = new ArrayList<>(4);
		if ((includeFlags & INCLUDE_HEAD_TAG) > 0) {
			children.add(new TagElement(this, CVSTag.DEFAULT));
		}
		if ((includeFlags & INCLUDE_BASE_TAG) > 0) {
			children.add(new TagElement(this, CVSTag.BASE));
		}
		if ((includeFlags & INCLUDE_BRANCHES) > 0) {
			children.add(branches);
		}
		if ((includeFlags & INCLUDE_VERSIONS) > 0) {
			children.add(versions);
		}
		if ((includeFlags & INCLUDE_DATES) > 0) {
			children.add(dates);
		}
		return children.toArray(new Object[children.size()]);
	}
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == IWorkbenchAdapter.class) return adapter.cast(this);
		return null;
	}
	@Override
	public ImageDescriptor getImageDescriptor(Object object) {
		return null;
	}
	@Override
	public String getLabel(Object o) {
		return null;
	}
	@Override
	public Object getParent(Object o) {
		return null;
	}
}
