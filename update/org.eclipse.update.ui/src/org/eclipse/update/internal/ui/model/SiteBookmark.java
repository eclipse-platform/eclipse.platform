/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.ui.model;

import java.net.*;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.ui.*;

public class SiteBookmark extends NamedModelObject 
							implements ISiteAdapter {

    private static final long serialVersionUID = 1L;
    public static final String P_URL="p_url"; //$NON-NLS-1$
	public static final String P_TYPE="p_type"; //$NON-NLS-1$
	
	private URL url;
	transient private ISite site;
	transient private Vector catalog = new Vector();
	transient private SiteCategory otherCategory;
	private boolean webBookmark;
	private boolean selected;
	private String [] ignoredCategories = new String[0];
	private boolean readOnly = false;
	private boolean local = false;
	private boolean unavailable = false;
	private String description;

	public SiteBookmark() {
	}
	
	public SiteBookmark(String name, URL url, boolean webBookmark) {
		this(name, url, webBookmark, false);
	}
	
	public SiteBookmark(String name, URL url, boolean webBookmark, boolean selected) {
		super(name);
		this.url = url;
		this.webBookmark = webBookmark;
		this.selected = selected;
	}

	public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof SiteBookmark))
            return false;
        SiteBookmark b = (SiteBookmark)obj;
        if (url == null)
            return false;
        return url.equals(b.url);
    }

    public int hashCode() {
        if (url == null)
            return super.hashCode();
        else
            return url.hashCode();
    }

    public void setSelected(boolean selected) {
		this.selected = selected;
	}
	
	public boolean isSelected() {
		return selected;
	}
	
	public String [] getIgnoredCategories() {
		return ignoredCategories;
	}
	
	public void setIgnoredCategories(String [] categories) {
		this.ignoredCategories = categories;
	}
	
	public void setWebBookmark(boolean value) {
		if (isLocal()) return;
		this.webBookmark = value;
		notifyObjectChanged(P_TYPE);
	}
	
	public boolean isWebBookmark() {
		return webBookmark;
	}
	
	public URL getURL() {
		return url;
	}


	public void setURL(URL url) {
		this.url = url;
		site = null;
		notifyObjectChanged(P_URL);
	}
	
	public ISite getSite(IProgressMonitor monitor) {
		return getSite(true, monitor);
	}
	
	public ISite getSite(boolean showDialogIfFailed, IProgressMonitor monitor) {
		if (site==null) {
			try {
				connect(monitor);
			}
			catch (CoreException e) {
				UpdateUI.logException(e, showDialogIfFailed);
			}
		}
		return site;
	}
	
	public boolean isSiteConnected() {
		return site!=null;
	}
	
	public void connect(IProgressMonitor monitor) throws CoreException {
		connect(true, monitor);
	}
	
	public void connect(boolean useCache, IProgressMonitor monitor) throws CoreException {
		try {
			if (monitor==null) monitor = new NullProgressMonitor();
			monitor.beginTask("", 2); //$NON-NLS-1$
			monitor.subTask(NLS.bind("SiteBookmark.connecting", url.toString())); //$NON-NLS-1$
			site = SiteManager.getSite(url, useCache, new SubProgressMonitor(monitor, 1));
			if (site!=null) {
				createCatalog(new SubProgressMonitor(monitor, 1));
				unavailable = false;
			} else {
				catalog = new Vector();
				unavailable = true;
			}
		} catch (CoreException e) {
			unavailable = true;
			throw e;
		}
	}
	
	public boolean isUnavailable() {
		return unavailable;
	}
	
	public void setUnavailable(boolean value) {
		unavailable = value;
	}
	
	private void createCatalog(IProgressMonitor monitor) {
		catalog = new Vector();
		otherCategory = new SiteCategory(this, null, null);
		// Add all the categories
		ICategory [] categories = site.getCategories();
		
		ISiteFeatureReference [] featureRefs;
		featureRefs = site.getRawFeatureReferences();
		
		monitor.beginTask("", featureRefs.length + categories.length); //$NON-NLS-1$

		for (int i=0; i<categories.length; i++) {
			ICategory category = categories[i];
			addCategoryToCatalog(category);
			monitor.worked(1);
		}
		// Add features to categories

		for (int i=0; i<featureRefs.length; i++) {
			ISiteFeatureReference featureRef = featureRefs[i];
			addFeatureToCatalog(featureRef);
			monitor.worked(1);
		}
		if (otherCategory.getChildCount()>0)
		   catalog.add(otherCategory);
		
		// set the site description
		IURLEntry descURL = site.getDescription();
		if (descURL != null)
			description = descURL.getAnnotation();
	}

	public Object [] getCatalog(boolean withCategories, IProgressMonitor monitor) {
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
			catalog.add(new SiteCategory(this, name, category));
		}
		else {
			IPath path = new Path(name);
			name = path.lastSegment().toString();
			path = path.removeLastSegments(1);
			SiteCategory parentCategory = findCategory(path, catalog.toArray());
			if (parentCategory!=null) {
				parentCategory.add(new SiteCategory(this, name, category));
			}
		}
	}
	private void addFeatureToCatalog(ISiteFeatureReference feature) {
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
	
	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}
	
	public boolean isReadOnly() {
		return readOnly;
	}
	
	public void setLocal(boolean local) {
		this.local = local;
	}
	
	public boolean isLocal() {
		return local;
	}

	/**
	 * @param description The description to set.
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @return Returns the description.
	 */
	public String getDescription() {
		if (description == null && isSiteConnected()) {
			IURLEntry descURL = site.getDescription();
			if (descURL != null)
				description = descURL.getAnnotation();
		}
		return description;
	}
}
