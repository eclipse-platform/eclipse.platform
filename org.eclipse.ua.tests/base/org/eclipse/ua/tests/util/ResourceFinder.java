/*******************************************************************************
 * Copyright (c) 2002, 2016 IBM Corporation and others.
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
package org.eclipse.ua.tests.util;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

/*
 * Utility methods for finding resources.
 */
public class ResourceFinder {

	/*
	 * Finds the specified file in the given plugin and returns a URL to it.
	 */
	public static URL findFile(Bundle plugin, String path) {
		return FileLocator.find(plugin, new Path(path), null);
	}

	/*
	 * Finds and returns URLs to all files in the plugin directory under the given
	 * folder with the given suffix. Can also recursively traverse all subfolders.
	 */
	public static URL[] findFiles(Bundle plugin, String folder, String suffix, boolean recursive) {
		String fullLocation = plugin.getLocation();
		String location = fullLocation.substring(fullLocation.indexOf('@') + 1);
		IPath path = new Path(location).append(folder);
		File file = path.toFile();

		/*
		 * If it's a relative path, append it to the install location.
		 */
		if (!file.exists()) {
			path = new Path(Platform.getInstallLocation().getURL().toString().substring("file:".length()) + path);
			file = path.toFile();
		}

		File[] files = path.toFile().listFiles();
		return findFiles(files, suffix, recursive);
	}

	/*
	 * Finds and returns URLs to all files in the given list that have the given suffix, and
	 * recursively traverses subdirectories if requested.
	 */
	private static URL[] findFiles(File[] files, String suffix, boolean recursive) {
		List<URL> list = new ArrayList<>();
		if (files != null) {
			for (File file : files) {
				if (file.isDirectory()) {
					if (recursive) {
						list.addAll(Arrays.asList(findFiles(file.listFiles(), suffix, recursive)));
					}
				}
				else {
					try {
						URL url = file.toURI().toURL();
						if (url.toString().endsWith(suffix)) {
							list.add(url);
						}
					}
					catch (MalformedURLException e) {
					}
				}
			}
		}

		URL[] array = new URL[list.size()];
		list.toArray(array);
		return array;
	}

}
