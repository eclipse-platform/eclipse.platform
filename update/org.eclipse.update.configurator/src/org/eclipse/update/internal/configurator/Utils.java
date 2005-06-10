/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.configurator;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.osgi.framework.log.*;

public class Utils {
	private static final String KEY_PREFIX = "%"; //$NON-NLS-1$
	private static final String KEY_DOUBLE_PREFIX = "%%"; //$NON-NLS-1$
	// os
	public static boolean isWindows = System.getProperty("os.name").startsWith("Win"); //$NON-NLS-1$ //$NON-NLS-2$	
	static FrameworkLog log;
	// Install location	
	private static URL installURL;

	public static void debug(String s) {
		if (ConfigurationActivator.DEBUG)
			System.out.println("PlatformConfig: " + s); //$NON-NLS-1$
	}
	
	/**
	 * Creates a CoreException from some other exception.
	 * The type of the CoreException is <code>IStatus.ERROR</code>
	 * If the exception passed as a parameter is also a CoreException,
	 * the new CoreException will contain all the status of the passed
	 * CoreException.
	 * 
	 * @see IStatus#ERROR
	 * @param s exception string
	 * @param e actual exception being reported
	 * @return a CoreException
	 * @since 2.0
	 */
	public static CoreException newCoreException(String s, Throwable e) {

		// check the case of a multistatus
		IStatus status;
		if (e instanceof CoreException) {
			if (s == null)
				s = ""; //$NON-NLS-1$
			status = new MultiStatus("org.eclipse.update.configurator", 0, s, e); //$NON-NLS-1$
			IStatus childrenStatus = ((CoreException) e).getStatus();
			((MultiStatus) status).add(childrenStatus);
			((MultiStatus) status).addAll(childrenStatus);
		} else {
			StringBuffer completeString = new StringBuffer(""); //$NON-NLS-1$
			if (s != null)
				completeString.append(s);
			if (e != null) {
				completeString.append(" ["); //$NON-NLS-1$
				String msg = e.getLocalizedMessage();
				completeString.append(msg!=null?msg:e.toString());
				completeString.append("]"); //$NON-NLS-1$
			}
			status = newStatus(completeString.toString(), e);
		}
		return new CoreException(status); //$NON-NLS-1$
	}

	public static IStatus newStatus(String message, Throwable e) {
		return new Status(IStatus.ERROR, "org.eclipse.update.configurator", IStatus.OK, message, e); //$NON-NLS-1$
	}
	
	public static void log(String message) {
		log(newStatus(message, null));
	}
	
	public static void log(IStatus status) {
		if (log != null) {
			log.log(new FrameworkLogEntry(ConfigurationActivator.PI_CONFIGURATOR, status.getMessage(), 0, status.getException(), null));
		} else {
			System.out.println(status.getMessage());
			if (status.getException() != null)
				status.getException().printStackTrace();
		}
	}
	
	/**
	 * Returns the url as a platform:/ url, if possible, else leaves it unchanged
	 * @param url
	 * @return
	 */
	public static URL asPlatformURL(URL url) {
		try {
			URL platformURL = new URL("platform:/base/"); //$NON-NLS-1$  // try using platform-relative URL
			URL resolvedPlatformURL = Platform.asLocalURL(platformURL);
			// TODO workaround bug in platform url resolution
			if (resolvedPlatformURL.getProtocol().equals("file")) //$NON-NLS-1$
				resolvedPlatformURL = new File(resolvedPlatformURL.getFile()).toURL();
			String platformURLAsString = resolvedPlatformURL.toExternalForm();
			String urlAsString = url.toExternalForm();
			if (urlAsString.startsWith(platformURLAsString))
				return new URL(platformURL.toExternalForm() + urlAsString.substring(platformURLAsString.length()) );
			else
				return url;
		} catch (Exception e) {
			return url;
		}
	}
	

	/**
	 * 
	 */
	public static boolean isValidEnvironment(String os, String ws, String arch, String nl) {
		if (os!=null && !isMatching(os, Platform.getOS())) return false;
		if (ws!=null && !isMatching(ws, Platform.getWS())) return false;
		if (arch!=null && !isMatching(arch, Platform.getOSArch())) return false;
		if (nl!=null && !isMatchingLocale(nl, Platform.getNL())) return false;
		return true;
	}

