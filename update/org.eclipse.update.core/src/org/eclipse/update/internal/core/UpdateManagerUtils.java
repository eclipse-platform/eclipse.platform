package org.eclipse.update.internal.core;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
import java.io.*;
import java.net.*;
import java.util.*;

import org.eclipse.core.boot.BootLoader;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.update.core.*;
import org.eclipse.update.core.model.InstallAbortedException;

/**
 * 
 */
public class UpdateManagerUtils {

	private static boolean OS_UNIX = BootLoader.OS_HPUX.equals(BootLoader.getOS()) || BootLoader.OS_AIX.equals(BootLoader.getOS()) || BootLoader.OS_LINUX.equals(BootLoader.getOS()) || BootLoader.OS_SOLARIS.equals(BootLoader.getOS());

	private static Map table;
	static {
		table = new HashMap();
		table.put("compatible", new Integer(IImport.RULE_COMPATIBLE)); //$NON-NLS-1$
		table.put("perfect", new Integer(IImport.RULE_PERFECT)); //$NON-NLS-1$
		table.put("equivalent", new Integer(IImport.RULE_EQUIVALENT)); //$NON-NLS-1$
		table.put("greaterOrEqual", new Integer(IImport.RULE_GREATER_OR_EQUAL)); //$NON-NLS-1$
	}

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
			rootURLFileString.replace(File.separatorChar, '/');
			if (!rootURLFileString.endsWith("/")) {
				int index = rootURLFileString.lastIndexOf('/');
				if (index != -1) {
					rootURLFileString = rootURLFileString.substring(0, index);
				}
			}
			String urlFileString = url.getFile();

