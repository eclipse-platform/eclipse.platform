/*******************************************************************************
 *  Copyright (c) 2000, 2012 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Wind River - Pawel Piech - Added use of adapters to support non-standard models (bug 213074)
 *******************************************************************************/
package org.eclipse.debug.internal.ui.actions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.ISuspendResume;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.actions.IRunToLineTarget;
import org.eclipse.debug.ui.contexts.DebugContextEvent;
import org.eclipse.debug.ui.contexts.IDebugContextListener;
import org.eclipse.debug.ui.contexts.IDebugContextService;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * Global retargettable run to line action.
 *
 * @since 3.0
 */
public class RetargetRunToLineAction extends RetargetAction {

	private DebugContextListener fContextListener = new DebugContextListener();
	private ISuspendResume fTargetElement = null;

	class DebugContextListener implements IDebugContextListener {

		protected void contextActivated(ISelection selection) {
			fTargetElement = null;
			if (selection instanceof IStructuredSelection) {
				IStructuredSelection ss = (IStructuredSelection) selection;
				if (ss.size() == 1) {
					fTargetElement = (ISuspendResume)
						DebugPlugin.getAdapter(ss.getFirstElement(), ISuspendResume.class);
				}
			}
			IAction action = getAction();
			if (action != null) {
				action.setEnabled(fTargetElement != null && isTargetEnabled());
			}
		}

		@Override
		public void debugContextChanged(DebugContextEvent event) {
			contextActivated(event.getContext());
		}


	}

	@Override
	public void dispose() {
		DebugUITools.getDebugContextManager().getContextService(fWindow).removeDebugContextListener(fContextListener);
		super.dispose();
	}

	@Override
	public void init(IWorkbenchWindow window) {
		super.init(window);
		IDebugContextService service = DebugUITools.getDebugContextManager().getContextService(window);
		service.addDebugContextListener(fContextListener);
		ISelection activeContext = service.getActiveContext();
		fContextListener.contextActivated(activeContext);
	}

	@Override
	public void init(IAction action) {
		super.init(action);
		action.setActionDefinitionId("org.eclipse.debug.ui.commands.RunToLine"); //$NON-NLS-1$
	}

	@Override
	protected boolean canPerformAction(Object target, ISelection selection,	IWorkbenchPart part) {
		return fTargetElement != null &&
			((IRunToLineTarget)target).canRunToLine(part, selection, fTargetElement);
	}

	@Override
	protected Class<IRunToLineTarget> getAdapterClass() {
		return IRunToLineTarget.class;
	}

	@Override
	protected void performAction(Object target, ISelection selection, IWorkbenchPart part) throws CoreException {
		((IRunToLineTarget)target).runToLine(part, selection, fTargetElement);
	}

	@Override
	protected String getOperationUnavailableMessage() {
		return ActionMessages.RetargetRunToLineAction_0;
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		if (fTargetElement == null) {
			action.setEnabled(false);
		} else {
			super.selectionChanged(action, selection);
		}
	}
}
