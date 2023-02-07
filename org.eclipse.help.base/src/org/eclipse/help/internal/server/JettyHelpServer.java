/*******************************************************************************
 * Copyright (c) 2008, 2020 IBM Corporation and others.
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
 *     George Suaridze <suag@1c.ru> (1C-Soft LLC) - Bug 560168
 *******************************************************************************/

package org.eclipse.help.internal.server;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.http.jetty.JettyConfigurator;
import org.eclipse.equinox.http.jetty.JettyConstants;
import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.help.internal.base.HelpBasePlugin;
import org.eclipse.help.server.HelpServer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;


public class JettyHelpServer extends HelpServer {

	private abstract static class WorkerThread extends Thread {
		private Throwable exception;

		public WorkerThread(String name) {
			super(name);
		}

		public synchronized void setException(Throwable status) {
			this.exception = status;
		}

		public synchronized Throwable getException() {
			return exception;
		}
	}

	private final class StartServerThread extends WorkerThread {

		private final String webappName;

		public StartServerThread(String webappName) {
			super("Start Help Server"); //$NON-NLS-1$
			this.webappName = webappName;
		}

		@Override
		public void run() {
			try {
				final Dictionary<String, Object> d = new Hashtable<>();
				final int SESSION_TIMEOUT_INTERVAL_IN_SECONDS = 30*60;  // 30 minutes
				configurePort();
				d.put("http.port", Integer.valueOf(getPortParameter())); //$NON-NLS-1$

				// set the base URL
				d.put("context.path", getContextPath()); //$NON-NLS-1$
				d.put("other.info", getOtherInfo()); //$NON-NLS-1$
				d.put(JettyConstants.CONTEXT_SESSIONINACTIVEINTERVAL, Integer.valueOf(SESSION_TIMEOUT_INTERVAL_IN_SECONDS));

				// suppress Jetty INFO/DEBUG messages to stderr
				Logger.getLogger("org.mortbay").setLevel(Level.WARNING); //$NON-NLS-1$

				if (bindServerToHostname()) {
					d.put("http.host", getHost()); //$NON-NLS-1$
				}

				JettyConfigurator.startServer(webappName, d);
			} catch (Throwable t) {
				setException(t);
			}
		}
	}

	private final class StopServerThread extends WorkerThread {

		private final String webappName;

		public StopServerThread(String webappName) {
			super("Stop Help Server"); //$NON-NLS-1$
			this.webappName = webappName;
		}

		@Override
		public void run() {
			try {
				JettyConfigurator.stopServer(webappName);
				port = -1;
			} catch (Throwable t) {
				setException(t);
			}
		}
	}


	private String host;
	protected int port = -1;
	protected static final int AUTO_SELECT_JETTY_PORT = 0;

	@Override
	public void start(final String webappName) throws Exception {
		WorkerThread startRunnable = new StartServerThread(webappName);
		execute(startRunnable);
		checkBundle();
	}

	/*
	 * Ensures that the bundle with the specified name and the highest available
	 * version is started and reads the port number
	 */
	protected void checkBundle() throws InvalidSyntaxException, BundleException {
		Bundle bundle = Platform.getBundle("org.eclipse.equinox.http.registry"); //$NON-NLS-1$
		if (bundle == null) {
			throw new BundleException("org.eclipse.equinox.http.registry"); //$NON-NLS-1$
		}
		if (bundle.getState() == Bundle.RESOLVED) {
			bundle.start(Bundle.START_TRANSIENT);
		}
		if (port == -1) {
			// Jetty selected a port number for us
			ServiceReference<?>[] reference = bundle.getBundleContext().getServiceReferences("org.osgi.service.http.HttpService", "(other.info=" + getOtherInfo() + ')'); //$NON-NLS-1$ //$NON-NLS-2$
			Object assignedPort = reference[0].getProperty("http.port"); //$NON-NLS-1$
			port = Integer.parseInt((String)assignedPort);
		}
	}

	@Override
	public void stop(final String webappName) throws CoreException {
		try {
			WorkerThread stopRunnable = new StopServerThread(webappName);
			execute(stopRunnable);
		}
		catch (Exception e) {
			Platform.getLog(getClass()).error("An error occured while stopping the help server", e); //$NON-NLS-1$
		}
	}

	private void execute(WorkerThread runnable) throws Exception {
		boolean interrupted = false;
		Thread thread = runnable;
		thread.setDaemon(true);
		thread.start();
		while(true) {
			try {
				thread.join();
				break;
			} catch (InterruptedException e) {
				interrupted = true;
			}
		}
		if (interrupted)
			Thread.currentThread().interrupt();

		Throwable t = runnable.getException();

		if (t != null) {
			if (t instanceof Exception) {
				throw (Exception)t;
			}
			throw (Error) t;
		}
	}

	@Override
	public int getPort() {
		return port;
	}

	private void configurePort() {
		if (port == -1) {
			String portCommandLineOverride = HelpBasePlugin.getBundleContext().getProperty("server_port"); //$NON-NLS-1$
			if (portCommandLineOverride != null && portCommandLineOverride.trim().length() > 0) {
				try {
					port = Integer.parseInt(portCommandLineOverride);
				}
				catch (NumberFormatException e) {
					String msg = "Help server port specified in VM arguments is invalid (" + portCommandLineOverride + ")"; //$NON-NLS-1$ //$NON-NLS-2$
					Platform.getLog(getClass()).error(msg, e);
				}
			}
		}
	}

	/*
	 * Get the port number which will be passed to Jetty
	 */
	protected int getPortParameter() {
		if (port == -1) {
			return AUTO_SELECT_JETTY_PORT;
		}
		return port;
	}

	@Override
	public String getHost() {
		if (host == null) {
			String hostCommandLineOverride = HelpBasePlugin.getBundleContext().getProperty("server_host"); //$NON-NLS-1$
			if (hostCommandLineOverride != null && hostCommandLineOverride.trim().length() > 0) {
				host = hostCommandLineOverride;
			}
			else {
				host = "127.0.0.1"; //$NON-NLS-1$
			}
		}
		return host;
	}

	protected String getOtherInfo() {
		return "org.eclipse.help"; //$NON-NLS-1$
	}

	protected String getContextPath() {
		return "/help"; //$NON-NLS-1$
	}

	public boolean bindServerToHostname() {
		if (BaseHelpSystem.getMode() == BaseHelpSystem.MODE_WORKBENCH) {
			return true;
		}
		String host = HelpBasePlugin.getBundleContext().getProperty("server_host"); //$NON-NLS-1$
		return host != null && host.trim().length() > 0;
	}

}
