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
import org.eclipse.update.operations.OperationsManager;

public class UnconfigureAndUninstallFeaturesOperation extends
		BatchFeatureOperation implements
		IUnconfigureAndUninstallFeaturesOperation {

	public UnconfigureAndUninstallFeaturesOperation(IConfiguredSite[] targetSites,
			IFeature[] features) {
		super(targetSites, features);
	}

	protected IOperation createOperation(IConfiguredSite targetSite, IFeature feature) {
		return ((OperationFactory)OperationsManager.getOperationFactory()).createUnconfigureAndUninstallFeatureOperation(targetSite, feature);
	}
}
