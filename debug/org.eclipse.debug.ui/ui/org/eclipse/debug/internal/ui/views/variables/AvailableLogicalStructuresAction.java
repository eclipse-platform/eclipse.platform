/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILogicalStructureType;
import org.eclipse.debug.core.model.IExpression;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.PlatformUI;

/**
 * Drop down action that displays available logical structures for a selected
 * variable or expression.
 */
public class AvailableLogicalStructuresAction extends Action implements IMenuCreator {

	private VariablesView fView;
	private Menu fMenu;
	private IValue fValue;
	private ILogicalStructureType[] fTypes;

	public AvailableLogicalStructuresAction(VariablesView view) {
		setView(view);
		setToolTipText(VariablesViewMessages.AvailableLogicalStructuresAction_0);
		setText(VariablesViewMessages.AvailableLogicalStructuresAction_1);
		setImageDescriptor(DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_ELCL_SHOW_LOGICAL_STRUCTURE));
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IDebugHelpContextIds.VARIABLES_SELECT_LOGICAL_STRUCTURE);
		setEnabled(false);
		setMenuCreator(this);
		init();
	}

	@Override
	public void run() {
	}

	protected VariablesView getView() {
		return fView;
	}

	protected void setView(VariablesView view) {
		fView = view;
	}

	@Override
	public void dispose() {
		if (fMenu != null) {
			fMenu.dispose();
		}
		fView= null;
		fValue = null;
		fTypes = null;
	}

	@Override
	public Menu getMenu(Control parent) {
		return null;
	}

	protected void addActionToMenu(Menu parent, Action action) {
		ActionContributionItem item= new ActionContributionItem(action);
		item.fill(parent, -1);
	}

	@Override
	public Menu getMenu(Menu parent) {
		if (fMenu != null) {
			fMenu.dispose();
		}

		fMenu= new Menu(parent);
		ILogicalStructureType[] types = getTypes();
		ILogicalStructureType enabledType = DebugPlugin.getDefaultStructureType(types);
		if (types != null && types.length > 0) {
			for (int i = 0; i < types.length; i++) {
				ILogicalStructureType type= types[i];
				Action action = new SelectLogicalStructureAction(getView(), type, getValue(), types);
				action.setChecked((enabledType == type) && getView().isShowLogicalStructure());
				StringBuilder label= new StringBuilder();
				//add the numerical accelerator
				if (i < 9) {
					label.append('&');
					label.append(i + 1);
					label.append(' ');
				}
				label.append(action.getText());
				if (enabledType == type) {
					action.setText(label + " " + VariablesViewMessages.AvailableLogicalStructuresAction_2); //$NON-NLS-1$
				} else {
					action.setText(label.toString());
				}
				addActionToMenu(fMenu, action);
			}
		}

		new MenuItem(fMenu, SWT.SEPARATOR);
		EditVariableLogicalStructureAction editVariableLogicalStructureAction = new EditVariableLogicalStructureAction(getView());
		editVariableLogicalStructureAction.setText(VariablesViewMessages.AvailableLogicalStructuresAction_3);
		addActionToMenu(fMenu, editVariableLogicalStructureAction);
		return fMenu;
	}

	public void init() {
		setValue(null);
		setTypes(null);
		ISelection s = getView().getViewer().getSelection();
		if (s instanceof IStructuredSelection) {
			IStructuredSelection selection = (IStructuredSelection) s;
			if (selection.size() == 1) {
				Object obj = selection.getFirstElement();
				IValue value = null;
				if (obj instanceof IVariable) {
					IVariable var = (IVariable) obj;
					try {
						value = var.getValue();
					} catch (DebugException e) {
					}
				} else if (obj instanceof IExpression) {
					IExpression expression = (IExpression) obj;
					value = expression.getValue();
				}
				if (value != null) {
					ILogicalStructureType[] types = DebugPlugin.getLogicalStructureTypes(value);
					if (types.length > 0) {
						setTypes(types);
						setValue(value);
						setEnabled(true);
						return;
					}
				}
			}
		}
	}

	protected ILogicalStructureType[] getTypes() {
		return fTypes;
	}

	private void setTypes(ILogicalStructureType[] types) {
		fTypes = types;
	}

	protected IValue getValue() {
		return fValue;
	}

	private void setValue(IValue value) {
		fValue = value;
	}
}
