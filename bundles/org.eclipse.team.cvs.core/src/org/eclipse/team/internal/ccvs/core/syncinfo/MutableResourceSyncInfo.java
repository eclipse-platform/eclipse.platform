/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.core.syncinfo;

import java.util.Date;

import org.eclipse.core.runtime.Assert;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.client.Command.KSubstOption;

/**
 * Mutable version of ResourceSyncInfo. Can be used when either creating a resource sync
 * object from scratch (e.g. without an entry line) or want to modify an existing
 * <code>ResourceSyncInfo</code> instance. Example usage:
 * <pre>
 * ResourceSyncInfo info = resource.getSyncInfo();
 * if(info!=null) {
 *   MutableResourceSyncInfo newInfo = info.cloneMutable();
 *   newInfo.setRevision("1.22");
 *   resource.setSyncInfo(newInfo);
 * }
 * </pre>
 * @see ResourceSyncInfo
 */
public class MutableResourceSyncInfo extends ResourceSyncInfo {
	
	protected MutableResourceSyncInfo(ResourceSyncInfo info) {
		this.name = info.getName();
		setRevision(info.getRevision());
		setTag(info.getTag());
		this.timeStamp = info.getTimeStamp();
		this.isDirectory = info.isDirectory();
		this.keywordMode = info.getKeywordMode();
		this.isDeleted = info.isDeleted();
		if(info.isMergedWithConflicts()) {
			setSyncType(TYPE_MERGED_WITH_CONFLICTS);
		} else if(info.isMerged()) {
			setSyncType(TYPE_MERGED);
		} else {
			setSyncType(TYPE_REGULAR);
		}
	}
	
	/**
	 * Creates a default sync info, if revision is <code>null</code> then
	 * the sync info will be considered in the newly added state.
	 */ 
	public MutableResourceSyncInfo(String name, String revision) {
		Assert.isNotNull(name);
		this.name = name;
		setRevision(revision);
	}
	
	/**
	 * Sets the revision.
	 * @param revision The revision to set
	 */
	public void setRevision(String revision) {
		super.setRevision(revision);
	}
	
	/**
	 * Sets the timeStamp.
	 * @param timeStamp The timeStamp to set
	 */
	public void setTimeStamp(Date timeStamp) {
		this.timeStamp = timeStamp;
	}
	
	/**
	 * Sets the timeStamp.
	 * @param timeStamp The timeStamp to set
	 */
	public void setTimeStamp(Date timeStamp, boolean clearMerged) {
		setTimeStamp(timeStamp);
		if (clearMerged) setSyncType(TYPE_REGULAR);
	}
	
	/**
	 * Sets the keywordMode.
	 * @param keywordMode The keywordMode to set
	 */
	public void setKeywordMode(KSubstOption keywordMode) {
		this.keywordMode = keywordMode;
	}

	/**
	 * Sets the tag.
	 * @param tag The tag to set
	 */
	public void setTag(CVSTag tag) {
		super.setTag(tag);
	}
	
	/**
	 * Sets the deleted state.
	 * @param isDeleted The deleted state of this resource sync
	 */
	public void setDeleted(boolean isDeleted) {
		this.isDeleted = isDeleted;;
	}
	
	/**
	 * Sets to the added state. The timestamp and other are cleared.
	 */
	public void setAdded() {
		setRevision(ADDED_REVISION);
	}
	
	/**
	 * Sets that this resource sync is a result of a non-conflicting merge
	 */
	public void setMerged() {
		// if already merged state then ignore
		if(syncType==TYPE_REGULAR) {			
			this.syncType = TYPE_MERGED;
		}
	}
	
	@Override
	public void setEntryLine(String entryLine) throws CVSException {
		super.setEntryLine(entryLine);
	}
}
