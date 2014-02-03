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
package org.eclipse.update.search;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.update.core.IFeature;
import org.eclipse.update.core.IFeatureReference;
import org.eclipse.update.core.ISite;
import org.eclipse.update.core.ISiteWithMirrors;
import org.eclipse.update.core.IURLEntry;
import org.eclipse.update.core.SiteManager;
import org.eclipse.update.internal.core.ExtendedSite;
import org.eclipse.update.internal.core.Messages;
import org.eclipse.update.internal.operations.UpdateUtils;
import org.eclipse.update.internal.search.SiteSearchCategory;
import org.eclipse.update.internal.search.UpdatePolicy;
import org.eclipse.update.internal.search.UpdateSiteAdapter;
import org.eclipse.update.internal.search.UpdatesSearchCategory;

/**
 * This class is central to update search. The search pattern
 * is encapsulated in update search category, while the search
 * scope is defined in the scope object. When these two objects
 * are defined and set, search can be performed using the 
 * provided method. Search results are reported to the
 * result collector, while search progress is tracked using
 * the progress monitor.
 * <p>Classes that implement <samp>IUpdateSearchResultCollector</samp>
 * should call 'accept' to test if the match should be
 * accepted according to the filters added to the request.
 * 
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 * @see UpdateSearchScope
 * @see IUpdateSearchCategory
 * @since 3.0
 * @deprecated The org.eclipse.update component has been replaced by Equinox p2.
 * This API will be deleted in a future release. See bug 311590 for details.
 */
public class UpdateSearchRequest {
	private IUpdateSearchCategory category;
	private UpdateSearchScope scope;
	private boolean searchInProgress = false;
	private AggregateFilter aggregateFilter = new AggregateFilter();
	
	private static class UpdateSearchSite
	extends UpdateSiteAdapter
	implements IUpdateSearchSite {
	private String[] categoriesToSkip;

	public UpdateSearchSite(
		String label,
		URL siteURL,
		String[] categoriesToSkip) {
		super(label, siteURL);
		this.categoriesToSkip = categoriesToSkip;
	}
	public String[] getCategoriesToSkip() {
		return categoriesToSkip;
	}
}

	class MirroredUpdateSiteAdapter extends UpdateSiteAdapter {
		public MirroredUpdateSiteAdapter(IURLEntry mirror) {
			super(mirror.getAnnotation(), mirror.getURL());
		}
	}
	
	class AggregateFilter implements IUpdateSearchFilter {
		private ArrayList filters;
		public void addFilter(IUpdateSearchFilter filter) {
			if (filters == null)
				filters = new ArrayList();
			if (filters.contains(filter) == false)
				filters.add(filter);
		}
		
		public void removeFilter(IUpdateSearchFilter filter) {
			if (filters == null)
				return;
			filters.remove(filter);
		}
	
        /**
         * @deprecated In 3.1 only the accept (IFeatureReference) will be used
         */
		public boolean accept(IFeature match) {
			if (filters == null)
				return true;
			for (int i = 0; i < filters.size(); i++) {
				IUpdateSearchFilter filter = (IUpdateSearchFilter) filters.get(i);
				if (filter.accept(match) == false)
					return false;
			}
			return true;
		}
		
		public boolean accept(IFeatureReference match) {
			if (filters == null)
				return true;
			for (int i = 0; i < filters.size(); i++) {
				IUpdateSearchFilter filter = (IUpdateSearchFilter) filters.get(i);
				if (filter.accept(match) == false)
					return false;
			}
			return true;
		}
	}

	/**
	 * Returns an updates search category for use in discovering updates
	 * to existing function on update sites.
	 * 
	 * @return an updates search category
	 * @since 3.1
	 */
	public static IUpdateSearchCategory createDefaultUpdatesSearchCategory() {
		return new UpdatesSearchCategory();
	}
	
	/**
	 * Returns a site search category for use in discovering new function on update sites.
	 * 
	 * @return a site search category
	 * @since 3.1
	 */
	public static IUpdateSearchCategory createDefaultSiteSearchCategory() {
		return new SiteSearchCategory();
	}
	
