/*******************************************************************************
 * Copyright (c) 2004, 2015 IBM Corporation and others.
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
package org.eclipse.core.tests.runtime;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

public class RuntimeTestsPlugin extends Plugin {

	public static final String PI_RUNTIME_TESTS = "org.eclipse.core.tests.runtime"; //$NON-NLS-1$
	public static final String TEST_FILES_ROOT = "Plugin_Testing/";
	private static RuntimeTestsPlugin plugin;
	private BundleContext bundleContext;

	public RuntimeTestsPlugin() {
		plugin = this;
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		this.bundleContext = context;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		context = null;
	}

	public static BundleContext getContext() {
		return plugin != null ? plugin.bundleContext : null;
	}

	public static Plugin getPlugin() {
		return plugin;
	}

	public static boolean delete(File file) {
		if (!file.exists())
			return true;
		if (file.isDirectory()) {
			File[] children = file.listFiles();
			if(children == null) {
				children = new File[0];
			}
			for (File element : children)
				delete(element);
		}
		return file.delete();
	}

	public static String getUniqueString() {
		return System.currentTimeMillis() + "-" + Math.random();
	}

	/*
	 * Copied (kinda) from AbstractProvisioningTest.
	 */
	public static File getTempFolder() {
		String name = getUniqueString();
		String tempDir = System.getProperty("java.io.tmpdir");
		File testFolder = new File(tempDir, name);
		if (testFolder.exists())
			delete(testFolder);
		testFolder.mkdirs();
		return testFolder;
	}

	/*
	 * Look up and return a file handle to the given entry in the bundle.
	 *
	 * Code copied from AbstractProvisioningTest in the p2 tests.
	 */
	public static File getTestData(String entry) {
		if (entry == null)
			return null;
		URL base = RuntimeTestsPlugin.getContext().getBundle().getEntry(entry);
		if (base == null)
			return null;
		try {
			String osPath = new Path(FileLocator.toFileURL(base).getPath()).toOSString();
			File result = new File(osPath);
			return result.getCanonicalPath().equals(result.getPath()) ? result : null;
		} catch (IOException e) {
			return null;
		}
	}

	/*
	 * Copied from AbstractProvisioningTest.
	 */
	public static void copy(File source, File target) throws IOException {
		copy(source, target, null);
	}

	/*
	 * Copy
	 * - if we have a file, then copy the file
	 * - if we have a directory then merge
	 *
	 * Copied from AbstractProvisioningTest.
	 */
	public static void copy(File source, File target, FileFilter filter) throws IOException {
		if (!source.exists())
			return;
		if (source.isDirectory()) {
			if (target.exists() && target.isFile())
				target.delete();
			if (!target.exists())
				target.mkdirs();
			File[] children = source.listFiles(filter);
			if(children == null) {
				throw new IOException("Content from directory '" + source.getAbsolutePath() + "' can not be listed."); //$NON-NLS-1$ //$NON-NLS-2$
			}
			for (File element : children)
				copy(element, new File(target, element.getName()));
			return;
		}
		InputStream input = null;
		OutputStream output = null;
		try {
			input = new BufferedInputStream(new FileInputStream(source));
			output = new BufferedOutputStream(new FileOutputStream(target));

			byte[] buffer = new byte[8192];
			int bytesRead = 0;
			while ((bytesRead = input.read(buffer)) != -1)
				output.write(buffer, 0, bytesRead);
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					System.err.println("Exception while trying to close input stream on: " + source.getAbsolutePath());
					e.printStackTrace();
				}
			}
			if (output != null) {
				try {
					output.close();
				} catch (IOException e) {
					System.err.println("Exception while trying to close output stream on: " + target.getAbsolutePath());
					e.printStackTrace();
				}
			}
		}
	}

}
