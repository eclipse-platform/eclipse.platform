/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.mappings;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.team.internal.ccvs.core.mapping.CVSCheckedInChangeSet;
import org.eclipse.team.internal.core.subscribers.ActiveChangeSet;
import org.eclipse.team.internal.core.subscribers.ChangeSet;
import org.eclipse.team.internal.ui.mapping.ResourceModelSorter;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

public class ChangeSetSorter extends ResourceModelSorter {

	// Comment sorting options
	public final static int DATE = 1;
	public final static int COMMENT = 2;
	public final static int USER = 3;
	private ISynchronizePageConfiguration configuration;
	
	public ChangeSetSorter() {
		super();
	}

	@Override
	public int compare(Viewer viewer, Object o1, Object o2) {
		//have to deal with non-resources in navigator
		//if one or both objects are not resources, returned a comparison 
		//based on class.
		if (o1 instanceof  ChangeSet && o2 instanceof ChangeSet) {
			ChangeSet s1 = (ChangeSet) o1;
			ChangeSet s2 = (ChangeSet) o2;
			if (s1 instanceof ActiveChangeSet && s2 instanceof ActiveChangeSet) {
				return compareNames(((ActiveChangeSet)s1).getTitle(), ((ActiveChangeSet)s2).getTitle());
			}
			if (s1 instanceof CVSCheckedInChangeSet && s2 instanceof CVSCheckedInChangeSet) {
				CVSCheckedInChangeSet r1 = (CVSCheckedInChangeSet)s1;
				CVSCheckedInChangeSet r2 = (CVSCheckedInChangeSet)s2;
				if (getCommentCriteria() == DATE)
					return r1.getDate().compareTo(r2.getDate());
				else if (getCommentCriteria() == COMMENT)
					return compareNames(r1.getComment(), r2.getComment());
				else if (getCommentCriteria() == USER)
					return compareNames(r1.getAuthor(), r2.getAuthor());
				else
					return 0;
			}
			if (s1 instanceof ActiveChangeSet) {
				return -1;
			} else if (s2 instanceof ActiveChangeSet) {
				return 1;
			}
			if (s1 instanceof CVSCheckedInChangeSet) {
				return -1;
			} else if (s2 instanceof CVSCheckedInChangeSet) {
				return 1;
			}
		}
		return super.compare(viewer, o1, o2);
	}

	private int compareNames(String s1, String s2) {
		return collator.compare(s1, s2);
	}
	
	public int getCommentCriteria() {
		return ChangeSetActionProvider.getSortCriteria(configuration);
	}

	public void setConfiguration(ISynchronizePageConfiguration configuration) {
		this.configuration = configuration;
	}
}
