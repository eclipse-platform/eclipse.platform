/*******************************************************************************
 * Copyright (c) 2005, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Chris Aniszczyk (IBM Corp.) - Fixed NPE
 *******************************************************************************/
package org.eclipse.update.internal.core;

import org.eclipse.update.core.IUpdateConstants;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.util.NLS;
import org.eclipse.update.core.*;
import org.eclipse.update.core.model.InstallAbortedException;
import org.eclipse.update.internal.core.connection.ConnectionFactory;
import org.eclipse.update.internal.core.connection.IResponse;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * 
 */
public class UpdateManagerUtils {

	private static boolean OS_UNIX = org.eclipse.osgi.service.environment.Constants.OS_HPUX
			.equals(Platform.getOS())
			|| org.eclipse.osgi.service.environment.Constants.OS_AIX
					.equals(Platform.getOS())
			|| org.eclipse.osgi.service.environment.Constants.OS_LINUX
					.equals(Platform.getOS())
			|| org.eclipse.osgi.service.environment.Constants.OS_SOLARIS
					.equals(Platform.getOS())
			|| org.eclipse.osgi.service.environment.Constants.OS_MACOSX
					.equals(Platform.getOS());
	private static FragmentEntry[] noFragments = new FragmentEntry[0];
	private static Map table;

	static {
		table = new HashMap();
		table.put("compatible", new Integer(IUpdateConstants.RULE_COMPATIBLE)); //$NON-NLS-1$
		table.put("perfect", new Integer(IUpdateConstants.RULE_PERFECT)); //$NON-NLS-1$
		table.put("equivalent", new Integer(IUpdateConstants.RULE_EQUIVALENT)); //$NON-NLS-1$
		table.put("greaterOrEqual", new Integer(IUpdateConstants.RULE_GREATER_OR_EQUAL)); //$NON-NLS-1$
	}

	// manage URL to File
	private static Map urlFileMap;

	private static Map localFileFragmentMap;
	private static Stack bufferPool;
	private static final int BUFFER_SIZE = 4096; // 4kbytes
	private static final int INCREMENT_SIZE = 10240; // 10kbytes
	/**
	 * return the urlString if it is a absolute URL
	 * otherwise, return the default URL if the urlString is null
	 * The defaultURL may point ot a file, create a file URL then
	 * if the urlString or the default URL are relatives, prepend the rootURL to it
	 */
	public static URL getURL(URL rootURL, String urlString, String defaultURL) throws MalformedURLException {
		URL url = null;

		// if no URL , provide Default
		if (urlString == null || urlString.trim().equals("")) { //$NON-NLS-1$

			// no URL, no default, return right now...
			if (defaultURL == null || defaultURL.trim().equals("")) //$NON-NLS-1$
				return null;
			else
				urlString = defaultURL;
		}

		// URL can be relative or absolute	
		if (urlString.startsWith("/") && urlString.length() > 1) //$NON-NLS-1$
			urlString = urlString.substring(1);
		try {
			url = new URL(urlString);
		} catch (MalformedURLException e) {
			// the url is not an absolute URL
			// try relative
			url = new URL(rootURL, urlString);
		}

		return url;
	}