	/**
	 * The constructor that accepts the search category and 
	 * scope objects.
	 * @param category the actual search pattern that should be applied
	 * @param scope a list of sites that need to be scanned during the search
	 */
	public UpdateSearchRequest(
		IUpdateSearchCategory category,
		UpdateSearchScope scope) {
		this.category = category;
		this.scope = scope;
	}
	/**
	 * Returns the search catagory used in this request.
	 * @return the search category
	 */
	
	public IUpdateSearchCategory getCategory() {
		return category;
	}
	
	/**
	 * Returns the scope of this search request.
	 * @return search scope
	 */
	
	public UpdateSearchScope getScope() {
		return scope;
	}
	/**
	 * Adds a filter to this request. This method does nothing
	 * if search is alrady in progress. 
	 * @param filter the filter 
	 * @see UpdateSearchRequest#removeFilter
	 */
	public void addFilter(IUpdateSearchFilter filter) {
		if (searchInProgress)
			return;
		aggregateFilter.addFilter(filter);
	}
	/**
	 * Removes the filter from this request. This method does
	 * nothing if search is alrady in progress.
	 * @param filter the filter to remove
	 * @see UpdateSearchRequest#addFilter
	 */

	public void removeFilter(IUpdateSearchFilter filter) {
		if (searchInProgress)
			return;
		aggregateFilter.removeFilter(filter);
	}

	/**
	 * Sets the scope object. It is possible to reuse the search request
	 * object by modifying the scope and re-running the search.
	 * @param scope the new search scope
	 */
	public void setScope(UpdateSearchScope scope) {
		this.scope = scope;
	}
	/**
	 * Tests whether this search request is current running.
	 * @return <samp>true</samp> if the search is currently running, <samp>false</samp> otherwise.
	 */
	public boolean isSearchInProgress() {
		return searchInProgress;
	}

