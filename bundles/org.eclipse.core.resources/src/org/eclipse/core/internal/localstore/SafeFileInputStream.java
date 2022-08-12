/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
package org.eclipse.core.internal.localstore;

import java.io.*;

/**
 * Given a target and a temporary locations, it tries to read the contents
 * from the target. If a file does not exist at the target location, it tries
 * to read the contents from the temporary location.
 *
 * @see SafeFileOutputStream
 */
public class SafeFileInputStream extends FilterInputStream {
	protected static final String EXTENSION = ".bak"; //$NON-NLS-1$
	private static final int DEFAUT_BUFFER_SIZE = 2048;

	public SafeFileInputStream(File file) throws IOException {
		this(file.getAbsolutePath(), null);
	}

	/**
	 * If targetPath is null, the file will be created in the default-temporary directory.
	 */
	public SafeFileInputStream(String targetPath, String tempPath) throws IOException {
		super(getInputStream(targetPath, tempPath, DEFAUT_BUFFER_SIZE));
	}

	/**
	 * If targetPath is null, the file will be created in the default-temporary directory.
	 */
	public SafeFileInputStream(String targetPath, String tempPath, int bufferSize) throws IOException {
		super(getInputStream(targetPath, tempPath, bufferSize));
	}

	private static InputStream getInputStream(String targetPath, String tempPath, int bufferSize) throws IOException {
		File target = new File(targetPath);
		if (!target.exists()) {
			if (tempPath == null)
				tempPath = target.getAbsolutePath() + EXTENSION;
			target = new File(tempPath);
		}
		return new BufferedInputStream(new FileInputStream(target), bufferSize);
	}
}