	/**
	 * return a relative String to rootURL 
	 * if url contains rootURL so
	 * new URL(rootURL, resultString) == url
	 * 
	 */
	public static String getURLAsString(URL rootURL, URL url) {
		String result = null;

		if (rootURL == null) {
			return (url == null) ? null : url.toString();
		}

		// if no URL , return null
		if (url != null) {

			result = url.toExternalForm();

			if (rootURL.getHost() != null && !rootURL.getHost().equals(url.getHost()))
				return result;

			if (rootURL.getProtocol() != null && !rootURL.getProtocol().equals(url.getProtocol()))
				return result;

			if (rootURL.getPort() != url.getPort())
				return result;

			String rootURLFileString = rootURL.getFile();
			rootURLFileString = rootURLFileString.replace(File.separatorChar, '/');
			if (!rootURLFileString.endsWith("/")) { //$NON-NLS-1$
				int index = rootURLFileString.lastIndexOf('/');
				if (index != -1) {
					rootURLFileString = rootURLFileString.substring(0, index);
				}
			}
			String urlFileString = url.getFile();

			if (urlFileString.startsWith(rootURLFileString)) {
				result = urlFileString.substring(rootURLFileString.length());
				result = result.replace(File.separatorChar, '/');
			} else {
				// we need to check the following
				// file:/C:/ and file:C:/
				if ("file".equalsIgnoreCase(url.getProtocol())) { //$NON-NLS-1$
					File rootFile = new File(rootURLFileString);
					File urlFile = new File(urlFileString);

					File relativePath = urlFile;
					while (relativePath != null && !rootFile.equals(relativePath.getParentFile())) {
						relativePath = relativePath.getParentFile();
					}

					if (relativePath == null) {
						UpdateCore.warn("Cannot calculate relative path"); //$NON-NLS-1$
						return url.toString();
					} else {
						String relativeRootString = relativePath.getParentFile().getAbsolutePath();
						String fullString = urlFile.getAbsolutePath();
						if (!fullString.startsWith(relativeRootString)) {
							UpdateCore.warn("Full path:" + fullString + " does not start with " + relativeRootString); //$NON-NLS-1$ //$NON-NLS-2$
							return url.toString();
						} else {
							String returnString = fullString.substring(relativeRootString.length() + 1);
							if (urlFile.isDirectory())
								returnString += File.separator;
							// we lost the last slash when tranforming in File
							returnString = returnString.replace(File.separatorChar, '/');
							return returnString;
						}

					}

				} else {
					result = url.toString();
				}
			}
		}

		return result;
	}

	/**
	 * returns a translated String
	 */
	public static String getResourceString(String infoURL, ResourceBundle bundle) {
		String result = null;
		if (infoURL != null) {
			result = Platform.getResourceString(UpdateCore.getPlugin().getBundle(), infoURL, bundle);
		}
		return result;
	}

	/**
	 * 
	 */
	public static URL copyToLocal(InputStream sourceContentReferenceStream, String localName, InstallMonitor monitor) throws MalformedURLException, IOException, InstallAbortedException {
		URL result = null;
		// create the Dir if they do not exist
		// get the path from the File to resolve File.separator..
		// do not use the String as it may contain URL like separator
		File localFile = new File(localName);
		int index = localFile.getPath().lastIndexOf(File.separator);
		if (index != -1) {
			File dir = new File(localFile.getPath().substring(0, index));
			if (!dir.exists())
				dir.mkdirs();
		}

		// transfer the content of the File
		if (!localFile.isDirectory()) {
			OutputStream localContentReferenceStream = new FileOutputStream(localFile);
			try {
				Utilities.copy(sourceContentReferenceStream, localContentReferenceStream, monitor);
			} finally {
				try {
					localContentReferenceStream.close();
				} catch (IOException e){}
			}
		}
		result = localFile.toURL();
		return result;
	}

	/*
	 * [20305] need to slam permissions for executable libs on some
	 * platforms. This is a temporary fix
	 */
	public static void checkPermissions(ContentReference ref, String filePath) {

		if (ref.getPermission() != 0) {
			UpdateCore.warn("Change permission for " + filePath + " to " + ref.getPermission()); //$NON-NLS-1$ //$NON-NLS-2$
			// FIXME: change the code to use JNI
		}

		if (filePath != null && OS_UNIX && ref.getPermission() != 0) {
			// add execute permission on shared libraries 20305
			// do not remove write permission 20896
			// chmod a+x *.sl
			try {
				Process pr = Runtime.getRuntime().exec(new String[] { "chmod", "a+x", filePath }); //$NON-NLS-1$ //$NON-NLS-2$
				Thread chmodOutput = new StreamConsumer(pr.getInputStream());
				chmodOutput.setName("chmod output reader"); //$NON-NLS-1$
				chmodOutput.start();
				Thread chmodError = new StreamConsumer(pr.getErrorStream());
				chmodError.setName("chmod error reader"); //$NON-NLS-1$
				chmodError.start();
			} catch (IOException ioe) {
			}

		}
	}

