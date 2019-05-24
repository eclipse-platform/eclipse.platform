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
package org.eclipse.team.internal.ccvs.core.connection;
 
import java.io.*;
import java.net.Socket;

import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jsch.core.IJSchService;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.util.Util;
import org.eclipse.team.internal.core.streams.*;

import com.jcraft.jsch.Proxy;

/**
 * A connection used to talk to an cvs pserver.
 */
public class PServerConnection implements IServerConnection {
	
	public static final char NEWLINE= 0xA;
	
	/** default CVS pserver port */
	private static final int DEFAULT_PORT= 2401;
	
	/** error line indicators */
	private static final char ERROR_CHAR = 'E';
	private static final String ERROR_MESSAGE = "error 0";//$NON-NLS-1$
	private static final String NO_SUCH_USER = "no such user";//$NON-NLS-1$
	
	private static final char[] SCRAMBLING_TABLE=new char[] {
	0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,
	16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,
	114,120,53,79,96,109,72,108,70,64,76,67,116,74,68,87,
	111,52,75,119,49,34,82,81,95,65,112,86,118,110,122,105,
	41,57,83,43,46,102,40,89,38,103,45,50,42,123,91,35,
	125,55,54,66,124,126,59,47,92,71,115,78,88,107,106,56,
	36,121,117,104,101,100,69,73,99,63,94,93,39,37,61,48,
	58,113,32,90,44,98,60,51,33,97,62,77,84,80,85,223,
	225,216,187,166,229,189,222,188,141,249,148,200,184,136,248,190,
	199,170,181,204,138,232,218,183,255,234,220,247,213,203,226,193,
	174,172,228,252,217,201,131,230,197,211,145,238,161,179,160,212,
	207,221,254,173,202,146,224,151,140,196,205,130,135,133,143,246,
	192,159,244,239,185,168,215,144,139,165,180,157,147,186,214,176,
	227,231,219,169,175,156,206,198,129,164,150,210,154,177,134,127,
	182,128,158,208,162,132,167,209,149,241,153,251,237,236,171,195,
	243,233,253,240,194,250,191,155,142,137,245,235,163,242,178,152
	};

	/** Communication strings */
	private static final String BEGIN= "BEGIN AUTH REQUEST";//$NON-NLS-1$
	private static final String END=   "END AUTH REQUEST";//$NON-NLS-1$
	private static final String LOGIN_OK= "I LOVE YOU";//$NON-NLS-1$
	private static final String LOGIN_FAILED= "I HATE YOU";//$NON-NLS-1$
	
	private String password;
	private ICVSRepositoryLocation cvsroot;

	private Socket fSocket;
	
	private InputStream inputStream;
	private OutputStream outputStream;
	
	@Override
	public void close() throws IOException {
		try {
			if (inputStream != null) inputStream.close();
		} finally {
			inputStream = null;
			try {
				if (outputStream != null) outputStream.close();
			} finally {
				outputStream = null;
				try {
					if (fSocket != null) fSocket.close();
				} finally {
					fSocket = null;
				}
			}
		}
	}

	@Override
	public void open(IProgressMonitor monitor) throws IOException, CVSAuthenticationException {
		
		monitor.subTask(CVSMessages.PServerConnection_authenticating);
		monitor.worked(1);
		
		InputStream is = null;
		OutputStream os = null;
		
		Proxy proxy = getProxy();
		if (proxy!=null) {
			String host = cvsroot.getHost();
			int port = cvsroot.getPort();
			if (port == ICVSRepositoryLocation.USE_DEFAULT_PORT) {
			port = DEFAULT_PORT;
		}
		try {
			int timeout = CVSProviderPlugin.getPlugin().getTimeout() * 1000;
			IJSchService service = CVSProviderPlugin.getPlugin().getJSchService();
			service.connect(proxy, host, port, timeout, monitor);
		} catch( Exception ex) {
			ex.printStackTrace();
			throw new IOException(ex.getMessage());
		}
		is = proxy.getInputStream();
		os = proxy.getOutputStream();
		
		} else {
			fSocket = createSocket(monitor);
			is = fSocket.getInputStream();
			os = fSocket.getOutputStream();
		}
		
		boolean connected = false;
		try {
			this.inputStream = new BufferedInputStream(new PollingInputStream(is,
				cvsroot.getTimeout(), monitor));
			this.outputStream = new PollingOutputStream(new TimeoutOutputStream(
				os, 8192 /*bufferSize*/, 1000 /*writeTimeout*/, 1000 /*closeTimeout*/),
				cvsroot.getTimeout(), monitor);
			authenticate();
			connected = true;
		} finally {
			if (! connected) close();
		}
	}

