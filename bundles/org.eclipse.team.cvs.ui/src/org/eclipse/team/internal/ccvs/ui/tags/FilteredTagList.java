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
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.util.StringMatcher;
import org.eclipse.ui.model.IWorkbenchAdapter;

/**
 * Workbench model element that returns a filtered list of tags
 */
public class FilteredTagList implements IWorkbenchAdapter, IAdaptable {

	private final TagSource tagSource;
	private final int[] types;
	private StringMatcher matcher;

	public FilteredTagList(TagSource tagSource, int[] types) {
		this.tagSource = tagSource;
		this.types = types;
	}
	
	@Override
	public Object[] getChildren(Object o) {
		CVSTag[] tags = getTags();
		List<TagElement> filtered = new ArrayList<>();
		for (CVSTag tag : tags) {
			if (select(tag)) {
				filtered.add(new TagElement(this, tag));
			}
		}
		return filtered.toArray(new Object[filtered.size()]);
	}

	private boolean select(CVSTag tag) {
		if (matcher == null) return true;
		return matcher.match(tag.getName());
	}

	private CVSTag[] getTags() {
		return tagSource.getTags(types);
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

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == IWorkbenchAdapter.class) return adapter.cast(this);
		return null;
	}
	
	public void setPattern(String pattern) {
		if (!pattern.endsWith("*")) { //$NON-NLS-1$
			pattern += "*"; //$NON-NLS-1$
		}
		matcher = new StringMatcher(pattern, true, false);
	}

	public CVSTag[] getMatchingTags() {
		CVSTag[] tags = getTags();
		List<CVSTag> filtered = new ArrayList<>();
		for (CVSTag tag : tags) {
			if (select(tag)) {
				filtered.add(tag);
			}
		}
		return filtered.toArray(new CVSTag[filtered.size()]);
	}

}
