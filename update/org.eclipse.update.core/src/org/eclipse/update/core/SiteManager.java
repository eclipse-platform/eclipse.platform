/*******************************************************************************
 *  Copyright (c) 2000, 2012 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Laurent Fourrier (laurent@fourrier.nom.fr) - HTTP Proxy code and NetAccess Plugin
 *******************************************************************************/
package org.eclipse.update.core;

import java.net.URL;

import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.*;
import org.eclipse.update.configuration.ILocalSite;
import org.eclipse.update.internal.core.InternalSiteManager;
import org.eclipse.update.internal.core.UpdateCore;

/**
 * Site Manager.
 * A helper class used for creating site instance. 
 * Site manager is a singleton class. It cannot be instantiated; 
 * all functionality is provided by static methods.
 * 
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 * @see org.eclipse.update.core.ISite
 * @see org.eclipse.update.configuration.ILocalSite
 * @see org.eclipse.update.configuration.IConfiguredSite
 * @since 2.0
 * @deprecated The org.eclipse.update component has been replaced by Equinox p2.
 * This API will be deleted in a future release. See bug 311590 for details.
 */
public class SiteManager {

	private static String os;
	private static String ws;
	private static String arch;
	private static String nl;

	private SiteManager() {
	}

	/** 
	 * Returns a site object for the site specified by the argument URL.
	 * Typically, the URL references a site manifest file on an update 
	 * site. An update site acts as a source of features for installation
	 * actions.
	 * 
	 * @param siteURL site URL
	 * @return site object for the url
	 * @exception CoreException
	 * @deprecated use getSite(URL,IPogressMonitor) instead
	 * @since 2.0 
	 */
	public static ISite getSite(URL siteURL) throws CoreException {
		return InternalSiteManager.getSite(siteURL, true,null);
	}

	/** 
	 * Returns a site object for the site specified by the argument URL.
	 * Typically, the URL references a site manifest file on an update 
	 * site. An update site acts as a source of features for installation
	 * actions.
	 * 
	 * @param siteURL site URL
	 * @param monitor the progress monitor
	 * @return site object for the url or <samp>null</samp> in case a 
	 * user canceled the connection in the progress monitor.
	 * @exception CoreException
	 * @since 2.1 
	 */
	public static ISite getSite(URL siteURL, IProgressMonitor monitor) throws CoreException {
		return InternalSiteManager.getSite(siteURL, true, monitor);
	}

	/** 
	 * Returns a site object for the site specified by the argument URL.
	 * Typically, the URL references a site manifest file on an update 
	 * site. An update site acts as a source of features for installation
	 * actions.
	 * 
	 * @param siteURL site URL
	 * @param usesCache <code>false</code> if the cache should be refreshed, and the site entirely reparsed, <code>false</code> otherwise.
	 * @return site object for the url
	 * @exception CoreException
	 * @deprecated use getSite(URL,boolean,IPogressMonitor) instead
	 * @since 2.0 
	 */
	public static ISite getSite(URL siteURL, boolean usesCache) throws CoreException {
		return InternalSiteManager.getSite(siteURL, usesCache,null);
	}

	/** 
	 * Returns a site object for the site specified by the argument URL.
	 * Typically, the URL references a site manifest file on an update 
	 * site. An update site acts as a source of features for installation
	 * actions.
	 * 
	 * @param siteURL site URL
	 * @param usesCache <code>false</code> if the cache should be refreshed, and the site entirely reparsed, <code>false</code> otherwise.
	 * @param monitor the progress monitor
	 * @return site object for the url or <samp>null</samp> in case a 
	 * user canceled the connection in the progress monitor.
	 * @exception CoreException
	 * @since 2.1
	 */
	public static ISite getSite(URL siteURL, boolean usesCache, IProgressMonitor monitor) throws CoreException {
		return InternalSiteManager.getSite(siteURL, usesCache, monitor);
	}


	/**
	 * Returns the "local site". A local site is a logical collection
	 * of configuration information plus one or more file system 
	 * installation directories, represented as intividual sites. 
	 * These are potential targets for installation actions.
	 * 
	 * @return the local site
	 * @exception CoreException
	 * @since 2.0 
	 */
	public static ILocalSite getLocalSite() throws CoreException {
		return InternalSiteManager.getLocalSite();
	}

	/**
	 * Trigger handling of newly discovered features. This method
	 * can be called by the executing application whenever it
	 * is invoked with the -newUpdates command line argument.
	 * 
	 * @throws CoreException if an error occurs.
	 * @since 2.0
	 * @deprecated Do not use this method
	 */
	public static void handleNewChanges() throws CoreException {
	}
	/**
	 * Returns system architecture specification. A comma-separated list of arch
	 * designators defined by the platform. 
	 * 
	 * This information is used as a hint by the installation and update
	 * support.
	 * 
	 * @return system architecture specification
	 * @since 2.1
	 */
	public static String getOSArch() {
		if (arch == null)
			arch = Platform.getOSArch();
		return arch;
	}

	/**
	 * Returns operating system specification. A comma-separated list of os
	 * designators defined by the platform.
	 * 
	 * This information is used as a hint by the installation and update
	 * support.
	 *
	 * @return the operating system specification.
	 * @since 2.1
	 */
	public static String getOS() {
		if (os == null)
			os = Platform.getOS();
		return os;
	}

