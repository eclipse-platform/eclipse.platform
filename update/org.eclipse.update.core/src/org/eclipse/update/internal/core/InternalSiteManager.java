/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.core;


import java.io.*;
import java.net.*;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.update.configuration.*;
import org.eclipse.update.core.*;
import org.eclipse.update.core.model.*;
import org.eclipse.update.internal.operations.UpdateUtils;

/**
 * 
 */
public class InternalSiteManager {

	public static ILocalSite localSite;

	public static final String DEFAULT_SITE_TYPE = SiteURLContentProvider.SITE_TYPE;
	private static final String DEFAULT_EXECUTABLE_SITE_TYPE = SiteFileContentProvider.SITE_TYPE;
	private static final String SIMPLE_EXTENSION_ID = "deltaHandler";
	//$NON-NLS-1$

	private static Map estimates;

	// cache found sites
	private static Map sites = new HashMap();
	// cache timestamps
	private static Map siteTimestamps = new HashMap();
	public static boolean globalUseCache = true;

	// true if an exception occured creating localSite
	// so we cache it and don't attempt to create it again
	private static CoreException exceptionOccured = null;

	/*
	 * @see SiteManager#getLocalSite()
	 */
	public static ILocalSite getLocalSite() throws CoreException {
		return internalGetLocalSite(false);
	}

	/*
	 * Internal call if optimistic reconciliation needed
	 */
	private static ILocalSite internalGetLocalSite(boolean isOptimistic) throws CoreException {

		// if an exception occured while retrieving the Site
		// rethrow it
		if (exceptionOccured != null)
			throw exceptionOccured;

		if (localSite == null) {
			try {
				localSite = SiteLocal.internalGetLocalSite(isOptimistic);
			} catch (CoreException e) {
				exceptionOccured = e;
				throw e;
			}
		}
		return localSite;
	}
	
	private static boolean isValidCachedSite(URL siteURL) {
		if (!sites.containsKey(siteURL.toExternalForm()))
			return false;
			
		Long timestamp = (Long)siteTimestamps.get(siteURL);
		if (timestamp == null)
			return false;
		long localLastModified = timestamp.longValue();
		
		return UpdateManagerUtils.isSameTimestamp(siteURL, localLastModified);
	}

	/*
	 * @see ILocalSite#getSite(URL)
	 */
	public static ISite getSite(URL siteURL, boolean useCache, IProgressMonitor monitor) throws CoreException {
		ISite site = null;
		if (monitor==null) monitor = new NullProgressMonitor();

		if (siteURL == null)
			return null;

		// use cache if set up globally (globalUseCache=true)
		// and passed as parameter (useCache=true)
		if ((useCache && globalUseCache) && isValidCachedSite(siteURL)) {
			site = (ISite) sites.get(siteURL.toExternalForm());
			return site;
		}

		// consider file protocol also if the URL points to a directory
		// and no site.xml exist
		// if the user points to a file, consider DEFAULT_SITE_TYPE
		// site.xml will have to specify the type
		boolean fileProtocol = "file".equalsIgnoreCase(siteURL.getProtocol());
		boolean directoryExists = false;
		if (fileProtocol) {
			File dir;
			dir = new File(siteURL.getFile());
			if (dir != null && dir.isDirectory()) {
				if (!(new File(dir, Site.SITE_XML).exists()))
					directoryExists = true;
			}
		}

		//PERF: if file: <path>/ and directory exists then consider executable
		monitor.beginTask(Policy.bind("InternalSiteManager.ConnectingToSite"), 8);
		if (fileProtocol && directoryExists) {
			site = attemptCreateSite(DEFAULT_EXECUTABLE_SITE_TYPE, siteURL, monitor);
			monitor.worked(4); // only one attempt
		} else {
			try {
				monitor.worked(3);
				site = attemptCreateSite(DEFAULT_SITE_TYPE, siteURL, monitor);
				monitor.worked(1);
			} catch (CoreException preservedException) {
				if (!monitor.isCanceled()) {
					// attempt a retry is the protocol is file, with executbale type
					if (!fileProtocol)
						throw preservedException;

					try {
						site = attemptCreateSite(DEFAULT_EXECUTABLE_SITE_TYPE, siteURL, monitor);
					} catch (CoreException retryException) {
						IStatus firstStatus = preservedException.getStatus();
						MultiStatus multi = new MultiStatus(firstStatus.getPlugin(), IStatus.OK, Policy.bind("InternalSiteManager.FailedRetryAccessingSite"), retryException); //$NON-NLS-1$
						multi.addAll(firstStatus);
						throw preservedException;
					}
				}
			}
		}

		if (site != null) {
			sites.put(siteURL.toExternalForm(), site);
			try {
				Response response = UpdateCore.getPlugin().get(URLEncoder.encode(siteURL));
				siteTimestamps.put(siteURL, new Long(response.getLastModified()));
			} catch (MalformedURLException e) {
			} catch (IOException e) {
			}
		}

		//flush the JarFile we may hold on to
		// we keep the temp not to create them again
		JarContentReference.shutdown(); // make sure we are not leaving jars open for this site

		//flush mapping of downloaded JAR files
		// FIXME : provide better cache flushing after 2.1
		// FIXED: everything downloaded is cached and timestamped.
		//        Timestamps are compared to lastModifed on the server
		//        and we download only when there is a differenc
		// Utilities.flushLocalFile();

		return site;
	}

