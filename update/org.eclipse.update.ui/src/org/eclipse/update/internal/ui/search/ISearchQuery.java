package org.eclipse.update.internal.ui.search;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.update.core.IFeature;
import org.eclipse.update.core.ISite;
import org.eclipse.update.internal.ui.model.ISiteAdapter;

public interface ISearchQuery {
/**
 * Returns a URL of an explicit site that needs to 
 * be used for this query. This site is searched first.
 * In addition, local file system and bookmarked sites
 * can be searched as well if selected in search options.
 * @return a url of a site that needs to be searched for
 * this query or <samp>null</samp> if a general scan 
 * should be used.
 */
	public ISiteAdapter getSearchSite();
	
/**
 * Returns an array of features that match the search query
 * using the provided site as a source.
 */
	public IFeature [] getMatchingFeatures(ISite site, IProgressMonitor monitor);
}