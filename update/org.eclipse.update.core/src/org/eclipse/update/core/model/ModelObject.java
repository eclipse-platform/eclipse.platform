/*******************************************************************************
 *  Copyright (c) 2000, 2012 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.core.model;

import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.update.core.Feature;
import org.eclipse.update.core.SiteManager;
import org.eclipse.update.internal.core.Messages;
import org.eclipse.update.internal.core.UpdateCore;
import org.eclipse.update.internal.core.UpdateManagerUtils;

/**
 * Root model object. Extended by all model objects.
 * <p>
 * This class cannot be instantiated and must be subclassed.
 * </p>
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 * @deprecated The org.eclipse.update component has been replaced by Equinox p2.
 * This API will be deleted in a future release. See bug 311590 for details.
 */
public abstract class ModelObject extends PlatformObject {

	private boolean readOnly = false;

	private static final String KEY_PREFIX = "%"; //$NON-NLS-1$
	private static final String KEY_DOUBLE_PREFIX = KEY_PREFIX + KEY_PREFIX;

	private static Map bundles;

	/**
	 * Creates a base model object.
	 * 
	 * @since 2.0
	 */
	protected ModelObject() {
	}

	/**
	 * Checks that this model object is writeable.  A runtime exception
	 * is thrown if it is not.
	 * 
	 * @since 2.0
	 */
	protected final void assertIsWriteable() {
		Assert.isTrue(!isReadOnly(), Messages.ModelObject_ModelReadOnly);	
	}

	/**
	 * Sets this model object and all of its descendents to be read-only.
	 * Subclasses may extend this implementation.
	 * 
	 * @see #isReadOnly
	 * @since 2.0
	 */
	public void markReadOnly() {
		readOnly = true;
	}

	/**
	 * Returns whether or not this model object is read-only.
	 * 
	 * @return <code>true</code> if this model object is read-only,
	 *		<code>false</code> otherwise
	 * @see #markReadOnly
	 * @since 2.0
	 */
	public boolean isReadOnly() {
		return readOnly;
	}

	/**
	 * Delegate setting of read-only
	 *
	 * @param o object to delegate to. Must be of type ModelObject.
	 * @see #isReadOnly
	 * @since 2.0
	 */
	protected void markReferenceReadOnly(ModelObject o) {
		if (o == null)
			return;
		o.markReadOnly();
	}

	/**
	 * Delegate setting of read-only
	 *
	 * @param o object array to delegate to. Each element must be of type ModelObject.
	 * @see #isReadOnly
	 * @since 2.0
	 */
	protected void markListReferenceReadOnly(ModelObject[] o) {
		if (o == null)
			return;
		for (int i = 0; i < o.length; i++) {
			o[i].markReadOnly();
		}
	}

	/**
	 * Resolve the model element. This method allows any relative URL strings
	 * to be resolved to actual URL. It also allows any translatable strings
	 * to be localized.
	 * 
	 * Subclasses need to override this method to perform the actual resolution.
	 * @param base base URL.
	 * @param bundleURL resource bundle URL.
	 * @exception MalformedURLException
	 * @since 2.0
	 */
	public void resolve(URL base, URL bundleURL) throws MalformedURLException {
		return;
	}

	/**
	 * Delegate resolution to referenced model
	 *
	 * @param o object to delegate to. Must be of type ModelObject.
	 * @param url base URL.
	 * @param bundleURL resource bundle URL.
	 * @exception MalformedURLException
	 * @since 2.0
	 */
	protected void resolveReference(ModelObject o, URL url, URL bundleURL) throws MalformedURLException {
		if (o == null)
			return;
		o.resolve(url, bundleURL);
	}

	/**
	 * Delegate resolution to list of referenced models
	 *
	 * @param o object array to delegate to. Each element must be of type ModelObject.
	 * @param url base URL.
	 * @param bundleURL resource bundle URL.
	 * @exception MalformedURLException
	 * @since 2.0
	 */
	protected void resolveListReference(ModelObject[] o, URL url, URL bundleURL) throws MalformedURLException {
		if (o == null)
			return;
		for (int i = 0; i < o.length; i++) {
			o[i].resolve(url, bundleURL);
		}
	}

