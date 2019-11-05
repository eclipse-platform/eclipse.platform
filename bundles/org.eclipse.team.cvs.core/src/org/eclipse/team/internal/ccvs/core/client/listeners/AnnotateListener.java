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
package org.eclipse.team.internal.ccvs.core.client.listeners;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.client.CommandOutputListener;

public class AnnotateListener extends CommandOutputListener {

/**
 * Handle output from the CVS Annotate command.
 */	
	ByteArrayOutputStream aStream = new ByteArrayOutputStream();
	List<CVSAnnotateBlock> blocks = new ArrayList<>();
	int lineNumber;
	
	public IStatus messageLine(String line, ICVSRepositoryLocation location, ICVSFolder commandRoot, IProgressMonitor monitor) {
		String error = null;
		CVSAnnotateBlock aBlock = new CVSAnnotateBlock(line, lineNumber++);
		if (!aBlock.isValid()) {
			error = line;
		}
		
		/**
		 * Make sure all lines have a line terminator.
		 */
		try {
			aStream.write(line.substring(aBlock.getSourceOffset()).getBytes());
			if (!(line.endsWith("\r") || line.endsWith("\r\n"))) { //$NON-NLS-1$ //$NON-NLS-2$
				aStream.write(System.getProperty("line.separator").getBytes()); //$NON-NLS-1$
			}
		} catch (IOException e) {
		}
		add(aBlock);
		if (error != null)
			return new CVSStatus(IStatus.ERROR, CVSStatus.ERROR_LINE_PARSE_FAILURE, error, commandRoot);
		return OK;
	}
	
	public InputStream getContents() {
		return new ByteArrayInputStream(aStream.toByteArray());
	}
	
	public List getCvsAnnotateBlocks() {
		return blocks;
	}
	/**
	 * Add an annotate block to the receiver merging this block with the
	 * previous block if it is part of the same change.
	 * @param aBlock
	 */
	private void add(CVSAnnotateBlock aBlock) {
		
		int size = blocks.size();
		if (size == 0) {
			blocks.add(aBlock);
		} else {
			CVSAnnotateBlock lastBlock = blocks.get(size - 1);
			if (lastBlock.getRevision().equals(aBlock.getRevision())) {
				lastBlock.setEndLine(aBlock.getStartLine());
			} else {
				blocks.add(aBlock);
			}
		}
	}

	@Override
	public IStatus errorLine(String line, ICVSRepositoryLocation location, ICVSFolder commandRoot, IProgressMonitor monitor) {
		if(line.startsWith(CVSMessages.AnnotateListener_3)) { 
			String error = CVSMessages.AnnotateListener_4; 
			return new CVSStatus(IStatus.ERROR, CVSStatus.SERVER_ERROR, error, commandRoot);
		}
		return super.errorLine(line, location, commandRoot, monitor);
	}

	/**
	 * Set the contents of the listener to the provided contents.
	 * This is done if the contents fetched by the annotate command
	 * has a charater set that may have been mangled by the transfer
	 * @param remoteContents the actual contens of the file
	 */
	public void setContents(InputStream remoteContents) {
		try {
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			int n = remoteContents.read(buffer);
			while (n != -1) {
				stream.write(buffer, 0, n);
				n = remoteContents.read(buffer);
			}
			aStream = stream;
		} catch (IOException e) {
			// Log and continue
			CVSProviderPlugin.log(CVSException.wrapException(e));
		}
	}
}
