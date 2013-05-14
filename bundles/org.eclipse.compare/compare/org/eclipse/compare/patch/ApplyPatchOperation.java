/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.patch;

import java.io.BufferedReader;
import java.io.IOException;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.internal.ComparePreferencePage;
import org.eclipse.compare.internal.CompareUIPlugin;
import org.eclipse.compare.internal.core.patch.FilePatch2;
import org.eclipse.compare.internal.core.patch.PatchReader;
import org.eclipse.compare.internal.patch.FilePatch;
import org.eclipse.compare.internal.patch.PatchWizard;
import org.eclipse.compare.internal.patch.PatchWizardDialog;
import org.eclipse.compare.internal.patch.Utilities;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.ide.IDE;

/**
 * An operation that provides an interface to the Apply Patch Wizard. Users specify
 * the input in terms of an <code>IStorage</code> (note: input must be in unified diff
 * format), an <code>IResource</code> target to apply the patch to and can provide <code>CompareConfiguration</code>
 * elements to supply the label and images used on the preview page and hunk merge page. Finally, the
 * user can also supply a title and image to override the default ones provided by the Apply Patch Wizard.
 * Note that the Apply Patch Wizard does not require any particular set of inputs, and in the absence of
 * any user supplied values, it will work in default mode.
 * 
 * @since 3.3
 *
 */
public class ApplyPatchOperation implements Runnable {

	private IWorkbenchPart part;
	
	/**
	 * Used for the Preview Patch page.
	 */
	private CompareConfiguration configuration;
	
	/**
	 * The patch to use as an input into the Apply Patch wizard
	 */
	private IStorage patch;
	
	/**
	 * Specific <code>IResource</code> target to patch.
	 */
	private IResource target;
	
	/**
	 * An optional image for the patch wizard
	 */
	private ImageDescriptor patchWizardImage;
	
	
	/**
	 * An optional title for the patchWizard
	 */
	private String patchWizardTitle;

	private boolean saveAllEditors = true;
	
	/**
	 * Return whether the given storage contains a patch.
	 * @param storage the storage
	 * @return whether the given storage contains a patch
	 * @throws CoreException if an error occurs reading the contents from the storage
	 */
	public static boolean isPatch(IStorage storage) throws CoreException {
		return internalParsePatch(storage).length > 0;
	}
	
	/**
	 * Parse the given patch and return the set of file patches that it contains.
	 * @param storage the storage that contains the patch
	 * @return the set of file patches that the storage contains
	 * @throws CoreException if an error occurs reading the contents from the storage
	 */
	public static IFilePatch[] parsePatch(IStorage storage) throws CoreException {
		return internalParsePatch(storage);
	}
	
	/**
	 * Creates a new ApplyPatchOperation with the supplied compare configuration, patch and target.
	 * The behaviour of the Apply Patch wizard is controlled by the number of parameters supplied:
	 * <ul>
	 * <li>If a patch is supplied, the initial input page is skipped. If a patch is not supplied the wizard
	 * will open on the input page.</li>
	 * <li>If the patch is a workspace patch, the target selection page is skipped and the preview page is 
	 * displayed.</li>
	 * <li>If the patch is not a workspace patch and the target is specified, the target page is still
	 * shown with the target selected.</li>
	 * </ul> 
	 * 
	 * @param part 	an IWorkbenchPart or <code>null</code>
	 * @param patch		an IStorage containing a patch in unified diff format or <code>null</code>
	 * @param target	an IResource which the patch is to be applied to or <code>null</code>
	 * @param configuration	a CompareConfiguration supplying the labels and images for the preview patch page
	 */
	public ApplyPatchOperation(IWorkbenchPart part, IStorage patch, IResource target, CompareConfiguration configuration) {
		Assert.isNotNull(configuration);
		this.part = part;
		this.patch = patch;
		this.target = target;
		this.configuration = configuration;
	}
	
	/**
	 * Create an operation for the given part and resource. This method is a convenience
	 * method that calls {@link #ApplyPatchOperation(IWorkbenchPart, IStorage, IResource, CompareConfiguration)}
	 * with appropriate defaults for the other parameters.
	 * @param targetPart an IResource which the patch is to be applied to or <code>null</code>
	 * @param resource an IResource which the patch is to be applied to or <code>null</code>
	 * @see #ApplyPatchOperation(IWorkbenchPart, IStorage, IResource, CompareConfiguration)
	 */
	public ApplyPatchOperation(IWorkbenchPart targetPart, IResource resource) {
		this(targetPart, null, resource, new CompareConfiguration());
	}

	/**
	 * Open the Apply Patch wizard using the values associated with this operation.
	 * This method must be called from the UI thread.
	 */
	public void openWizard() {
		saveAllEditors();
		
		if (saveAllEditors) {
			PatchWizard wizard = new PatchWizard(patch, target, configuration);
			if (patchWizardImage != null)
				wizard.setDefaultPageImageDescriptor(patchWizardImage);
			if (patchWizardTitle != null)
				wizard.setWindowTitle(patchWizardTitle);
			wizard.setNeedsProgressMonitor(true);

			new PatchWizardDialog(getShell(), wizard).open();
		}
	}

	/**
	 * Return the parent shell to be used when the wizard is opened.
	 * By default, the site of the part is used to get the shell.
	 * Subclasses may override.
	 * @return the parent shell to be used when the wizard is opened
	 */
	protected Shell getShell() {
		if (part == null)
			return CompareUIPlugin.getShell();
		return part.getSite().getShell();
	}
	
	/**
	 * This method will save all dirty editors. It will prompt the user if the Compare preference to save
	 * dirty editors before viewing a patch is <code>false</code>. Clients can use this or provide their own
	 * implementation.
	 */
	protected void saveAllEditors(){
		saveAllEditors = IDE.saveAllEditors(new IResource[]{ResourcesPlugin.getWorkspace().getRoot()}, !ComparePreferencePage.getSaveAllEditors());
	}
	
	/**
	 * Sets the title of the patch wizard. Needs to be set before {@link #openWizard()} is called.
	 * @param title	a string to display in the title bar
	 */
	public void setPatchWizardTitle(String title){
		this.patchWizardTitle = title;
	}
	
	/**
	 * Sets the image descriptor to use in the patch wizard. Needs to be set before  {@link #openWizard()} is called.
	 * @param descriptor an image descriptor
	 */
	public void setPatchWizardImageDescriptor(ImageDescriptor descriptor){
		this.patchWizardImage = descriptor;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		openWizard();
	}
	
	private static IFilePatch[] internalParsePatch(IStorage storage)
			throws CoreException {
		BufferedReader reader = Utilities.createReader(storage);
		try {
			PatchReader patchReader = new PatchReader() {
				protected FilePatch2 createFileDiff(IPath oldPath, long oldDate,
						IPath newPath, long newDate) {
					return new FilePatch(oldPath, oldDate, newPath,
							newDate);
				}
			};
			patchReader.parse(reader);
			FilePatch2[] fileDiffs = patchReader.getAdjustedDiffs();

			IFilePatch[] filePatch = new IFilePatch[fileDiffs.length];
			for (int i = 0; i < fileDiffs.length; i++) {
				filePatch[i] = (FilePatch) fileDiffs[i];
			}

			return filePatch;
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR,
					CompareUIPlugin.PLUGIN_ID, 0, e.getMessage(), e));
		} finally {
			try {
				reader.close();
			} catch (IOException e) { // ignored
			}
		}
	}

	
}
