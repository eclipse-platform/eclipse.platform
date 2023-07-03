/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.sourcelookup;

import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.internal.ui.views.launch.LaunchView;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.SelectionListenerAction;

/**
 * Does source lookup for the selected stack frame again.
 *
 * @since 3.0
 */
public class LookupSourceAction extends SelectionListenerAction {

	private ISourceLookupDirector director = null;
	private LaunchView fView = null;
	private IStackFrame frame = null;

	public LookupSourceAction(LaunchView view) {
		super(SourceLookupUIMessages.LookupSourceAction_0);
		setEnabled(false);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IDebugHelpContextIds.LOOKUP_SOURCE_ACTION);
		fView = view;
	}

	@Override
	protected boolean updateSelection(IStructuredSelection selection) {
		director = null;
		frame = null;
		if (selection.size() == 1) {
			Object object = selection.getFirstElement();
			if (object instanceof IStackFrame) {
				frame = (IStackFrame)object;
				ILaunch launch = frame.getLaunch();
				if (launch != null && launch.getLaunchConfiguration() != null &&
						launch.getSourceLocator() instanceof ISourceLookupDirector) {
					director = (ISourceLookupDirector) launch.getSourceLocator();
				}
			}
		}
		return director != null;
	}
	@Override
	public void run() {
		ISelection selection = fView.getViewer().getSelection();
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ss = (IStructuredSelection) selection;
			if (ss.size() == 1) {
				IWorkbenchPage page = fView.getSite().getPage();
				SourceLookupManager.getDefault().displaySource(ss.getFirstElement(), page, true);
			}
		}
	}
}
