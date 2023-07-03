/*******************************************************************************
 * Copyright (c) 2004, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial implementation
 *     Pawel Piech (Wind River) - added a breadcrumb mode to Debug view (Bug 252677)
 *     Wind River Systems - refactored on top of VirtualTreeModelViewer
 *******************************************************************************/
package org.eclipse.debug.examples.ui.pda.adapters;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.internal.ui.viewers.FindElementDialog;
import org.eclipse.debug.internal.ui.viewers.model.ILabelUpdateListener;
import org.eclipse.debug.internal.ui.viewers.model.TimeTriggeredProgressMonitorDialog;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IChildrenUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ILabelUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelDelta;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelDeltaVisitor;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IViewerUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IViewerUpdateListener;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ModelDelta;
import org.eclipse.debug.internal.ui.viewers.model.provisional.PresentationContext;
import org.eclipse.debug.internal.ui.viewers.model.provisional.TreeModelViewer;
import org.eclipse.debug.internal.ui.viewers.model.provisional.VirtualItem;
import org.eclipse.debug.internal.ui.viewers.model.provisional.VirtualTreeModelViewer;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.IDebugView;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IUpdate;

/**
 * Action which prompts user with a filtered list selection dialog to find an element in tree.
 *
 * @since 3.3
 */
public class PDAVirtualFindAction extends Action implements IUpdate {

	private final TreeModelViewer fClientViewer;

	protected static class VirtualViewerListener implements IViewerUpdateListener, ILabelUpdateListener {

		private boolean fViewerUpdatesComplete = false;
		private boolean fLabelUpdatesComplete = false;
		private IProgressMonitor fProgressMonitor;
		private int fRemainingUpdatesCount = 0;

		@Override
		public void labelUpdateStarted(ILabelUpdate update) {}
		@Override
		public void labelUpdateComplete(ILabelUpdate update) {
			incrementProgress(1);
		}
		@Override
		public void labelUpdatesBegin() {
			fLabelUpdatesComplete = false;
		}
		@Override
		public void labelUpdatesComplete() {
			fLabelUpdatesComplete = true;
			completeProgress();
		}

		@Override
		public void updateStarted(IViewerUpdate update) {}
		@Override
		public void updateComplete(IViewerUpdate update) {
			if (update instanceof IChildrenUpdate) {
				incrementProgress(((IChildrenUpdate)update).getLength());
			}
		}
		@Override
		public void viewerUpdatesBegin() {
			fViewerUpdatesComplete = false;
		}
		@Override
		public void viewerUpdatesComplete() {
			fViewerUpdatesComplete = true;
			completeProgress();
		}

		private void completeProgress() {
			IProgressMonitor pm;
			synchronized (this) {
				pm = fProgressMonitor;
			}
			if (pm != null && fLabelUpdatesComplete && fViewerUpdatesComplete) {
				pm.done();
			}
		}

		private void incrementProgress(int count) {
			IProgressMonitor pm;
			synchronized (this) {
				pm = fProgressMonitor;
				fRemainingUpdatesCount -= count;
			}
			if (pm != null && fLabelUpdatesComplete && fViewerUpdatesComplete) {
				pm.worked(count);
			}
		}

	}

	private static class FindLabelProvider extends LabelProvider {
		private final VirtualTreeModelViewer fVirtualViewer;
		private final Map<VirtualItem, String> fTextCache = new HashMap<>();

		public FindLabelProvider(VirtualTreeModelViewer viewer, List<VirtualItem> items) {
			fVirtualViewer = viewer;
			for (int i = 0; i < items.size(); i++) {
				VirtualItem item = items.get(i);
				fTextCache.put(item, fVirtualViewer.getText(item, 0));
			}
		}

		@Override
		public Image getImage(Object element) {
			return fVirtualViewer.getImage((VirtualItem) element, 0);
		}

		@Override
		public String getText(Object element) {
			return fTextCache.get(element);
		}
	}

