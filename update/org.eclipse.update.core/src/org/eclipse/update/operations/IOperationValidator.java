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
package org.eclipse.update.operations;

import org.eclipse.core.runtime.*;
import org.eclipse.update.configuration.*;
import org.eclipse.update.core.*;

/**
 * This class contains various validation methods to be invoked before or during executing update manager
 * operations.
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
public interface IOperationValidator {

	/**
	 * Called before performing install.
	 * @return the error status, or null if no errors
	 */
	public IStatus validatePendingInstall(IFeature oldFeature, IFeature newFeature);

	/**
	 * Called before performing operation.
	 * @return the error status, or null if no errors
	 */
	public IStatus validatePendingConfig(IFeature feature);
	
	/**
	 * Called before performing operation.
	 * @return the error status, or null if no errors
	 */
	public IStatus validatePendingUnconfig(IFeature feature);
	
	/**
	 * Called before performing operation.
	 * @return the error status, or null if no errors
	 */
	public IStatus validatePendingReplaceVersion(IFeature feature, IFeature anotherFeature);
	
	/**
	 * Called before doing a revert/ restore operation
	 * @return the error status, or null if no errors
	 */
	public IStatus validatePendingRevert(IInstallConfiguration config);

	/**
	 * Called by the UI before doing a batched processing of
	 * several pending changes.
	 * @return the error status, or null if no errors
	 */
	public IStatus validatePendingChanges(IInstallFeatureOperation[] jobs);

	/**
	 * Check the current state.
	 * @return the error status, or null if no errors
	 */
	public IStatus validateCurrentState();
	
	/**
	 * Checks if the platform configuration has been modified outside this program.
	 * @return the error status, or null if no errors
	 */
	public IStatus validatePlatformConfigValid();
}
