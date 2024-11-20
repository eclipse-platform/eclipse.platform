/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.history;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.CompareViewerPane;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFileState;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.history.HistoryPageCompareEditorInput;
import org.eclipse.team.ui.history.IHistoryPageSource;
import org.eclipse.ui.part.IPage;

public class ReplaceLocalHistory extends ShowLocalHistory {

	@Override
	public void run(IAction action) {
		final IFile file = (IFile) getSelection().getFirstElement();
		IFileState states[]= getLocalHistory();
		if (states == null || states.length == 0)
			return;
		Runnable r = () -> showCompareInDialog(getShell(), file);
		TeamUIPlugin.getStandardDisplay().asyncExec(r);
	}

	private void showCompareInDialog(Shell shell, Object object){
		IHistoryPageSource pageSource = LocalHistoryPageSource.getInstance();
		if (pageSource != null && pageSource.canShowHistoryFor(object)) {
			CompareConfiguration cc = new CompareConfiguration();
			cc.setLeftEditable(false);
			cc.setRightEditable(false);
			HistoryPageCompareEditorInput input = new HistoryPageCompareEditorInput(cc, pageSource, object) {
				@Override
				public boolean isEditionSelectionDialog() {
					return true;
				}
				@Override
				public String getOKButtonLabel() {
					return TeamUIMessages.ReplaceLocalHistory_0;
				}

				@Override
				protected IPage createPage(CompareViewerPane parent, IToolBarManager toolBarManager) {
					var page = super.createPage(parent, toolBarManager, false);
					Tree tree = getTree(page);
					runDefaultSelectionEventOnSelectionChange(tree);
					return page;
				}

				private void runDefaultSelectionEventOnSelectionChange(Tree tree) {
					if (tree == null) {
						return;
					}
					var handler = new NotifyDefaultSelectionOnWidgetSelectedHandler(tree);
					tree.addSelectionListener(handler);
					tree.addMouseListener(handler);
				}

				private Tree getTree(IPage page) {
					Control control = page.getControl();
					if (!(control instanceof Composite composite)) {
						return null;
					}
					Control[] children = composite.getChildren();
					if (children == null) {
						return null;
					}
					for (Control child : children) {
						if (child instanceof Tree t) {
							return t;
						}
					}
					return null;
				}
				@Override
				public boolean okPressed() {
					try {
						Object o = getSelectedEdition();
						FileRevisionTypedElement right = (FileRevisionTypedElement) ((ICompareInput)o).getRight();
						IFile file = (IFile)getCompareResult();
						file.setContents(right.getContents(), false, true, null);
					} catch (CoreException e) {
						Utils.handle(e);
						return false;
					}
					return true;
				}
			};
			CompareUI.openCompareDialog(input);
		}
	}

	@Override
	protected String getPromptTitle() {
		return TeamUIMessages.ReplaceLocalHistory_1;
	}

	private static final class NotifyDefaultSelectionOnWidgetSelectedHandler extends SelectionAdapter
			implements MouseListener {
		private boolean mouseDownPressed = false;
		private Runnable runOnMouseUp;
		private final Tree tree;

		public NotifyDefaultSelectionOnWidgetSelectedHandler(Tree tree) {
			this.tree = tree;
		}
		@Override
		public void widgetSelected(SelectionEvent e) {
			if (tree.getSelectionCount() != 1) {
				return;
			}
			Runnable r = () -> {
				Event event = new Event();
				event.item = e.item;
				tree.notifyListeners(SWT.DefaultSelection, event);
			};
			if (mouseDownPressed) {
				// run DefaultSelection event after mouseUp
				runOnMouseUp = r;
				return;
			}
			r.run();
		}

		@Override
		public void mouseDoubleClick(MouseEvent e) {
		}

		@Override
		public void mouseDown(MouseEvent e) {
			mouseDownPressed = true;
		}

		@Override
		public void mouseUp(MouseEvent e) {
			mouseDownPressed = false;
			if (runOnMouseUp != null) {
				runOnMouseUp.run();
				runOnMouseUp = null;
			}
		}
	}
}
