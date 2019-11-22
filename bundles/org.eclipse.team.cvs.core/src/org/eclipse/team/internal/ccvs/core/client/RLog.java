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
package org.eclipse.team.internal.ccvs.core.client;

import java.util.Date;

import org.eclipse.team.internal.ccvs.core.CVSTag;

/**
 * The "cvs rlog..." command
 */
public class RLog extends RemoteCommand {
	
	/*** Local options: specific to rlog ***/
	public static final LocalOption NO_TAGS = new LocalOption("-N"); //$NON-NLS-1$ 
	public static final LocalOption ONLY_INCLUDE_CHANGES = new LocalOption("-S"); //$NON-NLS-1$ 
	public static final LocalOption REVISIONS_ON_DEFAULT_BRANCH = new LocalOption("-b"); //$NON-NLS-1$ 
	public static final LocalOption LOCAL_DIRECTORY_ONLY = new LocalOption("-l"); //$NON-NLS-1$ 
	/**
	 * Makes a -r option for rlog. Here are the currently supported options:
	 * 
	 * <pre>{@code
	 * tag1   tag2 result
	 * ====== ==== =================================
	 * date   date date<date (all revisions between date and later)
	 * tag	  tag  tag:tag (all revisions between tag and tag, must be on same branch)
	 * branch date >date (all revisions of date or later)
	 * branch tag  tag: (all revisions from tag to the end of branchs tip)
	 * }</pre>
	 * Valid for: rlog
	 */
	public static LocalOption makeTagOption(CVSTag tag1, CVSTag tag2) {
		int type1 = tag1.getType();
		int type2 = tag2.getType();
		
		if(type1 == type2) {
			switch (type1) {
				case CVSTag.HEAD:
				case CVSTag.BRANCH:
					// A range of branches - all revisions on all the branches in that range.
				case CVSTag.VERSION:
					// Revisions from tag1 to tag2 (they must be on the same branch).
					return new LocalOption("-r" + tag1.getName() + ":" + tag2.getName(), null); //$NON-NLS-1$ //$NON-NLS-2$
				case CVSTag.DATE:
					// Selects revisions created between DATE1 and DATE2. If DATE1 is after DATE2, use > instead; otherwise, no log messages are retrieved.
					Date date1 = tag1.asDate();
					Date date2 = tag2.asDate();
					String operator = "<"; //$NON-NLS-1$
					if(date1.compareTo(date2) > 0) {
						operator = ">"; //$NON-NLS-1$
					}
					return new LocalOption("-d", tag1.getName() + operator + tag2.getName()); //$NON-NLS-1$
				default:
					// Unknow tag type!!!
					throw new IllegalArgumentException();
			}
		}
		
		if((type1 == CVSTag.BRANCH || type1 == CVSTag.HEAD) && type2 == CVSTag.DATE) {
			return new LocalOption("-d", ">" + tag2.getName()); //$NON-NLS-1$ //$NON-NLS-2$
		}
		
		if((type1 == CVSTag.BRANCH || type1 == CVSTag.HEAD) && type2 == CVSTag.VERSION) {
			return new LocalOption("-r" + tag2.getName() + ":", null); //$NON-NLS-1$ //$NON-NLS-2$
		}
		
		// defaults
		switch (type1) {
			case CVSTag.HEAD:
			case CVSTag.BRANCH:
				// All revisions on this branch
			case CVSTag.VERSION:
				// revisions in this tag
				return new LocalOption("-r" + tag1.getName(), null); //$NON-NLS-1$ 
			case CVSTag.DATE:
				// Revisions at this date tag
				return new LocalOption("-d", tag1.getName()); //$NON-NLS-1$ 
			default:
				// Unknow tag type!!!
				throw new IllegalArgumentException();
		}
	}
	
	/***
	 * Experimental - Used for obtaining the latest revisions on HEAD or the specified branch. 
	 * @param tag1
	 * @return the option to use
	 * 
	 * Valid for rlog
	 */
	public static LocalOption getCurrentTag(CVSTag tag1) {
		
		int type = tag1.getType();
		
		switch (type){
			case CVSTag.HEAD:
			return new LocalOption("-r"); //$NON-NLS-1$
		
			case CVSTag.BRANCH:
			return new LocalOption("-r" + tag1.getName() + "."); //$NON-NLS-1$ //$NON-NLS-2$
		
			case CVSTag.VERSION:
			return new LocalOption("-r" + tag1.getName()); //$NON-NLS-1$
			
			case CVSTag.DATE:
			return new LocalOption("-d", tag1.asDate().toString()); //$NON-NLS-1$
			default:
				// Unknow tag type!!!
				throw new IllegalArgumentException();
		}
		
	}
	
	@Override
	protected String getRequestId() {
		return "rlog"; //$NON-NLS-1$
	}
}
