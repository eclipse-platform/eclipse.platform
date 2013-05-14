/*******************************************************************************
 * Copyright (c) 2006, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.internal.patch;

import java.util.ArrayList;

import org.eclipse.compare.internal.core.patch.DiffProject;
import org.eclipse.compare.internal.core.patch.FilePatch2;
import org.eclipse.compare.internal.core.patch.Hunk;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.navigator.ResourceComparator;

class RetargetPatchElementDialog extends Dialog {
	
	private static class RetargetPatchContentProvider extends BaseWorkbenchContentProvider {
		private final PatchDiffNode node;
		public RetargetPatchContentProvider(PatchDiffNode node) {
			 this.node = node;
		}
		public Object[] getChildren(Object element) {
			if (element instanceof IWorkspaceRoot) {
				// Don't show closed projects
				IProject[] allProjects= ((IWorkspaceRoot) element).getProjects();
				ArrayList accessibleProjects= new ArrayList();
				for (int i= 0; i<allProjects.length; i++) {
					if (allProjects[i].isOpen()) {
						accessibleProjects.add(allProjects[i]);
					}
				}
				return accessibleProjects.toArray();
			}
			// When retargeting a diff project, don't support expansion
			if (element instanceof IProject && node instanceof PatchProjectDiffNode) {
				return new Object[0];
			}
			return super.getChildren(element);
		}
	}
	
	private final PatchDiffNode fSelectedNode;
	private final WorkspacePatcher fPatcher;
	private TreeViewer fViewer;
	private IResource fSelectedResource;
	
	public RetargetPatchElementDialog(Shell shell, WorkspacePatcher patcher, PatchDiffNode node) {
		super(shell);
		Assert.isNotNull(patcher);
		Assert.isNotNull(node);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		this.fPatcher = patcher;
		fSelectedNode= node;
	}

	protected Control createButtonBar(Composite parent) {
		Control control = super.createButtonBar(parent);
		Button okButton = this.getButton(IDialogConstants.OK_ID);
		okButton.setEnabled(false);
		return control;
	}
	
	protected Control createDialogArea(Composite parent) {
		Composite composite= (Composite) super.createDialogArea(parent);

		initializeDialogUnits(parent);

		getShell().setText(PatchMessages.PreviewPatchPage_RetargetPatch);

		GridLayout layout= new GridLayout();
		layout.numColumns= 1;
        layout.marginHeight= convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
        layout.marginWidth= convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);			
		composite.setLayout(layout);
		final GridData data= new GridData(SWT.FILL, SWT.FILL, true, true);
		composite.setLayoutData(data);

		//add controls to composite as necessary
		Label label= new Label(composite, SWT.LEFT|SWT.WRAP);
		label.setText(getTreeLabel());
		final GridData data2= new GridData(SWT.FILL, SWT.BEGINNING, true, false);
		label.setLayoutData(data2);

		fViewer= new TreeViewer(composite, SWT.BORDER);
		GridData gd= new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.widthHint= 0;
		gd.heightHint= 0;
		fViewer.getTree().setLayoutData(gd);
		fViewer.setContentProvider(new RetargetPatchContentProvider(fSelectedNode));
		fViewer.setLabelProvider(new WorkbenchLabelProvider());
		fViewer.setComparator(new ResourceComparator(ResourceComparator.NAME));
		fViewer.setInput(getViewerInput());
		IResource resource = getInitialSelection();
		if (resource != null) {
			fViewer.setSelection(new StructuredSelection(resource));
			fViewer.expandToLevel(resource, 0);
		}
		setupListeners();

		Dialog.applyDialogFont(composite);
		return parent;
	}

	private IResource getViewerInput() {
		if (fPatcher.isWorkspacePatch())
			return ResourcesPlugin.getWorkspace().getRoot();
		return fPatcher.getTarget();
	}
	
	private IResource getInitialSelection() {
		if (fSelectedNode instanceof PatchFileDiffNode) {
			PatchFileDiffNode node = (PatchFileDiffNode) fSelectedNode;
			return fPatcher.getTargetFile(node.getDiffResult().getDiff());
		} else if (fSelectedNode instanceof HunkDiffNode) {
			HunkDiffNode node = (HunkDiffNode) fSelectedNode;
			return fPatcher.getTargetFile(node.getHunkResult().getDiffResult().getDiff());
		} else if (fSelectedNode instanceof PatchProjectDiffNode) {
			PatchProjectDiffNode node = (PatchProjectDiffNode) fSelectedNode;
			DiffProject diffProject = node.getDiffProject();
			return Utilities.getProject(diffProject);
		}
		return null;
	}

	private String getTreeLabel() {
		if (fSelectedNode instanceof PatchProjectDiffNode) {
			PatchProjectDiffNode node = (PatchProjectDiffNode) fSelectedNode;
			DiffProject project = node.getDiffProject();
			return NLS.bind(PatchMessages.PreviewPatchPage_SelectProject, project.getName());
		} else if (fSelectedNode instanceof PatchFileDiffNode) {
			PatchFileDiffNode node = (PatchFileDiffNode) fSelectedNode;
			//copy over all hunks to new target resource
			FilePatch2 diff = node.getDiffResult().getDiff();
			return NLS.bind(PatchMessages.RetargetPatchElementDialog_0, fPatcher.getPath(diff));
		} else if (fSelectedNode instanceof HunkDiffNode) {
			HunkDiffNode node = (HunkDiffNode) fSelectedNode;
			Hunk hunk = node.getHunkResult().getHunk();
			return NLS.bind(PatchMessages.RetargetPatchElementDialog_1, fPatcher.getPath(hunk.getParent()));
		}
		return ""; //$NON-NLS-1$
	}

	protected void okPressed() {
		if (fSelectedResource != null){
			if (fSelectedNode instanceof PatchProjectDiffNode && fSelectedResource instanceof IProject) {
				PatchProjectDiffNode node = (PatchProjectDiffNode) fSelectedNode;
				DiffProject project = node.getDiffProject();
				fPatcher.retargetProject(project, (IProject)fSelectedResource);
			} else if (fSelectedNode instanceof PatchFileDiffNode && fSelectedResource instanceof IFile) {
				PatchFileDiffNode node = (PatchFileDiffNode) fSelectedNode;
				//copy over all hunks to new target resource
				FilePatch2 diff = node.getDiffResult().getDiff();
				fPatcher.retargetDiff(diff, (IFile)fSelectedResource);
			} else if (fSelectedNode instanceof HunkDiffNode && fSelectedResource instanceof IFile) {
				HunkDiffNode node = (HunkDiffNode) fSelectedNode;
				fPatcher.retargetHunk(node.getHunkResult().getHunk(), (IFile)fSelectedResource);
			}
		}
		super.okPressed();	
	}

	void setupListeners() {
		fViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection s= (IStructuredSelection) event.getSelection();
				Object obj= s.getFirstElement();
				if (obj instanceof IResource){
					fSelectedResource = (IResource) obj;
					if (fSelectedNode instanceof PatchProjectDiffNode) {
						if (fSelectedResource instanceof IProject){
							Button okButton = getButton(IDialogConstants.OK_ID);
							okButton.setEnabled(true);
						}
					} else if (fSelectedNode instanceof PatchFileDiffNode
							|| fSelectedNode instanceof HunkDiffNode) {
						if (fSelectedResource instanceof IFile){
							Button okButton = getButton(IDialogConstants.OK_ID);
							okButton.setEnabled(true);
						}
					}
				}
			}
		});

		fViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				ISelection s= event.getSelection();
				if (s instanceof IStructuredSelection) {
					Object item= ((IStructuredSelection) s).getFirstElement();
					if (fViewer.getExpandedState(item))
						fViewer.collapseToLevel(item, 1);
					else
						fViewer.expandToLevel(item, 1);
				}
			}
		});

	}

	protected Point getInitialSize() {
		final Point size= super.getInitialSize();
		size.x= convertWidthInCharsToPixels(75);
		size.y+= convertHeightInCharsToPixels(20);
		return size;
	}
}