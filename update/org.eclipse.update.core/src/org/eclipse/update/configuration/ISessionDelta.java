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
package org.eclipse.update.configuration;

import java.util.Date;

import org.eclipse.core.runtime.*;
import org.eclipse.update.core.*;

/**
 * Installation Change.
 * Represents the changes the reconciler found.
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 * @since 2.0
 * @deprecated Do not use this interface
 * This API will be deleted in a future release. See bug 311590 for details.
 */
public interface ISessionDelta extends IAdaptable {

	/**
	 * Indicates a processing type to enable the features
	 * 
	 * @since 2.0
	 * <p>
	 * <b>Note:</b> This field is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public int ENABLE = 1;

	/**
	 * Indicates a processing type to disable the features
	 * 
	 * @since 2.0
	 * <p>
	 * <b>Note:</b> This field is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
 	public int DISABLE = 2;

	/**
	 * Returns the list of Features found during reconciliation
	 * 
	 * @return an array of feature references, or an empty array
	 * @since 2.0 
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public IFeatureReference[] getFeatureReferences();

	/**
	 * Returns the date the reconciliation occured
	 * 
	 * @return the date of the reconciliation
	 * @since 2.0 
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public Date getDate();

	/**
	 * Returns the type of the processing type
	 * that will affect all the associated features. 
	 * 
	 * @return the processing type
	 * @see ISessionDelta#ENABLE
	 * @see ISessionDelta#DISABLE
	 * @since 2.0
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public int getType();

	/**
	 * Process all the feature references of the 
	 * Session Delta. 
	 * Removes the Session Delta from the file system after processing it.
	 * 
	 * @param progressMonitor the progress monitor
	 * @throws CoreException if an error occurs. 
	 * @since 2.0 
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public void process(IProgressMonitor progressMonitor) throws CoreException;

	/**
	 * Process the selected feature references of the Session Delta.
	 * Removes the Session Delta from the file system after processing it.
	 *
	 * @param selected list of selected feature references to be processed
	 * @param monitor the progress monitor
	 * @throws CoreException if an error occurs.
	 * @since 2.0
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public void process(IFeatureReference [] selected, IProgressMonitor monitor) throws CoreException;
	
	/**
	 * Removes the Session Delta from the file system without processing it.
	 * 
	 * @since 2.0 
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public void delete();	
}
