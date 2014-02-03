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

 
import org.eclipse.core.runtime.*;
import org.eclipse.update.core.*;
 
 /**
  * A site content consumer manages the storage or archives, plugins and
  * feature inside an <code> ISite</code>
  */
 
public interface ISiteContentConsumer {

	/**
	 * Stores a content reference into the SiteContentConsumer
	 * @param contentReference the content reference to store
	 * @param monitor the progress monitor
	 * @throws CoreException if an error occurs storing the content reference
	 * @since 2.0 
	 */
	public void store(ContentReference contentReference, IProgressMonitor monitor) throws CoreException;

	/**
	 * opens a Non plugin Entry for storage
	 * @return the new FeatureContentConsumer for this <code>INonPluginEntry</code>
	 * @throws CoreException if the opens is done on a FeatureContentConsumer parent other than an IFeature.
	 * @since 2.0 
	 */
	public IContentConsumer open(INonPluginEntry nonPluginEntry) throws CoreException;

	/**
	 * opens a Non plugin Entry for storage
	 * @return the new FeatureContentConsumer for this <code>IPluginEntry</code>
	 * @throws CoreException if the opens is done on a FeatureContentConsumer parent other than an IFeature.
	 * @since 2.0 
	 */
	public IContentConsumer open(IPluginEntry pluginEntry) throws CoreException;
	
	/**
	 * closes the opened SiteContentConsumer
	 * @throws CoreException
	 * @since 2.0 
	 */
	public IFeatureReference close() throws CoreException ;	
	

	/**
	 * aborts the opened SiteContentConsumer
	 * @throws CoreException
	 * @since 2.0 
	 */
	public void abort() throws CoreException;		
	
}


