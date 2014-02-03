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
package org.eclipse.update.internal.operations;

import org.eclipse.core.runtime.*;
import org.eclipse.update.core.*;
import org.eclipse.update.operations.*;

/**
 * Swaps a feature.
 * ReplaceFeatureVersionOperation
 */
public class ReplaceFeatureVersionOperation
	extends FeatureOperation
	implements IConfigFeatureOperation {

	private IFeature anotherFeature;
	
	public ReplaceFeatureVersionOperation(
		IFeature feature,
		IFeature anotherFeature) {
		super(feature.getSite().getCurrentConfiguredSite(), feature);
		this.anotherFeature = anotherFeature;
	}

	public boolean execute(IProgressMonitor pm, IOperationListener listener)
		throws CoreException {

		IStatus status =
			OperationsManager.getValidator().validatePendingReplaceVersion(feature, anotherFeature);
		if (status != null) {
			throw new CoreException(status);
		}

		// unconfigure current feature first, then configure the other one
		
		PatchCleaner cleaner = new PatchCleaner(targetSite, feature);
		targetSite.unconfigure(feature);
		cleaner.dispose();
		
		targetSite.configure(anotherFeature);
//		ensureUnique();

		try {
			// Restart not needed
			boolean restartNeeded = false;

			// Check if this operation is cancelling one that's already pending
			IOperation pendingOperation =
				OperationsManager.findPendingOperation(feature);

			if (pendingOperation instanceof IConfigFeatureOperation) {
				// no need to do either pending change
				OperationsManager.removePendingOperation(pendingOperation);
			} else {
				OperationsManager.addPendingOperation(this);
				restartNeeded = true;
			}
			
			pendingOperation =
				OperationsManager.findPendingOperation(anotherFeature);
				
			if (pendingOperation instanceof IUnconfigFeatureOperation) {
				// no need to do either pending change
				OperationsManager.removePendingOperation(pendingOperation);
			} else {
				OperationsManager.addPendingOperation(this);
				restartNeeded = true;
			}
			
			markProcessed();
			if (listener != null)
				listener.afterExecute(this, null);

			restartNeeded = SiteManager.getLocalSite().save() && restartNeeded;

			// notify the model
			OperationsManager.fireObjectChanged(feature, null);

			return restartNeeded;
		} catch (CoreException e) {
			undo();
			UpdateUtils.logException(e);
			throw e;
		}
	}

	public void undo() throws CoreException {
		targetSite.unconfigure(anotherFeature);
		targetSite.configure(feature);
	}
}
