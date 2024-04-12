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
 *******************************************************************************/
package org.eclipse.ui.internal.intro.impl.model.util;

import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.internal.intro.impl.util.Log;
import org.eclipse.ui.internal.intro.impl.util.StringUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

/**
 * Bundle convenience methods.
 */
public class BundleUtil {

	private static String NL_TAG = "$nl$/"; //$NON-NLS-1$
	private final static String PRODUCT_PLUGIN = "PRODUCT_PLUGIN"; //$NON-NLS-1$
	private final static String PLUGINS_ROOT = "PLUGINS_ROOT/"; //$NON-NLS-1$

	/**
	 * Utility method to validate the state of a bundle. Log invalid bundles to
	 * log file.
	 */
	public static boolean bundleHasValidState(Bundle bundle) {
		if (bundle == null || bundle.getState() == Bundle.UNINSTALLED
				|| bundle.getState() == Bundle.INSTALLED) {

			if (bundle == null)
				Log.error("Intro tried accessing a NULL bundle.", null); //$NON-NLS-1$
			else {
				String msg = StringUtil.concat("Intro tried accessing Bundle: ", getBundleHeader( //$NON-NLS-1$
						bundle, Constants.BUNDLE_NAME), " vendor: ", //$NON-NLS-1$
						getBundleHeader(bundle, Constants.BUNDLE_VENDOR), " bundle state: ", //$NON-NLS-1$
						String.valueOf(bundle.getState()));
				Log.error(msg, null);
			}
			return false;
		}

		return true;
	}

	/**
	 * Retrieves the given key from the bundle header.
	 */
	public static String getBundleHeader(Bundle bundle, String key) {
		return bundle.getHeaders().get(key);
	}


	public static Bundle getBundleFromConfigurationElement(
			IConfigurationElement cfg) {
		return Platform.getBundle(cfg.getContributor().getName());
	}


	/**
	 * Get the resourcelocation, but do not force an $nl$ on it.
	 */
	public static String getResourceLocation(String resource,
			IConfigurationElement element) {
		Bundle bundle = getBundleFromConfigurationElement(element);
		return getResolvedResourceLocation(resource, bundle, false);
	}


	/**
	 * Returns the fully qualified location of the passed resource string from
	 * the passed plugin id. If the file could not be loaded from the plugin,
	 * the resource is returned as is.
	 */
	public static String getResolvedResourceLocation(String resource,
			String pluginId) {
		Bundle bundle = Platform.getBundle(pluginId);
		return getResolvedResourceLocation(resource, bundle, true);
	}


	/**
	 * Shorthand util method.
	 */
	public static String getResolvedResourceLocation(String resource,
			Bundle bundle) {
		return getResolvedResourceLocation(resource, bundle, true);
	}


	public static String getResolvedResourceLocation(String base,
			String resource, Bundle bundle) {
		// quick exits.
		if (resource == null)
			return null;

		String fullResource = IPath.fromOSString(base).append(resource).toString();
		String resolvedResource = getResolvedResourceLocation(fullResource,
			bundle, true);

		if (resolvedResource.equals(fullResource))
			// return resource as is when the resource does not exist.
			return resource;
		return resolvedResource;
	}


