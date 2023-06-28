/*******************************************************************************
 * Copyright (c) 2006, 2013 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.internal.core.commands;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.commands.IDebugCommandRequest;
import org.eclipse.debug.core.commands.IStepFiltersHandler;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStepFilters;

/**
 * Default toggle step filters command for the standard debug model.
 *
 * @since 3.3
 */
public class StepFiltersCommand extends ForEachCommand implements IStepFiltersHandler {

	@Override
	protected Object getTarget(Object element) {
		IDebugTarget[] targets = getDebugTargets(element);
		if (targets.length > 0) {
			IStepFilters[] filters = new IStepFilters[targets.length];
			for (int i = 0; i < targets.length; i++) {
				IDebugTarget target = targets[i];
				if (target instanceof IStepFilters) {
					filters[i] = (IStepFilters) target;
				} else {
					filters[i] = (IStepFilters) getAdapter(element, IStepFilters.class);
				}
				if (filters[i] == null) {
					return null;
				}
			}
			return filters;
		}
		return null;
	}

	private IDebugTarget[] getDebugTargets(Object element) {
		if (element instanceof IDebugElement) {
			IDebugElement debugElement = (IDebugElement) element;
			return new IDebugTarget[] { debugElement.getDebugTarget() };
		} else if (element instanceof ILaunch) {
			ILaunch launch = (ILaunch) element;
			return launch.getDebugTargets();
		} else if (element instanceof IProcess) {
			IProcess process = (IProcess) element;
			return process.getLaunch().getDebugTargets();
		} else {
			return new IDebugTarget[0];
		}
	}

	@Override
	protected void execute(Object target) throws CoreException {
		if (target == null) {
			return;
		}
		IStepFilters[] filters = (IStepFilters[]) target;
		for (IStepFilters filter : filters) {
			filter.setStepFiltersEnabled(DebugPlugin.isUseStepFilters());
		}
	}

	@Override
	protected boolean isExecutable(Object target) {
		IStepFilters[] filters = (IStepFilters[]) target;
		for (IStepFilters filter : filters) {
			if (filter == null || !filter.supportsStepFilters()) {
				return false;
			}
		}
		return true;
	}

	@Override
	protected Object getEnabledStateJobFamily(IDebugCommandRequest request) {
		return IStepFiltersHandler.class;
	}
}
