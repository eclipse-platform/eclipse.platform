/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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

import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.Locale;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.resources.RemoteFolder;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.internal.ccvs.ui.operations.FetchMembersOperation;
import org.eclipse.team.internal.ccvs.ui.operations.FetchMembersOperation.RemoteFolderFilter;
import org.eclipse.ui.progress.IDeferredWorkbenchAdapter;
import org.eclipse.ui.progress.IElementCollector;

import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.TimeZone;

public class CVSTagElement extends CVSModelElement implements IDeferredWorkbenchAdapter {
	CVSTag tag;
	ICVSRepositoryLocation root;

	private static final String REPO_VIEW_LONG_FORAMT = "dd MMM yyyy HH:mm:ss"; //$NON-NLS-1$
	private static final String REPO_VIEW_SHORT_FORMAT = "dd MMM yyyy"; //$NON-NLS-1$
	private static final String TIME_ONLY_COLUMN_FORMAT = "HH:mm:ss"; //$NON-NLS-1$
	private static SimpleDateFormat localLongFormat = new SimpleDateFormat(REPO_VIEW_LONG_FORAMT,Locale.getDefault());
	private static SimpleDateFormat localShortFormat = new SimpleDateFormat(REPO_VIEW_SHORT_FORMAT,Locale.getDefault());
	private static DateFormat localLongDateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.MEDIUM);
	private static DateFormat localShortDateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);
	private static DateFormat localYearDateFormat = DateFormat.getInstanceForSkeleton(DateFormat.YEAR);
	private static SimpleDateFormat timeColumnFormat = new SimpleDateFormat(TIME_ONLY_COLUMN_FORMAT, Locale.getDefault());

	static synchronized public String toDisplayString(Date date){
		String year = localYearDateFormat.format(date);
		boolean permitSimpleDateFormat = true;
		try {
			Integer.parseInt(year);			
		} catch (NumberFormatException e) {
			// This can happen if the Year is not a number but also includes an Era eg: JapaneseCalendar
			permitSimpleDateFormat = false;
		}
		DateFormat selectedFormat = localLongFormat;
		if (!permitSimpleDateFormat) {
			selectedFormat = localLongDateFormat;
		}
		String localTime = timeColumnFormat.format(date);
		timeColumnFormat.setTimeZone(TimeZone.getDefault());
		if(localTime.equals("00:00:00")){ //$NON-NLS-1$
			if (permitSimpleDateFormat) {
				selectedFormat = localShortFormat;
			} else {
				selectedFormat = localShortDateFormat;
			}
		}
		return selectedFormat.format(date);
	}
	
	public CVSTagElement(CVSTag tag, ICVSRepositoryLocation root) {
		this.tag = tag;
		this.root = root;
	}

	public ICVSRepositoryLocation getRoot() {
		return root;
	}

	public CVSTag getTag() {
		return tag;
	}

	public boolean equals(Object o) {
		if (!(o instanceof CVSTagElement))
			return false;
		CVSTagElement t = (CVSTagElement) o;
		if (!tag.equals(t.tag))
			return false;
		return root.equals(t.root);
	}

	public int hashCode() {
		return root.hashCode() ^ tag.hashCode();
	}

	public ImageDescriptor getImageDescriptor(Object object) {
		if (!(object instanceof CVSTagElement))
			return null;
		if (tag.getType() == CVSTag.BRANCH || tag.getType() == CVSTag.HEAD) {
			return CVSUIPlugin.getPlugin().getImageDescriptor(
				ICVSUIConstants.IMG_TAG);
		} else if (tag.getType() == CVSTag.VERSION) {
			return CVSUIPlugin.getPlugin().getImageDescriptor(
				ICVSUIConstants.IMG_PROJECT_VERSION);
		} else {
			// This could be a Date tag
			return CVSUIPlugin.getPlugin().getImageDescriptor(
					ICVSUIConstants.IMG_DATE);
		}
	}
	public String getLabel(Object o) {
		if (!(o instanceof CVSTagElement))
			return null;
		CVSTag aTag = ((CVSTagElement) o).tag;
		if(aTag.getType() == CVSTag.DATE){
			Date date = tag.asDate();
			if (date != null){
				return toDisplayString(date);
			}
		}
		return aTag.getName();
	}
	
	public String toString() {
		return tag.getName();
	}
	
	public Object getParent(Object o) {
		if (!(o instanceof CVSTagElement))
			return null;
		return ((CVSTagElement) o).root;
	}

	protected Object[] fetchChildren(Object o, IProgressMonitor monitor) throws TeamException {
		ICVSRemoteResource[] children = CVSUIPlugin.getPlugin().getRepositoryManager().getFoldersForTag(root, tag, monitor);
		if (getWorkingSet() != null)
			children = CVSUIPlugin.getPlugin().getRepositoryManager().filterResources(getWorkingSet(), children);
		return children;
	}

	public void fetchDeferredChildren(Object o, IElementCollector collector, IProgressMonitor monitor) {
		if (tag.getType() == CVSTag.HEAD || tag.getType() == CVSTag.DATE) {
			try {
				monitor = Policy.monitorFor(monitor);
				RemoteFolder folder = new RemoteFolder(null, root, ICVSRemoteFolder.REPOSITORY_ROOT_FOLDER_NAME, tag);
				monitor.beginTask(NLS.bind(CVSUIMessages.RemoteFolderElement_fetchingRemoteChildren, new String[] { root.toString() }), 100); 
				FetchMembersOperation operation = new FetchMembersOperation(null, folder, collector);
				operation.setFilter(new RemoteFolderFilter() {
					public ICVSRemoteResource[] filter(ICVSRemoteResource[] folders) {
						return CVSUIPlugin.getPlugin().getRepositoryManager().filterResources(getWorkingSet(), folders);
					}
				});
				operation.run(Policy.subMonitorFor(monitor, 100));
			} catch (final InvocationTargetException e) {
				handle(collector, e);
			} catch (InterruptedException e) {
				// Cancelled by the user;
			} finally {
				monitor.done();
			}
		} else {
			try {
				collector.add(fetchChildren(o, monitor), monitor);
			} catch (TeamException e) {
			    handle(collector, e);
			}
		}
	}

    public ISchedulingRule getRule(Object element) {
		return new RepositoryLocationSchedulingRule(root); 
	}
	
	public boolean isContainer() {
		return true;
	}
}
