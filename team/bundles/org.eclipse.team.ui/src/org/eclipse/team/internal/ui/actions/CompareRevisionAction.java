/*******************************************************************************
 * Copyright (c) 2006, 2010 IBM Corporation and others.
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

package org.eclipse.team.internal.ui.actions;

import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.ITypedElement;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.OpenStrategy;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.history.provider.FileRevision;
import org.eclipse.team.internal.core.history.LocalFileRevision;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.history.AbstractHistoryCategory;
import org.eclipse.team.internal.ui.history.CompareFileRevisionEditorInput;
import org.eclipse.team.internal.ui.history.FileRevisionTypedElement;
import org.eclipse.team.ui.history.HistoryPage;
import org.eclipse.team.ui.synchronize.SaveableCompareEditorInput;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IReusableEditor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.actions.BaseSelectionListenerAction;

public class CompareRevisionAction extends BaseSelectionListenerAction {
	HistoryPage page;
	IStructuredSelection selection;
	IFileRevision currentFileRevision;

	public CompareRevisionAction(String text, HistoryPage page) {
		super(text);
		this.page = page;
	}

	public CompareRevisionAction(HistoryPage page) {
		this(TeamUIMessages.LocalHistoryPage_CompareAction, page);
	}

	@Override
	public void run() {
		IStructuredSelection structSel = selection;

		if (structSel == null)
			return;

		Object[] objArray = structSel.toArray();

		IFileRevision file1 = null;
		IFileRevision file2 = null;

		switch (structSel.size()){
			case 1:
				file1 = getCurrentFileRevision();
				Object tempRevision = objArray[0];
				if (tempRevision instanceof IFileRevision) {
					file2 = (IFileRevision) tempRevision;
				} else {
					return;
				}
			break;

			case 2:
				Object tempRevision2 = objArray[0];
				Object tempRevision3 = objArray[1];

				if (tempRevision2 instanceof IFileRevision &&
					tempRevision3 instanceof IFileRevision){
					file1 = (IFileRevision) objArray[0];
					file2 = (IFileRevision) objArray[1];
				} else {
					return;
				}
			break;
		}

		if (file1 == null || file2 == null ||
			!file1.exists() || !file2.exists()){
			MessageDialog.openError(page.getSite().getShell(), TeamUIMessages.OpenRevisionAction_DeletedRevTitle, TeamUIMessages.CompareRevisionAction_DeleteCompareMessage);
			return;
		}

		IResource resource = getResource(file2);
		if (resource != null) {
			IFileRevision temp = file1;
			file1 = file2;
			file2 = temp;
		}
		ITypedElement left;
		resource = getResource(file1);
		if (resource != null) {
			left =  getElementFor(resource);
		} else {
			left = new FileRevisionTypedElement(file1, getLocalEncoding());
		}
		ITypedElement right = new FileRevisionTypedElement(file2, getLocalEncoding());

		openInCompare(left, right);
	}

	private String getLocalEncoding() {
		IResource resource = getResource(getCurrentFileRevision());
		if (resource instanceof IFile) {
			IFile file = (IFile) resource;
			try {
				return file.getCharset();
			} catch (CoreException e) {
				TeamUIPlugin.log(e);
			}
		}
		return null;
	}

	protected ITypedElement getElementFor(IResource resource) {
		return SaveableCompareEditorInput.createFileElement((IFile)resource);
	}

	private void openInCompare(ITypedElement left, ITypedElement right) {
		CompareEditorInput input = createCompareEditorInput(left, right, page.getSite().getPage());
		IWorkbenchPage workBenchPage = page.getSite().getPage();
		IEditorPart editor = Utils.findReusableCompareEditor(input,
				workBenchPage,
				new Class[] { CompareFileRevisionEditorInput.class });
		if (editor != null) {
			IEditorInput otherInput = editor.getEditorInput();
			if (otherInput.equals(input)) {
				// simply provide focus to editor
				if (OpenStrategy.activateOnOpen())
					workBenchPage.activate(editor);
				else
					workBenchPage.bringToTop(editor);
			} else {
				// if editor is currently not open on that input either re-use
				// existing
				CompareUI.reuseCompareEditor(input, (IReusableEditor) editor);
				if (OpenStrategy.activateOnOpen())
					workBenchPage.activate(editor);
				else
					workBenchPage.bringToTop(editor);
			}
		} else {
			CompareUI.openCompareEditor(input, OpenStrategy.activateOnOpen());
		}
	}

	protected CompareFileRevisionEditorInput createCompareEditorInput(
			ITypedElement left, ITypedElement right, IWorkbenchPage page) {
		return new CompareFileRevisionEditorInput(left,
				right, page);
	}

	private IResource getResource(IFileRevision revision) {
		if (revision instanceof LocalFileRevision) {
			LocalFileRevision local = (LocalFileRevision) revision;
			return local.getFile();
		}
		return null;
	}

	private IFileRevision getCurrentFileRevision() {
		return currentFileRevision;
	}

	public void setCurrentFileRevision(IFileRevision fileRevision){
		this.currentFileRevision = fileRevision;
	}

	/**
	 * DO NOT REMOVE, used in a product.
	 *
	 * @deprecated As of 3.5, replaced by
	 *             {@link Utils#findReusableCompareEditor(CompareEditorInput, IWorkbenchPage, Class[])}
	 */
	@SuppressWarnings("javadoc")
	@Deprecated
	public static IEditorPart findReusableCompareEditor(IWorkbenchPage workbenchPage) {
		return Utils.findReusableCompareEditor(null, workbenchPage,
				new Class[] { CompareFileRevisionEditorInput.class });
	}

	@Override
	protected boolean updateSelection(IStructuredSelection selection) {
		this.selection = selection;
		if (selection.size() == 1){
			Object el = selection.getFirstElement();
			if (el instanceof LocalFileRevision)
				this.setText(TeamUIMessages.CompareRevisionAction_Local);
			else if (el instanceof FileRevision){
				FileRevision tempFileRevision = (FileRevision) el;
				this.setText(NLS.bind(TeamUIMessages.CompareRevisionAction_Revision, new String[]{tempFileRevision.getContentIdentifier()}));
			}
			else
				this.setText(TeamUIMessages.CompareRevisionAction_CompareWithCurrent);
			return shouldShow();
		}
		else if (selection.size() == 2){
			this.setText(TeamUIMessages.CompareRevisionAction_CompareWithOther);
			return shouldShow();
		}

		return false;
	}

	private boolean shouldShow() {
		IStructuredSelection structSel = selection;
		Object[] objArray = structSel.toArray();

		if (objArray.length == 0)
			return false;

		for (Object obj : objArray) {
			//Don't bother showing if this a category
			if (obj instanceof AbstractHistoryCategory) {
				return false;
			}
			IFileRevision revision = (IFileRevision) obj;
			//check to see if any of the selected revisions are deleted revisions
			if (revision != null && !revision.exists())
				return false;
		}

		return true;
	}
}
