/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
package org.eclipse.help.internal.protocols;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.Vector;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.Platform;
import org.eclipse.help.internal.base.remote.HttpsUtility;
import org.eclipse.help.internal.base.remote.PreferenceFileHandler;
import org.eclipse.help.internal.base.remote.RemoteContentLocator;
import org.eclipse.help.internal.base.remote.RemoteHelp;
import org.eclipse.help.internal.base.remote.RemoteHelpInputStream;
import org.eclipse.help.internal.base.util.ProxyUtil;
import org.eclipse.help.internal.util.ResourceLocator;
import org.eclipse.help.internal.util.URLCoder;
import org.osgi.framework.Bundle;

/**
 * URLConnection to help documents in plug-ins
 */
public class HelpURLConnection extends URLConnection {

	private final static String PARAM_LANG = "lang"; //$NON-NLS-1$
	private final static String PRODUCT_PLUGIN = "PRODUCT_PLUGIN"; //$NON-NLS-1$
	public final static String PLUGINS_ROOT = "PLUGINS_ROOT/"; //$NON-NLS-1$
	private final static String PATH_RTOPIC = "/rtopic"; //$NON-NLS-1$
	private static final String PROTOCOL_HTTP = "http://"; //$NON-NLS-1$

	private static Hashtable<String, String[]> templates = new Hashtable<>();

	// document caching - disabled if running in dev mode
	protected static boolean cachingEnabled = true;
	static {
		String[] args = Platform.getCommandLineArgs();
		for (String arg : args) {
			if ("-dev".equals(arg)) { //$NON-NLS-1$
				cachingEnabled = false;
				break;
			}
		}
	}

	protected String pluginAndFile; // plugin/file
	protected String query; // after ?
	protected HashMap<String, Object> arguments;
	protected Bundle plugin;
	// file in a plug-in
	protected String file;
	protected String locale;
	private static String appserverImplPluginId;
	private boolean localOnly;

	/**
	 * Constructor for HelpURLConnection
	 */
	public HelpURLConnection(URL url) {
		this(url, false);
	}

	public HelpURLConnection(URL url, boolean localOnly) {
		super(url);
		this.localOnly = localOnly;
		String urlFile = url.getFile();

		// Strip off everything before and including the PLUGINS_ROOT
		int index = urlFile.indexOf(PLUGINS_ROOT);
		if (index != -1)
			urlFile = urlFile.substring(index + PLUGINS_ROOT.length());
		// Strip off the leading "/" and the query
		if (urlFile.startsWith("/")) //$NON-NLS-1$
			urlFile = urlFile.substring(1);

		int indx = urlFile.indexOf('?');
		if (indx != -1) {
			query = urlFile.substring(indx + 1);
			urlFile = urlFile.substring(0, indx);
		}
		this.pluginAndFile = urlFile;
		parseQuery();

		setDefaultUseCaches(isCacheable());
	}

	@Override
	public void connect() throws IOException {
	}

	/**
	 * see URLConnection#getInputStream(); Note: this method can throw IOException, but should never
	 * return null
	 */
	@SuppressWarnings("resource")
	@Override
	public InputStream getInputStream() throws IOException {
		// must override parent implementation, since it does nothing.
		Bundle plugin = getPlugin();
		if (plugin != null && plugin.getSymbolicName().equals(getAppserverImplPluginId())) {
			// Do not return documents from app server implementation plug-in
			throw new IOException("Resource not found."); //$NON-NLS-1$
		}

		if (getFile() == null || "".equals(getFile()) || getFile().contains("..\\")) { //$NON-NLS-1$ //$NON-NLS-2$
			throw new IOException("Resource not found."); //$NON-NLS-1$
		}

		int helpOption=localOnly ? PreferenceFileHandler.LOCAL_HELP_ONLY
			: PreferenceFileHandler.getEmbeddedHelpOption();
		InputStream in = null;
		if (plugin != null && (helpOption==PreferenceFileHandler.LOCAL_HELP_ONLY || helpOption==PreferenceFileHandler.LOCAL_HELP_PRIORITY)) {
			in = getLocalHelp(plugin);
		}
		if (in == null && (helpOption==PreferenceFileHandler.LOCAL_HELP_PRIORITY || helpOption==PreferenceFileHandler.REMOTE_HELP_PRIORITY)) {

			in = openFromRemoteServer(getHref(), getLocale());
			if( in != null ){
				in = new RemoteHelpInputStream(in);
			}
			if(in==null && plugin!=null && helpOption==PreferenceFileHandler.REMOTE_HELP_PRIORITY)
			{
				in = getLocalHelp(plugin);
			}
		}
		if (in == null) {
			throw new IOException("Resource not found."); //$NON-NLS-1$
		}
		return in;
	}