	/*
	 * Attempt to create a site
	 * if the site guessed is not the type found,
	 * attempt to create a type with the type found in the site.xml
	 */
	private static ISite attemptCreateSite(String guessedTypeSite, URL siteURL, IProgressMonitor monitor) throws CoreException {
		if (monitor == null) monitor = new NullProgressMonitor();
		ISite site = null;

		try {
			monitor.worked(1);
			site = createSite(guessedTypeSite, siteURL, monitor);
			monitor.worked(1); // if no error, occurs the retry branch doesn't need to be executed
		} catch (InvalidSiteTypeException e) {
			if (monitor.isCanceled()) return null;

			// the type in the site.xml is not the one expected	
			// attempt to use this type instead	
			//DEBUG:
			if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_TYPE) {
				UpdateCore.debug("The Site :" + siteURL.toExternalForm() + " is a different type than the guessed type based on the protocol. new Type:" + e.getNewType());
				//$NON-NLS-1$ //$NON-NLS-2$
			}

			try {
				if (e.getNewType() == null)
					throw e;
				site = createSite(e.getNewType(), siteURL, monitor);
			} catch (InvalidSiteTypeException e1) {
				throw Utilities.newCoreException(Policy.bind("InternalSiteManager.UnableToCreateSiteWithType", e.getNewType(), siteURL.toExternalForm()), e1);
				//$NON-NLS-1$ //$NON-NLS-2$
			}
		}

