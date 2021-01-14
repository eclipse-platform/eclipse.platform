/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
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
package org.eclipse.compare.internal.patch;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.internal.CompareUIPlugin;
import org.eclipse.compare.internal.ExceptionHandler;
import org.eclipse.compare.internal.Utilities;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.osgi.framework.FrameworkUtil;

public class PatchWizard extends Wizard {

	// dialog store id constants
	private final static String DIALOG_SETTINGS_KEY= "PatchWizard"; //$NON-NLS-1$

	private boolean fHasNewDialogSettings;

	protected InputPatchPage fPatchWizardPage;
	protected PatchTargetPage fPatchTargetPage;
	protected PreviewPatchPage2 fPreviewPage2;

	private final WorkspacePatcher fPatcher;

	private CompareConfiguration fConfiguration;
	private IStorage patch;

	private boolean patchReadIn = false;

	public PatchWizard(IStorage patch, IResource target, CompareConfiguration configuration) {
		Assert.isNotNull(configuration);
		this.fConfiguration = configuration;
		setDefaultPageImageDescriptor(CompareUIPlugin.getImageDescriptor("wizban/applypatch_wizban.png")); //$NON-NLS-1$
		setWindowTitle(PatchMessages.PatchWizard_title);
		initializeDialogSettings();
		fPatcher= new WorkspacePatcher(target);
		if (patch != null) {
			try {
				fPatcher.parse(patch);
				this.patch = patch;
				patchReadIn = true;
			} catch (IOException e) {
				MessageDialog.openError(null,
						PatchMessages.InputPatchPage_PatchErrorDialog_title,
						PatchMessages.InputPatchPage_ParseError_message);
			} catch (CoreException e) {
				ErrorDialog.openError(getShell(),
						PatchMessages.InputPatchPage_PatchErrorDialog_title,
						PatchMessages.InputPatchPage_PatchFileNotFound_message, e.getStatus());
			}
		}
	}

	private void initializeDialogSettings() {
		IDialogSettings workbenchSettings = PlatformUI
				.getDialogSettingsProvider(FrameworkUtil.getBundle(PatchWizard.class)).getDialogSettings();

		IDialogSettings section= workbenchSettings.getSection(DIALOG_SETTINGS_KEY);
		if (section == null) {
			fHasNewDialogSettings= true;
		} else {
			fHasNewDialogSettings= false;
			setDialogSettings(section);
		}
	}

	protected WorkspacePatcher getPatcher() {
		return fPatcher;
	}

	protected IStorage getPatch() {
		return patch;
	}

	IResource getTarget() {
		return fPatcher.getTarget();
	}

	@Override
	public void addPages() {
		if (patch == null)
			addPage(fPatchWizardPage = new InputPatchPage(this));
		if (patch == null || (!fPatcher.isWorkspacePatch() && !fPatcher.isGitPatch()))
			addPage(fPatchTargetPage = new PatchTargetPage(fPatcher));
		else if (fPatcher.isGitPatch())
			fPatcher.setTarget(ResourcesPlugin.getWorkspace().getRoot());
		fPreviewPage2 = new PreviewPatchPage2(fPatcher, fConfiguration);
		addPage(fPreviewPage2);
	}

	@Override
	public boolean performFinish() {

		IWizardPage currentPage = getContainer().getCurrentPage();
		if (currentPage.getName().equals(PreviewPatchPage2.PREVIEWPATCHPAGE_NAME)){
			PreviewPatchPage2 previewPage = (PreviewPatchPage2) currentPage;
			previewPage.ensureContentsSaved();
		}

		if (fPatchWizardPage != null){
			// make sure that the patch has been read
			if (!fPatchWizardPage.isPatchRead())
				fPatchWizardPage.readInPatch();
			fPatcher.refresh();

			// make sure that the patch is not invalid
			if (!fPatchWizardPage.checkPageComplete())
				return false;
		} else {
			//either we have a patch from the patch input page or one has
			//been specified; double check this
			Assert.isNotNull(patch);
			//make sure that the patch has been read in
			Assert.isTrue(patchReadIn);
		}

		if (!currentPage.getName().equals(PreviewPatchPage2.PREVIEWPATCHPAGE_NAME) && fPatcher.hasRejects()){
			if (!MessageDialog.openConfirm(getShell(), PatchMessages.PatchWizard_0, PatchMessages.PatchWizard_1)) {
				return false;
			}
		}

		try {
			// create scheduling rule based on the type of patch - single or workspace
			ISchedulingRule scheduleRule = null;
			if (fPatcher.isWorkspacePatch()) {
				// workspace patch
				ISchedulingRule[] projectRules = fPatcher.getTargetProjects();
				scheduleRule = new MultiRule(projectRules);
			} else {
				// single patch
				IResource resource = getTarget();
				if (resource.getType() == IResource.FILE) {
					// For a file, use the modify rule for the parent since we may need to include a reject file
					resource = resource.getParent();
				}
				scheduleRule = ResourcesPlugin.getWorkspace().getRuleFactory().modifyRule(resource);
			}

			WorkspaceModifyOperation op = new WorkspaceModifyOperation(scheduleRule) {
				@Override
				protected void execute(IProgressMonitor monitor) throws InvocationTargetException {
					try {
						fPatcher.applyAll(monitor, resoures -> Utilities.validateResources(resoures, getShell(), PatchMessages.PatchWizard_title));
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					}
				}
			};
			getContainer().run(true, false, op);

		} catch (InvocationTargetException e) {
			ExceptionHandler.handle(e, PatchMessages.PatchWizard_title, PatchMessages.PatchWizard_unexpectedException_message);
		} catch (InterruptedException e) {
			// cannot happen
			// NeedWork: use assert!
		}

		// Save the dialog settings
		if (fHasNewDialogSettings) {
			IDialogSettings workbenchSettings = PlatformUI
					.getDialogSettingsProvider(FrameworkUtil.getBundle(PatchWizardDialog.class)).getDialogSettings();
			IDialogSettings section = workbenchSettings.getSection(DIALOG_SETTINGS_KEY);
			section = workbenchSettings.addNewSection(DIALOG_SETTINGS_KEY);
			setDialogSettings(section);
		}

		if (fPatchWizardPage != null)
			fPatchWizardPage.saveWidgetValues();
		fPreviewPage2.saveWidgetValues();
		return true;
	}

	public void showPage(IWizardPage page) {
		getContainer().showPage(page);
	}

	@Override
	public IWizardPage getNextPage(IWizardPage page) {
		//no patch has been read in yet, input patch page
		if (!patchReadIn)
			return fPatchWizardPage;

		//Check to see if we're already on the patch target page and if
		//a target has been set - if it has return the next page in sequence (the preview patch page)
		if (page instanceof PatchTargetPage && getTarget() != null) {
			return super.getNextPage(page);
		} else if (page instanceof InputPatchPage && !fPatcher.isWorkspacePatch()) {
			//Check to see if we need a target
			return fPatchTargetPage;
		}
		return super.getNextPage(page);
	}

	/**
	 * Used to report that the patch has
	 *
	 */
	protected void patchReadIn() {
		patchReadIn = true;
	}

	public CompareConfiguration getCompareConfiguration() {
		return fConfiguration;
	}

	@Override
	public boolean canFinish() {
		IWizardPage currentPage = getContainer().getCurrentPage();
		if (currentPage.getName().equals(PreviewPatchPage2.PREVIEWPATCHPAGE_NAME)){
			return currentPage.isPageComplete();
		}
		return super.canFinish();
	}

}