	/**
	 * Returns a random file name for the local system
	 * attempt to conserve the extension if there is a '.' in the path
	 * and no File.Seperator after the '.'
	 * 
	 * \a\b\c.txt -> c987659385.txt
	 * c.txt -> c3854763.txt
	 * c	-> c953867549
	 */
	public static String getLocalRandomIdentifier(String remotePath, Date date) {
		int dotIndex = remotePath.lastIndexOf(".");	//$NON-NLS-1$
		int fileIndex = remotePath.lastIndexOf(File.separator);
		// if there is a separator after the dot
		// do not consider it as an extension
		String ext = (dotIndex != -1 && fileIndex < dotIndex) ? remotePath.substring(dotIndex) : ""; //$NON-NLS-1$
		// the name is the string between the separator and the dot
		// if there is no separator, it is the string up to the dot		
		// if there is no dot, go to the end of the string 
		if (fileIndex == -1)
			fileIndex = 0;
		if (dotIndex == -1)
			dotIndex = remotePath.length();
		// if I have a separator and no dot: /a/b/c -> c
		// if my separator is the last /a/b/c/, fileIndex and dotIndex are the same, so it will return the default temp name
		String name = (fileIndex < dotIndex) ? remotePath.substring(fileIndex, dotIndex) : "Eclipse_Update_TMP_"; //$NON-NLS-1$
		String result = name + date.getTime() + ext;
		return result;
	}

	/**
	 * remove a file or directory from the file system.
	 * used to clean up install
	 */
	public static void removeFromFileSystem(File file) {
		if (!file.exists() || !file.canWrite())
			return;

		if (file.isDirectory()) {
			String[] files = file.list();
			if (files != null) // be careful since file.list() can return null
				for (int i = 0; i < files.length; ++i)
					removeFromFileSystem(new File(file, files[i]));
		}

		if (!file.delete()) {
			String msg = NLS.bind(Messages.UpdateManagerUtils_UnableToRemoveFile, (new String[] { file.getAbsolutePath() }));
			UpdateCore.log(msg, new Exception());
		}
	}

	/**
	 * remove all the empty directories recursively
	 * used to clean up install
	 */
	public static void removeEmptyDirectoriesFromFileSystem(File file) {
		if (!file.isDirectory())
			return;

		String[] files = file.list();
		if (files != null) { // be careful since file.list() can return null
			for (int i = 0; i < files.length; ++i) {
				removeEmptyDirectoriesFromFileSystem(new File(file, files[i]));
			}
		}
		if (!file.delete()) {
			String msg = NLS.bind(Messages.UpdateManagerUtils_UnableToRemoveFile, (new String[] { file.getAbsolutePath() }));
			UpdateCore.log(msg, new Exception());
		}
	}

	/**
	 * Returns the plugin entries that are in source array and
	 * not in target array
	 */
	public static IPluginEntry[] diff(IPluginEntry[] sourceArray, IPluginEntry[] targetArray) { // No pluginEntry to Install, return Nothing to instal
		if (sourceArray == null || sourceArray.length == 0) {
			return new IPluginEntry[0];
		} // No pluginEntry installed, Install them all
		if (targetArray == null || targetArray.length == 0) {
			return sourceArray;
		} // if a IPluginEntry from sourceArray is NOT in
		// targetArray, add it to the list
		List list1 = Arrays.asList(targetArray);
		List result = new ArrayList(0);
		for (int i = 0; i < sourceArray.length; i++) {
			if (!list1.contains(sourceArray[i]))
				result.add(sourceArray[i]);
		}

		IPluginEntry[] resultEntry = new IPluginEntry[result.size()];
		if (result.size() > 0)
			result.toArray(resultEntry);
		return resultEntry;
	}

