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
 *     WindRiver - Bug 272367: "Show Type Names" attribute is not available in the IPresentationContext properties
 *******************************************************************************/
package org.eclipse.debug.internal.ui.actions.variables;


import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.actions.ActionMessages;
import org.eclipse.debug.internal.ui.viewers.model.provisional.TreeModelViewer;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.IDebugView;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.ui.PlatformUI;

/**
 * An action that toggles the state of a viewer to
 * show/hide type names of variables.
 * Only viewers that use a <code>VariableLabelProvider</code> to render its
 * elements are affected.
 */
public class ShowTypesAction extends Action {

	private IDebugView fView;

	public ShowTypesAction(IDebugView view) {
		super(ActionMessages.ShowTypesAction_Show__Type_Names_1, IAction.AS_CHECK_BOX);
		setView(view);
		setToolTipText(ActionMessages.ShowTypesAction_Show_Type_Names);
		setHoverImageDescriptor(DebugPluginImages.getImageDescriptor(IDebugUIConstants.IMG_LCL_TYPE_NAMES));
		setDisabledImageDescriptor(DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_DLCL_TYPE_NAMES));
		setImageDescriptor(DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_ELCL_TYPE_NAMES));
		setId(DebugUIPlugin.getUniqueIdentifier() + ".ShowTypesAction"); //$NON-NLS-1$
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IDebugHelpContextIds.SHOW_TYPES_ACTION);
	}

	/**
	 * @see Action#run()
	 */
	@Override
	public void run() {
		valueChanged(isChecked());
	}

	private void valueChanged(boolean on) {
		final Viewer viewer = getView().getViewer();
		if (viewer.getControl().isDisposed()) {
			return;
		}

		IDebugModelPresentation debugLabelProvider= getView().getAdapter(IDebugModelPresentation.class);
		if (debugLabelProvider != null) {
			Boolean typesStatus = on ? Boolean.TRUE : Boolean.FALSE;
			debugLabelProvider.setAttribute(IDebugModelPresentation.DISPLAY_VARIABLE_TYPE_NAMES, typesStatus);
			if (viewer instanceof TreeModelViewer) {
				TreeModelViewer treeViewer = (TreeModelViewer) viewer;
				treeViewer.getPresentationContext().setProperty(IDebugModelPresentation.DISPLAY_VARIABLE_TYPE_NAMES, typesStatus);
			}
			BusyIndicator.showWhile(viewer.getControl().getDisplay(), () -> viewer.refresh());
		}
	}

	protected IDebugView getView() {
		return fView;
	}

	protected void setView(IDebugView view) {
		fView = view;
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		if (enabled) {
			setToolTipText(ActionMessages.ShowTypesAction_Show_Type_Names);
		} else {
			setToolTipText(ActionMessages.ShowTypesAction_0);
		}
	}


}


