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
package org.eclipse.team.internal.ccvs.core;

import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.resources.team.FileModificationValidationContext;
import org.eclipse.core.resources.team.FileModificationValidator;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;

/**
 * Core validator that will load the UI validator only if a prompt is needed
 */
public class CVSCoreFileModificationValidator extends FileModificationValidator implements ICVSFileModificationValidator {
	
	FileModificationValidator uiValidator;

	@Override
	public IStatus validateEdit(IFile[] files, FileModificationValidationContext context) {
		IFile[] unmanagedReadOnlyFiles = getUnmanagedReadOnlyFiles(files);
		if (unmanagedReadOnlyFiles.length > 0) {
			IStatus status = setWritable(unmanagedReadOnlyFiles);
			if (!status.isOK()) {
				return status;
			}
		}
		IFile[] readOnlyFiles = getManagedReadOnlyFiles(files);
		if (readOnlyFiles.length == 0) return Status.OK_STATUS;
		return edit(readOnlyFiles, context);
	}

	@Override
	public IStatus validateSave(IFile file) {
		if (!needsCheckout(file)) {
			if (file.isReadOnly()) {
				setWritable(new IFile[] { file } );
			}
			return Status.OK_STATUS;
		}
		return edit(new IFile[] {file}, (FileModificationValidationContext)null);
	}

	/**
	 * Method for editing a set of files. Is overriden by the
	 * UI to prompt the user. Default behavior is to try and load the
	 * UI validator and, failing that, to edit without 
	 * prompting.
	 * @param readOnlyFiles
	 * @param context
	 * @return
	 */
	protected IStatus edit(IFile[] readOnlyFiles, FileModificationValidationContext context) {
		FileModificationValidator override = getUIValidator();
		if (override != null) {
			return override.validateEdit(readOnlyFiles, context);
		} else {
			performEdit(readOnlyFiles);
			return Status.OK_STATUS;
		}
	}

	private FileModificationValidator getUIValidator() {
		synchronized(this) {
			if (uiValidator == null) {
				uiValidator = getPluggedInValidator();
			}
		}
		return uiValidator;
	}
	
	@Override
	public IStatus validateMoveDelete(IFile[] files, IProgressMonitor monitor) {
		IFile[] readOnlyFiles = getManagedReadOnlyFiles(files);
		if (readOnlyFiles.length == 0) return Status.OK_STATUS;

		performEdit(readOnlyFiles);
		return Status.OK_STATUS;
	}
	
	/*
	 * Perform the headless edit check in the background.
	 * The user will be notified of any errors that occurred.
	 */
	protected void performEdit(final IFile[] readOnlyFiles) {
		setWritable(readOnlyFiles);
		Job job = new Job(CVSMessages.CVSCoreFileModificationValidator_editJob) {
			protected IStatus run(IProgressMonitor monitor) {
				try {
					performEdit(readOnlyFiles, monitor);
				} catch (CVSException e) {
					return e.getStatus();
				}
				return Status.OK_STATUS;
			}
		};
		scheduleEditJob(job);
	}

	protected void scheduleEditJob(Job job) {
		job.schedule();
	}

	protected CVSTeamProvider getProvider(IFile[] files) {
		CVSTeamProvider provider = (CVSTeamProvider)RepositoryProvider.getProvider(files[0].getProject(), CVSProviderPlugin.getTypeId());
		return provider;
	}
	
	protected void performEdit(IFile[] files, IProgressMonitor monitor) throws CVSException {
		getProvider(files).edit(files, false /* recurse */, true /* notify server */, true /* notify for writtable files */, ICVSFile.NO_NOTIFICATION, monitor);
	}
	
	private boolean needsCheckout(IFile file) {
		try {
			if (file.isReadOnly()) {
				ICVSFile cvsFile = CVSWorkspaceRoot.getCVSFileFor(file);
				boolean managed = cvsFile.isManaged();
				return managed;
			}
		} catch (CVSException e) {
			// Log the exception and assume we don't need a checkout
			CVSProviderPlugin.log(e);
		}
		return false;
	}
	
