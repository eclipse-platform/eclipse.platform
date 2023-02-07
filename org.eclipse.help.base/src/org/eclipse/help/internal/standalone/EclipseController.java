/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
package org.eclipse.help.internal.standalone;

import java.io.*;
import java.net.*;
import java.nio.channels.*;

/**
 * This program is used to start or stop Eclipse Infocenter application. It
 * should be launched from command line.
 */
public class EclipseController implements EclipseLifeCycleListener {
	public static final String CMD_INSTALL = "install"; //$NON-NLS-1$

	public static final String CMD_UPDATE = "update"; //$NON-NLS-1$

	public static final String CMD_ENABLE = "enable"; //$NON-NLS-1$

	public static final String CMD_DISABLE = "disable"; //$NON-NLS-1$

	public static final String CMD_UNINSTALL = "uninstall"; //$NON-NLS-1$

	public static final String CMD_SEARCH = "search"; //$NON-NLS-1$

	public static final String CMD_LIST = "listFeatures"; //$NON-NLS-1$

	public static final String CMD_ADDSITE = "addSite"; //$NON-NLS-1$

	public static final String CMD_REMOVESITE = "removeSite"; //$NON-NLS-1$

	public static final String CMD_APPLY = "apply"; //$NON-NLS-1$

	// control servlet path
	private static final String CONTROL_SERVLET_PATH = "/help/control"; //$NON-NLS-1$

	// application to launch
	protected String applicationId;

	// Eclipse connection params
	protected EclipseConnection connection;

	public Eclipse eclipse = null;

	// Inter process lock
	private FileLock lock;
	private RandomAccessFile raf;

	private boolean eclipseEnded = false;

	/**
	 * Constructs help system
	 *
	 * @param applicationId
	 *            ID of Eclipse help application
	 * @param args
	 *            array of String options and their values Option
	 *            <code>-eclipseHome dir</code> specifies Eclipse installation
	 *            directory. It must be provided, when current directory is not
	 *            the same as Eclipse installation directory. Additionally, most
	 *            options accepted by Eclipse execuable are supported.
	 */
	public EclipseController(String applicationId, String[] args) {

		this.applicationId = applicationId;
		Options.init(applicationId, args);
		connection = new EclipseConnection();
	}

	/**
	 * @see org.eclipse.help.standalone.Help#shutdown()
	 */
	public final synchronized void shutdown() throws Exception {
		try {
			obtainLock();
			sendHelpCommandInternal("shutdown", new String[0]); //$NON-NLS-1$
		} catch (MalformedURLException mue) {
			mue.printStackTrace();
		} catch (InterruptedException ie) {
		} finally {
			releaseLock();
		}
	}

	/**
	 * @see org.eclipse.help.standalone.Help#start()
	 */
	public final synchronized void start() throws Exception {
		try {
			obtainLock();
			startEclipse();
		} finally {
			releaseLock();
		}

	}

	/**
	 * Ensures the application is running, and sends command to the control
	 * servlet. If connection fails, retries several times, in case webapp is
	 * starting up.
	 */
	protected final synchronized void sendHelpCommand(String command,
			String[] parameters) throws Exception {
		try {
			obtainLock();
			sendHelpCommandInternal(command, parameters);
		} finally {
			releaseLock();
		}

	}

	/**
	 * Starts Eclipse if not yet running.
	 */
	private void startEclipse() throws Exception {
		boolean fullyRunning = isApplicationRunning();
		if (fullyRunning) {
			return;
		}
		if (Options.isDebug()) {
			System.out
					.println("Using workspace " + Options.getWorkspace().getAbsolutePath()); //$NON-NLS-1$
		}
		// delete old connection file
		Options.getConnectionFile().delete();
		connection.reset();

		if (Options.isDebug()) {
			System.out
					.println("Ensured old .connection file is deleted.  Launching Eclipse."); //$NON-NLS-1$
		}
		eclipseEnded = false;
		eclipse = new Eclipse(this);
		eclipse.start();
		fullyRunning = isApplicationRunning();
		while (!eclipseEnded && !fullyRunning) {
			try {
				Thread.sleep(250);
			} catch (InterruptedException ie) {
			}
			fullyRunning = isApplicationRunning();
		}
		if (eclipseEnded) {
			if (eclipse.getStatus() == Eclipse.STATUS_ERROR) {
				throw eclipse.getException();
			}
			return;
		}
		if (Options.isDebug()) {
			System.out.println("Eclipse launched"); //$NON-NLS-1$
		}
		// in case controller is killed
		Runtime.getRuntime().addShutdownHook(new EclipseCleaner());
	}

