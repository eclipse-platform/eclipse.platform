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

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.cheatsheets.CheatSheetPlugin;
import org.eclipse.ui.internal.cheatsheets.ICheatSheetResource;
import org.eclipse.ui.internal.cheatsheets.Messages;
import org.eclipse.ui.internal.cheatsheets.composite.model.AbstractTask;

public class ConfirmRestartDialog extends Dialog {

	public class TaskLabelProvider extends LabelProvider implements ITableLabelProvider {
		@Override
		public String getColumnText(Object obj, int index) {
			return treeLabelProvider.getText(obj);
		}

		@Override
		public Image getColumnImage(Object obj, int index) {
			return treeLabelProvider.getImage(obj);
		}

		@Override
		public Image getImage(Object obj) {
			return PlatformUI.getWorkbench().
					getSharedImages().getImage(ISharedImages.IMG_OBJ_ELEMENT);
		}

	}

	public static class TaskContentProvider  implements IStructuredContentProvider {

		Object[] input;

		@Override
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
			input = (Object[])newInput;
		}

		@Override
		public void dispose() {
		}

		@Override
		public Object[] getElements(Object parent) {
			return input;
		}
	}

	private AbstractTask[] tasks;

	private TreeLabelProvider treeLabelProvider;

	protected ConfirmRestartDialog(Shell parentShell, AbstractTask[] restartTasks,
								   TreeLabelProvider treeLabelProvider) {
		super(parentShell);
		this.tasks = restartTasks;
		this.treeLabelProvider = treeLabelProvider;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayout(new GridLayout());
		Label header = new Label(composite,SWT.NULL);
		header.setText(Messages.COMPOSITE_RESET_TASK_DIALOG_MESSAGE);
		TableViewer viewer = new TableViewer(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		viewer.setContentProvider(new TaskContentProvider());
		viewer.setLabelProvider(new TaskLabelProvider());
		viewer.setInput(tasks);
		GridData taskData = new GridData();
		taskData.widthHint = 400;
		taskData.heightHint = 200;
		taskData.horizontalAlignment = SWT.FILL;
		taskData.verticalAlignment = SWT.FILL;
		viewer.getControl().setLayoutData(taskData);
		return composite;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setImage(CheatSheetPlugin.getPlugin().getImage(ICheatSheetResource.CHEATSHEET_RETURN));
		setShellStyle(getShellStyle() | SWT.RESIZE);
		newShell.setText(Messages.COMPOSITE_RESET_TASK_DIALOG_TITLE);
	}

}
