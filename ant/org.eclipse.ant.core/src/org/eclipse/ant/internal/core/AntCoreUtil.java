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
package org.eclipse.ant.internal.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eclipse.ant.core.AntCorePlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.osgi.framework.BundleContext;

public class AntCoreUtil {

	private static BundleContext fgContext = null;

	public static void setBundleContext(BundleContext context) {
		fgContext = context;
	}

	public static BundleContext getBundleContext() {
		return fgContext;
	}

	/*
	 * Helper method to ensure an array is converted into an ArrayList.
	 */
	public static ArrayList<String> getArrayList(String[] args) {
		if (args == null) {
			return null;
		}
		// We could be using Arrays.asList() here, but it does not specify
		// what kind of list it will return. We need a list that
		// implements the method List.remove(Object) and ArrayList does.
		ArrayList<String> result = new ArrayList<>(args.length);
		for (String arg : args) {
			result.add(arg);
		}
		return result;
	}

	/*
	 * From a command line list, get the argument for the given parameter. The parameter and its argument are removed from the list.
	 *
	 * @return <code>null</code> if the parameter is not found or an empty String if no arguments are found
	 */
	public static String getArgument(List<String> commands, String param) {
		if (commands == null) {
			return null;
		}
		int index = commands.indexOf(param);
		if (index == -1) {
			return null;
		}
		commands.remove(index);
		if (index == commands.size()) {// if this is the last command
			return IAntCoreConstants.EMPTY_STRING;
		}

		String command = commands.get(index);
		if (command.startsWith("-")) { //new parameter //$NON-NLS-1$
			return IAntCoreConstants.EMPTY_STRING;
		}
		commands.remove(index);
		return command;
	}

	public static void processMinusDProperties(List<String> commands, Map<String, String> userProperties) {
		Iterator<String> iter = commands.iterator();
		while (iter.hasNext()) {
			String arg = iter.next();
			if (arg.startsWith("-D")) { //$NON-NLS-1$
				String name = arg.substring(2, arg.length());
				String value = null;
				int posEq = name.indexOf("="); //$NON-NLS-1$
				if (posEq == 0) {
					value = name.substring(1);
					name = IAntCoreConstants.EMPTY_STRING;
				} else if (posEq > 0 && posEq != name.length() - 1) {
					value = name.substring(posEq + 1).trim();
					name = name.substring(0, posEq);
				}

				if (value == null) {
					// the user has specified something like "-Debug"
					continue;
				}

				userProperties.put(name, value);
				iter.remove();
			}
		}
	}

	public static File getFileRelativeToBaseDir(String fileName, String base, String buildFileLocation) {
		IPath path = IPath.fromOSString(fileName);
		if (!path.isAbsolute()) {
			if (base != null) {
				File baseDir = new File(base);
				// relative to the base dir
				path = IPath.fromOSString(baseDir.getAbsolutePath());
			} else {
				// relative to the build file location
				path = IPath.fromOSString(buildFileLocation);
				path = path.removeLastSegments(1);
			}
			path = path.addTrailingSeparator();
			path = path.append(fileName);
		}

		return path.toFile();
	}

	/**
	 * Returns a list of Properties contained in the list of fileNames.
	 *
	 * @param fileNames
	 *            the names of the properties files to load from
	 * @param base
	 *            the base directory name
	 * @param buildFileLocation
	 * @return a list of {@link Properties} objects for each filename
	 * @throws IOException
	 */
	public static List<Properties> loadPropertyFiles(List<String> fileNames, String base, String buildFileLocation) throws IOException {
		ArrayList<Properties> allProperties = new ArrayList<>(fileNames.size());
		for (String filename : fileNames) {
			File file = getFileRelativeToBaseDir(filename, base, buildFileLocation);
			Properties props = new Properties();
			try (FileInputStream fis = new FileInputStream(file)) {
				props.load(fis);
			}
			Enumeration<?> propertyNames = props.propertyNames();
			while (propertyNames.hasMoreElements()) {
				String name = (String) propertyNames.nextElement();
				String value = props.getProperty(name);
				props.remove(name);
				IStringVariableManager stringVariableManager = VariablesPlugin.getDefault().getStringVariableManager();
				try {
					name = stringVariableManager.performStringSubstitution(name);
					value = stringVariableManager.performStringSubstitution(value);
				}
				catch (CoreException e) {
					AntCorePlugin.log(e);
				}
				props.setProperty(name, value);
			}
			allProperties.add(props);
		}
		return allProperties;
	}
}
