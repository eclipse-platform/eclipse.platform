/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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
package org.eclipse.team.examples.filesystem;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.team.FileModificationValidationContext;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;

/**
 * This class models a sentry that verifies whether resources are available for editing or overwriting.
 * This has been made a separate class for illustration purposes. It may have been more appropriate
 * to have FileSystemProvider implement IFileModificationValidator itself since the interface
 * only has two methods and their implementation is straight forward.
 */
public final class FileModificationValidator extends org.eclipse.core.resources.team.FileModificationValidator {

	private FileSystemOperations operations;

	/**
	 * Constructor for FileModificationValidator.
	 */
	public FileModificationValidator(RepositoryProvider provider) {
		operations = ((FileSystemProvider)provider).getOperations();
	}

	/**
	 * This method will convert any exceptions thrown by the SimpleAccessOperations.checkout() to a Status.
	 * @param resources the resources that are to be checked out
	 * @return IStatus a status indicator that reports whether the operation went smoothly or not.
	 */
	private IStatus checkout(IResource[] resources) {
		try {
			operations.checkout(resources, IResource.DEPTH_INFINITE, null);
		} catch (TeamException e) {
			return new Status(IStatus.ERROR, FileSystemPlugin.ID, 0, e.getLocalizedMessage(), e);
		}
		return Status.OK_STATUS;
	}

	/**
	 * This method will be called by the workbench/editor before it tries to edit one or more files.
	 * The idea is to prevent anyone from accidentally working on a file that they won't be able to check in changes to.
	 * @see org.eclipse.core.resources.IFileModificationValidator#validateEdit(IFile[], Object)
	 */
	@Override
	public IStatus validateEdit(IFile[] files, FileModificationValidationContext context) {
		Collection<IResource> toBeCheckedOut = new ArrayList<>();

		//Make a list of all the files that need to be checked out:
		for (IFile file : files) {
			if (!operations.isCheckedOut(file)) {
				toBeCheckedOut.add(file);
			}
		}

		return checkout(toBeCheckedOut.toArray(new IResource[toBeCheckedOut.size()]));
	}

	/**
	 * This method will be called by the workbench before it tries to save a file.
	 * It should not attempt to save any files that don't receive an OK status here.
	 * @see org.eclipse.core.resources.IFileModificationValidator#validateSave(IFile)
	 */
	@Override
	public IStatus validateSave(IFile file) {
		if (file.isReadOnly()) {
			return checkout(new IResource[] { file });
		}
		return Status.OK_STATUS;
	}

}