	/**
	 * Runs the search using the category and scope configured into
	 * this request. As results arrive, they are passed to the
	 * search result collector object.
	 * @param collector matched features are passed to this object
	 * @param monitor used to track the search progress
	 * @throws CoreException
	 */
	public void performSearch(
		IUpdateSearchResultCollector collector,
		IProgressMonitor monitor)
		throws CoreException, OperationCanceledException {
		
		ArrayList statusList = new ArrayList();

		searchInProgress = true;
		IUpdateSearchQuery[] queries = category.getQueries();
		IUpdateSearchSite[] candidates = scope.getSearchSites();
		Set visitedSitesURL = new HashSet();
		Set visitedSites = new HashSet();
		for(int i = 0; i < candidates.length; i++) {
			visitedSitesURL.add(candidates[i].getURL());
			//visitedSites.add(candidates[i]);
		}
		URL updateMapURL = scope.getUpdateMapURL();
		boolean searchFeatureProvidedSites = scope.isFeatureProvidedSitesEnabled();

		if (!monitor.isCanceled()) {
			
			int nsearchsites = 0;
			try {
			for (int i = 0; i < queries.length; i++) {
				if (queries[i].getQuerySearchSite() != null)
					nsearchsites++;
			}
			} catch (Throwable t) {
				t.printStackTrace();
				
			}
			
			int ntasks = nsearchsites + queries.length * candidates.length;
			if (updateMapURL!=null) ntasks++;

			monitor.beginTask(Messages.UpdateSearchRequest_searching, ntasks); 
			
			try {
				UpdatePolicy updatePolicy=null;
				if (updateMapURL!=null) {
					updatePolicy = new UpdatePolicy();
					IStatus status =UpdateUtils.loadUpdatePolicy(updatePolicy, updateMapURL, new SubProgressMonitor(monitor, 1));
					if (status != null)
						statusList.add(status);
				}
				
				List combinedAssociateSites = new ArrayList();
				for (int i = 0; i < queries.length; i++) {
					IUpdateSearchQuery query = queries[i];
					IQueryUpdateSiteAdapter qsite = query.getQuerySearchSite();
					// currently, the next conditional is only executed (qsite!=null) when
					// running an update search. 
					if (qsite != null && searchFeatureProvidedSites) {
						// do not update features that are installed in read-only locations
						if (query instanceof UpdatesSearchCategory.UpdateQuery) {
							IFeature feature = ((UpdatesSearchCategory.UpdateQuery)query).getFeature();
							if (feature != null && !feature.getSite().getCurrentConfiguredSite().verifyUpdatableStatus().isOK())
								continue;
						}
						// check for mapping
						IUpdateSiteAdapter mappedSite = getMappedSite(updatePolicy, qsite);
						// when there is no mapped site the feature is not updatable
						if (mappedSite == null || mappedSite.getURL() == null)
							continue;
						SubProgressMonitor subMonitor =
							new SubProgressMonitor(monitor, 1);
						List associateSites = new ArrayList();
						IStatus status =
							searchOneSite(
								mappedSite,
								null,
								query,
								collector,
								associateSites,
								subMonitor,
								true);
						if (status != null)
							statusList.add(status);
						if (monitor.isCanceled())
							break;
						combinedAssociateSites = combineAssociateSites( combinedAssociateSites, associateSites, visitedSitesURL, visitedSites);
					}
					
					for (int j = 0; j < candidates.length; j++) {
						if (monitor.isCanceled()) {
							break;
						}
						IUpdateSearchSite source = candidates[j];
						SubProgressMonitor subMonitor =
							new SubProgressMonitor(monitor, 1);
						List associateSites = new ArrayList();
						IStatus status =
							searchOneSite(
								source,
								source.getCategoriesToSkip(),
								query,
								collector,
								associateSites,
								subMonitor,
								true);
						if (status != null)
							statusList.add(status);
						combinedAssociateSites = combineAssociateSites( combinedAssociateSites, associateSites, visitedSitesURL, visitedSites);
					}
					if (monitor.isCanceled())
						break;
					
					
					for(int associateSitesDepth = 0; associateSitesDepth < 5; associateSitesDepth++) {
						List tempCombinedSites = new ArrayList();
						Iterator combinedAssociateSitesIterator = combinedAssociateSites.iterator();
						while(combinedAssociateSitesIterator.hasNext()) {
							
							IUpdateSearchSite source = (IUpdateSearchSite)combinedAssociateSitesIterator.next();
							
							List associateSites = new ArrayList();
							SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 1);
							IStatus status =
								searchOneSite(
									source,
									source.getCategoriesToSkip(),
									query,
									collector,
									associateSites,
									subMonitor,
									true);
							combinedAssociateSites = combineAssociateSites( tempCombinedSites, associateSites, visitedSitesURL, visitedSites);
							if (status != null)
								statusList.add(status);
						}	
						combinedAssociateSites = tempCombinedSites;
						
					}
					if (monitor.isCanceled())
						break;
				}
			} catch (CoreException e) {
				searchInProgress = false;
				monitor.done();
				throw e;
			}
		}
		searchInProgress = false;
		monitor.done();
		

		Iterator visitedSitesIterator = visitedSites.iterator();
		while (visitedSitesIterator.hasNext()) {
			IUpdateSearchSite associateSite = (IUpdateSearchSite)visitedSitesIterator.next();
			scope.addSearchSite(associateSite.getLabel(), associateSite.getURL(), null);
		}
		if (statusList.size() > 0) {
			if (statusList.size()==1 && ((IStatus)statusList.get(0)).getSeverity()==IStatus.CANCEL)
				throw new OperationCanceledException();
			IStatus[] children =
				(IStatus[]) statusList.toArray(new IStatus[statusList.size()]);
			MultiStatus multiStatus =
				new MultiStatus(
					"org.eclipse.update.core", //$NON-NLS-1$
					ISite.SITE_ACCESS_EXCEPTION,
					children,
					Messages.Search_networkProblems, 
					null);
			
			throw new CoreException(multiStatus);
		}
	}


	private List combineAssociateSites(List combinedAssociateSites, List associateSites, Set visitedSitesURL, Set visitedSites) {
		Iterator iterator = associateSites.iterator();

		while(iterator.hasNext()) {
			UpdateSearchSite associateSite = (UpdateSearchSite)iterator.next();
			if ( !visitedSitesURL.contains(associateSite.getURL())) {
				combinedAssociateSites.add(associateSite);
				visitedSitesURL.add(associateSite.getURL());
				visitedSites.add(associateSite);
			}
			
		}
		return combinedAssociateSites;
	}


