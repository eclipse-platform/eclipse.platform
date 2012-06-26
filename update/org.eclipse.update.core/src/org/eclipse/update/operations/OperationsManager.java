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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;

import org.eclipse.update.core.*;
import org.eclipse.update.internal.configurator.*;
import org.eclipse.update.internal.operations.*;

/**
 * Entry point for update manager operations. Use this class to obtain the factory that creates
 * update manager operations, or to get the operation validator.
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
public class OperationsManager {
	private static IOperationValidator validator;
	private static IOperationFactory operationFactory;
	private static Vector listeners = new Vector();
	private static Vector pendingOperations = new Vector();
	
	private static boolean inProgress;

	private OperationsManager() {
	}
	
	/**
	 * Each update operations must be created by the operation factory. 
	 * Use this method to obtain the factory.
	 * @return returns the operation factory
	 */
	public static IOperationFactory getOperationFactory() {
		if (operationFactory == null)
			operationFactory = new OperationFactory();
		return operationFactory;
	}

	/**
	 * Check if the feature is the subject of an update operation such as install,
	 * configure, etc. and return it. Currently there can only be one pending
	 * operation on a feature.
	 * @param feature feature to check for pending operations
	 * @return pending operation if any, otherwise null.
	 */
	public static IFeatureOperation findPendingOperation(IFeature feature) {
		for (int i = 0; i < pendingOperations.size(); i++) {
			IFeatureOperation operation =
				(IFeatureOperation) pendingOperations.elementAt(i);
			if (operation.getFeature().equals(feature))
				return operation;
		}
		return null;
	}
	
	/**
	 * Register a pending operation.
	 * @param operation pending operation
	 */
	public static void addPendingOperation(IOperation operation) {
		pendingOperations.add(operation);
		//fireObjectsAdded(this, new Object[] { change });
	}

	/**
	 * Unregister a pending operation.
	 * @param operation pending operation
	 */
	public static void removePendingOperation(IOperation operation) {
		pendingOperations.remove(operation);
		//fireObjectsRemoved(this, new Object[] { change });
	}

	/**
	 * Adds a model changed listener.
	 * @param listener update model change listener
	 */
	public static void addUpdateModelChangedListener(IUpdateModelChangedListener listener) {
		if (!listeners.contains(listener))
			listeners.add(listener);
	}

	/**
	 * Removes an model changed listener.
	 * @param listener update model change listener
	 */
	public static void removeUpdateModelChangedListener(IUpdateModelChangedListener listener) {
		if (listeners.contains(listener))
			listeners.remove(listener);
	}

	/**
	 * Notifies model changed listeners when features/sites/etc. are added.
	 * @param parent parent object
	 * @param children children added
	 */
	public static void fireObjectsAdded(Object parent, Object[] children) {
		for (Iterator iter = listeners.iterator(); iter.hasNext();) {
			IUpdateModelChangedListener listener =
				(IUpdateModelChangedListener) iter.next();
			listener.objectsAdded(parent, children);
		}
	}

	/**
	 * Notifies model changed listeners when features/sites/etc are removed.
	 * @param parent parent object
	 * @param children children removed
	 */
	public static void fireObjectsRemoved(Object parent, Object[] children) {
		for (Iterator iter = listeners.iterator(); iter.hasNext();) {
			IUpdateModelChangedListener listener =
				(IUpdateModelChangedListener) iter.next();
			listener.objectsRemoved(parent, children);
		}
	}

	/**
	 * Notifies model changed listeners when features/sites/etc. have changed.
	 * @param object changed object
	 * @param property changed object property
	 */
	public static void fireObjectChanged(Object object, String property) {
		for (Iterator iter = listeners.iterator(); iter.hasNext();) {
			IUpdateModelChangedListener listener =
				(IUpdateModelChangedListener) iter.next();
			listener.objectChanged(object, property);
		}
	}

	/**
	 * Returns true when any of the install operations requires a license agreement.
	 * @param jobs features to install
	 * @return true when any of the features to install have a license
	 */
	public static boolean hasSelectedJobsWithLicenses(IInstallFeatureOperation[] jobs) {
		for (int i = 0; i < jobs.length; i++) {
			if (UpdateUtils.hasLicense(jobs[i].getFeature()))
				return true;
		}
		return false;
	}

	/**
	 * Returns true when any of the features to install has optional features.
	 * @param jobs features to install
	 * @return true when any of the features has optional features
	 */
	public static boolean hasSelectedJobsWithOptionalFeatures(IInstallFeatureOperation[] jobs) {
		for (int i = 0; i < jobs.length; i++) {
			if (UpdateUtils.hasOptionalFeatures(jobs[i].getFeature()))
				return true;
		}
		return false;
	}

	/**
	 * Returns the list of operations that need a license agreement.
	 * @param jobs features to install
	 * @return the list of operation that need a license agreement
	 */
	public static IInstallFeatureOperation[] getSelectedJobsWithLicenses(IInstallFeatureOperation[] jobs) {
		ArrayList list = new ArrayList();
		for (int i = 0; i < jobs.length; i++) {
			if (UpdateUtils.hasLicense(jobs[i].getFeature()))
				list.add(jobs[i]);
		}
		return (IInstallFeatureOperation[]) list.toArray(
			new IInstallFeatureOperation[list.size()]);
	}

	/**
	 * Returns the list of operations that have optional features to install.
	 * @param jobs features to install
	 * @return list of operations that have optional features to install
	 */
	public static IInstallFeatureOperation[] getSelectedJobsWithOptionalFeatures(IInstallFeatureOperation[] jobs) {
		ArrayList list = new ArrayList();
		for (int i = 0; i < jobs.length; i++) {
			if (UpdateUtils.hasOptionalFeatures(jobs[i].getFeature()))
				list.add(jobs[i]);
		}
		return (IInstallFeatureOperation[]) list.toArray(
			new IInstallFeatureOperation[list.size()]);
	}
	
	/**
	 * Sets whether any operations is in progress.
	 * @param inProgress true when operation is in progress
	 */
	public static synchronized void setInProgress(boolean inProgress) {
		OperationsManager.inProgress = inProgress;
	}
	
	/**
	 * Returns true when some operation is being executed, false otherwise.
	 * @return true when some operation execution is in progress, false otherwise
	 */
	public static synchronized boolean isInProgress() {
		return inProgress;
	}

	/**
	 * Returns the operations validator.
	 * @return the operation validator
	 */
	public static IOperationValidator getValidator() {
		if (validator == null)
			validator = new OperationValidator();
		return validator;
	}
	
	/**
	 * Sets a custom operation validator
	 * @param validator the custom validator
	 */
	public static void setValidator(IOperationValidator validator) {
		OperationsManager.validator = validator;
	}
	
	/**
	 * Applies the changes made to the current configuration. 
	 * Care must be taken when using this method. Normally, if you install a new
	 * plugin it is safe to do it.
	 */
	public static void applyChangesNow() {
		ConfigurationActivator configurator = ConfigurationActivator.getConfigurator();
		configurator.installBundles();
		pendingOperations.clear();
	}
}
