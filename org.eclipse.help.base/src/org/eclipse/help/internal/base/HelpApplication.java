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
package org.eclipse.help.internal.base;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.Map;
import java.util.Properties;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.help.internal.server.WebappManager;
import org.eclipse.osgi.util.NLS;

/**
 * Help application. Starts webserver and help web application for use by
 * infocenter and stand-alone help. Application takes a parameter "mode", that
 * can take values: "infocenter" - when help system should run as infocenter,
 * "standalone" - when help system should run as standalone.
 */
public class HelpApplication implements IApplication, IExecutableExtension {
	private static final String APPLICATION_LOCK_FILE = ".applicationlock"; //$NON-NLS-1$
	private static final int STATE_EXITING = 0;
	private static final int STATE_RUNNING = 1;
	private static final int STATE_RESTARTING = 2;
	private static int status = STATE_RUNNING;
	private static boolean shutdownOnClose = false; // Shutdown help when the embedded browser is closed
	private File metadata;
	private FileLock lock;
	private RandomAccessFile raf;

	@Override
	public synchronized Object start(IApplicationContext context) throws Exception {
		if (status == STATE_RESTARTING) {
			return EXIT_RESTART;
		}

		metadata = new File(Platform.getLocation().toFile(), ".metadata/"); //$NON-NLS-1$
		if (!BaseHelpSystem.ensureWebappRunning()) {
			System.out.println(NLS.bind(HelpBaseResources.HelpApplication_couldNotStart, Platform.getLogFileLocation().toOSString()));
			return EXIT_OK;
		}

		if (status == STATE_RESTARTING) {
			return EXIT_RESTART;
		}

		writeHostAndPort();
		obtainLock();

		if (BaseHelpSystem.MODE_STANDALONE == BaseHelpSystem.getMode()) {
			//try running UI loop if possible
			DisplayUtils.runUI();
		}
		//run a headless loop;
		while (status == STATE_RUNNING) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException ie) {
				break;
			}
		}
		releaseLock();
		if (status == STATE_RESTARTING) {
			return EXIT_RESTART;
		}
		return EXIT_OK;
	}

	@Override
	public void stop() {
		stopHelp();

		// wait until start has finished
		synchronized(this) {}
	}

	/**
	 * Causes help service to stop and exit
	 */
	public static void stopHelp() {
		status = STATE_EXITING;
		if (BaseHelpSystem.MODE_STANDALONE == BaseHelpSystem.getMode()) {
			// UI loop may be sleeping if no SWT browser is up
			DisplayUtils.wakeupUI();
		}
	}

	/**
	 * Causes help service to exit and start again
	 */
	public static void restartHelp() {
		if (status != STATE_EXITING) {
			status = STATE_RESTARTING;
		}
	}

	@Override
	public void setInitializationData(IConfigurationElement configElement, String propertyName, Object data) {
		String value = (String)((Map<?, ?>)data).get("mode"); //$NON-NLS-1$
		if ("infocenter".equalsIgnoreCase(value)) { //$NON-NLS-1$
			BaseHelpSystem.setMode(BaseHelpSystem.MODE_INFOCENTER);
		}
		else if ("standalone".equalsIgnoreCase(value)) { //$NON-NLS-1$
			BaseHelpSystem.setMode(BaseHelpSystem.MODE_STANDALONE);
		}
	}

	private void writeHostAndPort() throws IOException {
		Properties p = new Properties();
		p.put("host", WebappManager.getHost()); //$NON-NLS-1$
		p.put("port", "" + WebappManager.getPort()); //$NON-NLS-1$ //$NON-NLS-2$

		File hostPortFile = new File(metadata, ".connection"); //$NON-NLS-1$
		hostPortFile.deleteOnExit();

		metadata.mkdirs();
		try (FileOutputStream out = new FileOutputStream(hostPortFile)) {
			p.store(out, null);
		}

	}

	@SuppressWarnings("resource")
	private void obtainLock() {
		File lockFile = new File(metadata, APPLICATION_LOCK_FILE);
		try {
			raf = new RandomAccessFile(lockFile, "rw"); //$NON-NLS-1$
			lock = raf.getChannel().lock();
		} catch (IOException ioe) {
			lock = null;
		}
	}

	private void releaseLock() {
		if (lock != null) {
			try {
				lock.channel().close();
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

	public static boolean isRunning() {
		return status == STATE_RUNNING;
	}

	public static boolean isShutdownOnClose() {
		return shutdownOnClose;
	}

	public static void setShutdownOnClose(boolean shutdownOnClose) {
		HelpApplication.shutdownOnClose = shutdownOnClose;
	}
}