	/**
	 * 
	 */
	public static URL asDirectoryURL(URL url) throws MalformedURLException {
		//url = URLEncoder.encode(url);
		String path = url.getFile();
		if (!path.endsWith("/")) { //$NON-NLS-1$
			int index = path.lastIndexOf('/');
			if (index != -1)
				path = path.substring(0, index + 1);
			// ignore any ref in original URL
			url = new URL(url.getProtocol(), url.getHost(), url.getPort(), path);
		}
		return url;
	}

	/*
	 * Compares two URL for equality
	 * Return false if one of them is null
	 */
	public static boolean sameURL(URL url1, URL url2) {

		if (url1 == null || url2 == null)
			return false;
		if (url1 == url2)
			return true;
		if (url1.equals(url2))
			return true;

		// check if URL are file: URL as we may
		// have 2 URL pointing to the same featureReference
		// but with different representation
		// (i.e. file:/C;/ and file:C:/)
		if (!"file".equalsIgnoreCase(url1.getProtocol())) //$NON-NLS-1$
			return false;
		if (!"file".equalsIgnoreCase(url2.getProtocol())) //$NON-NLS-1$
			return false;

		File file1 = getFileFor(url1);//new File(url1.getFile());
		File file2 = getFileFor(url2);

		if (file1 == null)
			return false;

		return (file1.equals(file2));
	}
	
	/*
	 * Method getFileFor.
	 * @param url1
	 * @return File
	 */
	private static File getFileFor(URL url1) {
		if (urlFileMap == null) urlFileMap = new HashMap();
		if (urlFileMap.get(url1)!=null) return (File)urlFileMap.get(url1);
		File newFile = new File(url1.getFile());
		urlFileMap.put(url1,newFile);
		return newFile; 
	}

	/*
	 * returns the list of FeatureReference that are parent of 
	 * the Feature or an empty array if no parent found.
	 * @param onlyOptional if set to <code>true</code> only return parents that consider the feature optional
	 * @param child
	 * @param possiblesParent
	 */
	public static IFeatureReference[] getParentFeatures(IFeature childFeature, IFeatureReference[] possiblesParent, boolean onlyOptional) throws CoreException {

		if (childFeature == null)
			return new IFeatureReference[0];

		List parentList = new ArrayList();
		IIncludedFeatureReference[] children = null;
		IFeature compareFeature = null;
		for (int i = 0; i < possiblesParent.length; i++) {
			try {
				IFeature possibleParentFeature = possiblesParent[i].getFeature(null);
				if (possibleParentFeature != null) {
					children = possibleParentFeature.getIncludedFeatureReferences();
					for (int j = 0; j < children.length; j++) {
						try {
							compareFeature = children[j].getFeature(null);
						} catch (CoreException e) {
							UpdateCore.warn("", e); //$NON-NLS-1$
						}
						if (childFeature.equals(compareFeature)) {
							if (onlyOptional) {
								if (UpdateManagerUtils.isOptional(children[j])) {
									parentList.add(possiblesParent[i]);
								} else {
									UpdateCore.warn("Feature :" + children[j] + " not optional. Not included in parents list."); //$NON-NLS-1$ //$NON-NLS-2$
								}
							} else {
								parentList.add(possiblesParent[i]);
							}
						}
					}
				}
			} catch (CoreException e) {
				UpdateCore.warn("", e); //$NON-NLS-1$
			}
		}

		IFeatureReference[] parents = new IFeatureReference[0];
		if (parentList.size() > 0) {
			parents = new IFeatureReference[parentList.size()];
			parentList.toArray(parents);
		}
		return parents;
	}

	/*
	 * If the return code of the HTTP connection is not 200 (OK)
	 * thow an IO exception
	 * 
	 */
	public static void checkConnectionResult(IResponse response,URL url) throws IOException {
		// did the server return an error code ?
		int result = response.getStatusCode();

		if (result != UpdateCore.HTTP_OK) { 
			String serverMsg = response.getStatusMessage();
			response.close();
			throw new FatalIOException(NLS.bind(Messages.ContentReference_HttpNok, (new Object[] { new Integer(result), serverMsg, url })));						
		}
	}

