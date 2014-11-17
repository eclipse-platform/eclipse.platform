/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Brock Janiczak (brockj@tpg.com.au) - Bug 144419 Avoid calculating encoding for each line read/written
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.core.connection;

 
import java.io.*;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.internal.ccvs.core.*;

/**
 * A connection to talk to a cvs server. The life cycle of a connection is
 * as follows:
 * <ul>
 *	<li> constructor: creates a new connection object that wraps the given
 *       repository location and connection method.
 *	<li> open: opens a connection.
 *	<li> send a request: use write* method or use the request stream directly.
 *	     <code>GetRequestStream</code> returns an output stream to directly
 *	     talk to the server.
 *	<li> read responses: use read* methods or use the response stream directly.
 *	     <code>GetResponseStream</code> returns an input stream to directly
 *	     read output from the server.
 *	<li> close: closes the connection. A closed connection can be reopened by
 *	     calling open again.
 * </ul>
 */
public class Connection {
	private static final byte NEWLINE= 0xA;
	
	private IServerConnection serverConnection;
	private ICVSRepositoryLocation fCVSRoot;
	private boolean fIsEstablished;
	private InputStream fResponseStream;
	private String fServerEncoding;
	private byte[] readLineBuffer = new byte[256];

	public Connection(ICVSRepositoryLocation cvsroot, IServerConnection serverConnection) {
		fCVSRoot = cvsroot;
		this.serverConnection = serverConnection;
		fServerEncoding = getEncoding(fCVSRoot);
	}
	
	private static byte[] append(byte[] buffer, int index, byte b) {
		if (index >= buffer.length) {
			byte[] newBuffer= new byte[index * 2];
			System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
			buffer= newBuffer;
		}
		buffer[index]= b;
		return buffer;
	}
	/**
	 * Closes the connection.
	 */
	public void close() {
		if (!isEstablished())
			return;
		try {
			serverConnection.close();
		} catch (IOException ex) {
			// Generally, errors on close are of no interest.
			// However, log them if debugging is on
			if (Policy.DEBUG) {
				CVSProviderPlugin.log(new CVSCommunicationException(CVSMessages.Connection_cannotClose, fCVSRoot, ex));
			}
		} finally {
			fResponseStream = null;
			fIsEstablished = false;
		}
	}
	/**
	 * Flushes the request stream.
	 */
	public void flush() throws CVSException {
		if (!isEstablished())
			return;
		try {
			getOutputStream().flush();	
		} catch(IOException e) {
			throw new CVSCommunicationException(fCVSRoot,e);
		}
	}
	
	/**
	 * Returns the <code>OutputStream</code> used to send requests
	 * to the server.
	 */
	public OutputStream getOutputStream() {
		if (!isEstablished())
			return null;
		return serverConnection.getOutputStream();
	}
	/**
	 * Returns the <code>InputStream</code> used to read responses from
	 * the server.
	 */
	public InputStream getInputStream() {
		if (!isEstablished())
			return null;
		if (fResponseStream == null)
			fResponseStream = serverConnection.getInputStream();
		return fResponseStream;	
	}

	/**
	 * Returns <code>true</code> if the connection is established;
	 * otherwise <code>false</code>.
	 */
	public boolean isEstablished() {
		return fIsEstablished;
	}

	/**
	 * Opens the connection.
	 */	
	public void open(IProgressMonitor monitor) throws CVSException {
		if (isEstablished())
			return;
		try {
			serverConnection.open(monitor);
		} catch (IOException e) {
			throw new CVSCommunicationException(NLS.bind(CVSMessages.Connection_0, new String[] { fCVSRoot.getLocation(true), CVSCommunicationException.getMessageFor(e) }), fCVSRoot, e); 
		}
		fIsEstablished= true; 
	}
	/**
	 * Reads a line from the response stream.
	 */
	public String readLine() throws CVSException {
		if (!isEstablished())
			throw new CVSCommunicationException(CVSMessages.Connection_readUnestablishedConnection,fCVSRoot,null);
		try { 
			InputStream in = getInputStream();
			int index = 0;
			int r;
			while ((r = in.read()) != -1) {
				if (r == NEWLINE) break;
				readLineBuffer = append(readLineBuffer, index++, (byte) r);
			}

			String result = new String(readLineBuffer, 0, index, fServerEncoding);
			if (Policy.isDebugProtocol()) Policy.printProtocolLine(result);
			return result;
		} catch (IOException e) {
			throw new CVSCommunicationException(fCVSRoot,e);
		}
	}
	
	static String readLine(ICVSRepositoryLocation location, InputStream in) throws IOException {
		byte[] buffer = new byte[256];
		int index = 0;
		int r;
		while ((r = in.read()) != -1) {
			if (r == NEWLINE)
				break;
			buffer = append(buffer, index++, (byte) r);
		}

		String result = new String(buffer, 0, index, getEncoding(location));
		if (Policy.isDebugProtocol())
		    Policy.printProtocolLine(result);
		return result;
	}

	//---- Helper to send strings to the server ----------------------------
	
	/**
	 * Sends the given string to the server.
	 */
	public void write(String s) throws CVSException {
        try {
			write(s.getBytes(fServerEncoding), false);
		} catch (UnsupportedEncodingException e) {
			IStatus status = new CVSStatus(IStatus.ERROR, CVSStatus.SERVER_ERROR, e.getMessage(), e, fCVSRoot);
			throw new CVSException (status);
		}
	}
	
	/**
	 * Return the encoding for the given repository location
	 * @return the encoding for the given repository location
	 */
	public static String getEncoding(ICVSRepositoryLocation location) {
		return location.getEncoding();
	}

	/**
	 * Sends the given string and a newline to the server. 
	 */
	public void writeLine(String s) throws CVSException {
		try {
			write(s.getBytes(fServerEncoding), true);
		} catch (UnsupportedEncodingException e) {
			IStatus status = new CVSStatus(IStatus.ERROR, CVSStatus.SERVER_ERROR, e.getMessage(), e, fCVSRoot);
			throw new CVSException (status);
		}
	}

	void write (byte[] bytes, boolean newLine) throws CVSException {
		write(bytes, 0, bytes.length, newLine);
	}
	
	/**
	 * Low level method to write a string to the server. All write* methods are
	 * funneled through this method.
	 */
	void write(byte[] b, int off, int len, boolean newline) throws CVSException {
		if (!isEstablished())
			throw new CVSCommunicationException(CVSMessages.Connection_writeUnestablishedConnection,fCVSRoot,null);
			
		if (Policy.isDebugProtocol())
		    Policy.printProtocol(new String(b, off, len), newline);
	
		try {
			OutputStream out= getOutputStream();
			out.write(b, off, len);
			if (newline)
				out.write(NEWLINE);
			
		} catch (IOException e) {
			throw new CVSCommunicationException(fCVSRoot,e);
		}
	}
}
