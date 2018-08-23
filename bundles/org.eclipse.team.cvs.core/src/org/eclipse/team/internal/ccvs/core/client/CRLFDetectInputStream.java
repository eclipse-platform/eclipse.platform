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

import java.io.*;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.internal.ccvs.core.*;

/**
 * Stream which detects CRLF in text file contents recieved from the server
 */
public class CRLFDetectInputStream extends FilterInputStream {

	private boolean previousCR;
	private String filename;
	private boolean reported = false;

	protected CRLFDetectInputStream(InputStream in, ICVSStorage file) {
		super(in);
		try {
			this.filename = getFileName(file);
		} catch (CVSException e) {
			this.filename = file.getName();
		}
	}

	private String getFileName(ICVSStorage storage) throws CVSException {
		String fileName;
		if (storage instanceof ICVSFile) {
			ICVSFile file = (ICVSFile)storage;
			fileName = file.getRepositoryRelativePath();
			if (fileName == null) {
				IResource resource = file.getIResource();
				if (resource == null) {
					fileName = file.getName();
				} else {
					// Use the resource path if there is one since the remote pat
					fileName = file.getIResource().getFullPath().toString();
				}
			}
		} else {
			fileName = storage.getName();
		}
		return fileName;
	}

	/**
	 * Wraps the underlying stream's method.
	 * Translates CR/LF sequences to LFs transparently.
	 * @throws InterruptedIOException if the operation was interrupted before all of the
	 *         bytes specified have been skipped, bytesTransferred will be zero
	 * @throws IOException if an i/o error occurs
	 */
	public int read() throws IOException {
		int next = in.read();
		if (next != -1) {
			testForCRLF((byte)next);
		}
		return next;
	}

	/**
	 * Wraps the underlying stream's method.
	 * Translates CR/LF sequences to LFs transparently.
	 * @throws InterruptedIOException if the operation was interrupted before all of the
	 *         bytes specified have been skipped, bytesTransferred may be non-zero
	 * @throws IOException if an i/o error occurs
	 */
	public int read(byte[] buffer, int off, int len) throws IOException {
		int count = super.read(buffer, off, len);
		for (int i = off; i < count; i++) {
			testForCRLF(buffer[i]);
		}
		return count;
	}
	
	/**
	 * Test the byte to see if a CRLF sequence was read
	 */
	private void testForCRLF(byte next) {
		if (reported) return;
		if (previousCR && next == '\n') {
			CVSProviderPlugin.log(IStatus.WARNING, NLS.bind(CVSMessages.CRLFDetectInputStream_0, new String[] { filename }), null); 
			reported = true;
		}
		previousCR = (next == '\r');
	}
}