	public static class StreamConsumer extends Thread {
		InputStream is;
		byte[] buf;
		public StreamConsumer(InputStream inputStream) {
			super();
			this.setDaemon(true);
			this.is = inputStream;
			buf = new byte[512];
		}
		public void run() {
			try {
				int n = 0;
				while (n >= 0)
					n = is.read(buf);
			} catch (IOException ioe) {
			}
		}
	}

	/**
	 * Return the optional children to install
	 * The optional features to install may not all be direct children 
	 * of the feature.
	 * Also include non-optional features
	 * 
	 * @param children all the nested features
	 * @param optionalfeatures optional features to install
	 * @return IFeatureReference[]
	 */
	public static IFeatureReference[] optionalChildrenToInstall(IFeatureReference[] children, IFeatureReference[] optionalfeatures) {

		List optionalChildrenToInstall = new ArrayList();
		for (int i = 0; i < children.length; i++) {
			IFeatureReference optionalFeature = children[i];
			if (!UpdateManagerUtils.isOptional(optionalFeature)) {
				optionalChildrenToInstall.add(optionalFeature);
			} else {
				for (int j = 0; j < optionalfeatures.length; j++) {
					if (optionalFeature.equals(optionalfeatures[j])) {
						optionalChildrenToInstall.add(optionalFeature);
						break;
					}
				}
			}
		}

		IFeatureReference[] result = new IFeatureReference[optionalChildrenToInstall.size()];
		if (optionalChildrenToInstall.size() > 0) {
			optionalChildrenToInstall.toArray(result);
		}

		return result;
	}

	/**
	 * returns the mapping of matching rules
	 * the default returns perfect
	 * 
	 * @since 2.0.2
	 */
	public static int getMatchingRule(String rule) {
		if (rule == null)
			return IUpdateConstants.RULE_COMPATIBLE;
		final Integer integer = (Integer) table.get(rule);
		if (integer == null)
			return IUpdateConstants.RULE_PERFECT;
		int ruleInt = integer.intValue();
		if (ruleInt == IUpdateConstants.RULE_NONE)
			return IUpdateConstants.RULE_PERFECT;
		return ruleInt;
	}
	
	/**
	 * returns the mapping of matching id rules
	 * the default returns perfect
	 * 
	 * @since 2.0.2
	 */
	public static int getMatchingIdRule(String rule) {
		
		if (rule == null)
			return IUpdateConstants.RULE_COMPATIBLE;
		if (rule!=null && rule.equalsIgnoreCase("prefix")) //$NON-NLS-1$
			return IUpdateConstants.RULE_PREFIX;
		return IUpdateConstants.RULE_PERFECT;
	}
	
	/**
	 * Method isOptional.
	 * @param featureReference
	 * @return boolean
	 */
	public static boolean isOptional(IFeatureReference featureReference) {
		if (featureReference==null) return false;
		if (featureReference instanceof IIncludedFeatureReference){
			return ((IIncludedFeatureReference)featureReference).isOptional();
		}
		return false;
	}

	/**
	 * 
	 */
	public static boolean isValidEnvironment(IPlatformEnvironment candidate) {
		if (candidate==null) return false;
		String os = candidate.getOS();
		String ws = candidate.getWS();
		String arch = candidate.getOSArch();
		String nl = candidate.getNL();
		if (os!=null && !isMatching(os, SiteManager.getOS())) return false;
		if (ws!=null && !isMatching(ws, SiteManager.getWS())) return false;
		if (arch!=null && !isMatching(arch, SiteManager.getOSArch())) return false;
		if (nl!=null && !isMatchingLocale(nl, SiteManager.getNL())) return false;
		return true;
	}

	/* Original code - commented out to provide a replacement as per bug 98387
	
	private static boolean isMatching(String candidateValues, String siteValues) {
		if (siteValues==null) return false;
		if ("*".equals(candidateValues)) return true; //$NON-NLS-1$
		if ("".equals(candidateValues)) return true; //$NON-NLS-1$
		siteValues = siteValues.toUpperCase();		
		StringTokenizer stok = new StringTokenizer(candidateValues, ","); //$NON-NLS-1$
		while (stok.hasMoreTokens()) {
			String token = stok.nextToken().toUpperCase();
			if (siteValues.indexOf(token)!=-1) return true;
		}
		return false;
	}
	*/
	
