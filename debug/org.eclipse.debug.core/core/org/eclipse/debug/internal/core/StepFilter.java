/*******************************************************************************
 * Copyright (c) 2014 itemis AG (http://www.itemis.eu) and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.debug.internal.core;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IStepFilter;

/**
 * Manage step filter extensions.
 *
 * @see IConfigurationElementConstants
 * @since 3.10
 */
public class StepFilter {

	private IConfigurationElement fConfigurationElement;

	private String fModelIdentifier;

	private IStepFilter fDelegate;

	public StepFilter(IConfigurationElement element) throws CoreException {
		fConfigurationElement = element;
		fModelIdentifier = fConfigurationElement.getAttribute(IConfigurationElementConstants.MODEL_IDENTIFIER);
		if (fModelIdentifier == null) {
			throw new CoreException(new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(), DebugPlugin.ERROR, DebugCoreMessages.StepFilter_0, null));
		}
		String className = fConfigurationElement.getAttribute(IConfigurationElementConstants.CLASS);
		if (className == null) {
			throw new CoreException(new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(), DebugPlugin.ERROR, DebugCoreMessages.StepFilter_1, null));
		}
	}

	/**
	 * Returns step filters for the given model identifier.
	 *
	 * @param modelIdentifier the model identifier for which step filters are
	 *            requested
	 * @return step filters
	 */
	public IStepFilter[] getStepFilters(String modelIdentifier) {
		if (fModelIdentifier.equals(modelIdentifier)) {
			IStepFilter delegate = getDelegate();
			return new IStepFilter[] { delegate };
		}
		return new IStepFilter[0];
	}

	/**
	 * Returns the IStepFilter for this extension.
	 *
	 * @return the {@link IStepFilter}
	 */
	protected IStepFilter getDelegate() {
		if (fDelegate == null) {
			try {
				fDelegate = (IStepFilter) fConfigurationElement.createExecutableExtension(IConfigurationElementConstants.CLASS);
			} catch (CoreException e) {
				DebugPlugin.log(e);
			}
		}
		return fDelegate;
	}

}