			if (urlFileString.startsWith(rootURLFileString)) {
				result = urlFileString.substring(rootURLFileString.length());
				result.replace(File.separatorChar, '/');
			} else {
				// we need to check the following
				// file:/C:/ and file:C:/
				if ("file".equalsIgnoreCase(url.getProtocol())) {
					File rootFile = new File(rootURLFileString);
					File urlFile = new File(urlFileString);

					File relativePath = urlFile;
					while (relativePath != null && !rootFile.equals(relativePath.getParentFile())) {
						relativePath = relativePath.getParentFile();
					}

					if (relativePath == null) {
						UpdateManagerPlugin.warn("Cannot calculate relative path");
						return url.toString();
					} else {
						String relativeRootString = relativePath.getParentFile().getAbsolutePath();
						String fullString = urlFile.getAbsolutePath();
						if (!fullString.startsWith(relativeRootString)) {
							UpdateManagerPlugin.warn("Full path:" + fullString + " does not start with " + relativeRootString);
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
			result = UpdateManagerPlugin.getPlugin().getDescriptor().getResourceString(infoURL, bundle);
		}
		return result;
	};

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
			UpdateManagerPlugin.warn("Change permission for " + filePath + " to " + ref.getPermission());
			// FIXME
		}

		if (filePath != null && OS_UNIX && ref.getPermission() != 0) {
			// add execute permission on shared libraries 20305
			// do not remove write permission 20896
			// chmod a+x *.sl
			try {
				Process pr = Runtime.getRuntime().exec(new String[] { "chmod", "a+x", filePath });
				Thread chmodOutput = new StreamConsumer(pr.getInputStream());
				chmodOutput.setName("chmod output reader");
				chmodOutput.start();
				Thread chmodError = new StreamConsumer(pr.getErrorStream());
				chmodError.setName("chmod error reader");
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
		int dotIndex = remotePath.lastIndexOf(".");
		//$NON-NLS-1$
		int fileIndex = remotePath.lastIndexOf(File.separator);
		// if there is a separator after the dot
		// do not consider it as an extension
		String ext = (dotIndex != -1 && fileIndex < dotIndex) ? remotePath.substring(dotIndex) : "";
		//$NON-NLS-1$
		// the name is the string between the separator and the dot
		// if there is no separator, it is the string up to the dot		
		// if there is no dot, go to the end of the string 
		if (fileIndex == -1)
			fileIndex = 0;
		if (dotIndex == -1)
			dotIndex = remotePath.length();
		// if I have a separator and no dot: /a/b/c -> c
		// if my separator is the last /a/b/c/, fileIndex and dotIndex are the same, so it will return the default temp name
		String name = (fileIndex < dotIndex) ? remotePath.substring(fileIndex, dotIndex) : "Eclipse_Update_TMP_";
		//$NON-NLS-1$
		String result = name + date.getTime() + ext;
		return result;
	}

	/**
	 * remove a file or directory from the file system.
	 * used to clean up install
	 */
	public static void removeFromFileSystem(File file) {
		if (!file.exists())
			return;

		if (file.isDirectory()) {
			String[] files = file.list();
			if (files != null) // be careful since file.list() can return null
				for (int i = 0; i < files.length; ++i)
					removeFromFileSystem(new File(file, files[i]));
		}

		if (!file.delete()) {
			String msg = Policy.bind("UpdateManagerUtils.UnableToRemoveFile", file.getAbsolutePath());
			//$NON-NLS-1$ //$NON-NLS-2$
			UpdateManagerPlugin.log(msg, new Exception());
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
			String msg = Policy.bind("UpdateManagerUtils.UnableToRemoveFile", file.getAbsolutePath());
			//$NON-NLS-1$ //$NON-NLS-2$
			UpdateManagerPlugin.log(msg, new Exception());
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
	 * Returns the parent URL of the given URL, or <code>null</code> if the
	 * given URL is the root.
	 * <table>
	 * <caption>Example</caption>
	 * <tr>
	 *   <th>Given URL</th>
	 *   <th>Parent URL</th>
	 * <tr>
	 *   <td>"http://hostname/"</td>
	 *   <td>null</td>
	 * <tr>
	 *   <td>"http://hostname/folder/file</td>
	 *   <td>"http://hostname/folder/</td>
	 * </table>
	 *
	 * @param url a URL
	 * @return    the parent of the given URL
	 */
	public static URL getParent(URL url) {
		String file = url.getFile();
		int len = file.length();
		if (len == 0 || len == 1 && file.charAt(0) == '/')
			return null;
		int lastSlashIndex = -1;
		for (int i = len - 2; lastSlashIndex == -1 && i >= 0; --i) {
			if (file.charAt(i) == '/')
				lastSlashIndex = i;
		}
		if (lastSlashIndex == -1)
			file = "";
		else
			file = file.substring(0, lastSlashIndex + 1);
		try {
			url = new URL(url.getProtocol(), url.getHost(), url.getPort(), file);
		} catch (MalformedURLException e) {
			Assert.isTrue(false, e.getMessage());
		}
		return url;
	}

	/**
	 * 
	 */
	public static URL asDirectoryURL(URL url) throws MalformedURLException {
		//url = URLEncoder.encode(url);
		String path = url.getFile();
		if (!path.endsWith("/")) {
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

		if (url1 == null)
			return false;
		if (url1.equals(url2))
			return true;

		// check if URL are file: URL as we may
		// have 2 URL pointing to the same featureReference
		// but with different representation
		// (i.e. file:/C;/ and file:C:/)
		if (!"file".equalsIgnoreCase(url1.getProtocol()))
			return false;
		if (!"file".equalsIgnoreCase(url2.getProtocol()))
			return false;

		File file1 = new File(url1.getFile());
		File file2 = new File(url2.getFile());

		if (file1 == null)
			return false;

		return (file1.equals(file2));
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
		IFeatureReference[] children = null;
		IFeature compareFeature = null;
		for (int i = 0; i < possiblesParent.length; i++) {
			try {
				IFeature possibleParentFeature = possiblesParent[i].getFeature();
				if (possibleParentFeature != null) {
					children = possibleParentFeature.getIncludedFeatureReferences();
					for (int j = 0; j < children.length; j++) {
						try {
							compareFeature = children[j].getFeature(true,null); //BUG 25796
						} catch (CoreException e) {
							UpdateManagerPlugin.warn("", e);
						};
						if (childFeature.equals(compareFeature)) {
							if (onlyOptional) {
								if (children[j].isOptional()) {
									parentList.add(possiblesParent[i]);
								} else {
									UpdateManagerPlugin.warn("Feature :" + children[j] + " not optional. Not included in parents list.");
								}
							} else {
								parentList.add(possiblesParent[i]);
							}
						}
					}
				}
			} catch (CoreException e) {
				UpdateManagerPlugin.warn("", e);
			};
		}

		IFeatureReference[] parents = new IFeatureReference[0];
		if (parentList.size() > 0) {
			parents = new IFeatureReference[parentList.size()];
			parentList.toArray(parents);
		}
		return parents;
	}

	/*
	 * returns the list of Features that are parent of 
	 * the Feature or an empty array if no parent found
	 * @param onlyOptional if set to <code>true</code> only return parents that consider the feature optional
	 * @param child
	 * @param possiblesParent
	 */
	public static IFeatureReference[] getParentFeatures(IFeatureReference child, IFeatureReference[] possiblesParent, boolean onlyOptional) throws CoreException {

		if (child == null)
			return new IFeatureReference[0];

		IFeature childFeature = null;
		try {
			childFeature = child.getFeature();
		} catch (CoreException e) {
			UpdateManagerPlugin.warn(null, e);
		}

		if (childFeature == null)
			return new IFeatureReference[0];

		return getParentFeatures(childFeature, possiblesParent, onlyOptional);
	}

	/*
	 * If the return code of the HTTP connection is not 200 (OK)
	 * thow an IO exception
	 * 
	 */
	public static void checkConnectionResult(URLConnection connection) throws IOException {
		// did the server return an error code ?
		if (connection instanceof HttpURLConnection) {
			int result = HttpURLConnection.HTTP_OK;
			HttpURLConnection httpConnection = null;
			URL url = null;
			try {
				httpConnection = (HttpURLConnection) connection;
				url = httpConnection.getURL();
				result = httpConnection.getResponseCode();
			} catch (IOException e) {
				// if an error occured, try again
				result = httpConnection.getResponseCode();
			}
			if (result != HttpURLConnection.HTTP_OK) {
				String serverMsg = httpConnection.getResponseMessage();
				throw new IOException(Policy.bind("ContentReference.HttpNok", new Object[] { new Integer(result), serverMsg, url })); //$NON-NLS-1$						
			}
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
			if (!optionalFeature.isOptional()) {
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
			return IImport.RULE_PERFECT;
		int ruleInt = ((Integer) table.get(rule)).intValue();
		if (ruleInt == IImport.RULE_NONE)
			return IImport.RULE_PERFECT;
		return ruleInt;
	}
}