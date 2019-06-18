/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
package org.eclipse.team.examples.pessimistic;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFileModificationValidator;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.team.FileModificationValidationContext;
import org.eclipse.core.resources.team.FileModificationValidator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.dialogs.CheckedTreeSelectionDialog;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.navigator.ResourceComparator;

/**
 * The <code>PessimisticModificationValidator</code> is an
 * implementation of the <code>IFileModificationValidator</code> for the
 * <code>PessimisticFilesystemProvider</code>.
 *
 * @see PessimiticFilesystemProvider
 * @see IFileModificationValidator
 */
public class PessimisticModificationValidator
extends FileModificationValidator {
	/*
	 * The provider for this validator
	 */
	private PessimisticFilesystemProvider fProvider;

	public PessimisticModificationValidator(PessimisticFilesystemProvider provider) {
		fProvider= provider;
	}

	@Override
	public IStatus validateEdit(IFile[] files, FileModificationValidationContext context) {
		if (files.length == 0) {
			return new Status( IStatus.OK, getUid(), IStatus.OK, "OK", null);
		}

		Set<IFile> checkOut = new HashSet<>();
		int reloadCount = 0;
		int checkoutFailCount = 0;

		Map<IFile, Integer> validateEditStatusMap = new HashMap<>(files.length);

		for (IFile file : files) {
			if (fProvider.isControlled(file) ) {
				if (fProvider.isCheckedout(file)) {
					setValidateEditStatus(validateEditStatusMap, file,
							IPessimisticFilesystemConstants.STATUS_OK_TO_EDIT );
				} else {
					checkOut.add(file);
				}
			} else {
				setValidateEditStatus(validateEditStatusMap, file,
						IPessimisticFilesystemConstants.STATUS_OK_TO_EDIT);
			}
		}

		if (!checkOut.isEmpty()) {
			if (context != null) {
				boolean shouldFail= shouldFailValidateEdit();
				int statusCode = checkout(checkOut, IPessimisticFilesystemConstants.PREF_CHECKED_IN_FILES_EDITED, shouldFail, context);
				if (shouldFail) {
					return new Status( IStatus.ERROR, getUid(), IStatus.ERROR, "Fail Validate Edit Preference true", null);
				}
				if(statusCode == IStatus.CANCEL) {
					return new Status( IStatus.CANCEL, getUid(), IStatus.ERROR, "Checkout canceled by user", null);
				}
			} else {
				if (isAutomaticCheckout()) {
					if (shouldFailValidateEdit()) {
						return new Status( IStatus.ERROR, getUid(), IStatus.ERROR, "Fail Validate Edit Preference true", null);
					}

					checkout(checkOut);
				}
			}

			for (IFile file : checkOut) {
				if ( fProvider.isCheckedout(file) ) {
					if ( !fProvider.hasContentChanged(file) ) {
						setValidateEditStatus(validateEditStatusMap, file,
								IPessimisticFilesystemConstants.STATUS_OK_TO_EDIT );
					} else {
						reloadCount++;
						setValidateEditStatus(validateEditStatusMap, file,
								IPessimisticFilesystemConstants.STATUS_PROMPT_FOR_RELOAD );
					}
				} else {
					checkoutFailCount++;
				}
			}
		}

		if (reloadCount + checkoutFailCount == 0) {
			return new Status( IStatus.OK, getUid(), IStatus.OK, "OK", null);
		}

		if (checkoutFailCount == files.length) {
			return new Status( IStatus.ERROR, getUid(), IStatus.ERROR, "No files were checked out.", null);
		}

		IStatus children[] = new Status[ files.length ];

		int mask = IPessimisticFilesystemConstants.STATUS_OK_TO_EDIT |
				IPessimisticFilesystemConstants.STATUS_PROMPT_FOR_RELOAD;

		for (int i = 0; i < files.length; i++) {
			int result = getValidateEditStatus(validateEditStatusMap, files[i]);
			if ((result & mask) != 0) {
				children[i] = new Status( IStatus.OK, getUid(), IStatus.OK, "OK", null);
			} else {
				children[i] = new Status( IStatus.ERROR, getUid(), IStatus.ERROR, "File " + files[i].getName() + " could not be checked out.", null);
			}
		}
		return new MultiStatus( getUid(), IStatus.OK, children, "Some files were not successfully checked out", null);
	}

	@Override
	public IStatus validateSave(IFile file) {
		int checkedInFilesSaved = getPreferences().getInt(IPessimisticFilesystemConstants.PREF_CHECKED_IN_FILES_SAVED);
		if (checkedInFilesSaved == IPessimisticFilesystemConstants.OPTION_DO_NOTHING) {
			return new Status( IStatus.OK, getUid(), IStatus.OK, "", null);
		}


		IStatus status = new Status( IStatus.OK, getUid(), IStatus.OK,
				"File is writable", null);

		if (!fProvider.isControlled(file)) {
			return status;
		}

		if (fProvider.isIgnored(file)) {
			return status;
		}

		if (fProvider.isCheckedout(file)) {
			return status;
		}
		Set<IFile> files = new HashSet<>(1);
		files.add(file);

		checkout(files, IPessimisticFilesystemConstants.PREF_CHECKED_IN_FILES_SAVED, false, null);

		if (fProvider.isCheckedout(file)) {
			return status;
		}
		return new Status(
				IStatus.ERROR,
				getUid(),
				IStatus.ERROR,
				file.getProjectRelativePath() + " could not be checked out",
				null);
	}

	/*
	 * Convenience method to get the plugin id
	 */
	private String getUid() {
		return PessimisticFilesystemProviderPlugin.PLUGIN_ID;
	}

	/*
	 * Convenience method to answer if the fail validate edit preference
	 * has been set.
	 */
	private boolean shouldFailValidateEdit() {
		return getPreferences().getBoolean(IPessimisticFilesystemConstants.PREF_FAIL_VALIDATE_EDIT);
	}

	/*
	 * Convenience method to answer if the check out preference is set to automatic.
	 */
	private boolean isAutomaticCheckout() {
		return getPreferences().getInt(IPessimisticFilesystemConstants.PREF_CHECKED_IN_FILES_EDITED_NOPROMPT) == IPessimisticFilesystemConstants.OPTION_AUTOMATIC;
	}

	/*
	 * Optionally prompts the user to select which resources should be
	 * checked out, and then checks the selected resources.
	 */
	private int promptAndCheckout(Set<IFile> resources, boolean beQuiet, boolean shouldFail, Object context) {
		if (resources.isEmpty()) {
			return IStatus.OK;
		}

		Set<IFile> temp = new HashSet<>(resources.size());
		for (IFile resource : resources) {
			if (fProvider.isControlled(resource) && !fProvider.isCheckedout(resource))
				temp.add(resource);
		}
		resources= temp;
		final int[] statusCode = new int[] {IStatus.OK};
		if (!beQuiet && !resources.isEmpty()) {
			final Shell shell= getShell(context);
			if (shell != null && !shell.isDisposed()) {
				Display display = shell.getDisplay();
				final Set[] result = {resources};
				display.syncExec(() -> {
					ILabelProvider labelProvider= new WorkbenchLabelProvider();
					Object[] resourceArray= result[0].toArray();
					ITreeContentProvider contentProvider= new ResourceSetContentProvider(result[0]);
					CheckedTreeSelectionDialog dialog= new CheckedTreeSelectionDialog(shell, labelProvider, contentProvider);
					dialog.setMessage("Select resources to be checked out.");
					dialog.setTitle("Check out resources");
					dialog.setContainerMode(true);
					dialog.setBlockOnOpen(true);
					dialog.setComparator(new ResourceComparator(ResourceComparator.NAME));
					dialog.setExpandedElements(resourceArray);
					dialog.setInitialSelections(resourceArray);
					dialog.setInput(ResourcesPlugin.getWorkspace().getRoot());
					int status= dialog.open();
					result[0]= null;
					if (status == Window.OK) {
						Object[] results= dialog.getResult();
						result[0] = new HashSet(results.length);
						Collections.addAll(result[0], results);
					} else if(status == Window.CANCEL) {
						statusCode[0] = IStatus.CANCEL;
					}
				});
				resources= result[0];
			} else {
				resources= null;
				PessimisticFilesystemProviderPlugin.getInstance().logError(new RuntimeException(), "Context is invalid: " + context);
			}
		}

		if (resources != null && !resources.isEmpty() && !shouldFail) {
			checkout(resources);
		}
		return statusCode[0];
	}

	/*
	 * Checks out the files contained in the resources set
	 */
	private void checkout(Set<IFile> resources) {
		if (resources.isEmpty())
			return;
		IFile[] checkOut= new IFile[resources.size()];
		resources.toArray(checkOut);
		fProvider.checkout(checkOut, null);
	}

	/*
	 * Convenience method to get the plugin preferences.
	 */
	private IPreferenceStore getPreferences() {
		return PessimisticFilesystemProviderPlugin.getInstance().getPreferenceStore();
	}

	/*
	 * Checks out the files if necessary and if the preferences allow.
	 */
	private int checkout(Set<IFile> resources, String itemId, boolean shouldFail, Object context) {
		if (resources.isEmpty()) {
			return IStatus.OK;
		}

		int preference= getPreferences().getInt(itemId);

		if (preference == IPessimisticFilesystemConstants.OPTION_DO_NOTHING)
			return IStatus.OK;

		boolean beQuiet= false;
		if (preference == IPessimisticFilesystemConstants.OPTION_AUTOMATIC) {
			beQuiet= true;
		}
		return promptAndCheckout(resources, beQuiet, shouldFail, context);
	}

	/*
	 * Convenience method to set the validate edit status for the given resource.
	 */
	private static void setValidateEditStatus(Map<IFile, Integer> map, IFile resource, int status) {
		map.put(resource, Integer.valueOf(status));
	}

	/*
	 * Convenience method to get the validate edit status for the given resource.
	 */
	private static int getValidateEditStatus(Map map, IFile resource) {
		Integer i= (Integer)map.get(resource);
		if (i == null)
			return 0;
		return i.intValue();
	}

	/*
	 * Convenience method to get a shell from an object.
	 */
	private Shell getShell(Object context) {
		if (context instanceof FileModificationValidationContext) {
			FileModificationValidationContext fmvc = (FileModificationValidationContext) context;
			return (Shell)fmvc.getShell();
		}

		if (context instanceof Shell) {
			return (Shell)context;
		}

		if (context instanceof Control) {
			Control control= (Control)context;
			return control.getShell();
		}

		if (context instanceof Widget) {
			Widget widget= (Widget)context;
			return widget.getDisplay().getActiveShell();
		}

		return null;
	}
}
