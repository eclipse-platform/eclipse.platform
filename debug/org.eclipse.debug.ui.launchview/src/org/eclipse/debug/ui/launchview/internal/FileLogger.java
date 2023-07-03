/*******************************************************************************
 * Copyright (c) 2017, 2019 SSI Schaefer IT Solutions GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SSI Schaefer IT Solutions GmbH
 *******************************************************************************/
package org.eclipse.debug.ui.launchview.internal;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.eclipse.core.runtime.ILog;
import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.osgi.util.NLS;

/**
 * Logger that can be attached to a {@linkplain IProcess} and that writes the
 * output to a file.
 * <p>
 * Please note that it is the responsibility of the caller to close the logger
 * if it is not used any more.
 * </p>
 */
public class FileLogger implements IStreamListener, Closeable {

	private final BufferedWriter writer;

	/** Creates a new logger that writes to the given file */
	public FileLogger(File file) throws IOException {
		writer = new BufferedWriter(new FileWriter(file));
	}

	@Override
	public void streamAppended(String text, IStreamMonitor monitor) {
		try {
			writer.write(text);
			writer.flush();
		} catch (Exception ex) {
			ILog.of(this.getClass()).warn(NLS.bind(LaunchViewMessages.FileLogger_FailedAppend, text), ex);
		}
	}

	@Override
	public void close() throws IOException {
		writer.close();
	}

}