	public PDAVirtualFindAction(IPresentationContext context) {
		setText("Find"); //$NON-NLS-1$
		setId(DebugUIPlugin.getUniqueIdentifier() + ".FindElementAction"); //$NON-NLS-1$
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IDebugHelpContextIds.FIND_ELEMENT_ACTION);
		setActionDefinitionId(IWorkbenchCommandConstants.EDIT_FIND_AND_REPLACE);
		fClientViewer = (TreeModelViewer)((IDebugView)context.getPart()).getViewer();
	}

	protected VirtualTreeModelViewer initVirtualViewer(TreeModelViewer clientViewer, VirtualViewerListener listener) {
		Object input = clientViewer.getInput();
		ModelDelta stateDelta = new ModelDelta(input, IModelDelta.NO_CHANGE);
		clientViewer.saveElementState(TreePath.EMPTY, stateDelta, IModelDelta.EXPAND);
		listener.fRemainingUpdatesCount = calcUpdatesCount(stateDelta);
		VirtualTreeModelViewer fVirtualViewer = new VirtualTreeModelViewer(
			clientViewer.getDisplay(),
			SWT.NONE,
			makeVirtualPresentationContext(clientViewer.getPresentationContext()));
		fVirtualViewer.addViewerUpdateListener(listener);
		fVirtualViewer.addLabelUpdateListener(listener);
		fVirtualViewer.setInput(input);
		if (fVirtualViewer.canToggleColumns()) {
			fVirtualViewer.setShowColumns(clientViewer.isShowColumns());
		}
		fVirtualViewer.updateViewer(stateDelta);
		return fVirtualViewer;
	}

	protected IPresentationContext makeVirtualPresentationContext(final IPresentationContext clientViewerContext) {
		return new PresentationContext(clientViewerContext.getId()) {

			{
				String[] clientProperties = clientViewerContext.getProperties();
				for (int i = 0; i < clientProperties.length; i++) {
					setProperty(clientProperties[i], clientViewerContext.getProperty(clientProperties[i]));
				}

			}

			@Override
			public String[] getColumns() {
				String[] clientColumns = super.getColumns();

				if (clientColumns == null || clientColumns.length == 0) {
					// No columns are used.
					return null;
				}

				// Try to find the name column.
				for (int i = 0; i < clientColumns.length; i++) {
					if (IDebugUIConstants.COLUMN_ID_VARIABLE_NAME.equals(clientColumns[i])) {
						return new String[] { IDebugUIConstants.COLUMN_ID_VARIABLE_NAME };
					}
				}

				return new String[] { clientColumns[0] };
			}
		};
	}

	@Override
	public void run() {
		final VirtualViewerListener listener = new VirtualViewerListener();
		VirtualTreeModelViewer virtualViewer = initVirtualViewer(fClientViewer, listener);

		ProgressMonitorDialog dialog = new TimeTriggeredProgressMonitorDialog(fClientViewer.getControl().getShell(), 500);
		final IProgressMonitor monitor = dialog.getProgressMonitor();
		dialog.setCancelable(true);

		try {
			dialog.run(
				true, true,
				new IRunnableWithProgress() {
					@Override
					public void run(final IProgressMonitor m) throws InvocationTargetException, InterruptedException {
						synchronized(listener) {
							listener.fProgressMonitor = m;
							listener.fProgressMonitor.beginTask(DebugUIPlugin.removeAccelerators(getText()), listener.fRemainingUpdatesCount);
						}

						while ((!listener.fLabelUpdatesComplete || !listener.fViewerUpdatesComplete) && !listener.fProgressMonitor.isCanceled()) {
							Thread.sleep(1);
						}
						synchronized(listener) {
							listener.fProgressMonitor = null;
						}
					}
				});
		} catch (InvocationTargetException e) {
			DebugUIPlugin.log(e);
			return;
		} catch (InterruptedException e) {
			return;
		}

		VirtualItem root = virtualViewer.getTree();
		if (!monitor.isCanceled()) {
			List<VirtualItem> list = new ArrayList<>();
			collectAllChildren(root, list);
			FindLabelProvider labelProvider = new FindLabelProvider(virtualViewer, list);
			VirtualItem result = performFind(list, labelProvider);
			if (result != null) {
				setSelectionToClient(virtualViewer, labelProvider, result);
			}
		}

		virtualViewer.removeLabelUpdateListener(listener);
		virtualViewer.removeViewerUpdateListener(listener);
		virtualViewer.dispose();
	}

	private int calcUpdatesCount(IModelDelta stateDelta) {
		final int[] count = new int[] {0};
		stateDelta.accept( new IModelDeltaVisitor() {
			@Override
			public boolean visit(IModelDelta delta, int depth) {
				if ((delta.getFlags() & IModelDelta.EXPAND) != 0) {
					count[0] += delta.getChildCount();
					return true;
				}
				return false;
			}
		});

		// Double it to account for separate element and label update ticks.
		return count[0] * 2;
	}

	private void collectAllChildren(VirtualItem element, List<VirtualItem> collect) {
		VirtualItem[] children = element.getItems();
		if (children != null) {
			for (int i = 0; i < children.length; i++) {
				if (!children[i].needsLabelUpdate()) {
					collect.add(children[i]);
					collectAllChildren(children[i], collect);
				}
			}
		}
	}

	protected VirtualItem performFind(List<VirtualItem> items, FindLabelProvider labelProvider) {
		FindElementDialog dialog = new FindElementDialog(
			fClientViewer.getControl().getShell(),
			labelProvider,
			items.toArray());
		dialog.setTitle("PDA Variables View Find"); //$NON-NLS-1$
		dialog.setMessage("&Specify an element to select (? = any character, * = any String):"); //$NON-NLS-1$
		if (dialog.open() == Window.OK) {
			Object[] elements = dialog.getResult();
			if (elements.length == 1) {
				return (VirtualItem)elements[0];
			}
		}
		return null;
	}

	protected void setSelectionToClient(VirtualTreeModelViewer virtualViewer, ILabelProvider labelProvider, VirtualItem findItem) {
		virtualViewer.getTree().setSelection(new VirtualItem[] { findItem } );
		ModelDelta stateDelta = new ModelDelta(virtualViewer.getInput(), IModelDelta.NO_CHANGE);
		virtualViewer.saveElementState(TreePath.EMPTY, stateDelta, IModelDelta.SELECT);
		fClientViewer.updateViewer(stateDelta);

		ISelection selection = fClientViewer.getSelection();
		if (!selection.isEmpty() &&
			selection instanceof IStructuredSelection &&
			((IStructuredSelection)selection).getFirstElement().equals(findItem.getData()) ) {
		} else {
			DebugUIPlugin.errorDialog(
				fClientViewer.getControl().getShell(),
 "Error", //$NON-NLS-1$
					"Could not select item:" + labelProvider.getText(findItem), //$NON-NLS-1$
					new Status(IStatus.ERROR, DebugUIPlugin.getUniqueIdentifier(), "Element no longer in viewer.")); //$NON-NLS-1$
		}
	}

	@Override
	public void update() {
		setEnabled( fClientViewer.getInput() != null && fClientViewer.getChildCount(TreePath.EMPTY) > 0 );
	}

}
