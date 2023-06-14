/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
package org.eclipse.help.internal.browser;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.eclipse.help.internal.base.HelpBasePlugin;

/**
 * Log for messages output by external browser processes.
 */
public class BrowserLog {
	private String logFileName;
	private boolean newSession;
	private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy kk:mm:ss.SS") //$NON-NLS-1$
			.withZone(ZoneId.systemDefault());
	String LN = System.lineSeparator();

	private static class LogHolder {
		static final BrowserLog instance = new BrowserLog();
	}
	/**
	 * Constructor
	 */
	private BrowserLog() {
		try {
			newSession = true;
			logFileName = HelpBasePlugin.getDefault().getStateLocation()
					.append("browser.log") //$NON-NLS-1$
					.toOSString();
		} catch (Exception e) {
			// can get here if platform is shutting down
		}
	}
	/**
	 * Obtains singleton
	 */
	private static BrowserLog getInstance() {
		return LogHolder.instance;
	}
	/**
	 * Appends a line to the browser.log
	 */
	public static synchronized void log(String message) {
		getInstance().append(message);
	}
	private void append(String message) {
		if (logFileName == null) {
			return;
		}
		try (Writer outWriter = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(logFileName, true), StandardCharsets.UTF_8))) {
			if (newSession) {
				newSession = false;
				outWriter.write(LN + formatter.format(Instant.now())
						+ " NEW SESSION" + LN); //$NON-NLS-1$
			}
			outWriter.write(formatter.format(Instant.now()) + " " + message + LN); //$NON-NLS-1$
			outWriter.flush();
			outWriter.close();
		} catch (Exception e) {
		}
	}
}