	private void sendHelpCommandInternal(String command, String[] parameters)
			throws Exception {
		if (!"shutdown".equalsIgnoreCase(command)) { //$NON-NLS-1$
			startEclipse();
		}
		if (!isApplicationRunning()) {
			return;
		}
		if (!connection.isValid()) {
			connection.renew();
		}
		try {
			String trustStoreLocation = Options.getTrustStoreLocation();
			if (trustStoreLocation != null) {
				System.setProperty("javax.net.ssl.trustStore", trustStoreLocation); //$NON-NLS-1$
			}
			String trustStorePassword = Options.getTrustStorePassword();
			if (trustStorePassword != null) {
				System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword);				 //$NON-NLS-1$
			}
			URL url = createCommandURL(command, parameters);
			if ("shutdown".equalsIgnoreCase(command) //$NON-NLS-1$
					&& Options.getConnectionFile().exists()) {
				connection.connect(url);
				long timeLimit = System.currentTimeMillis() + 60 * 1000;
				while (Options.getConnectionFile().exists()) {
					Thread.sleep(200);
					if (System.currentTimeMillis() > timeLimit) {
						System.out
								.println("Shutting down is taking too long.  Will not wait."); //$NON-NLS-1$
						break;
					}
				}

			} else {
				connection.connect(url);
			}
		} catch (MalformedURLException mue) {
			mue.printStackTrace();
		} catch (InterruptedException ie) {
		}
	}

	/**
	 * Builds a URL that communicates the specified command to help control
	 * servlet.
	 *
	 * @param command
	 *            standalone help system command e.g. "displayHelp"
	 * @param parameters
	 *            array of parameters of the command e.g.
	 *            {"http://www.eclipse.org"}
	 */
	private URL createCommandURL(String command, String[] parameters)
			throws MalformedURLException {
		StringBuilder urlStr = new StringBuilder();
		urlStr.append("http://"); //$NON-NLS-1$
		urlStr.append(connection.getHost());
		urlStr.append(":"); //$NON-NLS-1$
		urlStr.append(connection.getPort());
		urlStr.append(CONTROL_SERVLET_PATH);
		urlStr.append("?command="); //$NON-NLS-1$
		urlStr.append(command);
		for (String parameter : parameters) {
			urlStr.append("&"); //$NON-NLS-1$
			urlStr.append(parameter);
		}
		if (Options.isDebug()) {
			System.out.println("Control servlet URL=" + urlStr.toString()); //$NON-NLS-1$
		}
		return new URL(urlStr.toString());
	}

	@Override
	public void eclipseEnded() {
		eclipseEnded = true;
		connection.reset();
	}

	@SuppressWarnings("resource")
	private void obtainLock() throws IOException {
		if (lock != null) {
			// we already have lock
			return;
		}
		if (!Options.getLockFile().exists()) {
			Options.getLockFile().getParentFile().mkdirs();
		}
		raf = new RandomAccessFile(Options.getLockFile(), "rw"); //$NON-NLS-1$
		lock = raf.getChannel().lock();
		if (Options.isDebug()) {
			System.out.println("Lock obtained."); //$NON-NLS-1$
		}
	}

	private void releaseLock() {
		if (lock != null) {
			try {
				lock.channel().close();
				if (Options.isDebug()) {
					System.out.println("Lock released."); //$NON-NLS-1$
				}
				lock = null;
			} catch (IOException ioe) {
			}
		}
		if (raf != null) {
			try {
				raf.close();
			} catch (IOException ioe) {
			}
			raf = null;
		}
	}

	/**
	 * Tests whether HelpApplication is running by testing if .applicationlock
	 * is locked
	 */
	@SuppressWarnings("resource")
	private boolean isApplicationRunning() {
		File applicationLockFile = new File(Options.getLockFile()
				.getParentFile(), ".applicationlock"); //$NON-NLS-1$
		FileLock applicationLock = null;
		try (RandomAccessFile randomAccessFile = new RandomAccessFile(applicationLockFile, "rw")) { //$NON-NLS-1$

			applicationLock = randomAccessFile.getChannel().tryLock();
		} catch (IOException ioe) {
		} finally {
			if (applicationLock != null) {
				try {
					applicationLock.release();
				} catch (IOException ioe) {
				}
			}
			if (Options.isDebug()) {
				System.out
						.println("isApplicationRunning? " + (applicationLock == null)); //$NON-NLS-1$
			}
		}
		return applicationLock == null;
	}

	public class EclipseCleaner extends Thread {
		@Override
		public void run() {
			if (eclipse != null) {
				eclipse.killProcess();
			}
		}
	}

	/**
	 * @return true if commands contained a known command and it was executed
	 */
	protected boolean executeUpdateCommand(String updateCommand)
			throws Exception {
		String[] parameters = Options.getUpdateParameters();
		sendHelpCommandInternal(updateCommand, parameters);
		return true;
	}
}
