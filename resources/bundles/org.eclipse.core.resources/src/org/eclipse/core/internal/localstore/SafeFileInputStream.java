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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

/**
 * Given a target and a temporary locations, it tries to read the contents
 * from the target. If a file does not exist at the target location, it tries
 * to read the contents from the temporary location.
 *
 * @see SafeFileOutputStream
 */
public class SafeFileInputStream {

	public static InputStream of(String targetPath, String tempPath) throws IOException {
		Path target = Path.of(targetPath);
		try {
			// we assume the happy path here that the file is present to avoid extra I/O to
			// check if file is actually there, in case of failure (what should be really
			// rare) this will add a small penalty for exception creation).
			return SafeFileOutputStream.read(target, target);
		} catch (FileNotFoundException | NoSuchFileException e) {
			if (tempPath == null) {
				tempPath = targetPath + SafeFileOutputStream.EXTENSION;
			}
			return SafeFileOutputStream.read(Path.of(tempPath), target);
		}
	}
}
