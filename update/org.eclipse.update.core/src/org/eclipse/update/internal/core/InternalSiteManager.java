package org.eclipse.update.internal.core;

/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */

import java.io.File;
import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.update.configuration.*;
import org.eclipse.update.core.*;
import org.eclipse.update.core.model.InvalidSiteTypeException;
import org.eclipse.update.internal.model.InstallChangeParser;

/**
 * 
 */
public class InternalSiteManager {

	public static ILocalSite localSite;

	public static final String DEFAULT_SITE_TYPE = SiteURLContentProvider.SITE_TYPE;
	private static final String DEFAULT_EXECUTABLE_SITE_TYPE = SiteFileContentProvider.SITE_TYPE;
	private static final String SIMPLE_EXTENSION_ID = "deltaHandler";
	//$NON-NLS-1$

	// cache found sites
	private static Map sites = new HashMap();
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

	/*
	 * @see ILocalSite#getSite(URL)
	 */
	public static ISite getSite(URL siteURL, boolean useCache) throws CoreException {
		ISite site = null;

		if (siteURL == null)
			return null;

		// use cache if set up globally (globalUseCache=true)
		// and passed as parameter (useCache=true)
		if ((useCache && globalUseCache) && sites.containsKey(siteURL)) {
			site = (ISite) sites.get(siteURL);
			return site;
		}

		try {
			site = attemptCreateSite(DEFAULT_SITE_TYPE, siteURL);
		} catch (CoreException preservedException) {
			// attempt a retry is the protocol is file, with executbale type
			if (!"file".equalsIgnoreCase(siteURL.getProtocol()))
				throw preservedException;
			try {
				site = attemptCreateSite(DEFAULT_EXECUTABLE_SITE_TYPE, siteURL);
			} catch (CoreException retryException) {
				IStatus firstStatus = preservedException.getStatus();
				MultiStatus multi = new MultiStatus(firstStatus.getPlugin(), IStatus.OK, Policy.bind("InternalSiteManager.FailedRetryAccessingSite"), retryException); //$NON-NLS-1$
				multi.addAll(firstStatus);
				throw preservedException;
			}
		}

		if (site != null)
			sites.put(siteURL, site);

		return site;
	}

	/*
	 * Attempt to create a site
	 * if the site guessed is not the type found,
	 * attempt to create a type with the type found in the site.xml
	 */
	private static ISite attemptCreateSite(String guessedTypeSite, URL siteURL) throws CoreException {
		ISite site = null;

		try {
			site = createSite(guessedTypeSite, siteURL);
		} catch (InvalidSiteTypeException e) {

			// the type in the site.xml is not the one expected	
			// attempt to use this type instead	

			//DEBUG:
			if (UpdateManagerPlugin.DEBUG && UpdateManagerPlugin.DEBUG_SHOW_TYPE) {
				UpdateManagerPlugin.debug("The Site :" + siteURL.toExternalForm() + " is a different type than the guessed type based on the protocol. new Type:" + e.getNewType());
				//$NON-NLS-1$ //$NON-NLS-2$
			}

			InvalidSiteTypeException exception = (InvalidSiteTypeException) e;
			try {
				if (exception.getNewType() == null)
					throw e;
				site = createSite(exception.getNewType(), siteURL);
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
	private static ISite createSite(String siteType, URL url) throws CoreException, InvalidSiteTypeException {
		ISite site = null;
		ISiteFactory factory = SiteTypeFactory.getInstance().getFactory(siteType);

		try {

			site = factory.createSite(url);

		} catch (CoreException e) {
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
					site = factory.createSite(urlRetry);
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
					site = factory.createSite(urlRetry);
				} catch (CoreException e1) {
					throw Utilities.newCoreException(Policy.bind("InternalSiteManager.UnableToAccessURL", url.toExternalForm()), url.toExternalForm(), urlRetry.toExternalForm(), e, e1);
					//$NON-NLS-1$
				}
			}
		}
		return site;
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
				site = getSite(siteURL, false);
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

		String pluginID = UpdateManagerPlugin.getPlugin().getDescriptor().getUniqueIdentifier();

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
			ISessionDelta[] deltas = getSessionDeltas();
			handler.init(deltas);
			handler.open();
		}
	}

	/*
	 * Returns the list of sessions deltas found on the file system
	 * 
	 * Do not cache, calculate everytime
	 * because we delete the file in SessionDelta when the session
	 * has been seen by the user
	 * 
	 * So the shared state is the file system itself
	 */
	private static ISessionDelta[] getSessionDeltas() {
		List sessionDeltas = new ArrayList();
		IPath path = UpdateManagerPlugin.getPlugin().getStateLocation();
		InstallChangeParser parser;

		File file = path.toFile();
		if (file.isDirectory()) {
			File[] allFiles = file.listFiles();
			for (int i = 0; i < allFiles.length; i++) {
				try {
					// TRACE
					if (UpdateManagerPlugin.DEBUG && UpdateManagerPlugin.DEBUG_SHOW_RECONCILER) {
						UpdateManagerPlugin.debug("Found delta change:" + allFiles[i]);
					}
					parser = new InstallChangeParser(allFiles[i]);
					ISessionDelta change = parser.getInstallChange();
					if (change != null) {
						sessionDeltas.add(change);
					}
				} catch (Exception e) {
					if (UpdateManagerPlugin.DEBUG && UpdateManagerPlugin.DEBUG_SHOW_RECONCILER) {
						UpdateManagerPlugin.log("Unable to parse install change:" + allFiles[i], e);
					}
				}
			}
		}

		if (sessionDeltas.size() == 0)
			return new ISessionDelta[0];

		return (ISessionDelta[]) sessionDeltas.toArray(arrayTypeFor(sessionDeltas));
	}

	/*
	 * Returns a concrete array type for the elements of the specified
	 * list. The method assumes all the elements of the list are the same
	 * concrete type as the first element in the list.
	 * 
	 * @param l list
	 * @return concrete array type, or <code>null</code> if the array type
	 * could not be determined (the list is <code>null</code> or empty)
	 * @since 2.0
	 */
	private static Object[] arrayTypeFor(List l) {
		if (l == null || l.size() == 0)
			return null;
		return (Object[]) Array.newInstance(l.get(0).getClass(), 0);
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
			return ((SiteLocal) localSite).newFeaturesFound;
		}
		return false;
	}
}