	/**
	 * Returns system architecture specification. A comma-separated list of arch
	 * designators defined by the platform. 
	 * 
	 * This information is used as a hint by the installation and update
	 * support.
	 * @return system architecture specification.
	 * @since 2.1
	 */
	public static String getWS() {
		if (ws == null)
			ws = Platform.getWS();
		return ws;
	}

	/**
	 * Sets the arch.
	 * @param arch The arch to set
	 */
	public static void setOSArch(String arch) {
		SiteManager.arch = arch;
	}

	/**
	 * Sets the os.
	 * @param os The os to set
	 */
	public static void setOS(String os) {
		SiteManager.os = os;
	}

	/**
	 * Sets the ws.
	 * @param ws The ws to set
	 */
	public static void setWS(String ws) {
		SiteManager.ws = ws;
	}

	/**
	 * Sets the nl.
	 * @param nl The nl to set
	 */
	public static void setNL(String nl) {
		SiteManager.nl = nl;
	}
	
	/**
	 * Returns an estimate of bytes per second transfer rate for this URL
	 * @param site the URL of the site
	 * @return long a bytes per second estimate rate
	 * @since 2.1
 	 */	
	public static long getEstimatedTransferRate(URL site) {
		if (site == null)
			return 0;
		return InternalSiteManager.getEstimatedTransferRate(site.getHost());
	}

	/**
	 * Returns current locale
	 * 
	 * @return the string name of the current locale or <code>null</code>
	 * @since 2.1
	 */
	public static String getNL() {
		if (nl == null)
			nl = Platform.getNL();
		return nl;
	}

	/**
	 * Returns the HTTP Proxy Server or <code>null</code> if none.
	 * @return the HTTP proxy Server 
	 * @deprecated clients should access the {@link IProxyService} directly
	 */
	public static String getHttpProxyServer() {
		IProxyService service = UpdateCore.getPlugin().getProxyService();
		if (service != null && service.isProxiesEnabled()) {
			IProxyData data = service.getProxyData(IProxyData.HTTP_PROXY_TYPE);
			if (data != null)
				return data.getHost();
			
		}
		return null;
	}
	/**
	 * Returns the HTTP Proxy Port or <code>null</code> if none
	 * @return the HTTP proxy Port
	 * @deprecated clients should access the {@link IProxyService} directly
	 */
	public static String getHttpProxyPort() {
		IProxyService service = UpdateCore.getPlugin().getProxyService();
		if (service != null && service.isProxiesEnabled()) {
			IProxyData data = service.getProxyData(IProxyData.HTTP_PROXY_TYPE);
			if (data != null) {
				if (data.getPort() == -1)
					return "80";
				return String.valueOf(data.getPort());
			}
			
		}
		return null;
	}
	
	/**
	 * Returns <code>true</code> if the connection should use the 
	 * http proxy server, <code>false</code> otherwise
	 * @return is the http proxy server enable
	 * @deprecated clients should access the {@link IProxyService} directly
	 */
	public static boolean isHttpProxyEnable() {
		IProxyService service = UpdateCore.getPlugin().getProxyService();
		if (service != null && service.isProxiesEnabled()) {
			IProxyData data = service.getProxyData(IProxyData.HTTP_PROXY_TYPE);
			return (data != null && data.getHost() != null);
		}
		return false;
	}
	/**
	 * Sets the HTTP Proxy information
	 * Sets the HTTP proxy server for the HTTP proxy server 
	 * Sets the HTTP proxy port for the HTTP proxy server 
	 * If the proxy name is <code>null</code> or the proxy port is
	 * <code>null</code> the connection will not use HTTP proxy server.
	 * 
	 * @param enable <code>true</code> if the connection should use an http
	 * proxy server, <code>false </code> otherwise.
	 * @param httpProxyServer the HTTP proxy server name or IP address
	 * @param httpProxyPort the HTTP proxy port
	 * 
	 * @deprecated clients should use the {@link IProxyService} directly
	 */
	public static void setHttpProxyInfo(boolean enable, String httpProxyServer, String httpProxyPort) {
		IProxyService service = UpdateCore.getPlugin().getProxyService();
		if (service == null)
			return;
		// Make sure that the proxy service is enabled if needed but don't disable the
		// service if the http proxy is being disabled
		if (enable && !service.isProxiesEnabled())
			service.setProxiesEnabled(enable);

		if (service.isProxiesEnabled()) {
			IProxyData data = service.getProxyData(IProxyData.HTTP_PROXY_TYPE);
			if (data != null) {
				data.setHost(httpProxyServer);
				if (httpProxyPort == null || httpProxyPort.equals("80")) {
					data.setPort(-1);
				} else {
					try {
						int port = Integer.parseInt(httpProxyPort);
						data.setPort(port);
					} catch (NumberFormatException e) {
						UpdateCore.log(e);
					}
				}
				try {
					service.setProxyData(new IProxyData[] { data });
				} catch (CoreException e) {
					UpdateCore.log(e);
				}
			}
		}
	}
}
