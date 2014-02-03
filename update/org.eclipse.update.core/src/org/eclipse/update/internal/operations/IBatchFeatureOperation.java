/*******************************************************************************
 * Copyright (c) 2005, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.operations;

import org.eclipse.update.configuration.IConfiguredSite;
import org.eclipse.update.core.IFeature;
import org.eclipse.update.operations.IOperation;

public interface IBatchFeatureOperation extends IOperation {

	/**
	 * Returns the features to operate on.
	 * @return the features to operate on.
	 */
	public abstract IFeature[] getFeatures();
	/**
	 * Returns the site in which the operation is applied.
	 * @return the site that owns or will own the feature.
	 */
	public abstract IConfiguredSite[] getTargetSites();

	/**
	 * Sets the site in which the feature is being operated on.
	 * @param targetSite the site in which the featre is being operated on.
	 */
	public abstract void setTargetSites(IConfiguredSite[] targetSite);
}
