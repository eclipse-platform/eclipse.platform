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
package org.eclipse.debug.internal.ui.actions.breakpoints;


import java.util.Iterator;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointsListener;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.actions.ActionMessages;
import org.eclipse.debug.internal.ui.breakpoints.provisional.IBreakpointContainer;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;

public class EnableBreakpointsAction implements IViewActionDelegate, IPartListener, IBreakpointsListener {

	private IViewPart fView;
	private IAction fAction;

	public EnableBreakpointsAction() {
	}

	protected IViewPart getView() {
		return fView;
	}

	protected void setView(IViewPart view) {
		fView = view;
	}

	/**
	 * @see IViewActionDelegate#init(IViewPart)
	 */
	@Override
	public void init(IViewPart view) {
		setView(view);
		DebugPlugin.getDefault().getBreakpointManager().addBreakpointListener(this);
		view.getViewSite().getPage().addPartListener(this);
	}

	protected void update() {
		selectionChanged(getAction(), getView().getViewSite().getSelectionProvider().getSelection());
	}

	/**
	 * If this action can enable breakpoints
	 * @return always <code>true</code>
	 */
	protected boolean isEnableAction() {
		return true;
	}

	@Override
	public void run(IAction action) {
		IStructuredSelection selection= getSelection();
		final int size= selection.size();
		if (size == 0) {
			return;
		}

		final Iterator<?> itr = selection.iterator();
		final MultiStatus ms= new MultiStatus(DebugUIPlugin.getUniqueIdentifier(), DebugException.REQUEST_FAILED, ActionMessages.EnableBreakpointAction_Enable_breakpoint_s__failed_2, null);
		IWorkspaceRunnable runnable = new IWorkspaceRunnable() {
			@Override
			public void run(IProgressMonitor monitor) {
				while (itr.hasNext()) {
					Object element= itr.next();
					try {
						IBreakpoint[] breakpoints= null;
						IBreakpoint breakpoint = (IBreakpoint)DebugPlugin.getAdapter(element, IBreakpoint.class);
						if (breakpoint != null) {
							breakpoints= new IBreakpoint[] { breakpoint };
						} else if (element instanceof IBreakpointContainer) {
							breakpoints= ((IBreakpointContainer) element).getBreakpoints();
						}
						if (breakpoints != null) {
							setEnabled(breakpoints);
						}
					} catch (CoreException e) {
						ms.merge(e.getStatus());
					}
				}
			}
			public void setEnabled(IBreakpoint[] breakpoints) throws CoreException {
				boolean enable= isEnableAction();
				for (IBreakpoint breakpoint : breakpoints) {
					breakpoint.setEnabled(enable);
				}
			}
		};

		try {
			ResourcesPlugin.getWorkspace().run(runnable, null, 0, new NullProgressMonitor());
		} catch (CoreException e) {
			// Exceptions are handled by runnable
		}

		if (!ms.isOK()) {
			IWorkbenchWindow window= DebugUIPlugin.getActiveWorkbenchWindow();
			if (window != null) {
				DebugUIPlugin.errorDialog(window.getShell(), ActionMessages.EnableBreakpointAction_Enabling_breakpoints_3, ActionMessages.EnableBreakpointAction_Exceptions_occurred_enabling_the_breakpoint_s___4, ms); //
			} else {
				DebugUIPlugin.log(ms);
			}
		}
	}

