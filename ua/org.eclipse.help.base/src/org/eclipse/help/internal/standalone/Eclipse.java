/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

/**
 * Eclipse launcher. Spawns eclipse executable or
 * org.eclipse.equinox.launcher.Main.
 */
public class Eclipse extends Thread {
	// Eclipse exit codes
	private static final int NEEDS_RESTART = 23;
	// Launching status
	public static final int STATUS_INIT = 0;
	public static final int STATUS_STARTED = 1;
	public static final int STATUS_ERROR = 2;

	File dir;
	String[] cmdarray;
	private int status = STATUS_INIT;
	private Exception exception;
	Process pr;
	private final EclipseLifeCycleListener lifeCycleListener;
	/**
	 * Constructor
	 */
	public Eclipse(EclipseLifeCycleListener listener) {
		super();
		this.lifeCycleListener = listener;
		this.setName("Eclipse"); //$NON-NLS-1$
		this.dir = Options.getEclipseHome();
	}
	private void prepareCommand() throws Exception {
		if (Options.useExe()) {
			prepareEclipseCommand();
			ensureEclipseExeExists();
		} else {
			prepareJavaCommand();
		}
		ensureVmExists();
	}
	private void prepareEclipseCommand() {
		List<String> vmArgs = Options.getVmArgs();
		List<String> eclipseArgs = Options.getEclipseArgs();
		cmdarray = new String[3 + vmArgs.size() + 1 + eclipseArgs.size()];
		cmdarray[0] = new File(Options.getEclipseHome(), "eclipse").getAbsolutePath(); //$NON-NLS-1$
		cmdarray[1] = "-vm"; //$NON-NLS-1$
		cmdarray[2] = Options.getVm();
		for (int i = 0; i < eclipseArgs.size(); i++) {
			cmdarray[3 + i] = eclipseArgs.get(i);
		}
		cmdarray[3 + eclipseArgs.size()] = "-vmargs"; //$NON-NLS-1$
		for (int i = 0; i < vmArgs.size(); i++) {
			cmdarray[4 + eclipseArgs.size() + i] = vmArgs.get(i);
		}
	}
	private void prepareJavaCommand() throws Exception {
		List<String> vmArgs = Options.getVmArgs();
		List<String> eclipseArgs = Options.getEclipseArgs();
		cmdarray = new String[1 + vmArgs.size() + 3 + eclipseArgs.size()];
		cmdarray[0] = Options.getVm();
		for (int i = 0; i < vmArgs.size(); i++) {
			cmdarray[1 + i] = vmArgs.get(i);
		}
		cmdarray[1 + vmArgs.size()] = "-cp"; //$NON-NLS-1$
		cmdarray[2 + vmArgs.size()] = getStartupJar();
		cmdarray[3 + vmArgs.size()] = "org.eclipse.equinox.launcher.Main"; //$NON-NLS-1$
		for (int i = 0; i < eclipseArgs.size(); i++) {
			cmdarray[4 + vmArgs.size() + i] = eclipseArgs.get(i);
		}
	}
	/**
	 * Launches Eclipse process and waits for it.
	 */
	@Override
	@SuppressWarnings("resource")
	public void run() {
		try {
			prepareCommand();
			if (Options.isDebug()) {
				printCommand();
			}
			do {
				pr = Runtime.getRuntime().exec(cmdarray, (String[]) null, dir);
				(new StreamConsumer(pr.getInputStream())).start();
				(new StreamConsumer(pr.getErrorStream())).start();
				if (status == STATUS_INIT) {
					// started first time
					status = STATUS_STARTED;
				}
				try {
					pr.waitFor();
				} catch (InterruptedException e) {
				}
				if (Options.isDebug()) {
					System.out
							.println("Eclipse exited with status code " + pr.exitValue()); //$NON-NLS-1$
					if (pr.exitValue() == NEEDS_RESTART) {
						System.out
								.println("Updates are installed,  Eclipse will be restarted."); //$NON-NLS-1$
					}
				}
			} while (pr.exitValue() == NEEDS_RESTART);
		} catch (Exception exc) {
			exception = exc;
			status = STATUS_ERROR;
		} finally {
			if (status == STATUS_INIT) {
				status = STATUS_ERROR;
			}
			if (status == STATUS_ERROR && exception == null) {
				exception = new Exception("Unknown exception."); //$NON-NLS-1$
			}
			lifeCycleListener.eclipseEnded();
		}
	}
	/**
	 * Reads a stream
	 */
	public static class StreamConsumer extends Thread {
		BufferedReader bReader;
		public StreamConsumer(InputStream inputStream) {
			super();
			this.setName("Eclipse out/err consumer"); //$NON-NLS-1$
			this.setDaemon(true);
			bReader = new BufferedReader(new InputStreamReader(inputStream));
		}
		@Override
		public void run() {
			try {
				String line;
				while (null != (line = bReader.readLine())) {
					System.out.println(line);
				}
				bReader.close();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
	}
	private void ensureVmExists() throws Exception {
		File vmExe = new File(Options.getVm());
		if (vmExe.exists() && !vmExe.isDirectory()) {
			return;
		}
		vmExe = new File(Options.getVm() + ".exe"); //$NON-NLS-1$
		if (vmExe.exists() && !vmExe.isDirectory()) {
			return;
		}
		throw new Exception("File " + vmExe.getAbsolutePath() //$NON-NLS-1$
				+ " does not exists.  Pass a correct -vm option"); //$NON-NLS-1$
	}
	private void ensureEclipseExeExists() throws Exception {
		File eclipseExe = new File(Options.getEclipseHome(), "eclipse" //$NON-NLS-1$
				+ (System.getProperty("os.name").startsWith("Win") //$NON-NLS-1$ //$NON-NLS-2$
						? ".exe" //$NON-NLS-1$
						: "")); //$NON-NLS-1$
		if (eclipseExe.exists() && !eclipseExe.isDirectory()) {
			return;
		}
		throw new Exception("File " + eclipseExe.getAbsolutePath() //$NON-NLS-1$
				+ " does not exists.  Pass a correct -eclipsehome option"); //$NON-NLS-1$
	}
	private String getStartupJar() throws Exception {
		File pluginsDir = new File(Options.getEclipseHome(), "plugins/"); //$NON-NLS-1$
		if (!pluginsDir.exists() || !pluginsDir.isDirectory())
			throw new Exception("Plugins directory " + pluginsDir.getAbsolutePath() //$NON-NLS-1$
				+ " does not exists.  Pass a correct -eclipsehome option"); //$NON-NLS-1$
		File[] plugins = pluginsDir.listFiles();
		if(plugins == null) {
			throw new IOException("Content from plugins directory '" + pluginsDir.getAbsolutePath() + "' can not be listed."); //$NON-NLS-1$ //$NON-NLS-2$
		}
		for (File plugin : plugins) {
			String file = plugin.getName();
			if (file.startsWith("org.eclipse.equinox.launcher_") && file.endsWith(".jar") && !plugin.isDirectory()) //$NON-NLS-1$ //$NON-NLS-2$
				return "plugins/" + file; //$NON-NLS-1$
		}
		throw new Exception("Plugins directory " + pluginsDir.getAbsolutePath() //$NON-NLS-1$
				+ " does not contain a valid startup jar.  Pass a correct -eclipsehome option"); //$NON-NLS-1$
	}
	/**
	 * @return Exception
	 */
	public Exception getException() {
		return exception;
	}

	/**
	 * @return int
	 */
	public int getStatus() {
		return status;
	}
	private void printCommand() {
		System.out.println("Launch command is:"); //$NON-NLS-1$
		for (String cmd : cmdarray) {
			System.out.println("  " + cmd); //$NON-NLS-1$
		}

	}
	/**
	 * Forcibly kill Eclipse process.
	 */
	public void killProcess() {
		if (pr != null) {
			pr.destroy();
		}
	}
}
