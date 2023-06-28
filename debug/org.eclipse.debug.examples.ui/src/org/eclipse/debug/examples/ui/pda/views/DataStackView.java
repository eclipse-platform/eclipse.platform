/*******************************************************************************
 * Copyright (c) 2005, 2013 IBM Corporation and others.
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
 *     Bjorn Freeman-Benson - initial API and implementation
 *     Wind River - Pawel Piech - replaced actions with handlers (bug 229219)
 *     Pawel Piech (Wind River) - ported PDA Virtual Machine to Java (Bug 261400)
 ******************************************************************************/
package org.eclipse.debug.examples.ui.pda.views;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.examples.core.pda.model.PDAStackFrame;
import org.eclipse.debug.examples.core.pda.model.PDAThread;
import org.eclipse.debug.ui.AbstractDebugView;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.contexts.DebugContextEvent;
import org.eclipse.debug.ui.contexts.IDebugContextListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.progress.UIJob;


/**
 * View of the PDA VM data stack
 */
public class DataStackView extends AbstractDebugView implements IDebugContextListener {

	private PDAThread fThread;

	class StackViewContentProvider implements ITreeContentProvider {

		@Override
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof PDAThread) {
				try {
					return ((PDAThread)parentElement).getDataStack();
				} catch (DebugException e) {
				}
			}
			return new Object[0];
		}

		@Override
		public Object getParent(Object element) {
			if (element instanceof PDAThread) {
				return null;
			} else {
				return fThread;
			}
		}

		@Override
		public boolean hasChildren(Object element) {
			return element instanceof PDAThread;
		}

		@Override
		public Object[] getElements(Object inputElement) {
			return getChildren(inputElement);
		}

		@Override
		public void dispose() {
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

	}

	@Override
	protected Viewer createViewer(Composite parent) {
		TreeViewer viewer = new TreeViewer(parent);
		viewer.setLabelProvider(DebugUITools.newDebugModelPresentation());
		viewer.setContentProvider(new StackViewContentProvider());
		DebugUITools.getDebugContextManager().getContextService(getSite().getWorkbenchWindow()).addDebugContextListener(this);
		getSite().setSelectionProvider(viewer);
		return viewer;
	}

	@Override
	protected void createActions() {
	}

	@Override
	protected String getHelpContextId() {
		return null;
	}

	@Override
	protected void fillContextMenu(IMenuManager menu) {
		menu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	@Override
	protected void configureToolBar(IToolBarManager tbm) {
	}

	@Override
	public void dispose() {
		DebugUITools.getDebugContextManager().getContextService(getSite().getWorkbenchWindow()).removeDebugContextListener(this);
		super.dispose();
	}

	@Override
	public void debugContextChanged(final DebugContextEvent event) {
		new UIJob(getSite().getShell().getDisplay(), "DataStackView update") { //$NON-NLS-1$
			{
				setSystem(true);
			}

			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				if (getViewer() != null) { // runs asynchronously, view may be disposed
					update(event.getContext());
				}
				return Status.OK_STATUS;
			}
		}.schedule();
	}

	/**
	 * Updates the view for the selected thread (if suspended)
	 */
	private void update(ISelection context) {
		fThread = null;

		if (context instanceof IStructuredSelection) {
			Object element = ((IStructuredSelection)context).getFirstElement();
			if (element instanceof PDAThread) {
				fThread = (PDAThread)element;
			} else if (element instanceof PDAStackFrame) {
				fThread = (PDAThread)((PDAStackFrame)element).getThread();
			}
		}
		Object input = null;
		if (fThread != null && fThread.isSuspended()) {
			input = fThread;
		}
		getViewer().setInput(input);
		getViewer().refresh();
	}
}