	/**
	 * 
	 */	
	private static boolean isMatching(String candidateValues, String siteValues) {
		if (siteValues==null) return false;
		if ("*".equalsIgnoreCase(candidateValues)) return true; //$NON-NLS-1$
		siteValues = siteValues.toUpperCase();		
		StringTokenizer stok = new StringTokenizer(candidateValues, ","); //$NON-NLS-1$
		while (stok.hasMoreTokens()) {
			String token = stok.nextToken().toUpperCase();
			if (siteValues.indexOf(token)!=-1) return true;
		}
		return false;
	}
	
	/**
	 * 
	 */	
	private static boolean isMatchingLocale(String candidateValues, String locale) {
		if (locale==null) return false;
		if ("*".equalsIgnoreCase(candidateValues)) return true; //$NON-NLS-1$
		
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
	
	public static Locale getDefaultLocale() {
		String nl = Platform.getNL();
		// sanity test
		if (nl == null)
			return Locale.getDefault();
		
		// break the string into tokens to get the Locale object
		StringTokenizer locales = new StringTokenizer(nl,"_"); //$NON-NLS-1$
		if (locales.countTokens() == 1)
			return new Locale(locales.nextToken(), ""); //$NON-NLS-1$
		else if (locales.countTokens() == 2)
			return new Locale(locales.nextToken(), locales.nextToken());
		else if (locales.countTokens() == 3)
			return new Locale(locales.nextToken(), locales.nextToken(), locales.nextToken());
		else
			return Locale.getDefault();
	}
	
	
	/**
	 * Returns a resource string corresponding to the given argument 
	 * value and bundle.
	 * If the argument value specifies a resource key, the string
	 * is looked up in the given resource bundle. If the argument does not
	 * specify a valid key, the argument itself is returned as the
	 * resource string. The key lookup is performed against the
	 * specified resource bundle. If a resource string 
	 * corresponding to the key is not found in the resource bundle
	 * the key value, or any default text following the key in the
	 * argument value is returned as the resource string.
	 * A key is identified as a string begining with the "%" character.
	 * Note that the "%" character is stripped off prior to lookup
	 * in the resource bundle.
	 * <p>
	 * For example, assume resource bundle plugin.properties contains
	 * name = Project Name
	 * <pre>
	 *     resolveNLString(b,"Hello World") returns "Hello World"</li>
	 *     resolveNLString(b,"%name") returns "Project Name"</li>
	 *     resolveNLString(b,"%name Hello World") returns "Project Name"</li>
	 *     resolveNLString(b,"%abcd Hello World") returns "Hello World"</li>
	 *     resolveNLString(b,"%abcd") returns "%abcd"</li>
	 *     resolveNLString(b,"%%name") returns "%name"</li>
	 * </pre>
	 * </p>
	 * 
	 * @param resourceBundle resource bundle.
	 * @param string translatable string from model
	 * @return string, or <code>null</code>
	 * @since 2.0
	 */
	public static String getResourceString(ResourceBundle resourceBundle, String string) {

		if (string == null)
			return null;

		String s = string.trim();

		if (s.equals("")) //$NON-NLS-1$
			return string;

		if (!s.startsWith(KEY_PREFIX))
			return string;

		if (s.startsWith(KEY_DOUBLE_PREFIX))
			return s.substring(1);

		int ix = s.indexOf(" "); //$NON-NLS-1$
		String key = ix == -1 ? s : s.substring(0, ix);
		String dflt = ix == -1 ? s : s.substring(ix + 1);

		if (resourceBundle == null)
			return dflt;

		try {
			return resourceBundle.getString(key.substring(1));
		} catch (MissingResourceException e) {
			return dflt;
		}
	}

	public static boolean isAutomaticallyStartedBundle(String bundleURL) {
		if (bundleURL.indexOf("org.eclipse.osgi") != -1) //$NON-NLS-1$
			return true;
		
		String osgiBundles = System.getProperty("osgi.bundles"); //$NON-NLS-1$
		StringTokenizer st = new StringTokenizer(osgiBundles, ","); //$NON-NLS-1$
		while (st.hasMoreTokens()) {
			String token = st.nextToken().trim();
			int index = token.indexOf('@');
			if (index != -1)
				token = token.substring(0,index);
			if (token.startsWith("reference:file:")) { //$NON-NLS-1$
				File f = new File(token.substring(15));
				if (bundleURL.indexOf(f.getName()) != -1)
					return true;
			}
			if (bundleURL.indexOf(token) != -1)
				return true;
		}
		return false;
	}

	/**
	 * Returns an absolute URL by combining a base absolute URL and another URL relative to the first one.
	 * If the relative URL protocol does not match the base URL protocol, or if the relative URL path is not relative, 
	 * return it as is. 
	 */
	public static URL makeAbsolute(URL base, URL relativeLocation) {
		if (!"file".equals(base.getProtocol())) //$NON-NLS-1$
			// we only deal with file: URLs 
			return relativeLocation;
		if (relativeLocation.getProtocol() != null && !relativeLocation.getProtocol().equals(base.getProtocol()))
			// it is not relative, return as is (avoid creating garbage)
			return relativeLocation;
		IPath relativePath = new Path(relativeLocation.getPath());
		if (relativePath.isAbsolute())
			return relativeLocation;
		try {
			IPath absolutePath = new Path(base.getPath()).append(relativeLocation.getPath());
			// File.toURL() is the best way to create a file: URL
			return absolutePath.toFile().toURL();
		} catch (MalformedURLException e) {
			// cannot happen since we are building from two existing valid URLs
			Utils.log(e.getLocalizedMessage());
			return relativeLocation;
		}
	}

	/**
	 * Returns a URL which is equivalent to the given URL relative to the
	 * specified base URL. Works only for file: URLs
	 */
	public static URL makeRelative(URL base, URL location) {
		if (base == null)
			return location;
		if (!"file".equals(base.getProtocol())) //$NON-NLS-1$
			return location;
		if (!base.getProtocol().equals(location.getProtocol()))
			return location;
		if (base.getHost() == null ^ location.getHost() == null)
			return location;
		if (base.getHost() != null && !base.getHost().equals(location.getHost()))
			return location;
		if (base.getPort() != location.getPort())
			return location;
		IPath locationPath = new Path(location.getPath());
		if (!locationPath.isAbsolute())
			return location;
		IPath relativePath = makeRelative(new Path(base.getPath()), locationPath);
		try {
			return new URL(base.getProtocol(), base.getHost(), base.getPort(), relativePath.toString());
		} catch (MalformedURLException e) {
			String message = e.getMessage();
			if (message == null)
				message = ""; //$NON-NLS-1$
			Utils.log(Utils.newStatus(message, e));
		}
		return location;
	}

	/**
	 * Returns a path which is equivalent to the given location relative to the
	 * specified base path.
	 */
	public static IPath makeRelative(IPath base, IPath location) {
		if (location.getDevice() != null && !location.getDevice().equalsIgnoreCase(base.getDevice()))
			return location;
		int baseCount = base.segmentCount();
		int count = base.matchingFirstSegments(location);
		String temp = ""; //$NON-NLS-1$
		for (int j = 0; j < baseCount - count; j++)
			temp += "../"; //$NON-NLS-1$
		return new Path(temp).append(location.removeFirstSegments(count));
	}

	/**
	 * Returns a string URL which is equivalent to the given absolute location 
	 * made relative to the specified base path.
	 */
	public static String makeRelative(URL base, String absolute) {
		try {
			return makeRelative(base, new URL(absolute)).toExternalForm();
		} catch (MalformedURLException e) {
			// returns the original string if is invalid
			return absolute;
		}
	}

	/**
	 * Ensures file: URLs on Windows have the right form (i.e. '/' as segment separator, drive letter in lower case, etc)
	 */
	public static String canonicalizeURL(String url) {
		if (!(isWindows && url.startsWith("file:"))) //$NON-NLS-1$
			return url;
		try {
			String path = new URL(url).getPath();			
	        // normalize to not have leading / so we can check the form
	        File file = new File(path);
	        path = file.toString().replace('\\', '/');
            if (Character.isUpperCase(path.charAt(0))) {
                char[] chars = path.toCharArray();
                chars[0] = Character.toLowerCase(chars[0]);
                path = new String(chars);
                return new File(path).toURL().toExternalForm();
            }
		} catch (MalformedURLException e) {
			// default to original url
		}		
		return url;
	}	

	public static URL getInstallURL() {
		if (installURL == null)
			installURL = Platform.getInstallLocation().getURL();
		return installURL;
	}

}
