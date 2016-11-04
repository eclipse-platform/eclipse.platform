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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.team.FileModificationValidationContext;
import org.eclipse.core.resources.team.FileModificationValidator;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.ITeamStatus;
import org.eclipse.team.core.TeamStatus;

public class DefaultFileModificationValidator extends FileModificationValidator {

	/*
	 * A validator plugged in the the Team UI that will prompt
	 * the user to make read-only files writable. In the absence of
	 * this validator, edit/save fail on read-only files.
	 */
	private FileModificationValidator uiValidator;

	protected IStatus getDefaultStatus(IFile file) {
		return
			file.isReadOnly()
			? new TeamStatus(IStatus.ERROR, TeamPlugin.ID, ITeamStatus.READ_ONLY_LOCAL, NLS.bind(Messages.FileModificationValidator_fileIsReadOnly, new String[] { file.getFullPath().toString() }), null, file)
				: Status.OK_STATUS;
	}

	public IStatus validateEdit(IFile[] files, FileModificationValidationContext context) {
	    IFile[] readOnlyFiles = getReadOnly(files);
	    if (readOnlyFiles.length == 0)
	        return Status.OK_STATUS;
	    synchronized (this) {
	        if (uiValidator == null)
	            uiValidator = loadUIValidator();
	    }
	    if (uiValidator != null) {
	        return uiValidator.validateEdit(files, context);
	    }
	    // There was no plugged in validator so fail gracefully
		return getStatus(files);
	}

    protected IStatus getStatus(IFile[] files) {
        if (files.length == 1) {
			return getDefaultStatus(files[0]);
		}

		IStatus[] stati = new Status[files.length];
		boolean allOK = true;

		for (int i = 0; i < files.length; i++) {
			stati[i] = getDefaultStatus(files[i]);
			if(! stati[i].isOK())
				allOK = false;
		}

		return new MultiStatus(TeamPlugin.ID,
			0, stati,
			allOK
					? Messages.ok
					: Messages.FileModificationValidator_someReadOnly,
			null);
    }

    private IFile[] getReadOnly(IFile[] files) {
        List result = new ArrayList(files.length);
        for (int i = 0; i < files.length; i++) {
            IFile file = files[i];
            if (file.isReadOnly()) {
                result.add(file);
            }
        }
        return (IFile[]) result.toArray(new IFile[result.size()]);
    }

	public IStatus validateSave(IFile file) {
	    if (!file.isReadOnly())
	        return Status.OK_STATUS;
	    synchronized (this) {
	        if (uiValidator == null)
	            uiValidator = loadUIValidator();
	    }
	    if (uiValidator != null) {
	        return uiValidator.validateSave(file);
	    }
		return getDefaultStatus(file);
	}

    private FileModificationValidator loadUIValidator() {
        IExtensionPoint extension = Platform.getExtensionRegistry().getExtensionPoint(TeamPlugin.ID, TeamPlugin.DEFAULT_FILE_MODIFICATION_VALIDATOR_EXTENSION);
		if (extension != null) {
			IExtension[] extensions =  extension.getExtensions();
			if (extensions.length > 0) {
				IConfigurationElement[] configElements = extensions[0].getConfigurationElements();
				if (configElements.length > 0) {
					try {
                        Object o = configElements[0].createExecutableExtension("class"); //$NON-NLS-1$
                        if (o instanceof FileModificationValidator) {
                            return (FileModificationValidator)o;
                        }
                    } catch (CoreException e) {
                        TeamPlugin.log(e);
                    }
				}
			}
		}
		return null;
    }
}
