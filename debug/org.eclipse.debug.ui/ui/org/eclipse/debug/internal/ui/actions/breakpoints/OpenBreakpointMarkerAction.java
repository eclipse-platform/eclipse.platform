/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
 *     Lars Vogel <Lars.Vogel@gmail.com> - Bug 426110
 *     Alexander Fedorov <alexander.fedorov@arsysop.ru> - Bug 548799
 *******************************************************************************/
package org.eclipse.debug.internal.ui.actions.breakpoints;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.DelegatingModelPresentation;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.internal.ui.actions.ActionMessages;
import org.eclipse.jface.resource.ResourceLocator;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.eclipse.ui.ide.IDE;

public class OpenBreakpointMarkerAction extends SelectionProviderAction {

	protected static DelegatingModelPresentation fgPresentation = new DelegatingModelPresentation();
	private IBreakpoint breakpoint;
	private IEditorInput input;

	public OpenBreakpointMarkerAction(ISelectionProvider selectionProvider) {
		super(selectionProvider, ActionMessages.OpenBreakpointMarkerAction__Go_to_File_1);
		setToolTipText(ActionMessages.OpenBreakpointMarkerAction_Go_to_File_for_Breakpoint_2);
		ResourceLocator.imageDescriptorFromBundle("org.eclipse.ui.ide", "icons/full/elcl16/gotoobj_tsk.png") //$NON-NLS-1$ //$NON-NLS-2$
				.ifPresent(this::setImageDescriptor);
		ResourceLocator.imageDescriptorFromBundle("org.eclipse.ui.ide", "icons/full/dlcl16/gotoobj_tsk.png") //$NON-NLS-1$ //$NON-NLS-2$
				.ifPresent(this::setDisabledImageDescriptor);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(
			this,
			IDebugHelpContextIds.OPEN_BREAKPOINT_ACTION);
		setEnabled(false);
	}

	@Override
	public void run() {
		IWorkbenchWindow dwindow= DebugUIPlugin.getActiveWorkbenchWindow();
		if (dwindow == null) {
			return;
		}
		IWorkbenchPage page= dwindow.getActivePage();
		if (page == null) {
			return;
		}

		IStructuredSelection selection= getStructuredSelection();
		if (selection.isEmpty()) {
			setEnabled(false);
			return;
		}

		IEditorPart part= null;
		if (input != null) {
			String editorId = fgPresentation.getEditorId(input, breakpoint);
			if (editorId != null) {
				try {
					part= page.openEditor(input, editorId, true, IWorkbenchPage.MATCH_INPUT | IWorkbenchPage.MATCH_ID);
				} catch (PartInitException e) {
					DebugUIPlugin.errorDialog(dwindow.getShell(), ActionMessages.OpenBreakpointMarkerAction_Go_to_Breakpoint_1, ActionMessages.OpenBreakpointMarkerAction_Exceptions_occurred_attempting_to_open_the_editor_for_the_breakpoint_resource_2, e); //
				}
			}
		}
		if (part != null) {
			IDE.gotoMarker(part, breakpoint.getMarker());
		}
	}

	@Override
	public void selectionChanged(IStructuredSelection sel) {
		if (sel.size() == 1) {
			breakpoint = (IBreakpoint)DebugPlugin.getAdapter(sel.getFirstElement(), IBreakpoint.class);
			if (breakpoint != null) {
				input= fgPresentation.getEditorInput(breakpoint);
				if (input != null) {
					setEnabled(true);
					return;
				}
			}
		} else {
			breakpoint = null;
			input = null;
		}
		setEnabled(false);
	}
}
