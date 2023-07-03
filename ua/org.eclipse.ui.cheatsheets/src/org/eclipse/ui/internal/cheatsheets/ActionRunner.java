/*******************************************************************************
 * Copyright (c) 2005, 2018 IBM Corporation and others.
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

package org.eclipse.ui.internal.cheatsheets;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.cheatsheets.ICheatSheetAction;
import org.eclipse.ui.internal.cheatsheets.data.Action;
import org.eclipse.ui.internal.cheatsheets.views.CheatSheetManager;
import org.osgi.framework.Bundle;

/**
 * Class which can run actions and determine the outcome
 */
public class ActionRunner {
	public IStatus runAction(Action cheatSheetAction, CheatSheetManager csm) {

		IStatus status =  Status.OK_STATUS;
		String pluginId = cheatSheetAction.getPluginID();
		String className = cheatSheetAction.getActionClass();
		String[] params = cheatSheetAction.getParams();
		Bundle bundle = Platform.getBundle(pluginId);
		if (bundle == null) {
			String message = NLS.bind(Messages.ERROR_FINDING_PLUGIN_FOR_ACTION, (new Object[] {pluginId}));
			return new Status(IStatus.ERROR, ICheatSheetResource.CHEAT_SHEET_PLUGIN_ID, IStatus.OK, message, null);
		}
		Class<?> actionClass;
		IAction action;
		try {
			actionClass = bundle.loadClass(className);
		} catch (Exception e) {
			String message = NLS.bind(Messages.ERROR_LOADING_CLASS_FOR_ACTION, (new Object[] {className}));
			return new Status(IStatus.ERROR, ICheatSheetResource.CHEAT_SHEET_PLUGIN_ID, IStatus.OK, message, e);
		}
		try {
			action = (IAction) actionClass.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			String message = NLS.bind(Messages.ERROR_CREATING_CLASS_FOR_ACTION, (new Object[] {className}));
			return new Status(IStatus.ERROR, ICheatSheetResource.CHEAT_SHEET_PLUGIN_ID, IStatus.OK, message, e);
		}

		final boolean[] listenerFired = { false };
		final boolean[] listenerResult = { false };
		IPropertyChangeListener propertyChangeListener = event -> {
			if (event.getProperty().equals(IAction.RESULT) && event.getNewValue() instanceof Boolean) {
				listenerFired[0] = true;
				listenerResult[0] = ((Boolean) event.getNewValue()).booleanValue();
			}
		};

		// Add PropertyChangeListener to the action, so we can detemine if a action was succesfull
		action.addPropertyChangeListener(propertyChangeListener);

		// Run the action for this ViewItem
		if (action instanceof ICheatSheetAction) {
			// Prepare parameters
			String[] clonedParams = null;
			if(params != null && params.length > 0) {
				clonedParams = new String[params.length];
				System.arraycopy(params, 0, clonedParams, 0, params.length);
				for (int i = 0; i < clonedParams.length; i++) {
					String param = clonedParams[i];
					if(param != null && param.startsWith("${") && param.endsWith("}")) { //$NON-NLS-1$ //$NON-NLS-2$
						param = param.substring(2,param.length()-1);
						String value = csm.getDataQualified(param);
						clonedParams[i] = value == null ? ICheatSheetResource.EMPTY_STRING : value;
					}
				}
			}
			((ICheatSheetAction) action).run(clonedParams, csm);
		} else {
			try {
				action.run();
			} catch (Throwable e) {
				status = new Status(IStatus.ERROR, ICheatSheetResource.CHEAT_SHEET_PLUGIN_ID, IStatus.OK, Messages.EXCEPTION_RUNNING_ACTION, e);
			}
		}

		// Remove the PropertyChangeListener
		action.removePropertyChangeListener(propertyChangeListener);

		if (status.isOK() && listenerFired[0]) {
			if (!listenerResult[0]) {
				status =new Status(IStatus.WARNING, ICheatSheetResource.CHEAT_SHEET_PLUGIN_ID, IStatus.OK, Messages.ACTION_FAILED, null);
			}
		}

		return status;
	}

}
