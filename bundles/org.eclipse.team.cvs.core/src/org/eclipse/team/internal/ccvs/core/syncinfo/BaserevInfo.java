/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.util.Util;

/**
 * This class represents the information in the CVS/Baserev file
 */
public class BaserevInfo {
	private static final String BASEREV_PREFIX = "B"; //$NON-NLS-1$
	
	private String name;
	private String revision;
	
	public BaserevInfo(String entryLine) throws CVSException {
		setEntryLine(entryLine);
	}

	public BaserevInfo(String name, String revision) {
		this.name = name;
		this.revision = revision;
	}
	/**
	 * Return the entry line as it appears in the CVS/Baserev file
	 * @return String
	 */
	public String getEntryLine() {
		StringBuilder result = new StringBuilder();
		result.append(BASEREV_PREFIX);
		result.append(name);
		result.append(ResourceSyncInfo.SEPARATOR);
		result.append(revision);
		result.append(ResourceSyncInfo.SEPARATOR);
		return result.toString();
	}	
	private void setEntryLine(String entryLine) throws CVSException {
		if(entryLine.startsWith(BASEREV_PREFIX)) {
			entryLine = entryLine.substring(1);
		}
		String[] strings = Util.parseIntoSubstrings(entryLine, ResourceSyncInfo.SEPARATOR);
		// Accept either a length of 2 or 3. If the length is 3, we ignore the last
		// string as per the CVS spec.
		if(strings.length != 2 && strings.length != 3) {
			IStatus status = new CVSStatus(IStatus.ERROR,NLS.bind(CVSMessages.BaseRevInfo_malformedEntryLine, new String[] { entryLine }));
			throw new CVSException(status); 
		}

		name = strings[0];

		if(name.length()==0) {
			IStatus status = new CVSStatus(IStatus.ERROR,NLS.bind(CVSMessages.BaseRevInfo_malformedEntryLine, new String[] { entryLine }));
			throw new CVSException(status);  
		}

		revision = strings[1];

		if(revision.length()==0) {
			IStatus status = new CVSStatus(IStatus.ERROR,NLS.bind(CVSMessages.BaseRevInfo_malformedEntryLine, new String[] { entryLine }));
			throw new CVSException(status);  
		}
	}
	/**
	 * Returns the name.
	 * @return String
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the revision.
	 * @return String
	 */
	public String getRevision() {
		return revision;
	}

}
