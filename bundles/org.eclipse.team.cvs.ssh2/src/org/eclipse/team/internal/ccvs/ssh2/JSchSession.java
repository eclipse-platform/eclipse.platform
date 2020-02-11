/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Atsuhiko Yamanaka, JCraft,Inc. - initial API and implementation.
 *     IBM Corporation - ongoing maintenance
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ssh2;

import java.util.Enumeration;
import java.util.Hashtable;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jsch.core.IJSchService;
import org.eclipse.jsch.core.IJSchLocation;
import org.eclipse.jsch.core.IPasswordStore;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.internal.ccvs.core.*;

import com.jcraft.jsch.*;

class JSchSession {
	private static final int SSH_DEFAULT_PORT = 22;
	private static Hashtable<String, JSchSession> pool = new Hashtable<>();

	private final Session session;
	private final ICVSRepositoryLocation location;

	protected static int getCVSTimeoutInMillis() {
		//return CVSProviderPlugin.getPlugin().getTimeout() * 1000;
		// TODO Hard-code the timeout for now since JSch doesn't respect CVS timeout
		// See bug 92887
		return 60000;
	}

	public static boolean isAuthenticationFailure(JSchException ee) {
		return ee.getMessage().equals("Auth fail"); //$NON-NLS-1$
	}

	static JSchSession getSession(final ICVSRepositoryLocation location, String username, String password, String hostname, int port, IProgressMonitor monitor) throws JSchException {
		int actualPort = port;
		if (actualPort == ICVSRepositoryLocation.USE_DEFAULT_PORT)
			actualPort = getPort(location);

		String key = getPoolKey(username, hostname, actualPort);

		try {
			JSchSession jschSession = pool.get(key);
			if (jschSession != null && !jschSession.getSession().isConnected()) {
				pool.remove(key);
				jschSession = null;
			}

			if (jschSession == null) {
				IJSchService service = getJSchService();
				IJSchLocation jlocation=service.getLocation(username, hostname, actualPort);

				// As for the connection method "pserverssh2",
				// there is not a place to save the given password for ssh2.
				if (!location.getMethod().getName().equals("pserverssh2")) { //$NON-NLS-1$
					IPasswordStore pstore = new IPasswordStore() {
						@Override
						public void clear(IJSchLocation l) {
							location.flushUserInfo();
						}
						@Override
						public boolean isCached(IJSchLocation l) {
							return location.getUserInfoCached();
						}
						@Override
						public void update(IJSchLocation l) {
							location.setPassword(l.getPassword());
							location.setAllowCaching(true);
						}
					};
					jlocation.setPasswordStore(pstore);
				}
				jlocation.setComment(NLS.bind(CVSSSH2Messages.JSchSession_3, new String[] {location.toString()}));

				Session session = null;
				try {
					session = createSession(service, jlocation, password, monitor);
				} catch (JSchException e) {
					throw e;
				}
				if (session == null)
					throw new JSchException(CVSSSH2Messages.JSchSession_4);
				if (session.getTimeout() != location.getTimeout() * 1000)
					session.setTimeout(location.getTimeout() * 1000);
				JSchSession schSession = new JSchSession(session, location);
				pool.put(key, schSession);
				return schSession;
			}
			return jschSession;
		} catch (JSchException e) {
			pool.remove(key);
			if(e.toString().contains("Auth cancel")){  //$NON-NLS-1$
				throw new OperationCanceledException();
			}
			throw e;
		}
	}

	private static Session createSession(IJSchService service, IJSchLocation location, String password, IProgressMonitor monitor) throws JSchException {
		if (password != null)
			location.setPassword(password);
		Session session = service.createSession(location, null);
		session.setTimeout(getCVSTimeoutInMillis());
		if (password != null)
			session.setPassword(password);
		service.connect(session, getCVSTimeoutInMillis(), monitor);
		return session;
	}

	private static IJSchService getJSchService(){
		return CVSSSH2Plugin.getDefault().getJSchService();
	}

	private static String getPoolKey(String username, String hostname, int port) {
		return username + "@" + hostname + ":" + port; //$NON-NLS-1$ //$NON-NLS-2$
	}

	private static String getPoolKey(ICVSRepositoryLocation location){
		return location.getUsername() + "@" + location.getHost() + ":" + getPort(location); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private static int getPort(ICVSRepositoryLocation location) {
		int port = location.getPort();
		if (port == ICVSRepositoryLocation.USE_DEFAULT_PORT)
			port = SSH_DEFAULT_PORT;
		return port;
	}

	static void shutdown() {
		if (getJSch() != null && pool.size() > 0) {
			for (Enumeration e = pool.elements(); e.hasMoreElements();) {
				JSchSession session = (JSchSession) (e.nextElement());
				try {
					session.getSession().disconnect();
				} catch (Exception ee) {
					// Ignore
				}
			}
			pool.clear();
		}
	}

	static JSch getJSch() {
		return getJSchService().getJSch();
	}

	private JSchSession(Session session, ICVSRepositoryLocation location) {
		this.session = session;
		this.location = location;
	}

	public Session getSession() {
		return session;
	}

	public void dispose() {
		if (session.isConnected()) {
			session.disconnect();
		}
		pool.remove(getPoolKey(location));
	}

}
