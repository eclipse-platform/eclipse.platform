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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * The launcher ot start eclipse using webstart.
 * To use this launcher, the client must accept to give all security permissions. 
 */
//The bundles are discovered by finding all the jars on the classpath. Then they are added with their full path to the osgi.bundles list.
public class WebStartMain extends Main {
	private static final String PROP_WEBSTART_AUTOMATIC_INSTALLATION = "eclipse.webstart.automaticInstallation"; //$NON-NLS-1$
	private static final String DEFAULT_OSGI_BUNDLES = "org.eclipse.core.runtime@2:start"; //$NON-NLS-1$
	private static final String PROP_OSGI_BUNDLES = "osgi.bundles"; //$NON-NLS-1$
	private static final String PROP_WEBSTART_PRECISE_BUNDLEID = "eclipse.webstart.preciseBundleId"; //$NON-NLS-1$
	

	private String[] allJars = null; 	//List all the jars that are on the classpath
	private Map bundleList = null; //Map an entry (the part before the @) from the osgi.bundles list to a list of URLs. Ie: org.eclipse.core.runtime --> file:c:/foo/org.eclipse.core.runtime_3.1.0/..., file:c:/bar/org.eclipse.core.runtime/... 
	private Map bundleStartInfo = null; //Keep track of the start level info for each bundle from the osgi.bundle list.
	
	private boolean preciseIdExtraction = false; //Flag indicating if the extraction of the id must be done by looking at bundle ids.
	
	public static void main(String[] args) {
		System.setSecurityManager(null); //TODO Hack so that when the classloader loading the fwk is created we don't have funny permissions. This should be revisited. 
		int result = new WebStartMain().run(args);
		System.exit(result);
	}

	private void setDefaultBundles() {
		if (System.getProperty(PROP_OSGI_BUNDLES) != null)
			return;
		System.getProperties().put(PROP_OSGI_BUNDLES, DEFAULT_OSGI_BUNDLES);
	}

	protected void basicRun(String[] args) throws Exception {
		preciseIdExtraction = Boolean.getBoolean(PROP_WEBSTART_PRECISE_BUNDLEID);
		setDefaultBundles();
		addOSGiBundle();
		initializeBundleListStructure();
		mapURLsToBundleList();
		//Set the fwk location since the regular lookup would not find it
		String fwkURL = searchFor(framework, null);
		System.getProperties().put(PROP_FRAMEWORK, fwkURL);
		super.basicRun(args);
	}

	private void addOSGiBundle() {
		//Add osgi to the bundle list, so we can beneficiate of the infrastructure to find its location. It will be removed from the list later on 
		System.getProperties().put(PROP_OSGI_BUNDLES, System.getProperty(PROP_OSGI_BUNDLES) + ',' + framework);
	}

	protected URL[] getBootPath(String base) throws IOException {
		URL[] result = super.getBootPath(base);
		buildOSGiBundleList();
		cleanup();
		return result;
	}

	/*
	 * Null out all the fields containing data 
	 */
	private void cleanup() {
		allJars = null;
		bundleList = null;
		bundleStartInfo = null;
	}

	/*
	 * Find the target bundle among all the jars that are on the classpath.
	 * The start parameter is not used in this context
	 */
	protected String searchFor(final String target, String start) {
		ArrayList matches = (ArrayList) bundleList.get(target);
		int numberOfURLs = matches.size();
		if (numberOfURLs == 1) {
			return extractInnerURL((String) matches.get(0));
		}
		if (numberOfURLs == 0)
			return null;
		String urls[] = new String[numberOfURLs];
		return extractInnerURL(urls[findMax((String[]) matches.toArray(urls))]);
	}

	/* 
	 * Get all the jars available on the webstart classpath
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

	/*
	 * Extract the inner URL from a string representing a JAR url string.
	 */
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

	private void printArray(String header, String[] values) {
		System.err.println(header); //$NON-NLS-1$
		for (int i = 0; i < values.length; i++) {
			System.err.println("\t" + values[i]); //$NON-NLS-1$
		}
	}


	/*
	 * Initialize the data structure corresponding to the osgi.bundles list
	 */
	private void initializeBundleListStructure() {
		final char STARTLEVEL_SEPARATOR = '@';

		//In webstart the bundles list can only contain bundle names with or without a version.
		String prop = System.getProperty(PROP_OSGI_BUNDLES);
		if (prop == null || prop.trim().equals("")) { //$NON-NLS-1$
			bundleList = new HashMap(0);
			return;
		}

		bundleList = new HashMap(10);
		bundleStartInfo = new HashMap(10);
		StringTokenizer tokens = new StringTokenizer(prop, ","); //$NON-NLS-1$
		while (tokens.hasMoreTokens()) {
			String token = tokens.nextToken().trim();
			String bundleId = token;
			if (token.equals("")) //$NON-NLS-1$
				continue;
			int startLevelSeparator;
			if ((startLevelSeparator = token.lastIndexOf(STARTLEVEL_SEPARATOR)) != -1) {
				bundleId = token.substring(0, startLevelSeparator);
				bundleStartInfo.put(bundleId, token.substring(startLevelSeparator));
			}
			bundleList.put(bundleId, new ArrayList(1)); // put a list with one element as it is likely that the element will be present
		}
		
	}

