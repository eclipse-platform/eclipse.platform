/*******************************************************************************
 *  Copyright (c) 2000, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     James D Miles (IBM Corp.) - bug 181375, ArrayIndexOutOfBoundsException in SiteSearchCategory$Query
 *     James D Miles (IBM Corp.) - bug 191783, NullPointerException in FeatureDownloader
 *******************************************************************************/
package org.eclipse.update.internal.search;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.update.core.ICategory;
import org.eclipse.update.core.IFeature;
import org.eclipse.update.core.ISite;
import org.eclipse.update.core.ISiteFeatureReference;
import org.eclipse.update.internal.core.ExtendedSite;
import org.eclipse.update.internal.core.LiteFeature;
import org.eclipse.update.internal.core.UpdateCore;
import org.eclipse.update.search.IQueryUpdateSiteAdapter;
import org.eclipse.update.search.IUpdateSearchFilter;
import org.eclipse.update.search.IUpdateSearchQuery;
import org.eclipse.update.search.IUpdateSearchResultCollector;

/**
 * Searches an update site
 */
public class SiteSearchCategory extends BaseSearchCategory {
	private IUpdateSearchQuery[] queries;
	private boolean liteFeaturesAreOK;
	private static final String CATEGORY_ID =
		"org.eclipse.update.core.unified-search"; //$NON-NLS-1$

	private static class Query implements IUpdateSearchQuery {
		
		private boolean liteFeaturesAreOK;
		
		
		public Query() {
			liteFeaturesAreOK = false;
		}
		
		public Query(boolean liteFeaturesAreOK) {
			this.liteFeaturesAreOK = liteFeaturesAreOK;
		}
		
		public void setLiteFeaturesAreOK(boolean liteFeaturesAreOK) {
			this.liteFeaturesAreOK = liteFeaturesAreOK;
		}
		
		public void run(
			ISite site,
			String[] categoriesToSkip,
			IUpdateSearchFilter filter,
			IUpdateSearchResultCollector collector,
			IProgressMonitor monitor) {
			
			ISiteFeatureReference[] refs = site.getFeatureReferences();
			HashSet ignores = new HashSet();			
			Map liteFeatures = new HashMap();
			
			if (categoriesToSkip != null) {
				for (int i = 0; i < categoriesToSkip.length; i++) {
					ignores.add(categoriesToSkip[i]);
				}
			}
			List siteFeatureReferences = new ArrayList(Arrays.asList(refs));
			
			if (liteFeaturesAreOK && (site instanceof ExtendedSite) ) {
				
				ExtendedSite extendedSite = (ExtendedSite)site;
				LiteFeature[] liteFeaturesArray =  extendedSite.getLiteFeatures();
				if ( (liteFeaturesArray != null) && ( liteFeaturesArray.length != 0)) {
					for(int i = 0; i < liteFeaturesArray.length; i++) {
						liteFeatures.put(liteFeaturesArray[i].getVersionedIdentifier(), liteFeaturesArray[i]);					
					}
					(new FeatureDownloader(siteFeatureReferences, collector, filter, ignores, monitor, true, liteFeatures)).run();
					return;
				} else {
					liteFeaturesAreOK = false;
				}
			}
			

			
			monitor.beginTask("", refs.length); //$NON-NLS-1$
			ThreadGroup featureDownloaders = new ThreadGroup("FeatureDownloader"); //$NON-NLS-1$
			int numberOfThreads = (refs.length > 5)? 5: refs.length;

			Thread[] featureDownloader = new Thread[numberOfThreads];
			for( int i = 0; i < numberOfThreads; i++) {
				featureDownloader[i] = new Thread(featureDownloaders, new FeatureDownloader(siteFeatureReferences, collector, filter, ignores, monitor));
				featureDownloader[i].start();
			}
			
			int i =0;
			while(i < numberOfThreads){
				if (monitor.isCanceled()) {
					synchronized(siteFeatureReferences) { 
						siteFeatureReferences.clear();
					}
				}
				try	{
					featureDownloader[i].join(250);
					if(!featureDownloader[i].isAlive()){
						i++;
					}
				} catch (InterruptedException ie) {
				}
			}
		}

