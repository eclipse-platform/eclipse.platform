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

import org.eclipse.core.runtime.ILog;

/**
 * Used to receive output from processes
 */
public class StreamConsumer extends Thread {
	BufferedReader bReader;

	private String lastLine;

	public StreamConsumer(InputStream inputStream) {
		super();
		setDaemon(true);
		bReader = new BufferedReader(new InputStreamReader(inputStream));
	}

	@Override
	public void run() {
		try {
			String line;
			while (null != (line = bReader.readLine())) {
				lastLine = line;
				BrowserLog.log(line);
			}
			bReader.close();
		} catch (IOException ioe) {
			ILog.of(getClass()).error(
					"Exception occurred reading from web browser.", ioe); //$NON-NLS-1$
		}
	}

	/**
	 * @return last line obtained or null
	 */
	public String getLastLine() {
		return lastLine;
	}

}
