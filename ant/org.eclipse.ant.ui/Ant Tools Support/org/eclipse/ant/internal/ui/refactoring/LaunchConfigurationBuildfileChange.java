/*******************************************************************************
 * Copyright (c) 2003, 2013 IBM Corporation and others.
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
package org.eclipse.ant.internal.ui.refactoring;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.ant.core.AntCorePlugin;
import org.eclipse.ant.internal.core.IAntCoreConstants;
import org.eclipse.ant.internal.ui.AntUtil;
import org.eclipse.ant.launching.IAntLaunchConstants;
import org.eclipse.core.externaltools.internal.IExternalToolConstants;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class LaunchConfigurationBuildfileChange extends Change {

	private final ILaunchConfiguration fLaunchConfiguration;
	private final String fNewBuildfileLocation;
	private final String fNewProjectName;
	private String fNewLaunchConfigurationName;
	private final String fOldBuildfileLocation;
	private final String fOldProjectName;
	private final ILaunchConfigurationWorkingCopy fNewLaunchConfiguration;
	private String fNewConfigContainerName;

	/**
	 * Create a change for each launch configuration which needs to be updated for this IJavaProject rename.
	 * 
	 * @since 3.5
	 */
	public static Change createChangesForProjectRename(IProject project, String newProjectName) throws CoreException {
		String projectName = project.getDescription().getName();
		ILaunchConfiguration[] configs = getAntLaunchConfigurations();
		List<Change> changes = createChangesForProjectRename(configs, projectName, newProjectName);
		return createChangeFromList(changes, RefactoringMessages.LaunchConfigurationBuildfileChange_7);
	}

	/**
	 * Create a change for each launch configuration which needs to be updated for this buildfile rename.
	 */
	public static Change createChangesForBuildfileRename(IFile file, String newBuildfileName) throws CoreException {
		IContentDescription description = null;
		try {
			description = file.getContentDescription();
		}
		catch (CoreException e) {
			// missing file
			return null;
		}
		if (description == null) {
			return null;
		}
		IContentType contentType = description.getContentType();
		if (contentType == null || !AntCorePlugin.ANT_BUILDFILE_CONTENT_TYPE.equals(contentType.getId())) {
			return null; // not an Ant buildfile
		}
		ILaunchConfiguration[] configs = getAntLaunchConfigurations();
		List<Change> changes = createChangesForBuildfileRename(file, configs, file.getProject().getName(), newBuildfileName);
		return createChangeFromList(changes, RefactoringMessages.LaunchConfigurationBuildfileChange_7);
	}

	private static ILaunchConfiguration[] getAntLaunchConfigurations() throws CoreException {
		ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
		// Ant launch configurations
		ILaunchConfigurationType configurationType = manager.getLaunchConfigurationType(IAntLaunchConstants.ID_ANT_LAUNCH_CONFIGURATION_TYPE);
		ILaunchConfiguration[] configs = manager.getLaunchConfigurations(configurationType);
		return configs;
	}

	/**
	 * Take a list of Changes, and return a unique Change, a CompositeChange, or null.
	 */
	private static Change createChangeFromList(List<Change> changes, String changeLabel) {
		int nbChanges = changes.size();
		if (nbChanges == 0) {
			return null;
		} else if (nbChanges == 1) {
			return changes.get(0);
		} else {
			return new CompositeChange(changeLabel, changes.toArray(new Change[changes.size()]));
		}
	}

	/**
	 * Create a change for each launch configuration from the given list which needs to be updated for this IProject rename.
	 */
	private static List<Change> createChangesForProjectRename(ILaunchConfiguration[] configs, String projectName, String newProjectName) throws CoreException {
		List<Change> changes = new ArrayList<>();
		for (ILaunchConfiguration launchConfiguration : configs) {
			String launchConfigurationProjectName = launchConfiguration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, (String) null);
			if (projectName.equals(launchConfigurationProjectName)) {
				LaunchConfigurationBuildfileChange change = new LaunchConfigurationBuildfileChange(launchConfiguration, null, null, newProjectName, false);
				String newContainerName = computeNewContainerName(launchConfiguration);
				if (newContainerName != null) {
					change.setNewContainerName(newContainerName);
				}

				changes.add(change);
			}
		}
		return changes;
	}

	/**
	 * Create a change for each launch configuration from the given list which needs to be updated for this buildfile rename.
	 */
	private static List<Change> createChangesForBuildfileRename(IFile buildfile, ILaunchConfiguration[] configs, String projectName, String newBuildfileName) throws CoreException {
		List<Change> changes = new ArrayList<>();
		for (ILaunchConfiguration launchConfiguration : configs) {
			String launchConfigurationProjectName = launchConfiguration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, (String) null);
			if (projectName.equals(launchConfigurationProjectName)) {
				LaunchConfigurationBuildfileChange change = new LaunchConfigurationBuildfileChange(launchConfiguration, buildfile.getName(), newBuildfileName, null, false);
				changes.add(change);
			}
		}
		return changes;
	}

	protected void setNewContainerName(String newContainerName) {
		fNewConfigContainerName = newContainerName;
	}

	/**
	 * Creates a new container name for the given configuration
	 * 
	 * @return the new container name
	 * @since 3.5
	 */
	private static String computeNewContainerName(ILaunchConfiguration launchConfiguration) {
		IFile file = launchConfiguration.getFile();
		if (file != null) {
			return file.getParent().getProjectRelativePath().toString();
		}
		return null;
	}

	protected LaunchConfigurationBuildfileChange(ILaunchConfiguration launchConfiguration, String oldBuildFileName, String newBuildfileName, String newProjectName, boolean undo) throws CoreException {
		fLaunchConfiguration = launchConfiguration;
		fNewLaunchConfiguration = launchConfiguration.getWorkingCopy();
		fNewBuildfileLocation = newBuildfileName;
		fNewProjectName = newProjectName;
		fOldBuildfileLocation = oldBuildFileName;
		fOldProjectName = fLaunchConfiguration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, (String) null);
		if (fNewBuildfileLocation != null) {
			// generate the new configuration name
			String launchConfigurationName = fLaunchConfiguration.getName();
			fNewLaunchConfigurationName = launchConfigurationName.replaceAll(oldBuildFileName, newBuildfileName);
			if (launchConfigurationName.equals(fNewLaunchConfigurationName)
					|| (!undo && DebugPlugin.getDefault().getLaunchManager().isExistingLaunchConfigurationName(fNewLaunchConfigurationName))) {
				fNewLaunchConfigurationName = null;
			}
		}
	}

	@Override
	public String getName() {
		if (fNewLaunchConfigurationName != null) {
			return MessageFormat.format(RefactoringMessages.LaunchConfigurationBuildfileChange_0, new Object[] { fLaunchConfiguration.getName(),
					fNewLaunchConfigurationName });
		}
		if (fNewProjectName == null) {
			return MessageFormat.format(RefactoringMessages.LaunchConfigurationBuildfileChange_1, new Object[] { fLaunchConfiguration.getName() });
		}
		return MessageFormat.format(RefactoringMessages.LaunchConfigurationBuildfileChange_2, new Object[] { fLaunchConfiguration.getName() });
	}

	@Override
	public void initializeValidationData(IProgressMonitor pm) {
		// do nothing
	}

	@Override
	public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		if (fLaunchConfiguration.exists()) {
			String buildFileLocation = fLaunchConfiguration.getAttribute(IExternalToolConstants.ATTR_LOCATION, IAntCoreConstants.EMPTY_STRING);
			if (fOldBuildfileLocation == null
					|| (buildFileLocation.endsWith(fOldBuildfileLocation + '}') || buildFileLocation.endsWith(fOldBuildfileLocation))) {
				String projectName = fLaunchConfiguration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, (String) null);
				if (fOldProjectName.equals(projectName)) {
					return new RefactoringStatus();
				}
				return RefactoringStatus.createWarningStatus(MessageFormat.format(RefactoringMessages.LaunchConfigurationBuildfileChange_4, new Object[] {
						fLaunchConfiguration.getName(), fOldProjectName }));
			}
			return RefactoringStatus.createWarningStatus(MessageFormat.format(RefactoringMessages.LaunchConfigurationBuildfileChange_5, new Object[] {
					fLaunchConfiguration.getName(), fOldBuildfileLocation }));
		}
		return RefactoringStatus.createFatalErrorStatus(MessageFormat.format(RefactoringMessages.LaunchConfigurationBuildfileChange_6, new Object[] {
				fLaunchConfiguration.getName() }));
	}

	@Override
	public Change perform(IProgressMonitor pm) throws CoreException {
		if (fNewConfigContainerName != null) {
			IWorkspace workspace = ResourcesPlugin.getWorkspace();
			IWorkspaceRoot root = workspace.getRoot();
			IProject project = root.getProject(fNewProjectName);
			IContainer container = (IContainer) project.findMember(fNewConfigContainerName);
			fNewLaunchConfiguration.setContainer(container);
		}

		String oldBuildfileLocation = fNewLaunchConfiguration.getAttribute(IExternalToolConstants.ATTR_LOCATION, IAntCoreConstants.EMPTY_STRING);
		String oldProjectName;
		if (fNewBuildfileLocation != null) {
			String newBuildFileLocation = oldBuildfileLocation.replaceFirst(fOldBuildfileLocation, fNewBuildfileLocation);
			fNewLaunchConfiguration.setAttribute(IExternalToolConstants.ATTR_LOCATION, newBuildFileLocation);
			fNewLaunchConfiguration.setMappedResources(new IResource[] { getAssociatedFile(newBuildFileLocation) });
		}
		if (fNewProjectName != null) {
			oldProjectName = fOldProjectName;
			fNewLaunchConfiguration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, fNewProjectName);
			String newBuildFileLocation = oldBuildfileLocation.replaceFirst(oldProjectName, fNewProjectName);
			fNewLaunchConfiguration.setAttribute(IExternalToolConstants.ATTR_LOCATION, newBuildFileLocation);
			fNewLaunchConfiguration.setMappedResources(new IResource[] { getAssociatedFile(newBuildFileLocation) });
			String launchConfigurationName = fLaunchConfiguration.getName();
			fNewLaunchConfigurationName = launchConfigurationName.replaceFirst(oldProjectName, fNewProjectName);
			if (launchConfigurationName.equals(fNewLaunchConfigurationName)
					|| DebugPlugin.getDefault().getLaunchManager().isExistingLaunchConfigurationName(fNewLaunchConfigurationName)) {
				fNewLaunchConfigurationName = null;
			}
		} else {
			oldProjectName = null;
		}
		if (fNewLaunchConfigurationName != null) {
			fNewLaunchConfiguration.rename(fNewLaunchConfigurationName);
		}

		fNewLaunchConfiguration.doSave();

		// create the undo change
		return new LaunchConfigurationBuildfileChange(fNewLaunchConfiguration, fNewBuildfileLocation, fOldBuildfileLocation, oldProjectName, true);
	}

	@Override
	public Object getModifiedElement() {
		return fLaunchConfiguration;
	}

	private IFile getAssociatedFile(String location) {
		IFile file = null;
		IStringVariableManager manager = VariablesPlugin.getDefault().getStringVariableManager();
		try {
			String expandedLocation = manager.performStringSubstitution(location);
			if (expandedLocation != null) {
				file = AntUtil.getFileForLocation(expandedLocation, null);
			}
		}
		catch (CoreException e) {
			// do nothing
		}
		return file;
	}
}