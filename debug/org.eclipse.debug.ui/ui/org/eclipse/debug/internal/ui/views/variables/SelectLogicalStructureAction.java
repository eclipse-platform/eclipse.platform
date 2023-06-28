/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.views.variables;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILogicalStructureType;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.custom.BusyIndicator;

/**
 * Action to set the logical structure to display for a variable (enables/disables
 * logical structure types for the same variable).
 */
public class SelectLogicalStructureAction extends Action {

	private VariablesView fView;
	private ILogicalStructureType fType;
	private ILogicalStructureType[] fAvailableTypes;

	/**
	 *
	 * @param view Variables view
	 * @param type the type that this action will turn on/off
	 * @param value the value for which logical structures are to be chosen
	 * @param availableTypes the set of logical structure types that are being offered
	 *  to the user in addition to the type controlled by this action
	 */
	public SelectLogicalStructureAction(VariablesView view, ILogicalStructureType type, IValue value, ILogicalStructureType[] availableTypes) {
		super(type.getDescription(value), IAction.AS_CHECK_BOX);
		setView(view);
		fAvailableTypes= availableTypes;
		fType= type;
	}

	@Override
	public void run() {
		valueChanged();
	}

	private void valueChanged() {
		if (!getView().isAvailable()) {
			return;
		}
		BusyIndicator.showWhile(getView().getViewer().getControl().getDisplay(), () -> {
			// Checking this action sets the type to fType, unchecking it sets the type
			// to null ("none selected")
			ILogicalStructureType type = null;
			IAction action = getView().getAction("ToggleContentProviders"); //$NON-NLS-1$
			if (isChecked()) {
				type = fType;
				action.setChecked(true);
			}
			action.run();
			DebugPlugin.setDefaultStructureType(fAvailableTypes, type);
			getView().getViewer().refresh();
		});
	}

	protected VariablesView getView() {
		return fView;
	}

	protected void setView(VariablesView view) {
		fView = view;
	}
}