	/*
	 * Fixed bug 98387
	 */
	
	private static boolean isMatching(String candidateValues, String siteValues) {
		if (siteValues==null) return false;
		if ("*".equals(candidateValues)) return true; //$NON-NLS-1$
		if ("".equals(candidateValues)) return true; //$NON-NLS-1$
		StringTokenizer siteTokens = new StringTokenizer(siteValues, ",");  //$NON-NLS-1$
		//$NON-NLS-1$	
		while(siteTokens.hasMoreTokens()) {
		    StringTokenizer candidateTokens = new StringTokenizer
	                                       (candidateValues, ","); //$NON-NLS-1$
			String siteValue = siteTokens.nextToken();
			while (candidateTokens.hasMoreTokens()) {
				if (siteValue.equalsIgnoreCase
	                             (candidateTokens.nextToken())) return true;
			}
		}
		return false;
	}

	
	/**
	 * 
	 */	
	private static boolean isMatchingLocale(String candidateValues, String locale) {
		if (locale==null) return false;
		if ("*".equals(candidateValues)) return true; //$NON-NLS-1$
		if ("".equals(candidateValues)) return true; //$NON-NLS-1$
		
		locale = locale.toUpperCase();
		candidateValues = candidateValues.toUpperCase();	
		StringTokenizer stok = new StringTokenizer(candidateValues, ","); //$NON-NLS-1$
		while (stok.hasMoreTokens()) {
			String candidate = stok.nextToken();
			if (locale.indexOf(candidate) == 0)
				return true;
			if (candidate.indexOf(locale) == 0)
				return true;
		}
		return false;
	}

	
	/*
	 * 
	 */
	private static void appendEscapedChar(StringBuffer buffer, char c) {
		String replacement = getReplacement(c);
		if (replacement != null) {
			buffer.append('&');
			buffer.append(replacement);
			buffer.append(';');
		} else {
			if ((c >= ' ' && c <= 0x7E) || c == '\n' || c == '\r' || c == '\t') {
				buffer.append(c);
			} else {
				buffer.append("&#"); //$NON-NLS-1$
				buffer.append(Integer.toString(c));
				buffer.append(';');
			}
		}
	}

	/*
	 * 
	 */
	public static String xmlSafe(String s) {
		StringBuffer result = new StringBuffer(s.length() + 10);
		for (int i = 0; i < s.length(); ++i)
			appendEscapedChar(result, s.charAt(i));
		return result.toString();
	}
	
	/*
	 * 
	 */
	private static String getReplacement(char c) {
		// Encode special XML characters into the equivalent character references.
		// These five are defined by default for all XML documents.
		switch (c) {
			case '<' :
				return "lt"; //$NON-NLS-1$
			case '>' :
				return "gt"; //$NON-NLS-1$
			case '"' :
				return "quot"; //$NON-NLS-1$
			case '\'' :
				return "apos"; //$NON-NLS-1$
			case '&' :
				return "amp"; //$NON-NLS-1$
		}
		return null;
	}
	
	public static boolean isSameTimestamp(URL url, long timestamp) {	
		try {
			if (UpdateCore.getPlugin().getUpdateSession().isVisited(url)) {
				return true;
			}
			URL resolvedURL = URLEncoder.encode(url);
			IResponse response = ConnectionFactory.get(resolvedURL);
			long remoteLastModified = response.getLastModified();
			// 2 seconds tolerance, as some OS's may round up the time stamp
			// to the closest second. For safety, we make it 2 seconds.
			return Math.abs(remoteLastModified - timestamp)/1000 <= 2;
		} catch (MalformedURLException e) {
			return false;
		} catch (IOException e) {
			return false;
		}
	}
	/**
	 * The file is associated with a lookup key.
	 * @param key optional lookup key, or <code>null</code>.
	 * @param temp the local working file
	 */
	public synchronized static void mapLocalFileFragment(String key, FileFragment temp) {
		// create file association 
		if (key != null) {
			if (localFileFragmentMap == null)
				localFileFragmentMap = new HashMap();
			localFileFragmentMap.put(key, temp);
		}
	}

