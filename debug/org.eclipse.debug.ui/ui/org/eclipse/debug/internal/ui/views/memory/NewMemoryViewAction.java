/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
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

package org.eclipse.debug.internal.ui.views.memory;

import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.memory.IMemoryRendering;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

/**
 * Action for opening a new memory view.
 */
public class NewMemoryViewAction implements IViewActionDelegate {

	private MemoryView fView;

	@Override
	public void init(IViewPart view) {
		if (view instanceof MemoryView)
			fView = (MemoryView) view;
	}

	@Override
	public void run(IAction action) {

		String secondaryId = MemoryViewIdRegistry.getUniqueSecondaryId(IDebugUIConstants.ID_MEMORY_VIEW);
		try {
			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			IViewPart newView = page.showView(IDebugUIConstants.ID_MEMORY_VIEW, secondaryId, IWorkbenchPage.VIEW_ACTIVATE);

			// set initial selection for new view
			setInitialSelection(newView);
			setInitialViewSettings(newView);

		} catch (PartInitException e) {
			// if view cannot be opened, open error
			DebugUIPlugin.log(e);
		}
	}

	private void setInitialSelection(IViewPart newView) {
		ISelection selection = fView.getSite().getSelectionProvider().getSelection();
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection strucSel = (IStructuredSelection) selection;

			// return if current selection is empty
			if (strucSel.isEmpty())
				return;

			Object obj = strucSel.getFirstElement();

			if (obj == null)
				return;

			if (obj instanceof IMemoryRendering) {
				IMemoryBlock memBlock = ((IMemoryRendering) obj).getMemoryBlock();
				strucSel = new StructuredSelection(memBlock);
				newView.getSite().getSelectionProvider().setSelection(strucSel);
			} else if (obj instanceof IMemoryBlock) {
				newView.getSite().getSelectionProvider().setSelection(strucSel);
			}
		}
	}

	private void setInitialViewSettings(IViewPart newView) {
		if (fView != null && newView instanceof MemoryView) {
			MemoryView newMView = (MemoryView) newView;
			IMemoryViewPane[] viewPanes = fView.getViewPanes();
			int orientation = fView.getViewPanesOrientation();
			for (IMemoryViewPane viewPane : viewPanes) {
				// copy view pane visibility
				newMView.showViewPane(fView.isViewPaneVisible(viewPane.getId()), viewPane.getId());
			}

			// do not want to copy renderings as it could be very expensive
			// create a blank view and let user creates renderings as needed

			// set orientation of new view
			newMView.setViewPanesOrientation(orientation);
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {

	}

}
