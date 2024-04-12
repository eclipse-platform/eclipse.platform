/*******************************************************************************
 * Copyright (c) 2004, 2017 IBM Corporation and others.
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
 *     James D Miles (IBM Corp.) - bug 176250, Configurator needs to handle more platform urls 
 *******************************************************************************/
package org.eclipse.update.internal.configurator;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.framework.log.FrameworkLog;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.eclipse.osgi.service.resolver.PlatformAdmin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;

public class Utils {
	private static final String PROP_ARCH = "osgi.arch"; //$NON-NLS-1$
	private static final String PROP_NL = "osgi.nl"; //$NON-NLS-1$
	private static final String PROP_OS = "osgi.os"; //$NON-NLS-1$
	private static final String PROP_WS = "osgi.ws"; //$NON-NLS-1$
	private static final String PI_OSGI = "org.eclipse.osgi"; //$NON-NLS-1$
	private static final String KEY_PREFIX = "%"; //$NON-NLS-1$
	private static final String KEY_DOUBLE_PREFIX = "%%"; //$NON-NLS-1$
	// os
	public static boolean isWindows = System.getProperty("os.name").startsWith("Win"); //$NON-NLS-1$ //$NON-NLS-2$	
	static FrameworkLog log;
	private static ServiceTracker<?, PackageAdmin> bundleTracker;
	private static ServiceTracker<?, Location> instanceLocation;
	private static ServiceTracker<?, Location> configurationLocation;

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
			StringBuilder completeString = new StringBuilder();
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
		return new CoreException(status); 
	}

	public static IStatus newStatus(String message, Throwable e) {
		return new Status(IStatus.ERROR, "org.eclipse.update.configurator", IStatus.OK, message, e); //$NON-NLS-1$
	}
	
	public static void log(String message) {
		log(newStatus(message, null));
	}
	
	public static void log(IStatus status) {
		if (log != null) {
			log.log(new FrameworkLogEntry(ConfigurationActivator.PI_CONFIGURATOR, status.getSeverity(), 0, status.getMessage(), 0, status.getException(), null));
		} else {
			System.out.println(status.getMessage());
			if (status.getException() != null)
				status.getException().printStackTrace();
		}
	}
	
	/**
	 * Close the services that we were listening to.
	 */
	/*package*/ static synchronized void shutdown() {
		if (bundleTracker != null) {
			bundleTracker.close();
			bundleTracker = null;
		}
		if (instanceLocation != null) {
			instanceLocation.close();
			instanceLocation = null;
		}		
		if (configurationLocation != null) {
			configurationLocation.close();
			configurationLocation = null;
		}
	}

	/**
	 * Return a boolean value indicating whether or not we consider the
	 * platform to be running.
	 */
	public static boolean isRunning() {
		Bundle bundle = getBundle(PI_OSGI);
		return  bundle == null ? false : (bundle.getState() & (Bundle.ACTIVE | Bundle.STARTING)) != 0;
	}

	public static boolean isValidEnvironment(String os, String ws, String arch, String nl) {
		if (os!=null && !isMatching(os, getOS())) return false;
		if (ws!=null && !isMatching(ws, getWS())) return false;
		if (arch!=null && !isMatching(arch, getArch())) return false;
		if (nl!=null && !isMatchingLocale(nl, getNL())) return false;
		return true;
	}
	
	/**
	 * Return the current operating system value.
	 * 
	 * @see EnvironmentInfo#getOS()
	 */
	public static String getOS() {
		return getContext().getProperty(PROP_OS);
	}

	/**
	 * Return the current windowing system value.
	 * 
	 * @see EnvironmentInfo#getWS()
	 */
	public static String getWS() {
		return getContext().getProperty(PROP_WS);
	}

	/**
	 * Return the current system architecture value.
	 * 
	 * @see EnvironmentInfo#getOSArch()
	 */
	public static String getArch() {
		return getContext().getProperty(PROP_ARCH);
	}

	/**
	 * Return the current NL value.
	 * 
	 * @see EnvironmentInfo#getNL()
	 */
	public static String getNL() {
		return getContext().getProperty(PROP_NL);
	}
	
	/**
	 * Returns a number that changes whenever the set of installed plug-ins
	 * changes. This can be used for invalidating caches that are based on 
	 * the set of currently installed plug-ins. (e.g. extensions)
	 * 
	 * @see PlatformAdmin#getState()
	 * @see org.eclipse.osgi.service.resolver.State#getTimeStamp()
	 */
	public static long getStateStamp() {
		ServiceReference<PlatformAdmin> platformAdminReference = getContext().getServiceReference(PlatformAdmin.class);
		if (platformAdminReference == null)
			return -1;
		PlatformAdmin admin = getContext().getService(platformAdminReference);
		return admin == null ? -1 : admin.getState(false).getTimeStamp();
	}

	/**
	 * Return the resolved bundle with the specified symbolic name.
	 * 
	 * @see PackageAdmin#getBundles(String, String)
	 */
	public static synchronized Bundle getBundle(String symbolicName) {
		if (bundleTracker == null) {
			bundleTracker = new ServiceTracker<>(getContext(), PackageAdmin.class, null);
			bundleTracker.open();
		}
		PackageAdmin admin = bundleTracker.getService();
		if (admin == null)
			return null;
		Bundle[] bundles = admin.getBundles(symbolicName, null);
		if (bundles == null)
			return null;
		//Return the first bundle that is not installed or uninstalled
		for (Bundle bundle : bundles) {
			if ((bundle.getState() & (Bundle.INSTALLED | Bundle.UNINSTALLED)) == 0) {
				return bundle;
			}
		}
		return null;
	}

	/*
	 * Return the bundle context for this bundle.
	 */
	private static BundleContext getContext() {
		return ConfigurationActivator.getBundleContext();
	}

	/**
	 * Return the configuration location.
	 * 
	 * @see Location
	 */
	public static synchronized Location getConfigurationLocation() {
		if (configurationLocation == null) {
			Filter filter = null;
			try {
				filter = getContext().createFilter(Location.CONFIGURATION_FILTER);
			} catch (InvalidSyntaxException e) {
				// ignore this. It should never happen as we have tested the above format.
			}
			configurationLocation = new ServiceTracker<>(getContext(), filter, null);
			configurationLocation.open();
		}
		return configurationLocation.getService();
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
			if (siteValues.contains(token)) return true;
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
		String nl = getNL();
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
	 * A key is identified as a string beginning with the "%" character.
	 * Note that the "%" character is stripped off prior to lookup
	 * in the resource bundle.
	 * <p>
	 * For example, assume resource bundle plugin.properties contains
	 * name = Project Name
	 * </p>
	 * <pre>
	 *     resolveNLString(b,"Hello World") returns "Hello World"
	 *     resolveNLString(b,"%name") returns "Project Name"
	 *     resolveNLString(b,"%name Hello World") returns "Project Name"
	 *     resolveNLString(b,"%abcd Hello World") returns "Hello World"
	 *     resolveNLString(b,"%abcd") returns "%abcd"
	 *     resolveNLString(b,"%%name") returns "%name"
	 * </pre>
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

		if (s.isEmpty())
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
		if (bundleURL.contains("org.eclipse.osgi")) //$NON-NLS-1$
			return true;
		
		String osgiBundles = ConfigurationActivator.getBundleContext().getProperty("osgi.bundles"); //$NON-NLS-1$
		StringTokenizer st = new StringTokenizer(osgiBundles, ","); //$NON-NLS-1$
		while (st.hasMoreTokens()) {
			String token = st.nextToken().trim();
			int index = token.indexOf('@');
			if (index != -1)
				token = token.substring(0,index);
			if (token.startsWith("reference:file:")) { //$NON-NLS-1$
				File f = new File(token.substring(15));
				if (bundleURL.contains(f.getName()))
					return true;
			}
			if (bundleURL.contains(token))
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
		IPath relativePath = IPath.fromOSString(relativeLocation.getPath());
		if (relativePath.isAbsolute())
			return relativeLocation;
		try {
			IPath absolutePath = IPath.fromOSString(base.getPath()).append(relativeLocation.getPath());
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
		IPath locationPath = IPath.fromOSString(location.getPath());
		if (!locationPath.isAbsolute())
			return location;
		IPath relativePath = makeRelative(IPath.fromOSString(base.getPath()), locationPath);
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
		return IPath.fromOSString(temp).append(location.removeFirstSegments(count));
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

	/**
	 * Return the install location.
	 * 
	 * @see Location
	 */
	public static synchronized URL getInstallURL() {
		if (instanceLocation == null) {
			Filter filter = null;
			try {
				filter = getContext().createFilter(Location.INSTALL_FILTER);
			} catch (InvalidSyntaxException e) {
				// ignore this. It should never happen as we have tested the
				// above format.
			}
			instanceLocation = new ServiceTracker<>(getContext(), filter, null);
			instanceLocation.open();
		}

		Location location = instanceLocation.getService();

		// it is pretty much impossible for the install location to be null.  If it is, the
		// system is in a bad way so throw and exception and get the heck outta here.
		if (location == null)
			throw new IllegalStateException("The installation location must not be null"); //$NON-NLS-1$

		return  location.getURL();
	}

}
