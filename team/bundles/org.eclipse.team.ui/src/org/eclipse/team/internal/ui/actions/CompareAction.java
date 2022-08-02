/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.ITypedElement;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.history.CompareFileRevisionEditorInput;
import org.eclipse.team.internal.ui.synchronize.SaveablesCompareEditorInput;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IReusableEditor;
import org.eclipse.ui.IWorkbenchPage;

public class CompareAction extends TeamAction {

	@Override
	protected void execute(IAction action) throws InvocationTargetException,
			InterruptedException {

		IResource[] selectedResources = getSelectedResources();

		ITypedElement ancestor = null;
		ITypedElement left = null;
		ITypedElement right = null;

		if (selectedResources.length == 2) {
			if (selectedResources[0] != null)
				left = getElementFor(selectedResources[0]);

			if (selectedResources[1] != null)
				right = getElementFor(selectedResources[1]);

		} else if (selectedResources.length == 3) {
			// prompt for ancestor
			SelectAncestorDialog dialog = new SelectAncestorDialog(getShell(),
					selectedResources);
			int code = dialog.open();
			if (code != Window.OK)
				return;

			ancestor = getElementFor(dialog.ancestorResource);
			left = getElementFor(dialog.leftResource);
			right = getElementFor(dialog.rightResource);
		} else {
			return;
		}
		openInCompare(ancestor, left, right);
	}

	private void openInCompare(ITypedElement ancestor, ITypedElement left,
			ITypedElement right) {
		IWorkbenchPage workBenchPage = getTargetPage();
		CompareEditorInput input = new SaveablesCompareEditorInput(ancestor,
				left, right, workBenchPage);
		IEditorPart editor = Utils.findReusableCompareEditor(input,
				workBenchPage,
				new Class[] { CompareFileRevisionEditorInput.class });
		if (editor != null) {
			IEditorInput otherInput = editor.getEditorInput();
			if (otherInput.equals(input)) {
				// simply provide focus to editor
				workBenchPage.activate(editor);
			} else {
				// if editor is currently not open on that input either re-use
				// existing
				CompareUI.reuseCompareEditor(input, (IReusableEditor) editor);
				workBenchPage.activate(editor);
			}
		} else {
			CompareUI.openCompareEditor(input);
		}
	}

	@Override
	public boolean isEnabled() {
		int l = getSelectedResources().length;
		return l == 2 || l == 3;
	}

	private ITypedElement getElementFor(IResource resource) {
		return SaveablesCompareEditorInput.createFileElement((IFile) resource);
	}

	// see
	// org.eclipse.compare.internal.ResourceCompareInput.SelectAncestorDialog
	private static class SelectAncestorDialog extends MessageDialog {
		private IResource[] theResources;
		IResource ancestorResource;
		IResource leftResource;
		IResource rightResource;

		private Button[] buttons;

		public SelectAncestorDialog(Shell parentShell, IResource[] theResources) {
			super(parentShell, TeamUIMessages.SelectAncestorDialog_title, null,
					TeamUIMessages.SelectAncestorDialog_message,
					MessageDialog.QUESTION, new String[] {
							IDialogConstants.OK_LABEL,
							IDialogConstants.CANCEL_LABEL }, 0);
			this.theResources = theResources;
		}

		@Override
		protected Control createCustomArea(Composite parent) {
			Composite composite = new Composite(parent, SWT.NONE);
			composite.setLayout(new GridLayout());
			buttons = new Button[3];
			for (int i = 0; i < 3; i++) {
				buttons[i] = new Button(composite, SWT.RADIO);
				buttons[i].addSelectionListener(selectionListener);
				buttons[i].setText(NLS.bind(
						TeamUIMessages.SelectAncestorDialog_option,
						theResources[i].getFullPath().toPortableString()));
				buttons[i].setFont(parent.getFont());
				// set initial state
				buttons[i].setSelection(i == 0);
			}
			pickAncestor(0);
			return composite;
		}

		private void pickAncestor(int i) {
			ancestorResource = theResources[i];
			leftResource = theResources[i == 0 ? 1 : 0];
			rightResource = theResources[i == 2 ? 1 : 2];
		}

		private SelectionListener selectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Button selectedButton = (Button) e.widget;
				if (!selectedButton.getSelection())
					return;
				for (int i = 0; i < 3; i++)
					if (selectedButton == buttons[i])
						pickAncestor(i);
			}
		};
	}

}
