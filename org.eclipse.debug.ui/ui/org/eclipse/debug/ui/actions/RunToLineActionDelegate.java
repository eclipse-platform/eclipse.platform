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
 *     Mikhail Khodjaiants (QNX) - https://bugs.eclipse.org/bugs/show_bug.cgi?id=83464
 *     Wind River - Pawel Piech - Added use of adapters to support non-standard models (bug 213074)
 *******************************************************************************/
package org.eclipse.debug.ui.actions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdapterManager;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.ISuspendResume;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.actions.ActionMessages;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.contexts.DebugContextEvent;
import org.eclipse.debug.ui.contexts.IDebugContextListener;
import org.eclipse.debug.ui.contexts.IDebugContextManager;
import org.eclipse.debug.ui.contexts.IDebugContextService;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IActionDelegate2;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * A run to line action that can be contributed to a an editor or view. The action
 * will perform the "run to line" operation for parts that provide
 * an appropriate <code>IRunToLineTarget</code> adapter.
 * <p>
 * Clients may reference/contribute this class as an action delegate
 * in plug-in XML.
 * </p>
 * <p>
 * Since 3.1, this action also implements {@link org.eclipse.ui.IViewActionDelegate}.
 * </p>
 * @since 3.0
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class RunToLineActionDelegate implements IEditorActionDelegate, IActionDelegate2, IViewActionDelegate {

	private IWorkbenchPart fActivePart = null;
	private IRunToLineTarget fPartTarget = null;
	private IAction fAction = null;
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
			update();
		}

		@Override
		public void debugContextChanged(DebugContextEvent event) {
			contextActivated(event.getContext());
		}

	}

	@Override
	public void dispose() {
		DebugUITools.getDebugContextManager().getContextService(fActivePart.getSite().getWorkbenchWindow()).removeDebugContextListener(fContextListener);
		fActivePart = null;
		fPartTarget = null;

	}

	@Override
	public void run(IAction action) {
		if (fPartTarget != null && fTargetElement != null) {
			try {
				fPartTarget.runToLine(fActivePart, fActivePart.getSite().getSelectionProvider().getSelection(), fTargetElement);
			} catch (CoreException e) {
				DebugUIPlugin.errorDialog(fActivePart.getSite().getWorkbenchWindow().getShell(), ActionMessages.RunToLineAction_0, ActionMessages.RunToLineAction_1, e.getStatus()); //
			}
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		this.fAction = action;
		update();
	}

	public void update() {
		if (fAction == null) {
			return;
		}
		Runnable r = () -> {
			boolean enabled = false;
			if (fPartTarget != null && fTargetElement != null) {
				IWorkbenchPartSite site = fActivePart.getSite();
				if (site != null) {
					ISelectionProvider selectionProvider = site.getSelectionProvider();
					if (selectionProvider != null) {
						ISelection selection = selectionProvider.getSelection();
						enabled = fTargetElement.isSuspended()
								&& fPartTarget.canRunToLine(fActivePart, selection, fTargetElement);
					}
				}
			}
			fAction.setEnabled(enabled);
		};
		DebugUIPlugin.getStandardDisplay().asyncExec(r);
	}

	@Override
	public void init(IAction action) {
		this.fAction = action;
		if (action != null) {
			action.setText(ActionMessages.RunToLineActionDelegate_4);
			action.setImageDescriptor(DebugUITools.getImageDescriptor(IInternalDebugUIConstants.IMG_LCL_RUN_TO_LINE));
			action.setDisabledImageDescriptor(DebugUITools.getImageDescriptor(IInternalDebugUIConstants.IMG_DLCL_RUN_TO_LINE));
		}
	}

	@Override
	public void runWithEvent(IAction action, Event event) {
		run(action);
	}

	@Override
	public void setActiveEditor(IAction action, IEditorPart targetEditor) {
		init(action);
		bindTo(targetEditor);
	}

	@Override
	public void init(IViewPart view) {
		bindTo(view);
	}

	/**
	 * Binds this action to operate on the given part's run to line adapter.
	 *
	 * @param part the workbench part to bind this delegate to
	 */
	private void bindTo(IWorkbenchPart part) {
		IDebugContextManager manager = DebugUITools.getDebugContextManager();
		if (fActivePart != null && !fActivePart.equals(part)) {
			manager.getContextService(fActivePart.getSite().getWorkbenchWindow()).removeDebugContextListener(fContextListener);
		}
		fPartTarget = null;
		fActivePart = part;
		if (part != null) {
			IWorkbenchWindow workbenchWindow = part.getSite().getWorkbenchWindow();
			IDebugContextService service = manager.getContextService(workbenchWindow);
			service.addDebugContextListener(fContextListener);
			fPartTarget  = part.getAdapter(IRunToLineTarget.class);
			if (fPartTarget == null) {
				IAdapterManager adapterManager = Platform.getAdapterManager();
				// TODO: we could restrict loading to cases when the debugging context is on
				if (adapterManager.hasAdapter(part, IRunToLineTarget.class.getName())) {
					fPartTarget = (IRunToLineTarget) adapterManager.loadAdapter(part, IRunToLineTarget.class.getName());
				}
			}
			ISelection activeContext = service.getActiveContext();
			fContextListener.contextActivated(activeContext);
		}
		update();
	}
}