		return site;
	}

	/*
	 * create an instance of a class that implements ISite
	 * 
	 * the URL can be of the following form
	 * 1 protocol://...../
	 * 2 protocol://.....
	 * 3 protocol://..../site.xml
	 * 4 protocol://...#...
	 * 
	 * 1 If the file of the file of teh url ends with '/', attempt to open the stream.
	 * if it fails, add site.xml and attempt to open the stream
	 * 
	 * 2 attempt to open the stream
	 * 	fail
	 * 		add '/site.xml' and attempt to open the stream
	 * 	sucess
	 * 		attempt to parse, if it fails, add '/site.xml' and attempt to open the stream
	 * 
	 * 3 open the stream
	 * 
	 * 4 open the stream	
	 */
	private static ISite createSite(String siteType, URL url, IProgressMonitor monitor) throws CoreException, InvalidSiteTypeException {
		ISite site = null;
		ISiteFactory factory = SiteTypeFactory.getInstance().getFactory(siteType);

		try {
			if (monitor != null)
				monitor.worked(1);
			site = createSite(factory, url, monitor);
		} catch (CoreException e) {
			if (monitor != null && monitor.isCanceled())
				return null;

			// if the URL is pointing to either a file 
			// or a directory, without reference			
			if (url.getRef() != null) {
				// 4 nothing we can do
				throw Utilities.newCoreException(Policy.bind("InternalSiteManager.UnableToAccessURL", url.toExternalForm()), e);
				//$NON-NLS-1$
			} else if (url.getFile().endsWith("/")) { //$NON-NLS-1$
				// 1 try to add site.xml
				URL urlRetry;
				try {
					urlRetry = new URL(url, Site.SITE_XML);
				} catch (MalformedURLException e1) {
					throw Utilities.newCoreException(Policy.bind("InternalSiteManager.UnableToCreateURL", url.toExternalForm() + "+" + Site.SITE_XML), e1);
					//$NON-NLS-1$ //$NON-NLS-2$
				}
				try {
					if (monitor != null)
						monitor.worked(1);
					site = createSite(factory, urlRetry, monitor);
				} catch (CoreException e1) {
					throw Utilities.newCoreException(Policy.bind("InternalSiteManager.UnableToAccessURL", url.toExternalForm()), url.toExternalForm(), urlRetry.toExternalForm(), e, e1);
					//$NON-NLS-1$
				}
			} else if (url.getFile().endsWith(Site.SITE_XML)) {
				// 3 nothing we can do
				throw Utilities.newCoreException(Policy.bind("InternalSiteManager.UnableToAccessURL", url.toExternalForm()), e);
				//$NON-NLS-1$
			} else {
				// 2 try to add /site.xml 
				URL urlRetry;
				try {
					urlRetry = new URL(url.getProtocol(), url.getHost(), url.getPort(), url.getFile() + "/" + Site.SITE_XML);
					//$NON-NLS-1$
				} catch (MalformedURLException e1) {
					throw Utilities.newCoreException(Policy.bind("InternalSiteManager.UnableToCreateURL", url.toExternalForm() + "+" + Site.SITE_XML), e1);
					//$NON-NLS-1$ //$NON-NLS-2$
				}

				try {
					if (monitor != null)
						monitor.worked(1);
					site = createSite(factory, urlRetry, monitor);
				} catch (CoreException e1) {
					throw Utilities.newCoreException(Policy.bind("InternalSiteManager.UnableToAccessURL", url.toExternalForm()), url.toExternalForm(), urlRetry.toExternalForm(), e, e1);
					//$NON-NLS-1$
				}
			}
		}
		return site;
	}
	
	private static ISite createSite(ISiteFactory factory, URL url, IProgressMonitor monitor) throws CoreException, InvalidSiteTypeException {
		if (factory instanceof ISiteFactoryExtension)
			return ((ISiteFactoryExtension)factory).createSite(url, monitor);
		else
			return factory.createSite(url);
	}

	/*
	 * Creates a new site on the file system
	 * This is the only Site we can create.
	 * 
	 * @param siteLocation
	 * @throws CoreException
	 */
	public static ISite createSite(File siteLocation) throws CoreException {
		ISite site = null;
		if (siteLocation != null) {
			try {
				URL siteURL = siteLocation.toURL();
				site = getSite(siteURL, false, null);
			} catch (MalformedURLException e) {
				throw Utilities.newCoreException(Policy.bind("InternalSiteManager.UnableToCreateURL", siteLocation.getAbsolutePath()), e);
				//$NON-NLS-1$
			}
		}
		return site;
	}

	/*
	 * Prompt the user to configure or unconfigure
	 * newly discoverd features.
	 * @throws CoreException if an error occurs.
	 * @since 2.0
	 */
	public static void handleNewChanges() throws CoreException {
		// find extension point
		IInstallDeltaHandler handler = null;

		String pluginID = UpdateCore.getPlugin().getBundle().getSymbolicName();

		IPluginRegistry pluginRegistry = Platform.getPluginRegistry();

		IConfigurationElement[] elements = pluginRegistry.getConfigurationElementsFor(pluginID, SIMPLE_EXTENSION_ID);

		if (elements == null || elements.length == 0) {
			throw Utilities.newCoreException(Policy.bind("SiteReconciler.UnableToFindInstallDeltaFactory", pluginID + "." + SIMPLE_EXTENSION_ID), null);
			//$NON-NLS-1$
		} else {
			IConfigurationElement element = elements[0];
			handler = (IInstallDeltaHandler) element.createExecutableExtension("class");
			//$NON-NLS-1$
		}

		// instanciate and open
		if (handler != null) {
			handler.init(UpdateUtils.getSessionDeltas());
			handler.open();
		}
	}



	/*
	 * Reconcile the local site following a specific reconciliation strategy 
	 * The parameter is true if we need to follow an optimistic reconciliation
	 * returns true if there are delta to process
	 * 
	 * Called internally by UpdateManagerReconciler aplication
	 */
	public static boolean reconcile(boolean optimisticReconciliation) throws CoreException {
		// reconcile
		internalGetLocalSite(optimisticReconciliation);

		// check if new features have been found
		if (localSite instanceof SiteLocal) {
			return SiteLocal.newFeaturesFound;
		}
		return false;
	}

	/**
	 * Method downloaded.
	 * @param l size downloaded in bytes
	 * @param l1 time in seconds
	 * @param uRL
	 */
	public static void downloaded(long downloadSize, long time, URL url) {
		if (downloadSize <= 0 || time < 0)
			return;
		String host = url.getHost();
		long sizeByTime = (time == 0) ? 0 : downloadSize / time;
		Long value = new Long(sizeByTime);
		if (estimates == null) {
			estimates = new HashMap();
		} else {
			Long previous = (Long) estimates.get(host);
			if (previous != null) {
				value = new Long((previous.longValue() + sizeByTime) / 2);
			}
		}
		estimates.put(host, value);
	}
	/**
	 * Method getEstimatedTransferRate rate bytes/seconds.
	 * @param string
	 * @return long
	 */
	public static long getEstimatedTransferRate(String host) {
		if (estimates == null)
			return 0;
		Long value = (Long) estimates.get(host);
		if (value == null)
			return 0;
		return value.longValue();
	}

}