	/**
	 * Resolve a URL based on context
	 *
	 * @param context base URL.
	 * @param bundleURL resource bundle URL.
	 * @param urlString url string from model.
	 * @return URL, or <code>null</code>.
	 * @exception MalformedURLException
	 * @since 2.0
	 */
	protected URL resolveURL(URL context, URL bundleURL, String urlString) throws MalformedURLException {

		// URL string was not specified
		if (urlString == null || urlString.trim().equals("")) //$NON-NLS-1$
			return null;

		// check to see if we have NL-sensitive URL
		String resolvedUrlString = resolveNLString(bundleURL, urlString);

		resolvedUrlString = resolvePlatfromConfiguration(resolvedUrlString);

		// if we don't have a base url, use only the supplied string
		if (context == null)
			return new URL(resolvedUrlString);

		// otherwise return new URL in context of base URL
		return new URL(context, resolvedUrlString);
	}
	/**
	 * Resolves the URL based on platfrom Configuration
	 * $os$\$ws$\license.txt will become
	 * win32\win32\license.txt on a system where os=win32 and ws=win32
	 * 
	 * @param resolvedUrlString
	 * @return String
	 */
	private String resolvePlatfromConfiguration(String resolvedUrlString) {
		int osIndex = resolvedUrlString.indexOf("$os$"); //$NON-NLS-1$
		if (osIndex != -1)
			return getExtendedString(resolvedUrlString);

		int wsIndex = resolvedUrlString.indexOf("$ws$"); //$NON-NLS-1$
		if (wsIndex != -1)
			return getExtendedString(resolvedUrlString);

		int nlIndex = resolvedUrlString.indexOf("$nl$"); //$NON-NLS-1$
		if (nlIndex != -1)
			return getExtendedString(resolvedUrlString);

		int archIndex = resolvedUrlString.indexOf("$arch$"); //$NON-NLS-1$
		if (archIndex != -1)
			return getExtendedString(resolvedUrlString);

		return resolvedUrlString;
	}

	private String getExtendedString(String resolvedUrlString) {
		IPath path = new Path(resolvedUrlString);
		path = getExpandedPath(path);
		if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_WARNINGS) {
			UpdateCore.warn("Resolved :" + resolvedUrlString + " as:" + path.toOSString()); //$NON-NLS-1$ //$NON-NLS-2$
		}

		return path.toOSString();
	}

	private IPath getExpandedPath(IPath path) {
		String first = path.segment(0);
		if (first != null) {
			IPath rest = getExpandedPath(path.removeFirstSegments(1));
			if (first.equals("$ws$")) { //$NON-NLS-1$
				path = new Path(SiteManager.getWS()).append(rest);
			} else if (first.equals("$os$")) { //$NON-NLS-1$
				path = new Path(SiteManager.getOS()).append(rest);
			} else if (first.equals("$nl$")) { //$NON-NLS-1$
				path = new Path(SiteManager.getNL()).append(rest);
			} else if (first.equals("$arch$")) { //$NON-NLS-1$
				path = new Path(SiteManager.getOSArch()).append(rest);
			}
		}
		return path;
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
	 * @param bundleURL resource bundle url.
	 * @param string translatable string from model
	 * @return string, or <code>null</code>
	 * @since 2.0
	 */
	protected String resolveNLString(URL bundleURL, String string) {

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

		ResourceBundle b = getResourceBundle(bundleURL);

		if (b == null)
			return dflt;

		try {
			return b.getString(key.substring(1));
		} catch (MissingResourceException e) {
			return dflt;
		}
	}

	/**
	 * Returns a concrete array type for the elements of the specified
	 * list. The method assumes all the elements of the list are the same
	 * concrete type as the first element in the list.
	 * 
	 * @param l list
	 * @return concrete array type, or <code>null</code> if the array type
	 * could not be determined (the list is <code>null</code> or empty)
	 * @since 2.0
	 */
	protected Object[] arrayTypeFor(List l) {
		if (l == null || l.size() == 0)
			return null;
		return (Object[]) Array.newInstance(l.get(0).getClass(), 0);
	}

	/**
	 * Returns a concrete array type for the elements of the specified
	 * set. The method assumes all the elements of the set are the same
	 * concrete type as the first element in the set.
	 * 
	 * @param s set
	 * @return concrete array type, or <code>null</code> if the array type
	 * could not be determined (the set is <code>null</code> or empty)
	 * @since 2.0
	 */
	protected Object[] arrayTypeFor(Set s) {
		if (s == null || s.size() == 0)
			return null;
		Iterator i = s.iterator();
		return (Object[]) Array.newInstance(i.next().getClass(), 0);
	}

	/**
		* Helper method to access resouce bundle for feature. The default 
		* implementation attempts to load the appropriately localized 
		* feature.properties file.
		* 
		* @param url base URL used to load the resource bundle.
		* @return resource bundle, or <code>null</code>.
		* @since 2.0
		*/
	protected ResourceBundle getResourceBundle(URL url) {

		if (url == null)
			return null;

		if (bundles == null) {
			bundles = new HashMap();
		} else {
			ResourceBundle bundle = (ResourceBundle) bundles.get(url.toExternalForm());
			if (bundle != null)
				return bundle;
		}

		ResourceBundle bundle = null;
		try {
			url = UpdateManagerUtils.asDirectoryURL(url);
			ClassLoader l = new URLClassLoader(new URL[] { url }, null);
			bundle = ResourceBundle.getBundle(getPropertyName(), Locale.getDefault(), l);
			bundles.put(url.toExternalForm(), bundle);
		} catch (MissingResourceException e) {
			UpdateCore.warn(e.getLocalizedMessage() + ":" + url.toExternalForm()); //$NON-NLS-1$
		} catch (MalformedURLException e) {
			UpdateCore.warn(e.getLocalizedMessage()); 
		}
		return bundle;
	}

	/**
	 * Method getPropertyName.
	 * @return String
	 */
	protected String getPropertyName() {
		return Feature.FEATURE_FILE;
	}

}
