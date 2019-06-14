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


import java.util.StringTokenizer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.Team;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.client.Command.KSubstOption;
import org.eclipse.team.internal.ccvs.core.util.FileNameMatcher;

/**
 * The ImportStructureVisitor sends the content of the folder it is
 * used on to the server. It constructs the locations of the resources
 * because the resources do not yet have a remote-location.<br>
 * It can also ignore certain files and decides wether to send
 * a file in binary or text mode due to a specification that is passed 
 * as a "wrapper" argument.
 */
class ImportStructureVisitor implements ICVSResourceVisitor {
	
	private static final String KEYWORD_OPTION = "-k"; //$NON-NLS-1$
	private static final String QUOTE = "'"; //$NON-NLS-1$
	
	protected Session session;
	protected IProgressMonitor monitor;
	
	private FileNameMatcher ignoreMatcher;
	private FileNameMatcher wrapMatcher;
	
	/**
	 * Constructor for ImportStructureVisitor.
	 * @param requestSender
	 * @param mRoot
	 * @param monitor
	 */
	public ImportStructureVisitor(Session session, 
		String[] wrappers, IProgressMonitor monitor) {

		this.session = session;
		this.monitor = Policy.infiniteSubMonitorFor(monitor, 512);
		wrapMatcher = initWrapMatcher(wrappers);
	}
	

	/**
	 * Inits the wrapMatcher, that is responsible to find out
	 * whether a file is to be send as a binary (on an import)
	 * or not.
	 * 
	 * Takes wrappers of this format:
	 *   *.class -k 'o'
	 * 
	 * and inits the FileNameMatcher to give
	 *   -ko back if you call it with match("somename.class")
	 * 
	 * ignores all wrappers, that do not contain -k
	 */
	private FileNameMatcher initWrapMatcher(String[] wrappers) {
		
		FileNameMatcher wrapMatcher;
		
		if (wrappers == null) {
			return null;
		}
		
		wrapMatcher = new FileNameMatcher();
		
		for (String wrapper : wrappers) {
			if (!wrapper.contains(KEYWORD_OPTION)) {
				continue;
			}
			StringTokenizer st = new StringTokenizer(wrapper);
			String pattern = st.nextToken();
			String option = st.nextToken();
			// get rid of the quotes
			StringTokenizer quoteSt = 
				new StringTokenizer(st.nextToken(),QUOTE);
			option += quoteSt.nextToken();
			
			wrapMatcher.register(pattern,option);
		}
		
		return wrapMatcher;
	}	
	
	@Override
	public void visitFile(ICVSFile mFile) throws CVSException {
		if (ignoreMatcher != null && ignoreMatcher.match(mFile.getName())) {
			return;
		}
		
		boolean binary = Team.getFileContentManager().getType((IFile)mFile.getIResource()) == Team.BINARY;
		if (wrapMatcher != null) {
			String mode = wrapMatcher.getMatch(mFile.getName());
			if (mode != null) binary = KSubstOption.fromMode(mode).isBinary();
		}
		session.sendModified(mFile, binary, monitor);
	}

	@Override
	public void visitFolder(ICVSFolder mFolder) throws CVSException {
		
		if (ignoreMatcher != null && ignoreMatcher.match(mFolder.getName())) {
			return;
		}
		
		String localPath = mFolder.getRelativePath(session.getLocalRoot());
		monitor.subTask(NLS.bind(CVSMessages.AbstractStructureVisitor_sendingFolder, new String[] { localPath })); 
		
		session.sendConstructedDirectory(localPath);
		mFolder.acceptChildren(this);
	}

}
