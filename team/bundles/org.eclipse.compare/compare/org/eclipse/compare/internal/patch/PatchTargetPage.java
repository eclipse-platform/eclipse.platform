/*******************************************************************************
 * Copyright (c) 2005, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.internal.patch;

import java.text.MessageFormat;

import org.eclipse.compare.internal.ICompareContextIds;
import org.eclipse.compare.internal.Utilities;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.navigator.ResourceComparator;

/***
 * This page only shows up if the user is trying to apply
 * a non-workspace rooted patch.
 */
public class PatchTargetPage extends WizardPage {

	private boolean fShowError = false;

	// SWT widgets
	private TreeViewer fPatchTargets;
	private Button useWorkspaceAsTarget;
	private Button selectTarget;

	protected WorkspacePatcher fPatcher;

	protected final static String PATCHTARGETPAGE_NAME = "PatchTargetPage"; //$NON-NLS-1$

	public PatchTargetPage(WorkspacePatcher patcher) {
		super(PATCHTARGETPAGE_NAME, PatchMessages.PatchTargetPage_title, null);
		setMessage(PatchMessages.PatchTargetPage_message);
		fPatcher = patcher;
	}

	/*
	 * Get a path from the supplied text widget.
	 * @return org.eclipse.core.runtime.IPath
	 */
	protected IPath getPathFromText(Text textField) {
		return (IPath.fromOSString(textField.getText())).makeAbsolute();
	}

	@Override
	public void createControl(Composite parent) {

		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));
		setControl(composite);

		useWorkspaceAsTarget = createRadioButton(composite, PatchMessages.PatchTargetPage_0, 1);
		selectTarget = createRadioButton(composite, PatchMessages.InputPatchPage_SelectInput, 1);

		buildInputGroup(composite);

		updateWidgetEnablements();

		Dialog.applyDialogFont(composite);
		if (PlatformUI.isWorkbenchRunning()) {
			PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, ICompareContextIds.PATCH_INPUT_WIZARD_PAGE);
		}

		useWorkspaceAsTarget.addListener(SWT.Selection, event -> {
			fShowError = true;
			if (useWorkspaceAsTarget.getSelection()) {
				fPatchTargets.getTree().setEnabled(false);
				fPatcher.setTarget(ResourcesPlugin.getWorkspace().getRoot());
			} else {
				fPatchTargets.getTree().setEnabled(true);
				fPatcher.setTarget(Utilities.getFirstResource(fPatchTargets.getSelection()));
			}
			markPreviewPageToRecalucateIfNonWorkspacePatch();
			updateWidgetEnablements();
		});
	}

	private void markPreviewPageToRecalucateIfNonWorkspacePatch() {
		if (!fPatcher.isWorkspacePatch()) {
			PreviewPatchPage2 previewPage = (PreviewPatchPage2) getWizard()
					.getPage(PreviewPatchPage2.PREVIEWPATCHPAGE_NAME);
			previewPage.pageRecalculate = true;
		}
	}

	private Button createRadioButton(Composite parent, String label, int span) {
		Button button = new Button(parent, SWT.RADIO);
		button.setText(label);
		GridData data = new GridData();
		data.horizontalSpan = span;
		button.setLayoutData(data);
		return button;
	}

	@Override
	public IWizardPage getNextPage() {

		// if selected target is file ensure that patch file
		// contains only a patch for a single file
		if (!fPatcher.isWorkspacePatch() && fPatcher.getTarget() instanceof IFile && fPatcher.getDiffs().length > 1) {
			InputPatchPage inputPage = (InputPatchPage) getWizard().getPage(InputPatchPage.INPUTPATCHPAGE_NAME);
			String source = ""; //$NON-NLS-1$
			switch (inputPage.getInputMethod()) {
				case InputPatchPage.CLIPBOARD :
					source = PatchMessages.InputPatchPage_Clipboard_title;
					break;

				case InputPatchPage.FILE :
					source = PatchMessages.InputPatchPage_PatchFile_title;
					break;

				case InputPatchPage.WORKSPACE :
					source = PatchMessages.InputPatchPage_WorkspacePatch_title;
					break;
			}
			String format = PatchMessages.InputPatchPage_SingleFileError_format;
			String message = MessageFormat.format(format, source);
			MessageDialog.openInformation(null, PatchMessages.InputPatchPage_PatchErrorDialog_title, message);
			return this;
		}

		return super.getNextPage();
	}

	@Override
	public boolean canFlipToNextPage() {
		// we can't call getNextPage to determine if flipping is allowed since computing
		// the next page is quite expensive. So we say yes if the page is complete.
		return isPageComplete();
	}

	private void buildInputGroup(Composite parent) {
		Tree tree = new Tree(parent, SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 200;
		tree.setLayoutData(gd);

		fPatchTargets = new TreeViewer(tree);
		fPatchTargets.setLabelProvider(new WorkbenchLabelProvider());
		fPatchTargets.setContentProvider(new WorkbenchContentProvider());
		fPatchTargets.setComparator(new ResourceComparator(ResourceComparator.NAME));
		fPatchTargets.setInput(ResourcesPlugin.getWorkspace().getRoot());

		IResource target = fPatcher.getTarget();
		if (target != null && !(target instanceof IWorkspaceRoot)) {
			fPatchTargets.expandToLevel(target, 0);
			fPatchTargets.setSelection(new StructuredSelection(target));
		}

		// register listeners
		fPatchTargets.addSelectionChangedListener(event -> {
			fShowError = true;
			fPatcher.setTarget(Utilities.getFirstResource(event.getSelection()));
			updateWidgetEnablements();
		});

		fPatchTargets.addDoubleClickListener(event -> ((PatchWizard)getWizard()).showPage(getNextPage()));
	}

	/**
	 * Updates the enable state of this page's controls.
	 */
	private void updateWidgetEnablements() {
		String error = null;

		if (fPatcher.getTarget() == null) {
			useWorkspaceAsTarget.setSelection(false);
			selectTarget.setSelection(true);
			error = PatchMessages.InputPatchPage_NothingSelected_message;
			setPageComplete(false);
			if (fShowError)
				setErrorMessage(error);
			return;
		}
		setErrorMessage(null);
		useWorkspaceAsTarget.setSelection(fPatcher.getTarget() instanceof IWorkspaceRoot);
		selectTarget.setSelection(!useWorkspaceAsTarget.getSelection());
		setPageComplete(true);
	}

	/**
	 *	The Finish button was pressed. Try to do the required work now and answer
	 *	a boolean indicating success. If false is returned then the wizard will
	 *	not close.
	 *
	 * @return boolean
	 */
	public boolean finish() {
		return true;
	}

}