	protected IStatus setWritable(final IFile[] files) {
		for (IFile file : files) {
			ResourceAttributes attributes = file.getResourceAttributes();
			if (attributes != null) {
				attributes.setReadOnly(false);
			}
			try {
				file.setResourceAttributes(attributes);
			} catch (CoreException e) {
				return CVSException.wrapException(e).getStatus();
			}
		}
		return Status.OK_STATUS;
	}

	private IFile[] getManagedReadOnlyFiles(IFile[] files) {
		List readOnlys = new ArrayList();
		for (IFile iFile : files) {
			if (needsCheckout(iFile)) {
				readOnlys.add(iFile);
			}
		}
		return (IFile[]) readOnlys.toArray(new IFile[readOnlys.size()]);
	}
	
	protected IFile[] getUnmanagedReadOnlyFiles(IFile[] files) {
		List readOnlys = new ArrayList();
		for (IFile iFile : files) {
			if (iFile.isReadOnly() && !needsCheckout(iFile)) {
				readOnlys.add(iFile);
			}
		}
		return (IFile[]) readOnlys.toArray(new IFile[readOnlys.size()]);
	}

	private static FileModificationValidator getPluggedInValidator() {
		IExtension[] extensions = Platform.getExtensionRegistry().getExtensionPoint(CVSProviderPlugin.ID, CVSProviderPlugin.PT_FILE_MODIFICATION_VALIDATOR).getExtensions();
		if (extensions.length == 0)
			return null;
		IExtension extension = extensions[0];
		IConfigurationElement[] configs = extension.getConfigurationElements();
		if (configs.length == 0) {
			CVSProviderPlugin.log(IStatus.ERROR, NLS.bind("The CVS file modification validator is missing from extension {0}", (new Object[] {extension.getUniqueIdentifier()})), null);//$NON-NLS-1$
			return null;
		}
		try {
			IConfigurationElement config = configs[0];
			return (FileModificationValidator) config.createExecutableExtension("run");//$NON-NLS-1$
		} catch (CoreException ex) {
			CVSProviderPlugin.log(IStatus.ERROR, NLS.bind("The CVS file modification validator registered as ID {0} could not be instantiated", (new Object[] {extension.getUniqueIdentifier()})), ex);//$NON-NLS-1$
			return null;
		}
	}

	public ISchedulingRule validateEditRule(CVSResourceRuleFactory factory, IResource[] resources) {
		FileModificationValidator override = getUIValidator();
		if (override instanceof CVSCoreFileModificationValidator && override != this) {
			CVSCoreFileModificationValidator ui = (CVSCoreFileModificationValidator) override;
			return ui.validateEditRule(factory, resources);
		}
		return internalValidateEditRule(factory, resources);
	}

	protected final ISchedulingRule internalValidateEditRule(CVSResourceRuleFactory factory, IResource[] resources) {
		if (resources.length == 0)
			return null;
		//optimize rule for single file
		if (resources.length == 1)
			return isReadOnly(resources[0]) ? factory.getParent(resources[0]) : null;
		//need a lock on the parents of all read-only files
		HashSet rules = new HashSet();
		for (IResource resource : resources) {
			if (isReadOnly(resource)) {
				rules.add(factory.getParent(resource));
			}
		}
		return createSchedulingRule(rules);
	}

	protected ISchedulingRule createSchedulingRule(Set rules) {
		if (rules.isEmpty())
			return null;
		if (rules.size() == 1)
			return (ISchedulingRule) rules.iterator().next();
		ISchedulingRule[] ruleArray = (ISchedulingRule[]) rules
				.toArray(new ISchedulingRule[rules.size()]);
		return new MultiRule(ruleArray);
	}

	protected final boolean isReadOnly(IResource resource) {
		ResourceAttributes a = resource.getResourceAttributes();
		if (a != null) {
			return a.isReadOnly();
		}
		return false;
	}
}
