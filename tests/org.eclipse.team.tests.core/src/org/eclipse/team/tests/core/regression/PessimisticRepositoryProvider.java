/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
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
package org.eclipse.team.tests.core.regression;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFileModificationValidator;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.*;
import org.eclipse.team.core.RepositoryProvider;

/**
 * Repository provider that can be configured to be pessimistic.
 */
public class PessimisticRepositoryProvider extends RepositoryProvider implements IFileModificationValidator {
	private static PessimisticRepositoryProvider soleInstance;

	public static final String NATURE_ID = "org.eclipse.team.tests.core.regression.pessimistic-provider";

	public static boolean markWritableOnEdit;
	public static boolean markWritableOnSave;

	public PessimisticRepositoryProvider() {
		soleInstance = this;
	}

	@Override
	public void configureProject() {
	}

	@Override
	public String getID() {
		return NATURE_ID;
	}

	@Override
	public void deconfigure() {
	}
	@Override
	public boolean canHandleLinkedResourceURI() {
		return true;
	}
	@Override
	public IFileModificationValidator getFileModificationValidator() {
		return soleInstance;
	}

	@Override
	public IStatus validateEdit(final IFile[] files, Object context) {
		if (markWritableOnEdit) {
			try {
				ResourcesPlugin.getWorkspace().run(
					(IWorkspaceRunnable) monitor -> {
						for (int i = 0, length = files.length; i < length; i++) {
							try {
								setReadOnly(files[i], false);
							} catch (CoreException e) {
								e.printStackTrace();
							}
						}
					},
					null);
			} catch (CoreException e) {
				e.printStackTrace();
				return e.getStatus();
			}
		}
		return Status.OK_STATUS;
	}

	@Override
	public IStatus validateSave(IFile file) {
		if (markWritableOnSave) {
			try {
				setReadOnly(file, false);
			} catch (CoreException e) {
				e.printStackTrace();
				return e.getStatus();
			}
		}
		return Status.OK_STATUS;
	}

	public void setReadOnly(IResource resource, boolean readOnly) throws CoreException {
		ResourceAttributes resourceAttributes = resource.getResourceAttributes();
		if (resourceAttributes != null) {
			resourceAttributes.setReadOnly(readOnly);
			resource.setResourceAttributes(resourceAttributes);
		}
	}

	public boolean isReadOnly(IResource resource) throws CoreException {
		ResourceAttributes resourceAttributes = resource.getResourceAttributes();
		if (resourceAttributes != null) {
			return resourceAttributes.isReadOnly();
		}
		return false;
	}
}
