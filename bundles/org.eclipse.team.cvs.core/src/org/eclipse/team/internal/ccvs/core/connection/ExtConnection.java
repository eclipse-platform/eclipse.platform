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
package org.eclipse.team.internal.ccvs.core.connection;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.IServerConnection;
import org.eclipse.team.internal.ccvs.core.util.Util;
import org.eclipse.team.internal.core.streams.PollingInputStream;
import org.eclipse.team.internal.core.streams.PollingOutputStream;
import org.eclipse.team.internal.core.streams.TimeoutInputStream;
import org.eclipse.team.internal.core.streams.TimeoutOutputStream;

/**
 * Implements a connection method which invokes an external tool to 
 * establish the connection to the cvs server. Authentication and starting
 * of the cvs server are the responsibility of the external connection
 * tool.
 */
public class ExtConnection implements IServerConnection {

	// cvs format for the repository (e.g. :extssh:user@host:/home/cvs/repo)
	private ICVSRepositoryLocation location;
	private String password;

	// incoming from remote host
	InputStream inputStream;

	// outgoing to remote host
	OutputStream outputStream;
	
	// Process spawn to run the command
	Process process;
	
	protected ExtConnection(ICVSRepositoryLocation location, String password) {
		this.location = location;
		this.password = password;
	}
	
	/**
	 * Closes the connection.
	 */
	public void close() throws IOException {
		try {
			if (inputStream != null) inputStream.close();
		} finally {
			inputStream = null;
			try {
				if (outputStream != null) outputStream.close();
			} finally {
				outputStream = null;
				if (process != null) process.destroy();
			}
		}
	}
	
	/**
	 * Returns the <code>InputStream</code> used to read data from the
	 * server.
	 */
	public InputStream getInputStream() {
		return inputStream;
	}
	
	/**
	 * Returns the <code>OutputStream</code> used to send data to the
	 * server.
	 */
	public OutputStream getOutputStream() {
		return outputStream;
	}
	
	/**
	 * Opens the connection and invokes cvs in server mode.
	 *
	 * @see Connection#open()
	 */
	public void open(IProgressMonitor monitor) throws IOException {
		String[] command = ((CVSRepositoryLocation)location).getExtCommand(password);
		boolean connected = false;
		try {
			process = Util.createProcess(command, monitor);

			inputStream = new PollingInputStream(new TimeoutInputStream(process.getInputStream(),
				8192 /*bufferSize*/, 1000 /*readTimeout*/, -1 /*closeTimeout*/), location.getTimeout(), monitor);
			outputStream = new PollingOutputStream(new TimeoutOutputStream(process.getOutputStream(),
				8192 /*buffersize*/, 1000 /*writeTimeout*/, 1000 /*closeTimeout*/), location.getTimeout(), monitor);

			// XXX need to do something more useful with stderr
			connected = true;
		} finally {
			if (! connected) {
				try {
					close();
				} finally {
					// Ignore any exceptions during close
				}
			}
		}
	}
}
