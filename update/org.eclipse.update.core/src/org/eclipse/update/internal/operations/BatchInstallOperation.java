/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.operations;

import java.lang.reflect.*;

import org.eclipse.core.runtime.*;
import org.eclipse.update.core.*;
import org.eclipse.update.core.model.*;
import org.eclipse.update.internal.core.*;
import org.eclipse.update.operations.*;

public class BatchInstallOperation
	extends Operation
	implements IBatchOperation {
		
	protected IInstallFeatureOperation[] operations;

	public BatchInstallOperation(IInstallFeatureOperation[] operations) {
		super();
		this.operations = operations;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.update.operations.IMultiOperation#getOperations()
	 */
	public IFeatureOperation[] getOperations() {
		return operations;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.update.operations.IOperation#execute(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public boolean execute(IProgressMonitor monitor, IOperationListener listener) throws CoreException, InvocationTargetException {
		int installCount = 0;

		if (operations == null || operations.length == 0)
			return false;
					
		IStatus status = OperationsManager.getValidator().validatePendingChanges(operations);
		if (status != null && status.getCode() == IStatus.ERROR) {
			throw new CoreException(status);
		}
		
//		// Check for duplication conflicts
//		ArrayList conflicts =
//			DuplicateConflictsValidator.computeDuplicateConflicts(
//				operations,
//				config);
//		if (conflicts != null) {
//			boolean continueProcessing = false;
//			if (listener != null)
//				continueProcessing = listener.beforeExecute(this, conflicts);
//			if (!continueProcessing)
//				return false;
//		}
		
		OperationsManager.setInProgress(true);
		if (monitor == null) 
			monitor = new NullProgressMonitor();
			
		try {			
			if (listener != null)
				listener.beforeExecute(this, null);
			
			monitor.beginTask(
				Messages.OperationsManager_installing,
				operations.length);
			for (int i = 0; i < operations.length; i++) {
				SubProgressMonitor subMonitor =
					new SubProgressMonitor(
						monitor,
						1,
						SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK);

				operations[i].execute(subMonitor, listener);
				OperationsManager.addPendingOperation(operations[i]);

				operations[i].markProcessed();
				if (listener != null)
					listener.afterExecute(operations[i], null);

				//monitor.worked(1);
				installCount++;
			}
			return SiteManager.getLocalSite().save();
        } catch (InstallAbortedException e) {
            // saves the current configuration
            if (installCount > 0) {
                try {
                    SiteManager.getLocalSite().save();
                } catch (CoreException ce) {
                    UpdateUtils.logException(ce);
                }
            }
            throw new InvocationTargetException(e);
        } catch (CoreException e) {
            // saves the current configuration
            if (installCount > 0) {
                try {
                    SiteManager.getLocalSite().save();
                } catch (CoreException ce) {
                    UpdateUtils.logException(ce);
                }
            }
            throw new InvocationTargetException(e);
        } finally {
			OperationsManager.setInProgress(false);
			monitor.done();
		}
	}
}