	/**
	 * The file is associated with a lookup key.
	 * @param key optional lookup key, or <code>null</code>.
	 */
	public synchronized static void unMapLocalFileFragment(String key) {
		// remove file association 
		if (key != null && localFileFragmentMap !=null) {
			localFileFragmentMap.remove(key);
		}
	}
	
	/**
	 * Returns a previously cached local file (in temporary area) matching the
	 * specified key. 
	 * 
	 * @param key lookup key
	 * @return cached file, or <code>null</code>.
	 */
	public static synchronized FileFragment lookupLocalFileFragment(String key) {
		if (localFileFragmentMap == null)
			return null;
		return (FileFragment) localFileFragmentMap.get(key);
	}
	
	/**
	 * Copies specified input stream to the output stream. Neither stream
	 * is closed as part of this operation.
	 * 
	 * @param is input stream
	 * @param os output stream
	 * @param monitor progress monitor
     * @param expectedLength - if > 0, the number of bytes from InputStream will be verified
	 * @@return the offset in the input stream where copying stopped. Returns -1 if end of input stream is reached.
	 * @since 2.0
	 */
	public static long copy(InputStream is, OutputStream os, InstallMonitor monitor, long expectedLength) {
		byte[] buf = getBuffer();
		long offset=0;
		try {
			int len = is.read(buf);
			int nextIncrement = 0;
			while (len != -1) {
				os.write(buf, 0, len);
					offset += len;
				if (monitor != null) {
					nextIncrement += len;
					// update monitor periodically
					if (nextIncrement >= INCREMENT_SIZE){ 	
						monitor.incrementCount(nextIncrement);
						nextIncrement = 0;
					}
					if (monitor.isCanceled()) {
						return offset;
					}
				}
				if (expectedLength > 0 && offset == expectedLength) {
					// everything read do not return offset, otherwise trying
					// to read again from this offset will result in HTTP 416
					break;
				}
				
				len = is.read(buf);
			}
			if (nextIncrement > 0 && monitor != null)
				monitor.incrementCount(nextIncrement);
			if(expectedLength>0 && offset!=expectedLength)
				throw new IOException(NLS.bind(Messages.UpdateManagerUtils_inputStreamEnded, (new String[] { String.valueOf(offset), String.valueOf(expectedLength) })));
			return -1;
		} catch(IOException e){
			// Log the actual error, as this is no longer
			// passed up the calling stack
			UpdateCore.log(Messages.UpdateManagerUtils_copy + offset, e); 
			return offset;
		} finally {
			freeBuffer(buf);
		}
	}

	private static synchronized byte[] getBuffer() {
		if (bufferPool == null) {
			return new byte[BUFFER_SIZE];
		}

		try {
			return (byte[]) bufferPool.pop();
		} catch (EmptyStackException e) {
			return new byte[BUFFER_SIZE];
		}
	}

	private static synchronized void freeBuffer(byte[] buf) {
		if (bufferPool == null)
			bufferPool = new Stack();
		bufferPool.push(buf);
	}
	
	
	/**
	 * Returns a list of fragments. Zero length if no fragments.
	 * @param bundle the bundle to get fragments for
	 */
	public static FragmentEntry[] getFragments(Bundle bundle) {
		PackageAdmin pkgAdmin = UpdateCore.getPlugin().getPackageAdmin();
		Bundle[] fragmentBundles = pkgAdmin.getFragments(bundle);
		if (fragmentBundles == null) 
			return noFragments;
		
		FragmentEntry[] fragments = new FragmentEntry[fragmentBundles.length];
		for (int i = 0; i < fragments.length; i++) {
			fragments[i] = new FragmentEntry((String) fragmentBundles[i]
					.getHeaders().get(Constants.BUNDLE_SYMBOLICNAME),
					(String) fragmentBundles[i].getHeaders().get(
							Constants.BUNDLE_VERSION), Platform
							.getResourceString(fragmentBundles[i],
									(String) fragmentBundles[i].getHeaders()
											.get(Constants.BUNDLE_VERSION)),
					fragmentBundles[i].getLocation());
		}
		return fragments;	
	}
	
