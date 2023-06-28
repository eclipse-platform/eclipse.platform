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

import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.viewers.model.provisional.TreeModelViewer;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IUpdate;

/**
 * Action to toggle the use of contributed variables content providers on and off.
 * When on, all registered variables content providers for the current debug model
 * are used.  When off, the default content provider (that shows all children)
 * is used for all debug models.
 */
public class ToggleShowColumnsAction extends Action implements IUpdate {

	private TreeModelViewer fViewer;

	public ToggleShowColumnsAction(TreeModelViewer viewew) {
		super(VariablesViewMessages.ToggleShowColumnsAction_0, IAction.AS_CHECK_BOX);
		fViewer = viewew;
		setToolTipText(VariablesViewMessages.ToggleShowColumnsAction_1);
		setImageDescriptor(DebugUITools.getImageDescriptor(IInternalDebugUIConstants.IMG_OBJS_COMMON_TAB));
		setId(DebugUIPlugin.getUniqueIdentifier() + ".ToggleShowColumsAction"); //$NON-NLS-1$
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IDebugHelpContextIds.SHOW_COLUMNS_ACTION);
	}

	@Override
	public void run() {
		if (fViewer.getControl().isDisposed()) {
			return;
		}
		BusyIndicator.showWhile(fViewer.getControl().getDisplay(), () -> fViewer.setShowColumns(isChecked()));
	}

	@Override
	public void update() {
		setEnabled(fViewer.canToggleColumns());
		setChecked(fViewer.isShowColumns());
	}


}