	private Proxy getProxy() {
		IJSchService service = CVSProviderPlugin.getPlugin().getJSchService();
		if (service == null)
			return null;
		Proxy proxy = service.getProxyForHost(cvsroot.getHost(), IProxyData.SOCKS_PROXY_TYPE);
		if (proxy == null)
			proxy = service.getProxyForHost(cvsroot.getHost(), IProxyData.HTTPS_PROXY_TYPE);
		return proxy;
	}

	@Override
	public InputStream getInputStream() {
		return inputStream;
	}
	@Override
	public OutputStream getOutputStream() {
		return outputStream;
	}

	/**
	 * Creates a new <code>PServerConnection</code> for the given
	 * cvs root.
	 */
	PServerConnection(ICVSRepositoryLocation cvsroot, String password) {
		this.cvsroot = cvsroot;
		this.password = password;
	}
	/**
	 * Does the actual authentication.
	 */
	private void authenticate() throws IOException, CVSAuthenticationException {
		String scrambledPassword = scramblePassword(password);
	
		String user = cvsroot.getUsername();
		OutputStream out = getOutputStream();
		
		StringBuilder request = new StringBuilder();
		request.append(BEGIN);
		request.append(NEWLINE);
		request.append(cvsroot.getRootDirectory());
		request.append(NEWLINE);
		request.append(user);
		request.append(NEWLINE);
		request.append(scrambledPassword);
		request.append(NEWLINE);
		request.append(END);
		request.append(NEWLINE);
		out.write(request.toString().getBytes());
		out.flush();
		String line = Connection.readLine(cvsroot, getInputStream()).trim();
		
		// Return if we succeeded
		if (LOGIN_OK.equals(line))
			return;
		
		// Otherwise, determine the type of error
		if (line.length() == 0) {
			throw new IOException(CVSMessages.PServerConnection_noResponse);
		}
		
		// Accumulate a message from the error (E) stream
		String message = "";//$NON-NLS-1$
		String separator = ""; //$NON-NLS-1$

		if(!CVSProviderPlugin.getPlugin().isUseProxy()) {
			while (line.length() > 0 && line.charAt(0) == ERROR_CHAR) {
				if (line.length() > 2) {
					message += separator + line.substring(2);
					separator = " "; //$NON-NLS-1$
				}
				line = Connection.readLine(cvsroot, getInputStream());
			}
		} else {
			while (line.length() > 0) {
				message += separator + line;
				separator = "\n"; //$NON-NLS-1$
				line = Connection.readLine(cvsroot, getInputStream());
			}
		}
		
		// If the last line is the login failed (I HATE YOU) message, return authentication failure
		if (LOGIN_FAILED.equals(line)) {
			if (message.length() == 0) {
				throw new CVSAuthenticationException(CVSMessages.PServerConnection_loginRefused, CVSAuthenticationException.RETRY,cvsroot);
			} else {
				throw new CVSAuthenticationException(message, CVSAuthenticationException.RETRY,cvsroot);
			}
		}
		
		// Remove leading "error 0"
		if (line.startsWith(ERROR_MESSAGE))
			message += separator + line.substring(ERROR_MESSAGE.length() + 1);
		else
			message += separator + line;
		if (message.contains(NO_SUCH_USER))
			throw new CVSAuthenticationException(NLS.bind(CVSMessages.PServerConnection_invalidUser, (new Object[] {message})), CVSAuthenticationException.RETRY,cvsroot);
		throw new IOException(NLS.bind(CVSMessages.PServerConnection_connectionRefused, (new Object[] { message })));
	}

	/**
	 * Creates the actual socket
	 */
	protected Socket createSocket(IProgressMonitor monitor) throws IOException {
		// Determine what port to use
		int port = cvsroot.getPort();
		if (port == ICVSRepositoryLocation.USE_DEFAULT_PORT)
			port = DEFAULT_PORT;
		// Make the connection
		Socket result;
		try {
			result= Util.createSocket(cvsroot.getHost(), port, monitor);
			// Bug 36351: disable buffering and send bytes immediately
			result.setTcpNoDelay(true);
		} catch (InterruptedIOException e) {
			// If we get this exception, chances are the host is not responding
			throw new InterruptedIOException(NLS.bind(CVSMessages.PServerConnection_socket, (new Object[] {cvsroot.getHost()})));
		}
		result.setSoTimeout(1000); // 1 second between timeouts
		return result;
	}

	private String scramblePassword(String password) throws CVSAuthenticationException {
		int length = password.length();
		char[] out= new char[length];
		for (int i= 0; i < length; i++) {
			char value = password.charAt(i);
			if( value < 0 || value > 255 )
				throwInValidCharacter();
			out[i]= SCRAMBLING_TABLE[value];			
		}
		return "A" + new String(out);//$NON-NLS-1$
	}
	
	private void throwInValidCharacter() throws CVSAuthenticationException {
		throw new CVSAuthenticationException(CVSMessages.PServerConnection_invalidChars, CVSAuthenticationException.RETRY, cvsroot);
	}
	
}
