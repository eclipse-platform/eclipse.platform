package org.eclipse.update.internal.core;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.update.core.IArchiveReference;
import org.eclipse.update.core.SiteContentProvider;
/**
 * 
 */
public class SiteURLContentProvider extends SiteContentProvider {
	
	public static final String SITE_TYPE = "org.eclipse.update.core.http"; //$NON-NLS-1$

	/**
	 * Constructor for HTTPSite
	 */
	public SiteURLContentProvider(URL url) {
		super(url);
	}
	
	/*
	 * @see ISiteContentProvider#getArchiveReference(String)
	 */
	public URL getArchiveReference(String archiveId)  throws CoreException {
		URL contentURL = null;
		
		contentURL = getArchiveURLfor(archiveId);
		// if there is no mapping in the site.xml
		// for this archiveId, use the default one
		if (contentURL==null) {
			return super.getArchiveReference(archiveId);
		}
		
		return contentURL;
	}

	/**
	 * return the URL associated with the id of teh archive for this site
	 * return null if the archiveId is null, empty or 
	 * if teh list of archives on the site is null or empty
	 * of if there is no URL associated with the archiveID for this site
	 */
	private URL getArchiveURLfor(String archiveId) {
		URL result = null;
		boolean found = false;

		IArchiveReference[] siteArchives = getSite().getArchives();
		if (siteArchives.length > 0) {
			for (int i = 0; i < siteArchives.length && !found; i++) {
				if (UpdateManagerPlugin.DEBUG && UpdateManagerPlugin.DEBUG_SHOW_INSTALL)
					UpdateManagerPlugin.debug("GetArchiveURL for:"+archiveId+" compare to "+siteArchives[i].getPath());
				if (archiveId.trim().equalsIgnoreCase(siteArchives[i].getPath())) {
					result = siteArchives[i].getURL();
					found = true;
					break;
				}
			}
		}
		return result;
	}



	}

