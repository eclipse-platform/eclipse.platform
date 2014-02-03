/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.configurator.branding;

import java.io.*;
import java.net.*;
import java.text.MessageFormat; // Can't use ICU, possible launch problem?
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.update.internal.configurator.Messages;
import org.eclipse.update.internal.configurator.Utils;
import org.osgi.framework.*;

/**
 * Reads the information found in an "INI" file. This file must be in a
 * standard Java properties format. A properties file may also be provided
 * to NL values in the INI file - values must start with the % prefix. A
 * mapping file may also be provided that contains "fill-ins" for the
 * properties file - format being "n = some text", where n is a number.
 */
public class IniFileReader {
	private static final String PID = "org.eclipse.update.configurator"; //$NON-NLS-1$
	private static final Status OK_STATUS = new Status(IStatus.OK, PID, 0, "", null); //$NON-NLS-1$
	private static final String KEY_PREFIX = "%"; //$NON-NLS-1$
	private static final String KEY_DOUBLE_PREFIX = "%%"; //$NON-NLS-1$
	private static final String NLS_TAG = "$nl$"; //$NON-NLS-1$

	private String featureId;
	private String pluginId;
	private String iniFilename;
	private String propertiesFilename;
	private String mappingsFilename;
	private Properties ini = null;
	private PropertyResourceBundle properties = null;
	private String[] mappings = null;
	private Bundle bundle;

	/**
	 * Creates an INI file reader that can parse the contents into key,value pairs.
	 * 
	 * @param featureId the unique identifier of the feature, must not be <code>null</code>
	 * @param pluginId the unique identifier of the feature plug-in, must not be <code>null</code>
	 * @param iniFilename the INI file name, must not be <code>null</code>
	 * @param propertiesFilename the properties filename, can be <code>null</code> if not required
	 * @param mappingsFilename the mappings filename, can be <code>null</code> if not required
	 */
	public IniFileReader(String featureId, String pluginId, String iniFilename, String propertiesFilename, String mappingsFilename) {
		super();
		
		if (featureId == null || pluginId == null || iniFilename == null) {
			throw new IllegalArgumentException();
		}
			
		this.featureId = featureId;
		this.pluginId = pluginId;
		this.iniFilename = iniFilename;
		this.propertiesFilename = propertiesFilename;
		this.mappingsFilename = mappingsFilename;
	}

	/**
	 * Read the contents of the INI, properties, and mappings files.
	 * Does nothing if the content has already been read and parsed.
	 * 
	 * @return an <code>IStatus</code> indicating the success or failure
	 * 	of reading and parsing the INI file content
	 */
	public IStatus load() {
		if (ini != null)
			return OK_STATUS;
			
		// attempt to locate the corresponding plugin
		bundle = Utils.getBundle(pluginId);
		if (bundle == null || bundle.getState() == Bundle.UNINSTALLED || bundle.getState() == Bundle.INSTALLED) {
			bundle = null; // make it null for other test down the road
			String message = NLS.bind(Messages.IniFileReader_MissingDesc, (new String[] { featureId }));
			return new Status(IStatus.ERROR, PID, 0, message, null);
		}

		// Determine the ini file location
		URL iniURL = null;
		IOException ioe = null;
		iniURL = FileLocator.find(bundle, new Path(NLS_TAG).append(iniFilename), null);
		if (iniURL == null) {
			String message = NLS.bind(Messages.IniFileReader_OpenINIError, (new String[] { iniFilename }));
			return new Status(IStatus.ERROR, PID, 0, message, ioe);
		}
		
		// Determine the properties file location
		URL propertiesURL = null;
		if (propertiesFilename != null & propertiesFilename.length() > 0) {
			propertiesURL = FileLocator.find(bundle, new Path(NLS_TAG).append(propertiesFilename), null);
		}

		// Determine the mappings file location
		URL mappingsURL = null;
		if (mappingsFilename != null && mappingsFilename.length() > 0) {
			mappingsURL = FileLocator.find(bundle, new Path(NLS_TAG).append(mappingsFilename), null);
		}

		// OK to pass null properties and/or mapping file
		return load(iniURL, propertiesURL, mappingsURL);
	}
		
	/**
	 * Returns the string value for the given key, or <code>null</code>.
	 * The string value is NLS if requested.
	 * 
	 * @return the string value for the given key, or <code>null</code>
	 */
	public String getString(String key, boolean doNls, Hashtable runtimeMappings) {
		if (ini == null)
			return null;
		String value = ini.getProperty(key);
		if (value != null && doNls)
			return getResourceString(value, runtimeMappings);
		return value;
	}


	/**
	 * Returns a URL for the given key, or <code>null</code>.
	 * 
	 * @return a URL for the given key, or <code>null</code>
	 */
	public URL getURL(String key) {
		if (ini == null)
			return null;

		URL url = null;
		String fileName = ini.getProperty(key);
		if (fileName != null) {
			if (bundle == null)
				return null;
			url = FileLocator.find(bundle, new Path(fileName), null);
		}
		return url;
	}
	
