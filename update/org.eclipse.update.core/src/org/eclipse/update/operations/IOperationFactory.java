/*******************************************************************************
 *  Copyright (c) 2000, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.update.operations;

import org.eclipse.update.configuration.*;
import org.eclipse.update.core.*;

/**
 * A factory interface for creating operations.
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 * @since 3.0
 * @deprecated The org.eclipse.update component has been replaced by Equinox p2.
 * This API will be deleted in a future release. See bug 311590 for details.
 */
public interface IOperationFactory {
	/**
	 * Creates an operation for configuring an installed feature in the specified site.
	 * @param targetSite site containing the feature to configure
	 * @param feature feature to be configured
	 * @return the configure operation
	 */
	public IConfigFeatureOperation createConfigOperation(
		IConfiguredSite targetSite,
		IFeature feature);
		
	/**
	 * Creates an operation for unconfiguring a feature
	 * @param targetSite site containing the feature to unconfigure
	 * @param feature feature to be unconfigured
	 * @return the unconfigure operation
	 */
	public IUnconfigFeatureOperation createUnconfigOperation(
		IConfiguredSite targetSite,
		IFeature feature);
	
	/**
	 * Creates an operation for installing a feature.
	 * @param targetSite site in which the feature is to be installed
	 * @param feature feature to be installed
	 * @param optionalFeatures optionally included features to be installed (if any)
	 * @param unconfiguredOptionalFeatures when installing optional features, some can be left unconfigured
	 * @param verifier operation verification listener
	 * @return the install operation
	 */
	public IInstallFeatureOperation createInstallOperation(
		IConfiguredSite targetSite,
		IFeature feature,
		IFeatureReference[] optionalFeatures,
		IFeature[] unconfiguredOptionalFeatures,
		IVerificationListener verifier);
		
	/**
	 * Creates an operation to uninstall a feature
	 * @param targetSite site containing the feature to uninstall
	 * @param feature feature to be uninstalled
	 * @return the uninstall operation
	 */
	public IUninstallFeatureOperation createUninstallOperation(
		IConfiguredSite targetSite,
		IFeature feature);
		
	/**
	 * Creates an operation for replacing this feature by a previous version
	 * @param feature current feature
	 * @param anotherFeature the new feature to be swapped in
	 * @return the revert feature version operation
	 */
	public IConfigFeatureOperation createReplaceFeatureVersionOperation(
		IFeature feature,
		IFeature anotherFeature);
		
	/**
	 * Creates an operation for executing a set of feature operation in batch mode
	 * @param operations operation to execute in batch mode	
	 * @return the batch operation
	 */
	public IBatchOperation createBatchInstallOperation(IInstallFeatureOperation[] operations);
	
	/**
	 * Creates ann operation to configure/unconfigure an installation site (also known as enable/disable site)
	 * @param site site to configure/unconfigure
	 * @return the toggle site operation
	 */
	public IToggleSiteOperation createToggleSiteOperation(
		IConfiguredSite site);
		

	/**
	 * Creates an operation to revert to a previous installation configuration.
	 * @param config configuration to revert to
	 * @param problemHandler error handler
	 * @return the revert operation
	 */
	public IRevertConfigurationOperation createRevertConfigurationOperation(
		IInstallConfiguration config,
		IProblemHandler problemHandler);
}
