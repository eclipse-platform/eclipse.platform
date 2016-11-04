/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.core;


import java.util.*;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.team.FileModificationValidationContext;
import org.eclipse.core.resources.team.FileModificationValidator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.team.core.RepositoryProvider;

public class FileModificationValidatorManager extends FileModificationValidator {
	private FileModificationValidator defaultValidator;

	/*
	 * @see IFileModificationValidator#validateEdit(IFile[], Object)
	 * For all files, determine which provider.
	 * Ask each provider once for its files.
	 * Collect the resulting status' and return a MultiStatus.
	 */
	public IStatus validateEdit(IFile[] files, FileModificationValidationContext context) {
		ArrayList returnStati = new ArrayList();

		//map provider to the files under that provider's control
		Map providersToFiles = new HashMap(files.length);

		//for each file, determine which provider, map providers to files
		for (int i = 0; i < files.length; i++) {
			IFile file = files[i];
			RepositoryProvider provider = RepositoryProvider.getProvider(file.getProject());

			if (!providersToFiles.containsKey(provider)) {
				providersToFiles.put(provider, new ArrayList());
			}

			((ArrayList)providersToFiles.get(provider)).add(file);
		}

		Iterator providersIterator = providersToFiles.keySet().iterator();

		boolean allOK = true;

		//for each provider, validate its files
		while(providersIterator.hasNext()) {
			RepositoryProvider provider = (RepositoryProvider)providersIterator.next();
			ArrayList filesList = (ArrayList)providersToFiles.get(provider);
			IFile[] filesArray = (IFile[])filesList.toArray(new IFile[filesList.size()]);
			FileModificationValidator validator = getDefaultValidator();

			//if no provider or no validator use the default validator
			if (provider != null) {
				FileModificationValidator v = provider.getFileModificationValidator2();
				if (v != null) validator = v;
			}

			IStatus status = validator.validateEdit(filesArray, context);
			if(!status.isOK())
				allOK = false;

			returnStati.add(status);
		}

		if (returnStati.size() == 1) {
			return (IStatus)returnStati.get(0);
		}

		return new MultiStatus(TeamPlugin.ID,
			0,
			(IStatus[])returnStati.toArray(new IStatus[returnStati.size()]),
				allOK
					? Messages.ok
					: Messages.FileModificationValidator_editFailed,
			null);
	}

	/*
	 * @see IFileModificationValidator#validateSave(IFile)
	 */
	public IStatus validateSave(IFile file) {
		RepositoryProvider provider = RepositoryProvider.getProvider(file.getProject());
		FileModificationValidator validator = getDefaultValidator();

		//if no provider or no validator use the default validator
		if (provider != null) {
			FileModificationValidator v = provider.getFileModificationValidator2();
			if (v != null) validator = v;
		}

		return validator.validateSave(file);
	}

	private synchronized FileModificationValidator getDefaultValidator() {
	    if (defaultValidator == null) {
	        defaultValidator = new DefaultFileModificationValidator();
	    }
	    return defaultValidator;
	}
}
