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
 *     Olexiy Buyanskyy <olexiyb@gmail.com> - Bug 76386 - [History View] CVS Resource History shows revisions from all branches
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.core.client.listeners;

 
import java.util.Date;

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.resources.RemoteFile;

public class LogEntry extends PlatformObject implements ILogEntry {

	private RemoteFile file;
	private String author;
	private Date date;
	private String comment;
	private String state;
	private CVSTag[] tags;
	private CVSTag[] branches;
	private String[] revisions;
	
	/*
	 * Flatten the text in the multi-line comment
	 */
	public static String flattenText(String string) {
		StringBuilder buffer = new StringBuilder(string.length() + 20);
		boolean skipAdjacentLineSeparator = true;
		for (int i = 0; i < string.length(); i++) {
			char c = string.charAt(i);
			if (c == '\r' || c == '\n') {
				if (!skipAdjacentLineSeparator)
					buffer.append(CVSMessages.LogEntry_0); 
				skipAdjacentLineSeparator = true;
			} else {
				buffer.append(c);
				skipAdjacentLineSeparator = false;
			}
		}
		return buffer.toString();
	}
	
	public LogEntry(RemoteFile file, String revision, String author, Date date, String comment, String state, CVSTag[] tags, CVSTag[] branches) {
		this.file = file.toRevision(revision);
		this.author = author;
		this.date = date;
		this.comment = comment;
		this.state = state;
		this.tags = tags;
		this.branches = branches;
	}
	
	public LogEntry(RemoteFile file, String revision, String author, Date date, String comment, String state, CVSTag[] tags, CVSTag[] branches, String[] revisions) {
		this(file,revision,author,date,comment,state,tags,branches);
		this.revisions=revisions;
	}

	@Override
	public String getRevision() {
		return file.getRevision();
	}

	@Override
	public String getAuthor() {
		return author;
	}

	@Override
	public Date getDate() {
		return date;
	}

	@Override
	public String getComment() {
		return comment;
	}

	@Override
	public String getState() {
		return state;
	}

	@Override
	public CVSTag[] getBranches() {
		CVSTag[] result = new CVSTag[branches.length];
		System.arraycopy(branches, 0, result, 0, branches.length);
		return result;
	}

	@Override
	public CVSTag[] getTags() {
		CVSTag[] result = new CVSTag[tags.length];
		System.arraycopy(tags, 0, result, 0, tags.length);
		return result;
	}

	@Override
	public ICVSRemoteFile getRemoteFile() {
		return file;
	}
	
	@Override
	public boolean isDeletion() {
		return getState().equals("dead"); //$NON-NLS-1$
	}

	/**
	 * In the case where files on a branch haven't been modified since their
	 * initial branch point, they keep the revision number of their predecessor.
	 * In this case no revision info will be displayed while doing a log, so all
	 * branch revision numbers are recorded. This allows the user to pick which
	 * revision they are interested in.
	 * 
	 * @return an array of branch revision strings or an empty array if no
	 *         branch revisions were recorded
	 */
	public String[] getBranchRevisions(){
		
		if (revisions != null)
			return revisions;
		
		return new String[0];
	}
}