	@SuppressWarnings("resource")
	private InputStream getLocalHelp(Bundle plugin) {
		// first try using content provider, then try to find the file
		// inside doc.zip, and finally try the file system
		InputStream in = ResourceLocator.openFromProducer(plugin,
				query == null ? getFile() : getFile() + "?" + query, //$NON-NLS-1$
				getLocale());

		if (in == null) {
			in = ResourceLocator.openFromPlugin(plugin, getFile(), getLocale());
		}
		if (in == null) {
			in = ResourceLocator.openFromZip(plugin, "doc.zip", //$NON-NLS-1$
					getFile(), getLocale());
		}
		return in;
	}

	@Override
	public long getExpiration() {
		return isCacheable() ? new Date().getTime() + 10000 : 0;
	}

	public static void parseQuery(String query, HashMap<String, Object> arguments) {
		StringTokenizer stok = new StringTokenizer(query, "&"); //$NON-NLS-1$
		while (stok.hasMoreTokens()) {
			String aQuery = stok.nextToken();
			int equalsPosition = aQuery.indexOf('=');
			if (equalsPosition > -1) { // well formed name/value pair
				String arg = aQuery.substring(0, equalsPosition);
				String val = aQuery.substring(equalsPosition + 1);
				Object existing = arguments.get(arg);
				if (existing == null)
					arguments.put(arg, val);
				else if (existing instanceof Vector) {
					@SuppressWarnings("unchecked")
					Vector<String> vector = (Vector<String>) existing;
					vector.add(val);
					arguments.put(arg, existing);
				} else {
					Vector<Object> v = new Vector<>(2);
					v.add(existing);
					v.add(val);
					arguments.put(arg, v);
				}
			}
		}
	}

	/**
	 * NOTE: need to add support for multi-valued parameters (like filtering) Multiple values are
	 * added as vectors
	 */
	protected void parseQuery() {
		if (query != null && !"".equals(query)) { //$NON-NLS-1$
			if (arguments == null) {
				arguments = new HashMap<>(5);
			}
			parseQuery(query, arguments);
		}
	}

	@Override
	public String getContentType() {
		// Check if the file is hypertext or plain text
		String file = pluginAndFile.toLowerCase(Locale.US);
		if (file.endsWith(".html") || file.endsWith(".htm") //$NON-NLS-1$ //$NON-NLS-2$
				|| file.endsWith(".xhtml")) //$NON-NLS-1$
			return "text/html"; //$NON-NLS-1$
		else if (file.endsWith(".css")) //$NON-NLS-1$
			return "text/css"; //$NON-NLS-1$
		else if (file.endsWith(".gif")) //$NON-NLS-1$
			return "image/gif"; //$NON-NLS-1$
		else if (file.endsWith(".svg")) //$NON-NLS-1$
			return "image/svg+xml"; //$NON-NLS-1$
		else if (file.endsWith(".jpg")) //$NON-NLS-1$
			return "image/jpeg"; //$NON-NLS-1$
		else if (file.endsWith(".pdf")) //$NON-NLS-1$
			return "application/pdf"; //$NON-NLS-1$
		else if (file.endsWith(".xml")) //$NON-NLS-1$
			return "application/xml"; //$NON-NLS-1$
		else if (file.endsWith(".xsl")) //$NON-NLS-1$
			return "application/xsl"; //$NON-NLS-1$
		return "text/plain"; //$NON-NLS-1$
	}