	public static String getWritableXMLString(String value) {
		StringBuffer buf = new StringBuffer();
		if(value == null)
			return buf.toString();
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			switch (c) {
				case '&' :
					buf.append("&amp;"); //$NON-NLS-1$
					break;
				case '<' :
					buf.append("&lt;"); //$NON-NLS-1$
					break;
				case '>' :
					buf.append("&gt;"); //$NON-NLS-1$
					break;
				case '\'' :
					buf.append("&apos;"); //$NON-NLS-1$
					break;
				case '\"' :
					buf.append("&quot;"); //$NON-NLS-1$
					break;
				case 0x00 :
					buf.append(" "); //$NON-NLS-1$
					break;						
				default :
					buf.append(c);
					break;
			}
		}
		return buf.toString();
	}
	
	public static LiteFeature[] getLightFeatures(ExtendedSite site) {
		
		URL fullDigestURL;
		try {
			fullDigestURL = getFullDigestURL( site, Locale.getDefault().getCountry(), Locale.getDefault().getLanguage());
		} catch (MalformedURLException e) {
			UpdateCore.log("Could not access digest on the site: " + e.getMessage(), null); //$NON-NLS-1$
			return null;
		}
		
		Digest digest = new Digest( fullDigestURL);
		try {
			LiteFeature[] features =  (LiteFeature[])digest.parseDigest();
			for(int i = 0; i < features.length; i++) {
				features[i].setSite(site);
			}
			return features;
		} catch(Exception e){ 
			UpdateCore.log("Digest could not be parsed:" + e.getMessage(), null); //$NON-NLS-1$
			return null;
		}
	}
	
	private static URL getFullDigestURL(ExtendedSite site, String country, String language) throws MalformedURLException {
		
		String digestURL = (site.getDigestURL().endsWith("/")? site.getDigestURL(): site.getDigestURL() + "/"); //$NON-NLS-1$ //$NON-NLS-2$ 
		
		if (digestURL.indexOf("://") == -1) { //$NON-NLS-1$
			String siteURL = site.getLocationURL().toExternalForm();
			if (siteURL.endsWith(Site.SITE_XML)) {
				siteURL = siteURL.substring(0, siteURL.length() - Site.SITE_XML.length());
			} 
			if (digestURL.equals("/")) { //$NON-NLS-1$
				digestURL = siteURL;
			} else {
				if (digestURL.startsWith("/")) { //$NON-NLS-1$
					digestURL = digestURL.substring(1, digestURL.length());
				}
				digestURL = siteURL + digestURL;
			}
		}
		
		digestURL += "digest";  //$NON-NLS-1$
		
		if ( isLocalSupported(site, country, language)) {
			return new URL(digestURL + "_" + language + "_" + country + ".zip"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		if ( isLangaugeSupported(site, language)) {
			return new URL(digestURL + "_" + language + ".zip"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return new URL(digestURL + ".zip"); //$NON-NLS-1$
	}

	private static boolean isLangaugeSupported(ExtendedSite site, String language) {
		String[] availableLanguages =  site.getAvailableLocals();
		if ((availableLanguages == null) || (availableLanguages.length == 0)) {
			return false;
		}
		for(int i = 0; i < availableLanguages.length; i++) {
			if (availableLanguages[i].equals(language)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isLocalSupported(ExtendedSite site, String country, String language) {
		String localeCode = language + "_" + country; //$NON-NLS-1$
		String[] availableLocals =  site.getAvailableLocals();
		if ((availableLocals == null) || (availableLocals.length == 0)) {
			return false;
		}
		for(int i = 0; i < availableLocals.length; i++) {
			if (availableLocals[i].equals(localeCode)) {
				return true;
			}
		}
		return false;
	}
}
