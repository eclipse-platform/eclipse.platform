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
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

public class BreakpointLabelAction implements IViewActionDelegate {

	private IViewPart fView;
	protected IViewPart getView() {
		return fView;
	}

	protected void setView(IViewPart view) {
		fView = view;
	}

	private static record InlineEditor(Label label, Text text) {
		void dispose() {
			label.dispose();
			text.dispose();
		}
	}

	@Override
	public void run(IAction action) {
		IStructuredSelection selection = getSelection();
		if (!(selection instanceof TreeSelection treeSelect)
				|| treeSelect.size() != 1
				|| !(selection.getFirstElement() instanceof Breakpoint breakpoint)
				|| !(fView instanceof BreakpointsView breakpointView)) {
			return;
		}
		TreeModelViewer treeViewer = breakpointView.getTreeModelViewer();
		Widget item = treeViewer.findItem(treeSelect.getPaths()[0]);
		if (!(item instanceof TreeItem treeItem)) {
			return;
		}
		Rectangle editorBounds = computeInlineEditorBounds(treeItem);

		Label label = new Label(treeItem.getParent(), SWT.WRAP);
		label.setText(ActionMessages.BreakpointLabelDialog);
		Point defaultLabelSize = label.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		label.setBounds(editorBounds.x, editorBounds.y - 20, defaultLabelSize.x, defaultLabelSize.y);
		label.setBackground(treeItem.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));

		String current = treeItem.getText();
		Text textEditor = new Text(treeItem.getParent(), SWT.BORDER);
		textEditor.setBounds(editorBounds.x, editorBounds.y, editorBounds.width, editorBounds.height);
		textEditor.setText(current);
		textEditor.selectAll();
		textEditor.setFocus();

		final InlineEditor inlineEditor = new InlineEditor(label, textEditor);
		textEditor.addListener(SWT.FocusOut, event -> inlineEditor.dispose());
		textEditor.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.ESC) {
					inlineEditor.dispose();
				} else if (e.keyCode == SWT.CR) {
					String newLabel = textEditor.getText().strip();
					if (newLabel.equals(current)) {
						inlineEditor.dispose();
						return;
					}
					if (newLabel.isEmpty()) {
						newLabel = null; // reset to default breakpoint label
					}
					try {
						breakpoint.setBreakpointLabel(newLabel);
					} catch (CoreException e1) {
						DebugUIPlugin.log(e1);
					}
					inlineEditor.dispose();
				}
			}
		});
	}

	private static Rectangle computeInlineEditorBounds(TreeItem treeItem) {
		Rectangle bounds;
		try {
			bounds = treeItem.getBounds();
		} catch (ArrayIndexOutOfBoundsException e) {
			// TreeItem having FontData [Breakpoints having custom label]
			bounds = macBugWorkaround(treeItem);
		}
		int editorWidth = Math.max(computeEditorExtent(treeItem), bounds.width);
		return new Rectangle(bounds.x, bounds.y, editorWidth, bounds.height);
	}

	// Workaround for SWT bug on Mac where TreeItem.getBounds() throws exception
	// when custom fonts are used, see
	// https://github.com/eclipse-platform/eclipse.platform.swt/issues/2749
	private static Rectangle macBugWorkaround(TreeItem treeItem) {
		treeItem.setFont(null);
		Rectangle bounds = treeItem.getBounds(0);
		bounds.x = bounds.x + 10;
		bounds.width = computeEditorExtent(treeItem);
		return bounds;
	}

	private static int computeEditorExtent(TreeItem treeItem) {
		GC gc = new GC(treeItem.getParent());
		try {
			Point textWidth = gc.textExtent(treeItem.getText());
			// Editor needs space on both sides around text
			int width = textWidth.x + 20;
			// Give editor more room to grow for new text
			width = Math.max(100, width);
			return width;
		} finally {
			gc.dispose();
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
