package org.eclipse.update.internal.core;

/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
 
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.update.core.*;
 
 /**
  * A site content consumer manages the storage or archives, plugins and
  * feature inside an <code> ISite</code>
  */
 
public interface ISiteContentConsumer {

	/**
	 * Stores a content reference into the SiteContentConsumer
	 * @param ContentReference the content reference to store
	 * @param IProgressMonitor the progress monitor
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


