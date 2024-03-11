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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * This class should be used when there's a file already in the
 * destination and we don't want to lose its contents if a
 * failure writing this stream happens.
 * Basically, the new contents are written to a temporary location.
 * If everything goes OK, it is moved to the right place.
 */
public class SafeFileOutputStream extends OutputStream {
	private final String tempPath;
	private final String targetPath;
	private final ByteArrayOutputStream output;
	private static final String EXTENSION = ".bak"; //$NON-NLS-1$
	private static final Map<Path, Integer> fileHashes = Collections.synchronizedMap(new HashMap<>());

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
	@SuppressWarnings("unused")
	public SafeFileOutputStream(String targetPath, String tempPath) throws IOException {
		this.tempPath = tempPath != null ? tempPath : (targetPath + EXTENSION);
		this.targetPath = targetPath;
		output = new ByteArrayOutputStream();
	}

	@Override
	public void close() throws IOException {
		File target = new File(targetPath);
		Path targetP = target.toPath();
		File temp = getTempFile();
		byte[] newContent = output.toByteArray();
		Integer newHash = hash(newContent);
		Integer oldHash = fileHashes.put(targetP, newHash);
		if (!target.exists()) {
			if (!temp.exists()) {
				Files.write(targetP, newContent);
				return;
			}
			// If we do not have a file at target location, but we do have at temp location,
			// it probably means something wrong happened the last time we tried to write
			// it.
			// So, try to recover the backup file. And, if successful, write the new one.
			Files.copy(temp.toPath(), targetP);
		}

		if (Objects.equals(oldHash, newHash)) {
			// quick path: since hash did not change it is likely that content did not
			// change:
			byte[] oldContent = Files.readAllBytes(targetP);
			if (Arrays.equals(oldContent, newContent)) {
				return;
			}
		}

		try {
			Files.write(temp.toPath(), newContent);
			commit(temp, targetP);
		} catch (IOException e) {
			temp.delete();
			throw e; // rethrow
		}
	}

	private void commit(File temp, Path targetP) throws IOException {
		if (!temp.exists())
			return;
		Files.copy(temp.toPath(), targetP, StandardCopyOption.REPLACE_EXISTING);
		temp.delete();
	}

	private File getTempFile() {
		return new File(tempPath);
	}

	@Override
	public void flush() throws IOException {
		output.flush();
	}

	public String getTempFilePath() {
		return getTempFile().getAbsolutePath();
	}

	@SuppressWarnings("unused")
	@Override
	public void write(int b) throws IOException {
		output.write(b);
	}

	private static int hash(byte[] content) {
		return Arrays.hashCode(content) + content.length;
	}

	public static byte[] read(Path path) throws IOException {
		byte[] content = Files.readAllBytes(path);
		fileHashes.put(path, hash(content));
		return content;
	}
}