	/**
	 *
	 */
	public Vector<String> getMultiValue(String name) {
		if (arguments != null) {
			Object value = arguments.get(name);
			if (value instanceof Vector) {
				@SuppressWarnings("unchecked")
				Vector<String> vector = (Vector<String>) value;
				return vector;
			}
			return null;
		}
		return null;
	}

	/**
	 *
	 */
	public String getValue(String name) {
		if (arguments == null)
			return null;
		Object value = arguments.get(name);
		String stringValue = null;
		if (value instanceof String)
			stringValue = (String) value;
		else if (value instanceof Vector) {
			@SuppressWarnings("unchecked")
			Vector<String> vector = (Vector<String>) value;
			stringValue = vector.firstElement();
		} else
			return null;
		try {
			return URLCoder.decode(stringValue);
		} catch (Exception e) {
			return null;
		}

	}

	/**
	 * Returns the locale specified by client.
	 */
	protected String getLocale() {
		if (locale == null) {
			locale = getValue(PARAM_LANG);
			if (locale == null) {
				locale = Platform.getNL();
			}
		}
		return locale;
	}

	protected String getFile() {
		if (file == null) {
			// Strip the plugin id
			int start = pluginAndFile.indexOf('/') + 1;
			// Strip query string or anchor bookmark
			int end = pluginAndFile.indexOf('?');
			if (end == -1)
				end = pluginAndFile.indexOf('#');
			if (end == -1)
				end = pluginAndFile.length();
			file = pluginAndFile.substring(start, end);
			file = URLCoder.decode(file);
		}
		return file;
	}

	protected Bundle getPlugin() {
		if (plugin == null) {
			// Assume the url is pluginID/path_to_topic.html
			int i = pluginAndFile.indexOf('/');
			String pluginId = i == -1 ? "" : pluginAndFile.substring(0, i); //$NON-NLS-1$
			pluginId = URLCoder.decode(pluginId);
			if (PRODUCT_PLUGIN.equals(pluginId)) {
				IProduct product = Platform.getProduct();
				if (product != null) {
					plugin = product.getDefiningBundle();
					return plugin;
				}
			}
			plugin = Platform.getBundle(pluginId);
		}
		return plugin;
	}

	private String getHref() {
		return '/' + pluginAndFile;
	}

	public boolean isCacheable() {
		if (getValue("resultof") != null) //$NON-NLS-1$
			return false;
		return cachingEnabled;
	}

	@Override
	public String toString() {
		return pluginAndFile;
	}

	/**
	 * Obtains ID of plugin that contributes appserver implementation. *
	 *
	 * @return plug-in ID or null
	 */
	private static String getAppserverImplPluginId() {
		if (appserverImplPluginId == null) {

			// This part mimics AppserverPlugin.createWebappServer()

			// get the app server extension from the system plugin registry
			IExtensionRegistry pluginRegistry = Platform.getExtensionRegistry();
			IExtensionPoint point = pluginRegistry.getExtensionPoint("org.eclipse.help.appserver.server"); //$NON-NLS-1$
			if (point != null) {
				IExtension[] extensions = point.getExtensions();
				if (extensions.length != 0) {
					// We need to pick up the non-default configuration
					IConfigurationElement[] elements = extensions[0].getConfigurationElements();
					if (elements.length == 0)
						return null;
					IConfigurationElement serverElement = null;
					for (int i = 0; i < elements.length; i++) {
						String defaultValue = elements[i].getAttribute("default"); //$NON-NLS-1$
						if (defaultValue == null || defaultValue.equals("false")) { //$NON-NLS-1$
							serverElement = elements[i];
							break;
						}
					}
					// if all the servers are default, then pick the first one
					if (serverElement == null) {
						serverElement = elements[0];
					}
					//

					appserverImplPluginId = serverElement.getContributor().getName();

				}
			}
		}
		return appserverImplPluginId;
	}