	/**
	 * Returns a array of URL for the given key, or <code>null</code>. The
	 * property value should be a comma separated list of urls, tokens for
	 * which bundle cannot build an url will have a null entry.
	 * 
	 * @param key name of the property containing the requested urls
	 * @return a URL for the given key, or <code>null</code>
	 * @since 3.0
	 */
	public URL[] getURLs(String key) {
		if (ini == null || bundle == null)
			return null;

		String value = ini.getProperty(key);
		if (value == null)
			return null;

		StringTokenizer tokens = new StringTokenizer(value, ","); //$NON-NLS-1$
		ArrayList array = new ArrayList(10);
		while (tokens.hasMoreTokens()) {
			String str = tokens.nextToken().trim();
			array.add(FileLocator.find(bundle, new Path(str), null));
		}

		URL[] urls = new URL[array.size()];
		array.toArray(urls);
		return urls;
	}

	/**
	 * Returns the feature plugin label, or <code>null</code>.
	 * 
	 * @return the feature plugin lable, or <code>null</code> if none.
	 */
	public String getFeaturePluginLabel() {
		if (bundle == null)
			return null;
		return (String)bundle.getHeaders().get(Constants.BUNDLE_NAME);
	}
	
	/**
	 * Returns the provider name for this feature, or <code>null</code>.
	 * 
	 * @return the provider name for this feature, or <code>null</code>
	 */
	public String getProviderName() {
		if (bundle == null)
			return null;
		return (String)bundle.getHeaders().get(Constants.BUNDLE_VENDOR);
	}
	
	/*
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
	 *     <li>getResourceString("Hello World") returns "Hello World"</li>
	 *     <li>getResourceString("%name") returns "Project Name"</li>
	 *     <li>getResourceString("%name Hello World") returns "Project Name"</li>
	 *     <li>getResourceString("%abcd Hello World") returns "Hello World"</li>
	 *     <li>getResourceString("%abcd") returns "%abcd"</li>
	 *     <li>getResourceString("%%name") returns "%name"</li>
	 *     <li>getResourceString(<code>null</code>) returns <code>null</code></li>
	 * </pre>
	 * </p>
	 *
	 * @param value the value or <code>null</code>
	 * @param runtimeMappings runtime mappings or <code>null</code>
	 * @return the resource string
	 */
	 public String getResourceString(String value, Hashtable runtimeMappings) {
		
		if (value == null)
			return null;
		String s = value.trim();

		if (!s.startsWith(KEY_PREFIX))
			return s;

		if (s.startsWith(KEY_DOUBLE_PREFIX))
			return s.substring(1);

		int ix = s.indexOf(" "); //$NON-NLS-1$
		String key = ix == -1 ? s : s.substring(0, ix);
		String dflt = ix == -1 ? s : s.substring(ix + 1);

		if (properties == null)
			return dflt;

		String result = null;
		try {
			result = properties.getString(key.substring(1));
		} catch (MissingResourceException e) {
			return dflt;
		}
		if (runtimeMappings != null) {
			for (Enumeration e = runtimeMappings.keys(); e.hasMoreElements();) {
				String keyValue = (String) e.nextElement();
				int i = result.indexOf(keyValue);
				if (i != -1) {
					String s1 = result.substring(0,i);
					String s2 = (String) runtimeMappings.get(keyValue);
					String s3 = result.substring(i+keyValue.length());
					result = s1 + s2 + s3;
				}
			}
		}
	
		if (result.indexOf('{') != -1) {
			// We test for the curly braces since due to NL issues we do not
			// want to use MessageFormat unless we have to.
			try {
				result = MessageFormat.format(result, mappings);
			} catch (IllegalArgumentException e) {
				//ignore and return string without bound parameters
			}
		}
		
		return result;	
	}

	/*
	 * Read the contents of the ini, properties, and mappings files.
	 */
	private IStatus load(URL iniURL, URL propertiesURL, URL mappingsURL) {

		InputStream is = null;
		try {
			is = iniURL.openStream();
			ini = new Properties();
			ini.load(is);
		} catch (IOException e) {
			ini = null;
			String message = NLS.bind(Messages.IniFileReader_ReadIniError, (new String[] { iniURL.toExternalForm() }));
			return new Status(IStatus.ERROR, PID, 0, message, e);
		} finally {
			try {
				if (is != null)
					is.close();
			} catch (IOException e) {
			}
		}

		if (propertiesURL != null) {
			is = null;
			try {
				is = propertiesURL.openStream();
				properties = new PropertyResourceBundle(is);
			} catch (IOException e) {
				properties = null;
				String message = NLS.bind(Messages.IniFileReader_ReadPropError, (new String[] { propertiesURL.toExternalForm() }));
				return new Status(IStatus.ERROR, PID, 0, message, e);
			} finally {
				try {
					if (is != null)
						is.close();
				} catch (IOException e) {
				}
			}
		}

		PropertyResourceBundle bundle = null;
		if (mappingsURL != null) {
			is = null;
			try {
				is = mappingsURL.openStream();
				bundle = new PropertyResourceBundle(is);
			} catch (IOException e) {
				bundle = null;
				String message = NLS.bind(Messages.IniFileReader_ReadMapError, (new String[] { mappingsURL.toExternalForm() }));
				return new Status(IStatus.ERROR, PID, 0, message, e);
			} finally {
				try {
					if (is != null)
						is.close();
				} catch (IOException e) {
				}
			}
		}

		ArrayList mappingsList = new ArrayList();
		if (bundle != null) {
			boolean found = true;
			int i = 0;
			while (found) {
				try {
					mappingsList.add(bundle.getString(Integer.toString(i)));
				} catch (MissingResourceException e) {
					found = false;
				}
				i++;
			}
		}
		mappings = (String[])mappingsList.toArray(new String[mappingsList.size()]);
		
		return OK_STATUS;
	}
}
