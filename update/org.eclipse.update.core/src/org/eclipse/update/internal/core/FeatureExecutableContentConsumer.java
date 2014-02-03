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

 
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.update.core.ContentReference;
import org.eclipse.update.core.IContentConsumer;
import org.eclipse.update.core.IFeature;
import org.eclipse.update.core.IFeatureContentConsumer;
import org.eclipse.update.core.IFeatureReference;
import org.eclipse.update.core.INonPluginEntry;
import org.eclipse.update.core.IPluginEntry;

/**
 * ContentConsumer Implementation for a FeatureExecutable
 */

public class FeatureExecutableContentConsumer extends FeatureContentConsumer {

	private IFeature feature;
	private boolean closed= false;
	private boolean aborted= false;	
	private ISiteContentConsumer contentConsumer;
	private IFeatureContentConsumer parent = null;
	private List /* of IFeatureContentCOnsumer */ children;

	/*
	 * @see IContentConsumer#open(INonPluginEntry)
	 */
	public IContentConsumer open(INonPluginEntry nonPluginEntry)
		throws CoreException {
		ContentConsumer cons = new NonPluginEntryContentConsumer(
			getContentConsumer().open(nonPluginEntry));
		return cons;
	}

	/*
	 * @see IContentConsumer#open(IPluginEntry)
	 */
	public IContentConsumer open(IPluginEntry pluginEntry) throws CoreException {
		ContentConsumer cons = new PluginEntryContentConsumer(getContentConsumer().open(pluginEntry));
		return cons;		
	}

	/*
	 * @see IContentConsumer#addChild(IFeature)
	 */
	public void addChild(IFeature child) throws CoreException {
		IFeatureContentConsumer childConsumer = child.getFeatureContentConsumer();
		childConsumer.setParent(this);
		if (children==null) children = new ArrayList();
		children.add(childConsumer);
		return;
	}


	/*
	 * @see IFeatureContentConsumer#store(ContentReference, IProgressMonitor)
	 */
	public void store(ContentReference contentReference, IProgressMonitor monitor)
		throws CoreException {
		getContentConsumer().store(contentReference, monitor);
	}

	/*
	 * @see IFeatureContentConsumer#close()
	 */
	public IFeatureReference close() throws CoreException {
		
		if (!closed && getParent()!=null){
			closed=true;
			return null;
		}
		
		// parent consumer, log we are about to rename
		// log files have been downloaded
		if (getParent()==null){
			ErrorRecoveryLog.getLog().append(ErrorRecoveryLog.ALL_INSTALLED);
		}
		
		IFeatureReference ref= null;
		if (contentConsumer != null)
			ref = contentConsumer.close();
		
		// close nested feature
		IFeatureContentConsumer[] children = getChildren();
		for (int i = 0; i < children.length; i++) {
			children[i].close();
		}
							
		return ref;
	}

	/*
	 * @see IFeatureContentConsumer#setFeature(IFeature)
	 */
	public void setFeature(IFeature feature) {
		this.feature= feature;
	}

	/*
	 * Sets the parent 
	 */
	public void setParent(IFeatureContentConsumer featureContentConsumer) {
		this.parent= featureContentConsumer;
	}

	/*
	 * returns the Content Consumer for the feature
	 * 
	 * Right now we are the only one to implement SiteContentConsumer
	 * Need to be exposed as API post v2.0
	 */
	private ISiteContentConsumer getContentConsumer() throws CoreException {
		if (contentConsumer == null)
			if (feature.getSite() instanceof SiteFile) {
				SiteFile site= (SiteFile) feature.getSite();
				contentConsumer= site.createSiteContentConsumer(feature);
			} else {
				throw new UnsupportedOperationException();
			}
		return contentConsumer;
	}

	/*
	 * @see IFeatureContentConsumer#abort()
	 */
	public void abort() throws CoreException {

		if (aborted) return;
		
		IFeatureContentConsumer[] children = getChildren();
		for (int i = 0; i < children.length; i++) {
			try {
			children[i].abort();
			} catch (Exception e){
				//do Nothing
			}
		}

		// do not close plugin and non plugin content consumer
		// the contentConsumer will abort them
		// we do not need to abort the NonPluginEntryContentConsumer and PluginEntryContentConsume

		//implement the cleanup
		if (contentConsumer!=null)
			contentConsumer.abort();
		
		aborted = true;
	}

	/*
	 * @see IFeatureContentConsumer#getFeature()
	 */
	public IFeature getFeature(){
		return feature;
	}

	/*
	 * @see IFeatureContentConsumer#getParent()
	 */
	public IFeatureContentConsumer getParent(){
		return parent;
	}

	/*
	 * @see IFeatureContentConsumer#getChildren()
	 */
	public IFeatureContentConsumer[] getChildren(){
		if (children==null || children.size() == 0)
			return new IFeatureContentConsumer[0];

		return (IFeatureContentConsumer[]) children.toArray(arrayTypeFor(children));
	}
}
