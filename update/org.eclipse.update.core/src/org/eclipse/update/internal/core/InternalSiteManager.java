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
package org.eclipse.update.internal.core;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.update.configuration.ILocalSite;
import org.eclipse.update.core.ISite;
import org.eclipse.update.core.ISiteFactory;
import org.eclipse.update.core.ISiteFactoryExtension;
import org.eclipse.update.core.JarContentReference;
import org.eclipse.update.core.Site;
import org.eclipse.update.core.Utilities;
import org.eclipse.update.core.model.InvalidSiteTypeException;
import org.eclipse.update.internal.core.connection.ConnectionFactory;
import org.eclipse.update.internal.core.connection.IResponse;
import org.eclipse.update.internal.model.ITimestamp;

/**
 * 
 */
public class InternalSiteManager {

	public static ILocalSite localSite;

	public static final String DEFAULT_SITE_TYPE = SiteURLContentProvider.SITE_TYPE;
	private static final String DEFAULT_EXECUTABLE_SITE_TYPE = SiteFileContentProvider.SITE_TYPE;

	private static Map estimates;

	// cache found sites
	private static Map sites = new HashMap();
	// cache http updated url
	private static Map httpSitesUpdatedUrls = new HashMap();
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
		return internalGetLocalSite();
	}

	/*
	 * Internal call if optimistic reconciliation needed
	 */
	private static ILocalSite internalGetLocalSite() throws CoreException {

		// if an exception occured while retrieving the Site
		// rethrow it
		if (exceptionOccured != null)
			throw exceptionOccured;

		if (localSite == null) {
			try {
				localSite = LocalSite.internalGetLocalSite();
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
		if (httpSitesUpdatedUrls.containsKey(siteURL.toExternalForm())) {
			siteURL = (URL)httpSitesUpdatedUrls.get(siteURL.toExternalForm());
		}
		String siteURLString = siteURL.toExternalForm();
		if ((useCache && globalUseCache) && isValidCachedSite(siteURL)) {
			site = (ISite) sites.get(siteURLString);
			UpdateCore.getPlugin().getUpdateSession().markVisited(site.getURL());
			return site;
		}
		
		// try adding "eclipse" to the site url, in case this is an extension site
		if ("file".equals(siteURL.getProtocol()) ) { //$NON-NLS-1$
			File f = new File(siteURL.getFile());
			if (f.isDirectory() && !"eclipse".equals(f.getName())) { //$NON-NLS-1$
				f = new File(f, "eclipse"); //$NON-NLS-1$
				try {
					if ((useCache && globalUseCache) && isValidCachedSite(f.toURL())) {
						site = (ISite) sites.get(f.toURL().toExternalForm());
						return site;
					}
				} catch (MalformedURLException e) {
				}	
			}
		}

		// consider file protocol also if the URL points to a directory
		// and no site.xml exist
		// if the user points to a file, consider DEFAULT_SITE_TYPE
		// site.xml will have to specify the type
		boolean fileProtocol = "file".equalsIgnoreCase(siteURL.getProtocol()); //$NON-NLS-1$
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
		monitor.beginTask(Messages.InternalSiteManager_ConnectingToSite, 8); 
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
						MultiStatus multi = new MultiStatus(firstStatus.getPlugin(), IStatus.OK, Messages.InternalSiteManager_FailedRetryAccessingSite, retryException); 
						multi.addAll(firstStatus);
						throw preservedException;
					}
				}
			}
		}

		if (site != null) {
			sites.put(site.getURL().toExternalForm(), site);
			UpdateCore.getPlugin().getUpdateSession().markVisited(site.getURL());
			if (site instanceof ITimestamp) {
				siteTimestamps.put(site.getURL(), new Long(((ITimestamp)site).getTimestamp().getTime()));
			} else {
				try {
					IResponse response = ConnectionFactory.get(URLEncoder.encode(siteURL));
					siteTimestamps.put(siteURL, new Long(response.getLastModified()));
				} catch (MalformedURLException e) {
				} catch (IOException e) {
				}
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
				UpdateCore.debug("The Site :" + siteURL.toExternalForm() + " is a different type than the guessed type based on the protocol. new Type:" + e.getNewType());	//$NON-NLS-1$ //$NON-NLS-2$ 
			}

			try {
				if (e.getNewType() == null)
					throw e;
				site = createSite(e.getNewType(), siteURL, monitor);
			} catch (InvalidSiteTypeException e1) {
				throw Utilities.newCoreException(NLS.bind(Messages.InternalSiteManager_UnableToCreateSiteWithType, (new String[] { e.getNewType(), siteURL.toExternalForm() })), e1); 
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
		
		if (monitor == null) monitor = new NullProgressMonitor();
		//ISite site = null;
		ISiteFactory factory = SiteTypeFactory.getInstance().getFactory(siteType);
		URL fixedUrl;
		
		// see if we need to (and can) fix url by adding site.xml to it
		try {
			if ( (url.getRef() != null) || (url.getFile().endsWith(Site.SITE_XML) || (url.getProtocol().equalsIgnoreCase("file")))) { //$NON-NLS-1$
			 	fixedUrl = url;
			} else if (url.getFile().endsWith("/")) { //$NON-NLS-1$
				fixedUrl = new URL(url, Site.SITE_XML);
			} else {
				fixedUrl = new URL(url.getProtocol(), url.getHost(), url.getPort(), url.getFile() + "/" + Site.SITE_XML);	//$NON-NLS-1$
			}
		} catch (MalformedURLException mue) {
			fixedUrl = url;
		}
		
		try {
			try { 
				monitor.worked(1);
				return createSite( factory, fixedUrl, url, monitor);
			} catch (CoreException ce) {
				if (monitor.isCanceled()) 
					return null;
			
				if (!fixedUrl.equals(url)) {
					// try with original url
					 return createSite( factory, url, url, monitor);
				} else if (url.getProtocol().equalsIgnoreCase("file") && ! url.getFile().endsWith(Site.SITE_XML)){ //$NON-NLS-1$
					try {
						if (url.getFile().endsWith("/")) { //$NON-NLS-1$
							return createSite(factory, new URL(url,
									Site.SITE_XML), url, monitor);
						} else {
							return createSite(factory, new URL(url
									.getProtocol(), url.getHost(), url
									.getPort(), url.getFile()
									+ "/" + Site.SITE_XML), url, monitor); //$NON-NLS-1$							
						}
					} catch (MalformedURLException mue) {
						throw ce;
					}
				} else {
					throw ce;
				}
			}
		} catch(CoreException ce) {
			throw Utilities.newCoreException(NLS.bind(Messages.InternalSiteManager_UnableToAccessURL, (new String[] { url.toExternalForm() })), ce);
		}
	}
	
	private static ISite createSite(ISiteFactory factory, URL url, URL originalUrl, IProgressMonitor monitor) throws CoreException, InvalidSiteTypeException {
		
		ISite site;
			
		site = createSite(factory, url, monitor);
		httpSitesUpdatedUrls.put(originalUrl.toExternalForm(), url);	
		
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
				throw Utilities.newCoreException(NLS.bind(Messages.InternalSiteManager_UnableToCreateURL, (new String[] { siteLocation.getAbsolutePath() })), e);
			}
		}
		return site;
	}


	/**
	 * Method downloaded.
	 * @param downloadSize size downloaded in bytes
	 * @param time time in seconds
	 * @param url
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
	 * @param host
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
