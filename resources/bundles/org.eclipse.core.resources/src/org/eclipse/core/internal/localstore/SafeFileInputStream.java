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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.NoSuchFileException;

/**
 * Given a target and a temporary locations, it tries to read the contents
 * from the target. If a file does not exist at the target location, it tries
 * to read the contents from the temporary location.
 *
 * @see SafeFileOutputStream
 */
public class SafeFileInputStream {
	protected static final String EXTENSION = ".bak"; //$NON-NLS-1$

	public static InputStream of(String targetPath, String tempPath) throws IOException {
		File target = new File(targetPath);
		try {
			return new ByteArrayInputStream(SafeFileOutputStream.read(target.toPath()));
		} catch (FileNotFoundException | NoSuchFileException e) {
			if (tempPath == null)
				tempPath = target.getAbsolutePath() + EXTENSION;
			target = new File(tempPath);
			return new ByteArrayInputStream(SafeFileOutputStream.read(target.toPath()));
		}
	}
}
