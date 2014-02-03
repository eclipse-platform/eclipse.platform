/*******************************************************************************
 * Copyright (c) 2006, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.internal.launching.launchConfigurations;

import org.eclipse.ant.internal.launching.AntLaunchingUtil;
import org.eclipse.core.externaltools.internal.IExternalToolConstants;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationMigrationDelegate;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;

/**
 * Delegate for migrating Ant launch configurations.
 * The migration process involves a resource mapping being created such that launch configurations
 * can be filtered from the launch configuration dialog based on resource availability.
 * 
 * @since 3.2
 */
public class AntMigrationDelegate implements ILaunchConfigurationMigrationDelegate {
	
	/**
	 * Method to get the file for the specified launch configuration that should be mapped to the launch configuration  
	 * 
	 * @param candidate the launch configuration that the file will be mapped to.
	 * @return the buildfile or <code>null</code> if not in the workspace
	 */
	protected IFile getFileForCandidate(ILaunchConfiguration candidate) {
		IFile file= null;
		String expandedLocation= null;
		String location= null;
		IStringVariableManager manager = VariablesPlugin.getDefault().getStringVariableManager();
		try {
			location= candidate.getAttribute(IExternalToolConstants.ATTR_LOCATION, (String)null);
			if (location != null) {
				expandedLocation= manager.performStringSubstitution(location);
				if (expandedLocation != null) {
					file= AntLaunchingUtil.getFileForLocation(expandedLocation, null);
				}
			}
		} catch (CoreException e) {
		}
		return file;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.ILaunchConfigurationMigrationDelegate#isCandidate()
	 */
	public boolean isCandidate(ILaunchConfiguration candidate) throws CoreException {
		IResource[] mappedResources = candidate.getMappedResources();
		if (mappedResources != null && mappedResources.length > 0) {
			return false;
		}
		return getFileForCandidate(candidate) != null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.ILaunchConfigurationMigrationDelegate#migrate(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public void migrate(ILaunchConfiguration candidate) throws CoreException {
		IFile file = getFileForCandidate(candidate);
		ILaunchConfigurationWorkingCopy wc = candidate.getWorkingCopy();
		wc.setMappedResources(new IResource[] {file});
		wc.doSave();
	}
}