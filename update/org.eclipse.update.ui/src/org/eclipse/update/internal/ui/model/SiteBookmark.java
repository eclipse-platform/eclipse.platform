package org.eclipse.update.internal.ui.model;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.net.URL;
import org.eclipse.update.core.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.views.properties.*;
import org.eclipse.ui.model.*;
import java.util.*;
import org.eclipse.update.internal.ui.*;

public class SiteBookmark extends NamedModelObject 
							implements ISiteAdapter {
	public static final int USER = 0;
	public static final int LOCAL = 1;
	public static final int LOCAL_BOOKMARK = 2;
	private URL url;
	private ISite site;
	private Vector catalog;
	private SiteCategory otherCategory;
	private int type;

	public static final String P_URL="p_url";
	
	public SiteBookmark() {
	}
	
	public SiteBookmark(String name, URL url) {
		super(name);
		this.url = url;
	}
	
	public void setType(int type) {
		this.type = type;
	}
	
	public int getType() {
		return type;
	}
	
	public URL getURL() {
		return url;
	}


	public void setURL(URL url) {
		this.url = url;
		site = null;
		notifyObjectChanged(P_URL);
	}
	
	public ISite getSite() {
		return getSite(true);
	}
	
	public ISite getSite(boolean showDialogIfFailed) {
		if (site==null) {
			try {
				connect();
			}
			catch (CoreException e) {
				UpdateUIPlugin.logException(e, showDialogIfFailed);
			}
		}
		return site;
	}
	
	public boolean isSiteConnected() {
		return site!=null;
	}
	
	public void connect() throws CoreException {
		connect(true);
	}
	
	public void connect(boolean useCache) throws CoreException {
		site = SiteManager.getSite(url, useCache);
		createCatalog();
	}
	
	private void createCatalog() {
		catalog = new Vector();
		otherCategory = new SiteCategory(null, null);
		// Add all the categories
		ICategory [] categories;
		categories = site.getCategories();

		for (int i=0; i<categories.length; i++) {
			ICategory category = categories[i];
			addCategoryToCatalog(category);
		}
		// Add features to categories
		IFeatureReference [] featureRefs;
		featureRefs = site.getFeatureReferences();

		for (int i=0; i<featureRefs.length; i++) {
			IFeatureReference featureRef = featureRefs[i];
			addFeatureToCatalog(featureRef);
		}
		if (otherCategory.getChildCount()>0)
		   catalog.add(otherCategory);
	}

	public Object [] getCatalog(boolean withCategories) {
		if (withCategories)
			return catalog.toArray();
		else {
			// Make a flat catalog
			Vector flatCatalog = new Vector();
			for (int i=0; i<catalog.size(); i++) {
				SiteCategory category = (SiteCategory)catalog.get(i);
				category.addFeaturesTo(flatCatalog);
			}
			return flatCatalog.toArray();
		}
	}
	
	private void addCategoryToCatalog(ICategory category) {
		String name = category.getName();
		int loc = name.indexOf('/');
		if (loc == -1) {
			// first level
			catalog.add(new SiteCategory(name, category));
		}
		else {
			IPath path = new Path(name);
			name = path.lastSegment().toString();
			path = path.removeLastSegments(1);
			SiteCategory parentCategory = findCategory(path, catalog.toArray());
			if (parentCategory!=null) {
				parentCategory.add(new SiteCategory(name, category));
			}
		}
	}
	private void addFeatureToCatalog(IFeatureReference feature) {
		ICategory [] categories = feature.getCategories();
		boolean orphan = true;

		for (int i=0; i<categories.length; i++) {
			ICategory category = categories[i];
			String name = category.getName();
			IPath path = new Path(name);
			SiteCategory parentCategory = findCategory(path, catalog.toArray());
			if (parentCategory!=null) {
		   		parentCategory.add(new FeatureReferenceAdapter(feature));
		   		orphan = false;
			}
		}
		if (orphan)
			otherCategory.add(new FeatureReferenceAdapter(feature));
	}
	
	private SiteCategory findCategory(IPath path, Object [] children) {
		for (int i=0; i<children.length; i++) {
			Object child = children[i];
			if (child instanceof SiteCategory) {
				SiteCategory sc = (SiteCategory)child;
				if (sc.getName().equals(path.segment(0))) {
				   if (path.segmentCount()==1) return sc;
					else {
						path = path.removeFirstSegments(1);
						return findCategory(path, sc.getChildren());
					}
				}
			}
		}
		return null;
	}
	/**
	 * @see ISiteAdapter#getLabel()
	 */
	public String getLabel() {
		return getName();
	}


}