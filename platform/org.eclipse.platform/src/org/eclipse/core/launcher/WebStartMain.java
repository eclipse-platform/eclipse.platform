/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.launcher;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.jar.JarFile;

/**
 * The launcher ot start eclipse using webstart.
 * To use this launcher, the client must accept to give all security permissions. 
 */
//The bundles are discovered by finding all the jars on the classpath. Then they are added with their full path to the osgi.bundles list.
public class WebStartMain extends Main {
	private static final String PROP_WEBSTART_AUTOMATIC_INSTALLATION = "eclipse.webstart.automaticInstallation"; //$NON-NLS-1$
	private static final String DEFAULT_OSGI_BUNDLES = "org.eclipse.core.runtime@2:start"; //$NON-NLS-1$
	private static final String PROP_OSGI_BUNDLES = "osgi.bundles"; //$NON-NLS-1$

	//List all the jars that are on the classpath
	private String[] allJars = null;
	//The list all the bundles that have been to the bundle list 
	private Set onTheBundleList = new HashSet();

	public static void main(String[] args) {
		System.setSecurityManager(null);	 //Hack so that when the classloader loading the fwk is created we don't have funny permissions. This should be revisited. 
		int result = new WebStartMain().run(args);
		System.exit(result);
	}

	private void setDefaultBundles() {
		if (System.getProperty(PROP_OSGI_BUNDLES) != null)
			return;
		System.getProperties().put(PROP_OSGI_BUNDLES, DEFAULT_OSGI_BUNDLES);
	}

	protected void basicRun(String[] args) throws Exception {
		//Set the fwk location since the regular lookup would not find it
		String fwkURL = searchFor(framework, null);
		System.setProperty(PROP_FRAMEWORK, fwkURL);
		onTheBundleList.add(fwkURL);
		super.basicRun(args);
	}

	protected URL[] getBootPath(String base) throws IOException {
		URL[] result = super.getBootPath(base);
		setDefaultBundles();
		convertBundleList();
		addAllBundlesToBundleList();
		cleanup();
		return result;
	}

	/*
	 * Null out all the fields containing data 
	 */
	private void cleanup() {
		allJars = null;
		onTheBundleList = null;
	}

	protected String searchFor(final String target, String start) {
        //The searching can be improved 
		String[] jars = getAllJars();
		ArrayList selected = new ArrayList(3);
		for (int i = 0; i < jars.length; i++) {
			if (jars[i].indexOf(target) != -1)
				selected.add(jars[i]);
		}
		if (selected.size() == 0)
			return null;

		String[] selectedJars = new String[selected.size()];
		for (int i = 0; i < selected.size(); i++) {
			selectedJars[i] = extractFileName((String) selected.get(i));
		}
		if (debug)
			System.out.println(extractInnerURL((String) selected.get(findMax(selectedJars))));
		return extractInnerURL((String) selected.get(findMax(selectedJars)));
	}

	/*
	 * Extract the file name of the url - given that a file name
	 */
	private String extractFileName(String url) {
		String innerURL = extractInnerURL(url);
		return innerURL.substring(innerURL.lastIndexOf("/") + 1);
	}

	/* 
	 * Find all the jars available on the webstart classpath
	 */
	private String[] getAllJars() {
		if (allJars != null)
			return allJars;

		ArrayList collector = new ArrayList();
		try {
			Enumeration resources = WebStartMain.class.getClassLoader().getResources(JarFile.MANIFEST_NAME);
			while (resources.hasMoreElements()) {
				collector.add(((URL) resources.nextElement()).toExternalForm());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		allJars = new String[collector.size()];
		collector.toArray(allJars);
		if (debug)
			printArray("Jars found on the webstart path:\n", allJars); //$NON-//$NON-NLS-1$

		return allJars;
	}

	private String extractInnerURL(String url) {
		if (url.startsWith(JAR_SCHEME)) {
			url = url.substring(url.indexOf(JAR_SCHEME) + 4);
		}
		int lastBang = url.lastIndexOf('!');
		if (lastBang != -1) {
			url = url.substring(0, lastBang);
		}
		return decode(url);
	}

	/*
	 * convert the values on the osgi bundle list from: name@... to <bundleurl>@...
	 */
	private void convertBundleList() {
		final char SEPARATOR = '@';

		if (debug)
			System.out.println("Osgi bundles before conversion:\n" + System.getProperty(PROP_OSGI_BUNDLES)); //$NON-NLS-1$

		String[] bundles = getArrayFromList(System.getProperty(PROP_OSGI_BUNDLES));
		String result = "";
		for (int i = 0; i < bundles.length; i++) {
			//a entry in the bundle list is made of two parts: the bundle to install followed by an optional info. The separator for those is @
			String bundle = bundles[i];
			int positionExtraInfo = bundle.indexOf(SEPARATOR);
			String bundleName = null;
			if (positionExtraInfo == -1)
				bundleName = bundle;
			else
				bundleName = bundle.substring(0, positionExtraInfo);

			String bundleURL = searchFor(bundleName, null);
			if (bundleURL == null) {
				if (debug)
					System.out.println("Could not find " + bundleName); //$NON-NLS-1$
				continue;
			}
			onTheBundleList.add(bundleURL);
			bundleURL = REFERENCE_SCHEME + bundleURL;
			result += bundleURL;
			if (positionExtraInfo != -1)
				result += bundle.substring(positionExtraInfo);
			result += ',';
		}
		System.setProperty(PROP_OSGI_BUNDLES, result);
	}

	/*
	 * Add to the bundle list all the jars found the webstart classpath
	 */
    //This code can be improved
	private void addAllBundlesToBundleList() {
		if ("false".equalsIgnoreCase(System.getProperties().getProperty(PROP_WEBSTART_AUTOMATIC_INSTALLATION))) //$NON-NLS-1$
			return;

		String[] jarsOnClasspath = getAllJars();
		String[] result = new String[jarsOnClasspath.length];
		for (int i = 0; i < jarsOnClasspath.length; i++) {
			onTheBundleList.contains(jarsOnClasspath[i]);
			result[i] = REFERENCE_SCHEME + extractInnerURL(jarsOnClasspath[i]);
		}
		System.setProperty(PROP_OSGI_BUNDLES, System.getProperty(PROP_OSGI_BUNDLES) + arrayToString(result, ','));

		if (debug)
			printArray("Bundles list:\n", result);
	}

	private void printArray(String header, String[] values) {
		System.out.println(header); //$NON-NLS-1$
		for (int i = 0; i < values.length; i++) {
			System.out.println("\t" + values[i]); //$NON-NLS-1$
		}
	}

	private String arrayToString(String[] array, char separator) {
		StringBuffer result = new StringBuffer();
		for (int i = 0; i < array.length - 1; i++) {
			result.append(array[i]).append(separator);
		}
		result.append(array[array.length - 1]);
		return new String(result);
	}

}
