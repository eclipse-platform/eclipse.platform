/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation
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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.model.Breakpoint;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.actions.ActionMessages;
import org.eclipse.debug.internal.ui.viewers.model.provisional.TreeModelViewer;
import org.eclipse.debug.internal.ui.views.breakpoints.BreakpointsView;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

public class BreakpointLabelAction implements IViewActionDelegate {

	private IViewPart fView;
	protected IViewPart getView() {
		return fView;
	}

	protected void setView(IViewPart view) {
		fView = view;
	}

	@Override
	public void run(IAction action) {
		String emptyString = ""; //$NON-NLS-1$
		IStructuredSelection selection = getSelection();

		if (selection instanceof TreeSelection treeSelect && selection.getFirstElement() instanceof Breakpoint breakpoint) {
			if (treeSelect.size() == 1) {
				IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				IViewPart viewPart = page.findView(IDebugUIConstants.ID_BREAKPOINT_VIEW);
				if (viewPart instanceof BreakpointsView breakpointView) {
					TreeModelViewer treeViewer = breakpointView.getTreeModelViewer();
					Widget item = treeViewer.findItem(treeSelect.getPaths()[0]);
					if (item instanceof TreeItem tree) {
						String current = tree.getText();
						Rectangle bounds;
						try {
							bounds = tree.getBounds();
						} catch (ArrayIndexOutOfBoundsException e) { // TreeItem having FontData [Breakpoints having
																		// custom label]
							tree.setFont(null);
							GC gc = new GC(tree.getParent());
							Font currentFont = gc.getFont();
							gc.setFont(currentFont);
							Point textWidth = gc.textExtent(tree.getText());
							gc.dispose();
							bounds = tree.getBounds(0);
							bounds.x = bounds.x + 10;
							bounds.width = textWidth.x + 20;

						}
						Label label = new Label(tree.getParent(), SWT.WRAP);
						label.setText(ActionMessages.BreakpointLabelDialog);
						label.setBounds(bounds.x, bounds.y - 20, label.computeSize(SWT.DEFAULT, SWT.DEFAULT).x,
								label.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);

						Text inlineEditor = new Text(tree.getParent(), SWT.BORDER);
						inlineEditor.setBounds(bounds.x, bounds.y, bounds.width, bounds.height);
						inlineEditor.setText(current);
						inlineEditor.selectAll();
						inlineEditor.setFocus();

						inlineEditor.addListener(SWT.FocusOut, event -> {
							tree.setText(current);
							label.dispose();
							inlineEditor.dispose();

						});
						inlineEditor.addKeyListener(new KeyAdapter() {
							@Override
							public void keyPressed(KeyEvent e) {
								if (e.keyCode == SWT.ESC) {
									tree.setText(current);
									inlineEditor.dispose();
									label.dispose();
								} else if (e.keyCode == SWT.CR) {
									String newLabel = inlineEditor.getText();
									if (!newLabel.isEmpty() && !newLabel.equals(current)) {
										try {
											breakpoint.setBreakpointLabel(newLabel);
										} catch (CoreException e1) {
											DebugUIPlugin.log(e1);
										}
									} else if (newLabel.isEmpty()) {
										try {
											breakpoint.setBreakpointLabel(null); // Set to default
										} catch (CoreException e2) {
											DebugUIPlugin.log(e2);
										}
									}
									inlineEditor.dispose();
									label.dispose();
								}
							}
						});
						tree.setText(emptyString);

					}

				}
			}
		}

	}

	protected IStructuredSelection getSelection() {
		return (IStructuredSelection) getView().getViewSite().getSelectionProvider().getSelection();
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
	}

	@Override
	public void init(IViewPart view) {
		setView(view);
	}

}
