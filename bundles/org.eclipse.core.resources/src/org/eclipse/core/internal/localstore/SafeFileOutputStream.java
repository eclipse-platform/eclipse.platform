/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others.
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
 *     James Blackburn (Broadcom Corp.) - ongoing development
 *******************************************************************************/
package org.eclipse.core.internal.localstore;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * This class should be used when there's a file already in the
 * destination and we don't want to lose its contents if a
 * failure writing this stream happens.
 * Basically, the new contents are written to a temporary location.
 * If everything goes OK, it is moved to the right place.
 */
public class SafeFileOutputStream extends OutputStream {
	protected File temp;
	protected File target;
	protected OutputStream output;
	protected boolean failed;
	protected static final String EXTENSION = ".bak"; //$NON-NLS-1$

	/**
	 * Creates an output stream on a file at the given location
	 * @param file The file to be written to
	 */
	public SafeFileOutputStream(File file) throws IOException {
		this(file.getAbsolutePath(), null);
	}

	/**
	 * Creates an output stream on a file at the given location
	 * @param targetPath The file to be written to
	 * @param tempPath The temporary location to use, or <code>null</code> to
	 * use the same location as the target path but with a different extension.
	 */
	public SafeFileOutputStream(String targetPath, String tempPath) throws IOException {
		failed = false;
		target = new File(targetPath);
		createTempFile(tempPath);
		if (!target.exists()) {
			if (!temp.exists()) {
				output = new BufferedOutputStream(new FileOutputStream(target));
				return;
			}
			// If we do not have a file at target location, but we do have at temp location,
			// it probably means something wrong happened the last time we tried to write it.
			// So, try to recover the backup file. And, if successful, write the new one.
			Files.copy(temp.toPath(), target.toPath());
		}
		output = new BufferedOutputStream(new FileOutputStream(temp));
	}

	@Override
	public void close() throws IOException {
		try {
			output.close();
		} catch (IOException e) {
			failed = true;
			throw e; // rethrow
		}
		if (failed)
			temp.delete();
		else
			commit();
	}

	protected void commit() throws IOException {
		if (!temp.exists())
			return;
		Files.copy(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
		temp.delete();
	}

	protected void createTempFile(String tempPath) {
		if (tempPath == null)
			tempPath = target.getAbsolutePath() + EXTENSION;
		temp = new File(tempPath);
	}

	@Override
	public void flush() throws IOException {
		try {
			output.flush();
		} catch (IOException e) {
			failed = true;
			throw e; // rethrow
		}
	}

	public String getTempFilePath() {
		return temp.getAbsolutePath();
	}

	@Override
	public void write(int b) throws IOException {
		try {
			output.write(b);
		} catch (IOException e) {
			failed = true;
			throw e; // rethrow
		}
	}
}