	public static String getResolvedResourceLocation(String resource,
			Bundle bundle, boolean forceNLResolve) {
		// quick exits.
		if (resource == null)
			return null;

		if (bundle == null || !bundleHasValidState(bundle))
			return resource;

		URL localLocation = null;
		try {
			// resolve PLUGINS_ROOT
			int index = resource.indexOf(PLUGINS_ROOT);
			if (index != -1) {
				resource = resource.substring(index + PLUGINS_ROOT.length());
				index = resource.indexOf('/');
				if (index != -1) {
					String bundleName = resource.substring(0, index);
					if (PRODUCT_PLUGIN.equals(bundleName)) {
						IProduct product = Platform.getProduct();
						if (product != null) {
							Bundle productBundle = product.getDefiningBundle();
							if (productBundle != null) {
								bundleName = productBundle.getSymbolicName();
							}
						}
					}
					resource = resource.substring(index + 1);
					Bundle actualBundle = Platform.getBundle(bundleName);
					if (actualBundle != null) {
						return getResolvedResourceLocation(resource, actualBundle, forceNLResolve);
					}
				}
			}

			// we need to resolve this URL.
			String copyResource = resource;
			if (forceNLResolve && !copyResource.startsWith(NL_TAG)) {
				if (copyResource.startsWith("/") //$NON-NLS-1$
						|| copyResource.startsWith("\\")) //$NON-NLS-1$
					copyResource = resource.substring(1);
				copyResource = NL_TAG + copyResource;
			}
			IPath resourcePath = IPath.fromOSString(copyResource);
			localLocation = FileLocator.find(bundle, resourcePath, null);
			if (localLocation == null) {
				// localLocation can be null if the passed resource could not
				// be found relative to the plugin. log fact, return resource,
				// as is.
				String msg = StringUtil.concat("Could not find resource: ", //$NON-NLS-1$
						resource, " in ", getBundleHeader(bundle, Constants.BUNDLE_NAME)); //$NON-NLS-1$
				Log.warning(msg);
				return resource;
			}
			/*
			localLocation = FileLocator.toFileURL(localLocation);
			return localLocation.toExternalForm();
			*/
			return toExternalForm(localLocation);
		} catch (Exception e) {
			String msg = StringUtil.concat("Failed to load resource: ", //$NON-NLS-1$
					resource, " from ", getBundleHeader(bundle, Constants.BUNDLE_NAME)); //$NON-NLS-1$
			Log.error(msg, e);
			return resource;
		}
	}




	/** *** used by Intro parser ***** */
	/*
	 * Util method to return an URL to a plugin relative resource.
	 */
	public static URL getResourceAsURL(String resource, String pluginId) {
		Bundle bundle = Platform.getBundle(pluginId);
		return FileLocator.find(bundle, IPath.fromOSString(resource), null);
	}




	/** ********************* Used by HTML generator ****************** */
	/**
	 * Get the absolute path of the given bundle, in the form
	 * file:/path_to_plugin
	 */
	public static String getResolvedBundleLocation(Bundle bundle) {
		try {
			URL bundleLocation = bundle.getEntry(""); //$NON-NLS-1$
			if (bundleLocation == null)
				return null;
			/*
			bundleLocation = FileLocator.toFileURL(bundleLocation);
			return bundleLocation.toExternalForm();
			*/
			return toExternalForm(bundleLocation);
		} catch (IllegalStateException e) {
			Log.error("Failed to access bundle: " //$NON-NLS-1$
					+ bundle.getSymbolicName(), e);
			return null;
		}/* catch (IOException e) {
			Log.error("Failed to resolve URL path for bundle: " //$NON-NLS-1$
					+ bundle.getSymbolicName(), e);
			return null;
		} */
	}

	/**
	 * Get the absolute path of the bundle with id <code>bundleId</code>. If
	 * no such bundle is found, return null.
	 */
	public static String getResolvedBundleLocation(String bundleId) {
		Bundle bundle = Platform.getBundle(bundleId);
		if (bundle == null)
			return null;
		return getResolvedBundleLocation(bundle);
	}

	/*
	 * Bug 126085 - need to fix up file: protocol
	 * to a form that IE7 understands (file:///).
	 */

	private static String toExternalForm(URL localURL) {
		try {
			localURL = FileLocator.toFileURL(localURL);
			String result = localURL.toExternalForm();
			if (result.startsWith("file:/")) { //$NON-NLS-1$
				if (!result.startsWith("file:///")) { //$NON-NLS-1$
					result = "file:///"+result.substring(6); //$NON-NLS-1$
				}
			}
			return result;
		}
		catch (IOException e) {
			String msg = "Failed to resolve URL: " //$NON-NLS-1$
					+ localURL.toString();
				Log.error(msg, e);
				return localURL.toString();
		}
	}

}
