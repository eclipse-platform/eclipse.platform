package org.eclipse.update.core;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */

import java.io.File;
import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.update.configuration.IConfiguredSite;
import org.eclipse.update.configuration.ILocalSite;
import org.eclipse.update.internal.core.InternalSiteManager;

/**
 * Site Manager.
 * A helper class used for creating site instance. 
 * Site manager is a singleton class. It cannot be instantiated; 
 * all functionality is provided by static methods.
 * 
 * @see org.eclipse.update.core.ISite
 * @see org.eclipse.update.configuration.ILocalSite
 * @see org.eclipse.update.configuration.IConfiguredSite
 * @since 2.0
 */
public class SiteManager {

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
	 * @since 2.0 
	 */
	public static ISite getSite(URL siteURL) throws CoreException {
		return InternalSiteManager.getSite(siteURL);
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
	 */
	public static void handleNewChanges() throws CoreException{
		//InternalSiteManager.handleNewChanges();
	}
}