		/* (non-Javadoc)
		 * @see org.eclipse.update.internal.ui.search.ISearchQuery#getSearchSite()
		 */
		public IQueryUpdateSiteAdapter getQuerySearchSite() {
			return null;
		}
	}

	public SiteSearchCategory() {
		super(CATEGORY_ID);
		queries = new IUpdateSearchQuery[] { new Query()};
	}

	public SiteSearchCategory(boolean liteFeaturesAreOK) {
		this();
		this.liteFeaturesAreOK = liteFeaturesAreOK;
		queries = new IUpdateSearchQuery[] { new Query(liteFeaturesAreOK)};
	}

	public IUpdateSearchQuery[] getQueries() {
		return queries;
	}
	
	
	private static class FeatureDownloader implements Runnable {
		
		private List siteFeatureReferences;
		
		private IProgressMonitor monitor;
		
		private IUpdateSearchFilter filter;
		
		private IUpdateSearchResultCollector collector;
		
		private HashSet ignores;

		private boolean liteFeaturesAreOK;

		private Map liteFeatures;

		public FeatureDownloader(List siteFeatureReferences, IUpdateSearchResultCollector collector, IUpdateSearchFilter filter, HashSet ignores, IProgressMonitor monitor) {
			super();

			this.collector = collector;
			this.filter = filter;
			this.ignores = ignores;
			this.monitor = monitor;
			this.siteFeatureReferences = siteFeatureReferences;
		}
		
		public FeatureDownloader(List siteFeatureReferences, IUpdateSearchResultCollector collector, IUpdateSearchFilter filter, HashSet ignores, IProgressMonitor monitor, boolean liteFeaturesAreOK, Map liteFeatures) {
			this(siteFeatureReferences, collector, filter, ignores, monitor);
			this.liteFeaturesAreOK = liteFeaturesAreOK && (liteFeatures != null);
			this.liteFeatures = liteFeatures;
		}

		public void run() {
			
			ISiteFeatureReference siteFeatureReference = null;
			
			while (siteFeatureReferences.size() != 0) {
				
				synchronized(siteFeatureReferences) { 
					try{
						siteFeatureReference = (ISiteFeatureReference)siteFeatureReferences.remove(0);
					}catch(IndexOutOfBoundsException e){
						siteFeatureReference = null;
						break;
					}
				}
				if (siteFeatureReference != null) {
					boolean skipFeature = false;
					if (monitor.isCanceled())
						break;
					if (ignores.size() > 0) {
						ICategory[] categories = siteFeatureReference.getCategories();
						
						for (int j = 0; j < categories.length; j++) {
							ICategory category = categories[j];
							if (ignores.contains(category.getName())) {
								skipFeature = true;
								break;
							}
						}
					}
					try {
						if (!skipFeature) {
							if (filter.accept(siteFeatureReference)) {
								IFeature feature = null;
								if(liteFeaturesAreOK) {
									feature = (IFeature)liteFeatures.get(siteFeatureReference.getVersionedIdentifier());
								}
								if (feature == null){
									feature = siteFeatureReference.getFeature(null);
								}
								synchronized(siteFeatureReferences) {
									if ( (feature != null) && (filter.accept(siteFeatureReference)) ) {								
										collector.accept(feature);							    
										monitor.subTask(feature.getLabel());
									}
								}
							}
						}
					} catch (CoreException e) {
						UpdateCore.log(e);
					} finally {
						monitor.worked(1);
					}
				}
			}
			
		}
	}

	public boolean isLiteFeaturesAreOK() {
		return liteFeaturesAreOK;
	}

	public void setLiteFeaturesAreOK(boolean liteFeaturesAreOK) {
		this.liteFeaturesAreOK = liteFeaturesAreOK;
		for( int i = 0; i < queries.length; i++) {
			((Query)queries[i]).setLiteFeaturesAreOK(liteFeaturesAreOK);
		}
	}
}