/*
 * See if this query site adapter is mapped in the map file
 * to a different URL.
 */
	private IUpdateSiteAdapter getMappedSite(UpdatePolicy policy, IQueryUpdateSiteAdapter qsite) {
		if (policy!=null && policy.isLoaded()) {
			IUpdateSiteAdapter mappedSite = policy.getMappedSite(qsite.getMappingId());
			if (mappedSite!=null) 
				return mappedSite;
			else // no match - use original site if fallback allowed, or nothing.
				return policy.isFallbackAllowed()? qsite : null;
		}
		return qsite;
	}

/*
 * Search one site using the provided query.
 */
	private IStatus searchOneSite(
		IUpdateSiteAdapter siteAdapter,
		String[] categoriesToSkip,
		IUpdateSearchQuery query,
		IUpdateSearchResultCollector collector,
		List associateSites,
		SubProgressMonitor monitor,
		boolean checkMirrors)
		throws CoreException {
		
		String text = NLS.bind(Messages.UpdateSearchRequest_contacting, siteAdapter.getLabel());
		monitor.subTask(text);
		monitor.beginTask("", 10); //$NON-NLS-1$
		URL siteURL = siteAdapter.getURL();

		ISite site;
		try {
			site =
				SiteManager.getSite(
					siteURL,
					new SubProgressMonitor(monitor, 1));
			
			// If frozen connection was canceled, there will be no site.
			if (site == null) {
				monitor.worked(9);
				return null;
			}
			
			
			// prompt the user to pick up a site (do not recursively go into mirror sites on the mirror site)
			if ((collector instanceof IUpdateSearchResultCollectorFromMirror) &&
				(site instanceof ISiteWithMirrors) &&
                !(siteAdapter instanceof MirroredUpdateSiteAdapter)) {
				
				IURLEntry mirror = null;
				try {
					mirror = ((IUpdateSearchResultCollectorFromMirror)collector).getMirror((ISiteWithMirrors)site, siteAdapter.getLabel());
					if (site instanceof ExtendedSite) {
						((ExtendedSite)site).setSelectedMirror(mirror);
					}
				}
				catch (OperationCanceledException e) {
					monitor.setCanceled(true);
					return Status.CANCEL_STATUS;
				}
				
				if (mirror != null) 
					return searchOneSite(new MirroredUpdateSiteAdapter(mirror), categoriesToSkip, query, collector, associateSites, new SubProgressMonitor(monitor,1), false);
			}
		} catch (CoreException e) {
			// Test the exception. If the exception is
			// due to the site connection problems,
			// allow the search to move on to 
			// the next site. Otherwise,
			// rethrow the exception, causing the search
			// to terminate.
			IStatus status = e.getStatus();
			if (status == null)
//				|| status.getCode() != ISite.SITE_ACCESS_EXCEPTION)
				throw e;
			monitor.worked(10);
			return status;
		}

		text = NLS.bind(Messages.UpdateSearchRequest_checking, siteAdapter.getLabel());
		monitor.getWrappedProgressMonitor().subTask(text);

		if (site instanceof ExtendedSite) {
			//System.out.println("ExtendedSite is here"); //$NON-NLS-1$
			IURLEntry[] associateSitesList = ((ExtendedSite)site).getAssociateSites(); 
			if (associateSitesList != null) {
				for(int i = 0; i < associateSitesList.length; i++) {
					associateSites.add(new UpdateSearchSite(associateSitesList[i].getAnnotation(), associateSitesList[i].getURL(), null));
				}
			}
		}
		query.run(
			site,
			categoriesToSkip,
			aggregateFilter,
			collector,
			new SubProgressMonitor(monitor, 9));
		return null;
	}
}
