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
package org.eclipse.update.core;

import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.update.core.model.FeatureModel;
import org.eclipse.update.core.model.FeatureModelFactory;
import org.eclipse.update.core.model.ImportModel;
import org.eclipse.update.core.model.IncludedFeatureReferenceModel;
import org.eclipse.update.core.model.InstallHandlerEntryModel;
import org.eclipse.update.core.model.NonPluginEntryModel;
import org.eclipse.update.core.model.PluginEntryModel;
import org.eclipse.update.core.model.URLEntryModel;

/**
 * Base implementation of a feature factory.
 * The factory is responsible for constructing the correct
 * concrete implementation of the model objects for each particular
 * feature type. This class creates model objects that correspond
 * to the concrete implementation classes provided in this package.
 * The actual feature creation method is subclass responsibility.
 * <p>
 * This class must be subclassed by clients.
 * </p>
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 * @see org.eclipse.update.core.IFeatureFactory
 * @see org.eclipse.update.core.model.FeatureModelFactory
 * @since 2.0
 * @deprecated The org.eclipse.update component has been replaced by Equinox p2.
 * This API will be deleted in a future release. See bug 311590 for details.
 */
public abstract class BaseFeatureFactory extends FeatureModelFactory implements IFeatureFactory {

	/**
	 * 
	 * @deprecated implement createFeature(URL, ISite, IProgressMonitor) instead
	 * @see IFeatureFactory#createFeature(URL,ISite)
	 * @since 2.0
	 */
	public IFeature createFeature(URL url, ISite site) throws CoreException {
		return createFeature(url, site, null);
	}

	/**
	 * Create feature. Implementation of this method must be provided by 
	 * subclass
	 * 
	 * @see IFeatureFactory#createFeature(URL,ISite,IProgressMonitor)
	 * @since 2.0
	 */
	public abstract IFeature createFeature(URL url, ISite site, IProgressMonitor monitor) throws CoreException;

	/**
	 * Create a concrete implementation of feature model.
	 * 
	 * @see Feature
	 * @return feature model
	 * @since 2.0
	 */
	public FeatureModel createFeatureModel() {
		return new Feature();
	}

	/**
	 * Create a concrete implementation of included feature reference model.
	 * 
	 * @see IncludedFeatureReference
	 * @return feature model
	 * @since 2.1
	 */
	public IncludedFeatureReferenceModel createIncludedFeatureReferenceModel() {
		return new IncludedFeatureReference();
	}

	/**
	 * Create a concrete implementation of install handler model.
	 * 
	 * @see InstallHandlerEntry
	 * @return install handler entry model
	 * @since 2.0
	 */
	public InstallHandlerEntryModel createInstallHandlerEntryModel() {
		return new InstallHandlerEntry();
	}

	/**
	 * Create a concrete implementation of import dependency model.
	 * 
	 * @see Import
	 * @return import dependency model
	 * @since 2.0
	 */
	public ImportModel createImportModel() {
		return new Import();
	}

	/**
	 * Create a concrete implementation of plug-in entry model.
	 * 
	 * @see PluginEntry
	 * @return plug-in entry model
	 * @since 2.0
	 */
	public PluginEntryModel createPluginEntryModel() {
		return new PluginEntry();
	}

	/**
	 * Create a concrete implementation of non-plug-in entry model.
	 * 
	 * @see NonPluginEntry
	 * @return non-plug-in entry model
	 * @since 2.0
	 */
	public NonPluginEntryModel createNonPluginEntryModel() {
		return new NonPluginEntry();
	}

	/**
	 * Create a concrete implementation of annotated URL model.
	 * 
	 * @see URLEntry
	 * @return annotated URL model
	 * @since 2.0
	 */
	public URLEntryModel createURLEntryModel() {
		return new URLEntry();
	}
}
