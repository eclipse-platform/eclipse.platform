/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.synchronize;

import java.util.Date;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.team.internal.core.subscribers.ActiveChangeSet;
import org.eclipse.team.internal.core.subscribers.ChangeSet;
import org.eclipse.team.internal.core.subscribers.CheckedInChangeSet;
import org.eclipse.team.ui.synchronize.ISynchronizeModelElement;

/**
 * Sorter for the change log model provider.
 *
 * @since 3.0
 */
public class ChangeSetModelComparator extends ViewerComparator {

	private int commentCriteria;
	private ChangeSetModelProvider provider;

	// Comment sorting options
	public final static int DATE = 1;
	public final static int COMMENT = 2;
	public final static int USER = 3;

	public ChangeSetModelComparator(ChangeSetModelProvider provider, int commentCriteria) {
		this.provider = provider;
		this.commentCriteria = commentCriteria;
	}

	protected int classComparison(Object element) {
		if (element instanceof ChangeSetDiffNode) {
			ChangeSet set = ((ChangeSetDiffNode)element).getSet();
			if (set instanceof ActiveChangeSet) {
				return 0;
			}
			return 1;
		}
		return 2;
	}

	protected int compareClass(Object element1, Object element2) {
		return classComparison(element1) - classComparison(element2);
	}

	protected int compareNames(String s1, String s2) {
		return getComparator().compare(s1, s2);
	}

	private int compareDates(Date d1, Date d2) {
		if (d1 == null)
			d1 = new Date(0);
		if (d2 == null)
			d2 = new Date(0);
		return d1.compareTo(d2);
	}

	@Override
	public int compare(Viewer viewer, Object o1, Object o2) {
		//have to deal with non-resources in navigator
		//if one or both objects are not resources, returned a comparison
		//based on class.
		if (o1 instanceof  ChangeSetDiffNode && o2 instanceof ChangeSetDiffNode) {
			ChangeSet s1 = ((ChangeSetDiffNode) o1).getSet();
			ChangeSet s2 = ((ChangeSetDiffNode) o2).getSet();
			if (s1 instanceof ActiveChangeSet && s2 instanceof ActiveChangeSet) {
				return compareNames(((ActiveChangeSet)s1).getTitle(), ((ActiveChangeSet)s2).getTitle());
			}
			if (s1 instanceof CheckedInChangeSet && s2 instanceof CheckedInChangeSet) {
				CheckedInChangeSet r1 = (CheckedInChangeSet)s1;
				CheckedInChangeSet r2 = (CheckedInChangeSet)s2;
				if (commentCriteria == DATE)
					return compareDates(r1.getDate(), r2.getDate());
				else if (commentCriteria == COMMENT)
					return compareNames(r1.getComment(), r2.getComment());
				else if (commentCriteria == USER)
					return compareNames(r1.getAuthor(), r2.getAuthor());
				else
					return 0;
			}
			if (s1 instanceof ActiveChangeSet) {
				return -1;
			} else if (s2 instanceof ActiveChangeSet) {
				return 1;
			}
			if (s1 instanceof CheckedInChangeSet) {
				return -1;
			} else if (s2 instanceof CheckedInChangeSet) {
				return 1;
			}
		}

		if (o1 instanceof ISynchronizeModelElement && o2 instanceof ISynchronizeModelElement) {
			ViewerComparator embeddedSorter = provider.getEmbeddedComparator();
			if (embeddedSorter != null) {
				return embeddedSorter.compare(viewer, o1, o2);
			} else {
				return compareNames(((ISynchronizeModelElement)o1).getName(), ((ISynchronizeModelElement)o2).getName());
			}
		} else if (o1 instanceof ISynchronizeModelElement)
			return 1;
		else if (o2 instanceof ISynchronizeModelElement)
			return -1;

		return 0;
	}

	public int getCommentCriteria() {
		return commentCriteria;
	}
}
