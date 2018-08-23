/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.ui.repo;


import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.ui.model.*;
import org.eclipse.ui.progress.PendingUpdateAdapter;

public class RepositoryComparator extends ViewerComparator {
	
	/**
	 * Default sorting order, by label.
	 */
	public static final int ORDER_BY_LABEL = 0;

	public static final int ORDER_BY_LOCATION = 1;
	
	public static final int ORDER_BY_HOST = 2;

	private int orderBy = ORDER_BY_LABEL;
	
	private boolean ascending = true;
	
	public RepositoryComparator(int order, boolean ascending) {
		super();
		this.orderBy = order;
		this.ascending = ascending;
	}
	
	public RepositoryComparator() {
		super();
	}
	
	public int getOrderBy() {
		return orderBy;
	}
	
	public void setOrder(int orderBy) {
		this.orderBy = orderBy;
	}
	
	public void setAscending(boolean ascending) {
		this.ascending = ascending;
	}
	
	public boolean isAscending() {
		return ascending;
	}

	public int category(Object element) {
		if (element instanceof ICVSRemoteFolder) {
			if (((ICVSRemoteFolder)element).isDefinedModule()) {
				return 7;
			}
			return 1;
		}
		if (element instanceof RemoteModule) {
			ICVSRemoteResource resource = ((RemoteModule)element).getCVSResource();
			if (resource instanceof ICVSRemoteFolder) {
				ICVSRemoteFolder folder = (ICVSRemoteFolder) resource;
				if (folder.isDefinedModule()) {
					return 7;
				}
			}
			return 1;
		}
		if (element instanceof ICVSRemoteFile) {
			return 2;
		}
		if (element instanceof CVSTagElement) {
			CVSTagElement tagElement = (CVSTagElement)element;
			if (tagElement.getTag().getType() == CVSTag.HEAD) {
				return 0;
			} else if (tagElement.getTag().getType() == CVSTag.BRANCH) {
				return 4;
			} else if (tagElement.getTag().getType() == CVSTag.VERSION) {
				return 5;
			} else if (tagElement.getTag().getType() == CVSTag.DATE){
				return 6;
			}else{
				return 7;
			}
		}
		if (element instanceof BranchCategory) {
			return 4;
		}
		if (element instanceof VersionCategory) {
			return 5;
		} 
		if (element instanceof DateTagCategory){
			return 6;
		}
		if (element instanceof PendingUpdateAdapter) {
			return 10000;
		}
		return 0;
	}

	public int compare(Viewer viewer, Object o1, Object o2) {
		int cat1 = category(o1);
		int cat2 = category(o2);
		if (cat1 != cat2) return cat1 - cat2;
		
		if (o1 instanceof CVSTagElement && o2 instanceof CVSTagElement) {
			CVSTag tag1 = ((CVSTagElement)o1).getTag();
			CVSTag tag2 = ((CVSTagElement)o2).getTag();
			if (tag1.getType() == CVSTag.BRANCH) {
				return tag1.compareTo(tag2);
			} else {
				return -1 * tag1.compareTo(tag2);
			}
		}
		
		// Sort versions in reverse alphabetical order
		if (o1 instanceof ICVSRemoteFolder && o2 instanceof ICVSRemoteFolder) {
			ICVSRemoteFolder f1 = (ICVSRemoteFolder)o1;
			ICVSRemoteFolder f2 = (ICVSRemoteFolder)o2;
			if (f1.getName().equals(f2.getName())) {
				return compare(f1, f2);
			}
		}
		
		if (o1 instanceof ICVSRepositoryLocation && o2 instanceof ICVSRepositoryLocation) {
			return ((ICVSRepositoryLocation)o1).getLocation(false).compareTo(((ICVSRepositoryLocation)o2).getLocation(false));
		}
		
		if (o1 instanceof RepositoryRoot && o2 instanceof RepositoryRoot) {
			RepositoryRoot rr1 = (RepositoryRoot) o1;
			RepositoryRoot rr2 = (RepositoryRoot) o2;
			
			// use repository location strings to compare RepositoryRoots
			ICVSRepositoryLocation rl1 = rr1.getRoot();
			ICVSRepositoryLocation rl2 = rr2.getRoot();
			
			int compareResult = 0;
			switch (orderBy) {
			case ORDER_BY_HOST:
				compareResult = rl1.getHost().compareTo(rl2.getHost());
				if (compareResult != 0)
					break;
			case ORDER_BY_LOCATION:
				compareResult = rl1.getLocation(false).compareTo(
						rl2.getLocation(false));
				if (compareResult != 0)
					break;
				// add other cases here
			case ORDER_BY_LABEL:
				// for default order use super.compare (i.e. compare labels)
			default:
				compareResult = super.compare(viewer, o1, o2);
			}
			return ascending ? compareResult : -compareResult;
		}
		
		return super.compare(viewer, o1, o2);
	}

	/*
	 * Compare to remote folders whose names are the same.
	 */
	private int compare(ICVSRemoteFolder f1, ICVSRemoteFolder f2) {
		CVSTag tag1 = f1.getTag();
		CVSTag tag2 = f2.getTag();
		if (tag1 == null) return 1;
		if (tag2 == null) return -1;
		return tag2.compareTo(tag1);
	}
	
}