	/*
	 * Opens a connection to the document on the remote help server, if one was specified. If the
	 * document doesn't exist on the remote server, returns null.
	 */
	@SuppressWarnings("resource")
	private InputStream openFromRemoteServer(String href, String locale) {
		if (RemoteHelp.isEnabled()) {

			String pathSuffix = PATH_RTOPIC + href + '?' + PARAM_LANG + '=' + locale;

			/*
			 * Get the URL that maps to the contributorID Assume the url is
			 * pluginID/path_to_topic.html
			 */
			int i = pluginAndFile.indexOf('/');
			String pluginId = i == -1 ? "" : pluginAndFile.substring(0, i); //$NON-NLS-1$
			pluginId = URLCoder.decode(pluginId);

			String remoteURL = RemoteContentLocator.getUrlForContent(pluginId);

			InputStream in;
			if (remoteURL == null) {
				in = tryOpeningAllServers(pathSuffix);
			} else {
				in = openRemoteStream(remoteURL, pathSuffix);
			}

			return in;
		}
		return null;
	}

	@SuppressWarnings("resource")
	private InputStream getUnverifiedStream(String remoteURL,String pathSuffix)
	{
		URL url;
		InputStream in = null;
		try {

			if(remoteURL.startsWith(PROTOCOL_HTTP))
			{
				url = new URL(remoteURL + pathSuffix);
				HttpURLConnection connection = (HttpURLConnection) ProxyUtil.getConnection(url);
				in = connection.getInputStream();
			}
			else
			{
				url = HttpsUtility.getHttpsURL(remoteURL + pathSuffix);
				in = HttpsUtility.getHttpsStream(url);
			}

		} catch (Exception e) {
			// File not found on this server
		}
		return in;
	}


	private InputStream openRemoteStream(String remoteURL, String pathSuffix)  {
		InputStream in = getUnverifiedStream(remoteURL,pathSuffix);

		String errPage[] = templates.get(remoteURL);
		if (errPage==null)
		{
			try (InputStream is = getUnverifiedStream(remoteURL, "/rtopic/fakeurltogetatestpage/_ACEGIKMOQ246.html")) { //$NON-NLS-1$
				String error = getPageText(is);
				if (error != null) {
					errPage = error.split("\n"); //$NON-NLS-1$
					templates.put(remoteURL, errPage);
				} else {
					errPage = new String[0];
					templates.put(remoteURL, errPage);
				}
			} catch (IOException e) {
			}
		}


		// No error page, InfoCenter is at least 3.6, so it is
		// returning null already.
		if (errPage.length==0)
			return in;

		// Check to see if the URL is the error page for the
		// remote IC.  If so, return null.
		try (InputStream is = getUnverifiedStream(remoteURL, pathSuffix)) {
			if (compare(errPage, is)) {
				try {
					in.close();
				} catch (Exception ex) {
				}
				return null;
			}
		} catch (IOException e) {
		}
		return in;
	}

	private boolean compare(String lines[],InputStream in)
	{
		try{
			if (in!=null)
			{
				try (BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
					String line;
					int count = 0;

					while ((line = br.readLine()) != null) {
						if (count > lines.length)
							return false;

						if (!lines[count].equals(line))
							return false;
						count++;
					}
				}
				in.close();
				return true;
			}
		}catch(Exception ex)
		{}
		return false;
	}

	private String getPageText(InputStream in) {
		try{
			if (in!=null)
			{
				String line, result = ""; //$NON-NLS-1$
				try (BufferedReader br = new BufferedReader(new InputStreamReader(in))) {

					while ((line = br.readLine()) != null) {
						result += line + '\n';
					}
				}
				in.close();
				return result;
			}
		}catch(Exception ex){}

		return null;
	}

	private InputStream tryOpeningAllServers(String pathSuffix) {
		PreferenceFileHandler prefHandler = new PreferenceFileHandler();
		String host[] = prefHandler.getHostEntries();
		String port[] = prefHandler.getPortEntries();
		String protocol[] = prefHandler.getProtocolEntries();
		String path[] = prefHandler.getPathEntries();
		String isEnabled[] = prefHandler.isEnabled();

		int numICs = host.length;

		for (int i = 0; i < numICs; i++) {
			if (isEnabled[i].equalsIgnoreCase("true")) { //$NON-NLS-1$
				String urlStr = protocol[i]+"://" + host[i] + ':' + port[i] + path[i]; //$NON-NLS-1$
				InputStream is = openRemoteStream(urlStr, pathSuffix);
				if (is != null) {
					return is;
				}
			}
		}
		return null;
	}

}
