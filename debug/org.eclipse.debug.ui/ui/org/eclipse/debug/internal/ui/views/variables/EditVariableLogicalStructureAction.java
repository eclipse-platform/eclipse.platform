/*******************************************************************************
 * Copyright (c) 2005, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial implementation
 *******************************************************************************/
package org.eclipse.debug.internal.ui.views.variables;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILogicalStructureType;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.dialogs.PreferencesUtil;

/**
 * Action which prompts the user to edit the logical structure that
 * is currently active on the given object.
 */
public class EditVariableLogicalStructureAction extends Action {

	/**
	 * The editable structure for the currently selected variable or
	 * <code>null</code> if none.
	 */
	private ILogicalStructureType fStructure = null;
	private VariablesView fView = null;

	public EditVariableLogicalStructureAction(VariablesView view) {
		super();
		fView = view;
		ISelection selection = view.getViewer().getSelection();
		if (selection != null) {
			init(selection);
		}
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

	/**
	 * Prompt the user to edit the logical structure associated with the currently
	 * selected variable.
	 */
	@Override
	public void run() {
		PreferencesUtil.createPreferenceDialogOn(DebugUIPlugin.getShell(), "org.eclipse.jdt.debug.ui.JavaLogicalStructuresPreferencePage", //$NON-NLS-1$
				new String[] { "org.eclipse.jdt.debug.ui.JavaDetailFormattersPreferencePage", //$NON-NLS-1$
						"org.eclipse.jdt.debug.ui.JavaLogicalStructuresPreferencePage", //$NON-NLS-1$
						"org.eclipse.jdt.debug.ui.heapWalking", //$NON-NLS-1$
						"org.eclipse.jdt.debug.ui.JavaPrimitivesPreferencePage" }, (fStructure != null) //$NON-NLS-1$
								? fStructure.getId() + fStructure.getDescription() + fStructure.hashCode()
								: null).open();
	}

	private void init(ISelection selection) {
		fStructure= null;
		Object element = ((IStructuredSelection) selection).getFirstElement();
		if (element instanceof IVariable) {
			try {
				IValue value= ((IVariable) element).getValue();
				ILogicalStructureType type= getLogicalStructure(value);
				fStructure = type;
			} catch (DebugException e) {
				DebugUIPlugin.log(e.getStatus());
			}
		}
		setEnabled(fView.isShowLogicalStructure());
	}

	/**
	 * Returns the logical structure currently associated with the given
	 * value or <code>null</code> if none.
	 * @param value the value
	 * @return the logical structure currently associated with the given
	 *  value or <code>null</code> if none.
	 */
	private ILogicalStructureType getLogicalStructure(IValue value) {
		// This code is based on VariablesViewContentProvider#getLogicalValue(IValue)
		ILogicalStructureType type = null;
		ILogicalStructureType[] types = DebugPlugin.getLogicalStructureTypes(value);
		if (types.length > 0) {
			type= DebugPlugin.getDefaultStructureType(types);
		}
		return type;
	}
}