	private IStructuredSelection getSelection() {
		return (IStructuredSelection)getView().getViewSite().getSelectionProvider().getSelection();
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		setAction(action);
		if (!(selection instanceof IStructuredSelection)) {
			return;
		}
		IStructuredSelection sel= (IStructuredSelection)selection;

		Iterator<?> itr = sel.iterator();
		boolean allEnabled= true;
		boolean allDisabled= true;
		while (itr.hasNext()) {
			Object selected= itr.next();
			IBreakpoint bp = (IBreakpoint)DebugPlugin.getAdapter(selected, IBreakpoint.class);

			if (bp != null) {
				try {
					if (bp.isEnabled()) {
						allDisabled= false;
					} else {
						allEnabled= false;
					}
				} catch (CoreException ce) {
					handleException(ce);
				}
			}
			else if (selected instanceof IBreakpointContainer) {
				IBreakpoint[] breakpoints = ((IBreakpointContainer) selected).getBreakpoints();
				for (IBreakpoint breakpoint : breakpoints) {
					try {
						if (breakpoint.isEnabled()) {
							allDisabled= false;
						} else {
							allEnabled= false;
						}
					} catch (CoreException ce) {
						handleException(ce);
					}
				}
			} else {
				return;
			}

		}

		if (isEnableAction()) {
			action.setEnabled(!allEnabled);
		} else {
			action.setEnabled(!allDisabled);
		}
	}

	private void handleException(CoreException ce) {
		IWorkbenchWindow window= DebugUIPlugin.getActiveWorkbenchWindow();
		if (window != null) {
			DebugUIPlugin.errorDialog(window.getShell(), ActionMessages.EnableBreakpointAction_Enabling_breakpoints_3, ActionMessages.EnableBreakpointAction_Exceptions_occurred_enabling_the_breakpoint_s___4, ce); //
		} else {
			DebugUIPlugin.log(ce);
		}
	}


	/**
	 * Removes this action as a breakpoint and part listener.
	 */
	public void dispose() {
		DebugPlugin.getDefault().getBreakpointManager().removeBreakpointListener(this);
		getView().getViewSite().getPage().removePartListener(this);
	}

	/**
	 * @see IBreakpointsListener#breakpointsAdded(IBreakpoint[])
	 */
	@Override
	public void breakpointsAdded(IBreakpoint[] breakpoints) {
	}

	/**
	 * @see IBreakpointsListener#breakpointsRemoved(IBreakpoint[], IMarkerDelta[])
	 */
	@Override
	public void breakpointsRemoved(IBreakpoint[] breakpoints, IMarkerDelta[] deltas) {
		asynchUpdate();
	}

	/**
	 * @see IBreakpointsListener#breakpointsChanged(IBreakpoint[], IMarkerDelta[])
	 */
	@Override
	public void breakpointsChanged(IBreakpoint[] breakpoints, IMarkerDelta[] deltas) {
		asynchUpdate();
	}

	protected void asynchUpdate() {
		if (getAction() == null) {
			return;
		}
		IWorkbenchWindow window= getView().getViewSite().getPage().getWorkbenchWindow();
		if (window == null) {
			return;
		}
		Shell shell= window.getShell();
		if (shell == null || shell.isDisposed()) {
			return;
		}
		Runnable r = () -> {
			IWorkbenchWindow ww = getView().getViewSite().getPage().getWorkbenchWindow();
			if (ww == null) {
				return;
			}
			Shell s = ww.getShell();
			if (s == null || s.isDisposed()) {
				return;
			}
			update();
		};

		shell.getDisplay().asyncExec(r);
	}

	protected IAction getAction() {
		return fAction;
	}

	protected void setAction(IAction action) {
		fAction = action;
	}
	/**
	 * @see IPartListener#partActivated(IWorkbenchPart)
	 */
	@Override
	public void partActivated(IWorkbenchPart part) {
	}

	/**
	 * @see IPartListener#partBroughtToTop(IWorkbenchPart)
	 */
	@Override
	public void partBroughtToTop(IWorkbenchPart part) {
	}

	/**
	 * @see IPartListener#partClosed(IWorkbenchPart)
	 */
	@Override
	public void partClosed(IWorkbenchPart part) {
		if (part.equals(getView())) {
			dispose();
		}
	}

	/**
	 * @see IPartListener#partDeactivated(IWorkbenchPart)
	 */
	@Override
	public void partDeactivated(IWorkbenchPart part) {
	}

	/**
	 * @see IPartListener#partOpened(IWorkbenchPart)
	 */
	@Override
	public void partOpened(IWorkbenchPart part) {
	}
}

