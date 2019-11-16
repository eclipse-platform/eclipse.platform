/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corporation and others.
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
package org.eclipse.compare.tests;

import java.io.*;
import java.net.URL;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.eclipse.compare.internal.Utilities;
import org.eclipse.compare.patch.PatchConfiguration;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.*;
import org.junit.Assert;
import org.osgi.framework.Bundle;

public class PatchUtils {

	public static final String PATCHDATA = "patchdata";

	public static class PatchTestConfiguration {
		String subfolderName;
		PatchConfiguration pc;
		String patchFileName;
		String[] originalFileNames;
		String[] expectedFileNames;
		String[] actualFileNames;
	}

	public static class StringStorage implements IStorage {
		String fileName;

		public StringStorage(String old) {
			fileName = old;
		}

		@Override
		public <T> T getAdapter(Class<T> adapter) {
			return null;
		}

		@Override
		public boolean isReadOnly() {
			return false;
		}

		@Override
		public String getName() {
			return fileName;
		}

		@Override
		public IPath getFullPath() {
			return null;
		}

		@Override
		public InputStream getContents() throws CoreException {
			return new BufferedInputStream(asInputStream(fileName));
		}
	}

	public static class FileStorage implements IStorage {
		File file;

		public FileStorage(File file) {
			this.file = file;
		}

		@Override
		public InputStream getContents() throws CoreException {
			try {
				return new FileInputStream(file);
			} catch (FileNotFoundException e) {
				// ignore, should never happen
			}
			return null;
		}

		@Override
		public IPath getFullPath() {
			return new Path(file.getAbsolutePath());
		}

		@Override
		public String getName() {
			return file.getName();
		}

		@Override
		public boolean isReadOnly() {
			return true;
		}

		@Override
		public <T> T getAdapter(Class<T> adapter) {
			return null;
		}
	}

	public static class JarEntryStorage implements IStorage {
		JarEntry jarEntry;
		JarFile jarFile;

		public JarEntryStorage(JarEntry jarEntry, JarFile jarFile) {
			this.jarEntry = jarEntry;
			this.jarFile = jarFile;
		}

		@Override
		public InputStream getContents() throws CoreException {
			try {
				return jarFile.getInputStream(jarEntry);
			} catch (IOException e) {
				// ignore, should never happen
			}
			return null;
		}

		@Override
		public IPath getFullPath() {
			return new Path(jarFile.getName());
		}

		@Override
		public String getName() {
			return jarEntry.getName();
		}

		@Override
		public boolean isReadOnly() {
			return true;
		}

		@Override
		public <T> T getAdapter(Class<T> adapter) {
			return null;
		}
	}

	public static String asString(InputStream exptStream) throws IOException {
		return Utilities.readString(exptStream, ResourcesPlugin.getEncoding());
	}

	public static InputStream asInputStream(String name) {
		IPath path = new Path(PATCHDATA).append(name);
		try {
			URL url = new URL(getBundle().getEntry("/"), path.toString());
			return url.openStream();
		} catch (IOException e) {
			Assert.fail("Failed while reading " + name);
			return null; // never reached
		}
	}

	public static BufferedReader getReader(String name) {
		InputStream resourceAsStream = PatchUtils.asInputStream(name);
		InputStreamReader reader2 = new InputStreamReader(resourceAsStream);
		return new BufferedReader(reader2);
	}

	public static Bundle getBundle() {
		return CompareTestPlugin.getDefault().getBundle();
	}

}