	/*
	 * Associate urls from the list of jars with a bundle from the bundle list
	 */
	private void mapURLsToBundleList() {
		String[] allJars = getAllJars();
		for (int i = 0; i < allJars.length; i++) {
			String bundleId = extractBundleId(allJars[i]);
			if (bundleId == null)
				continue;
			ArrayList bundleURLs = (ArrayList) bundleList.get(bundleId);
			if (bundleURLs == null) {
				int versionIdPosition = bundleId.lastIndexOf('_');
				if (versionIdPosition == -1)
					continue;
				bundleURLs = (ArrayList) bundleList.get(bundleId.subSequence(0, versionIdPosition));
				if (bundleURLs == null)
					continue;
			}
			bundleURLs.add(allJars[i]);
			allJars[i] = null; //Remove the entry from the list
		}
	}

	/*
	 * return a string of the form <bundle>_<version>
	 */
	private String extractBundleId(String url) {
		if (preciseIdExtraction)
			return extractBundleIdFromManifest(url);
		else 
			return extractBundleIdFromBundleURL(url);
	}

	private String extractBundleIdFromManifest(String url) {
		final String BUNDLE_SYMBOLICNAME = "Bundle-SymbolicName"; //$NON-NLS-1$
		final String BUNDLE_VERSION = "Bundle-Version"; //$NON-NLS-1$
		
		Manifest mf;
		try {
			mf = new Manifest(new URL(url).openStream()); 
			String symbolicNameString = mf.getMainAttributes().getValue(BUNDLE_SYMBOLICNAME);
			if (symbolicNameString==null)
				return null;
			
			String bundleVersion = mf.getMainAttributes().getValue(BUNDLE_VERSION);
			if (bundleVersion == null)
				bundleVersion = ""; //$NON-NLS-1$
			else
				bundleVersion = '_' + bundleVersion;
			
			int pos = symbolicNameString.lastIndexOf(';');
			if (pos != -1)
				return symbolicNameString.substring(0, pos) + bundleVersion;
			return symbolicNameString + bundleVersion;
		} catch (MalformedURLException e) {
			//Ignore
		} catch (IOException e) {
			//Ignore
		}
		return null;

	}

	private String extractBundleIdFromBundleURL(String url) {
		int lastBang = url.lastIndexOf('!');
		if (lastBang == -1)
			return null;
		boolean jarSuffix = url.regionMatches(true, lastBang - 4, ".jar", 0, 4); //$NON-NLS-1$
		int bundleIdStart = url.lastIndexOf('/', lastBang);
		return url.substring(bundleIdStart + 3, lastBang - (jarSuffix ? 4 : 0)); // + 3 because URLs from webstart have a funny prefix
	}
	
	private void buildOSGiBundleList() {
		//Remove the framework from the bundle list because it does not need to be installed. See addOSGiBundle
		bundleList.remove(framework);

		String[] jarsOnClasspath = getAllJars();
		StringBuffer finalBundleList = new StringBuffer(jarsOnClasspath.length * 25);
		
		//Add the bundles from the bundle list.
		Collection allSelectedBundles = bundleList.entrySet();
		for (Iterator iter = allSelectedBundles.iterator(); iter.hasNext();) {
			Map.Entry entry = (Map.Entry) iter.next();
			ArrayList matches = (ArrayList) entry.getValue();
			int numberOfURLs = matches.size();
			
			//Get the start info
			String startInfo = (String) bundleStartInfo.get(entry.getKey());
			if (startInfo == null)
				startInfo = ""; //$NON-NLS-1$
			
			if (numberOfURLs == 1) {
				finalBundleList.append(REFERENCE_SCHEME).append(extractInnerURL((String) matches.get(0))).append(startInfo).append(',');
				continue;
			}
			if (numberOfURLs == 0)
				continue;
			String urls[] = new String[numberOfURLs];
			int found = findMax((String[]) matches.toArray(urls));
			for (int i = 0; i < urls.length; i++) {
				if (i != found)
					continue;
				finalBundleList.append(REFERENCE_SCHEME).append(extractInnerURL((String) urls[found])).append(startInfo).append(',');
			}
		}
		
		//Add all the other bundles if required - the common case is to add those
		if (! Boolean.FALSE.toString().equalsIgnoreCase(System.getProperties().getProperty(PROP_WEBSTART_AUTOMATIC_INSTALLATION))) {
			for (int i = 0; i < jarsOnClasspath.length; i++) {
				if (jarsOnClasspath[i] != null)
					finalBundleList.append(REFERENCE_SCHEME).append(extractInnerURL(jarsOnClasspath[i])).append(',');
			}			
		}

		System.getProperties().put(PROP_OSGI_BUNDLES, finalBundleList.toString());
		if (debug)
			log(finalBundleList.toString()); //$NON-NLS-1$
	}

}
