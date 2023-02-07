/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
package org.eclipse.help.internal.browser;

import java.io.*;
import java.nio.charset.StandardCharsets;

import org.eclipse.core.runtime.*;
import org.eclipse.help.browser.*;
import org.eclipse.help.internal.base.*;
import org.eclipse.osgi.util.NLS;

/**
 * Browser adapter for Linux-based browsers supporting -remote openURL command line option i.e.
 * Mozilla and Netscape.
 * <p>
 * The {@link MozillaFactory} creates this adapter.
 * </p>
 */
public class MozillaBrowserAdapter implements IBrowser {
	// delay that it takes mozilla to start responding
	// to remote command after mozilla has been called
	protected static final int DELAY = 5000;

	protected long browserFullyOpenedAt = 0;

	private BrowserThread lastBrowserThread = null;

	private int x, y;

	private int width, height;

	private boolean setLocationPending = false;

	private boolean setSizePending = false;

	protected String executable;

	protected String executableName;

	protected Thread uiThread;

	/**
	 * Constructor
	 *
	 * @param executable     executable filename to launch
	 * @param executableName name of the program to display when error occurs
	 */
	MozillaBrowserAdapter(String executable, String executableName) {
		this.uiThread = Thread.currentThread();
		this.executable = executable;
		this.executableName = executableName;
	}

	@Override
	public void close() {
	}

	@Override
	public void displayURL(String url) {
		if (lastBrowserThread != null)
			lastBrowserThread.exitRequested = true;
		if (setLocationPending || setSizePending) {
			url = createPositioningURL(url);
		}
		lastBrowserThread = new BrowserThread(url);
		lastBrowserThread.start();
		setLocationPending = false;
		setSizePending = false;
	}

	@Override
	public boolean isCloseSupported() {
		return false;
	}

	@Override
	public boolean isSetLocationSupported() {
		return true;
	}

	@Override
	public boolean isSetSizeSupported() {
		return true;
	}

	@Override
	public void setLocation(int x, int y) {
		this.x = x;
		this.y = y;
		setLocationPending = true;
	}

	@Override
	public void setSize(int width, int height) {
		this.width = width;
		this.height = height;
		setSizePending = true;
	}

	private synchronized String createPositioningURL(String url) {
		IPath pluginPath = HelpBasePlugin.getDefault().getStateLocation();
		File outFile = pluginPath.append("mozillaPositon") //$NON-NLS-1$
				.append("position.html") //$NON-NLS-1$
				.toFile();
		try {
			outFile.getParentFile().mkdirs();
			try (PrintWriter writer = new PrintWriter(
					new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile), StandardCharsets.UTF_8)),
					false)) {
				writer.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">"); //$NON-NLS-1$
				writer.println("<html><head>"); //$NON-NLS-1$
				writer.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\">"); //$NON-NLS-1$
				writer.print("<title></title><script type=\"text/javascript\">"); //$NON-NLS-1$
				if (setSizePending)
					writer.print("window.resizeTo(" + width + "," + height + ");"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				if (setLocationPending)
					writer.print("window.moveTo(" + x + "," + y + ");"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				writer.print("location.replace(\"" + url + "\");"); //$NON-NLS-1$ //$NON-NLS-2$
				writer.print("</script></head><body>"); //$NON-NLS-1$
				writer.print("<a href=\"" + url + "\">--&gt;</a>"); //$NON-NLS-1$ //$NON-NLS-2$
				writer.print("</body></html>"); //$NON-NLS-1$
			}
			return "file://" + outFile.getAbsolutePath(); //$NON-NLS-1$
		} catch (IOException ioe) {
			// return the original url
			return url;
		}
	}

	private class BrowserThread extends Thread {
		public boolean exitRequested = false;

		private String url;

		public BrowserThread(String urlName) {
			this.url = urlName;
		}

		/**
		 * @param browserCmd
		 * @return int 0 if success
		 */
		@SuppressWarnings("resource")
		private int openBrowser(String browserCmd) {
			try {
				Process pr = Runtime.getRuntime().exec(browserCmd);
				StreamConsumer outputs = new StreamConsumer(pr.getInputStream());
				(outputs).start();
				StreamConsumer errors = new StreamConsumer(pr.getErrorStream());
				(errors).start();
				pr.waitFor();
				int ret = pr.exitValue();

				if (ret == 0 && errorsInOutput(outputs, errors)) {
					return -1;
				}
				return ret;
			} catch (InterruptedException e) {
			} catch (IOException e) {
				Platform.getLog(getClass()).error("Launching " + executableName + " has failed.", e); //$NON-NLS-1$ //$NON-NLS-2$
				String msg = NLS.bind(HelpBaseResources.MozillaBrowserAdapter_executeFailed, executableName);
				BaseHelpSystem.getDefaultErrorUtil()
						.displayError(msg, uiThread);
				// return success, so second command does not execute
				return 0;
			}
			return -1;
		}

		/**
		 * On some OSes 0 is always returned by netscape -remote. It is
		 * necessary to examine output to find out failure
		 *
		 * @param outputs
		 * @param errors
		 * @return
		 */
		private boolean errorsInOutput(StreamConsumer outputs, StreamConsumer errors) {
			try {
				outputs.join(1000);
				if (outputs.getLastLine() != null && (outputs.getLastLine().contains("No running window found") //$NON-NLS-1$
						|| outputs.getLastLine().contains("not running on display"))) {//$NON-NLS-1$
					return true;
				}
				errors.join(1000);
				if (errors.getLastLine() != null && (errors.getLastLine().contains("No running window found") //$NON-NLS-1$
						|| errors.getLastLine().contains("not running on display"))) {//$NON-NLS-1$

					return true;
				}
			} catch (InterruptedException ie) {
				// ignore
			}
			return false;
		}

		@Override
		public void run() {
			// If browser is opening, wait until it fully opens,
			waitForBrowser();
			if (exitRequested)
				return;
			if (openBrowser(executable + " -remote openURL(" + url + ")") == 0) {//$NON-NLS-1$ //$NON-NLS-2$
				return;
			}
			if (exitRequested)
				return;
			browserFullyOpenedAt = System.currentTimeMillis() + DELAY;
			openBrowser(executable + " " + url); //$NON-NLS-1$
		}

		private void waitForBrowser() {
			while (System.currentTimeMillis() < browserFullyOpenedAt)
				try {
					if (exitRequested)
						return;
					Thread.sleep(100);
				} catch (InterruptedException ie) {
				}
		}
	}
}
