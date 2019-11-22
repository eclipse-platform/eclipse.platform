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
package org.eclipse.team.tests.ccvs.core;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.IServerConnection;
import org.eclipse.team.internal.ccvs.core.connection.CVSAuthenticationException;

/**
 * @author Administrator
 *
 * To change this generated comment go to 
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class TestConnection implements IServerConnection {
	public static TestConnection currentConnection;
	
	public static List<String> previousLines;
	public static StringBuffer currentLine;
	
	private ByteArrayInputStream serverResponse;
	
	private static final String VALID_SERVER_REQUESTS = "Valid-requests Root Valid-responses valid-requests Repository Directory Max-dotdot Static-directory Sticky Checkin-prog Update-prog Entry Kopt Checkin-time Modified Is-modified UseUnchanged Unchanged Notify Questionable Case Argument Argumentx Global_option Gzip-stream wrapper-sendme-rcsOptions Set Kerberos-encrypt Gssapi-encrypt Gssapi-authenticate expand-modules ci co update diff log rlog add remove update-patches gzip-file-contents status rdiff tag rtag import admin export history release watch-on watch-off watch-add watch-remove watchers editors init annotate rannotate noop version";

	public static IServerConnection createConnection(ICVSRepositoryLocation location, String password) {
		currentConnection = new TestConnection();
		return currentConnection;
	}
	
	public static String getLastLine() {
		if (previousLines.isEmpty())
			return null;
		return previousLines.get(previousLines.size() - 1);
	}
	
	@Override
	public void open(IProgressMonitor monitor) throws IOException, CVSAuthenticationException {
		resetStreams();
	}

	@Override
	public void close() throws IOException {
		resetStreams();
	}

	private void resetStreams() {
		currentLine = new StringBuffer();
		previousLines = new ArrayList<>();
	}
	
	@Override
	public InputStream getInputStream() {
		// TODO Auto-generated method stub
		return new InputStream() {
			@Override
			public int read() throws IOException {
				if (serverResponse == null) {
					throw new IOException("Not prepared to make a response");
				} else {
					return serverResponse.read();
				}	
			}
		};
	}

	@Override
	public OutputStream getOutputStream() {
		return new OutputStream() {
			@Override
			public void write(int output) throws IOException {
				byte b = (byte)output;
				if (b == '\n') {
					String sentLine = currentLine.toString();
					previousLines.add(sentLine);
					currentLine = new StringBuffer();
					respondToSentLine(sentLine);
				} else {
					currentLine.append((char)b);
				}
			}
		};
	}

	/**
	 * @param sentLine
	 */
	protected void respondToSentLine(String sentLine) {
		if (sentLine.equals("valid-requests")) {
			serverResponse = new ByteArrayInputStream((VALID_SERVER_REQUESTS + "\nok\n").getBytes());
		}
	}

}
