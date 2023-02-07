/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
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

package org.eclipse.ui.internal.cheatsheets.composite.explorer;


import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.internal.cheatsheets.composite.parser.ICompositeCheatsheetTags;
import org.eclipse.ui.internal.provisional.cheatsheets.ICompositeCheatSheet;
import org.eclipse.ui.internal.provisional.cheatsheets.ICompositeCheatSheetTask;
import org.eclipse.ui.internal.provisional.cheatsheets.TaskExplorer;

public class TreeTaskExplorer extends TaskExplorer {
	private TreeViewer viewer;


	@Override
	public String getId() {
		return ICompositeCheatsheetTags.TREE;
	}

	@Override
	public void createControl(Composite parent, FormToolkit toolkit) {
		Tree tree = new Tree(parent, toolkit.getOrientation());
		toolkit.adapt(tree, false, false);
		viewer = new TreeViewer(tree);
		viewer.setContentProvider(new TreeContentProvider());
		viewer.setLabelProvider(new TreeLabelProvider());
	}

	@Override
	public void taskUpdated(ICompositeCheatSheetTask task) {
		viewer.update(task, null);
	}

	@Override
	public Control getControl() {
		return viewer.getControl();
	}

	@Override
	public void setFocus() {
		viewer.getControl().setFocus();
	}

	@Override
	public ISelectionProvider getSelectionProvider() {
		return viewer;
	}

	@Override
	public void setCompositeCheatSheet(ICompositeCheatSheet compositeCheatSheet) {
		viewer.setInput(compositeCheatSheet);
	}

	@Override
	public void dispose() {
		viewer.getLabelProvider().dispose();
	}

	@Override
	public void setSelection(ISelection selection, boolean reveal) {
		viewer.setSelection(selection, reveal);
	}

}