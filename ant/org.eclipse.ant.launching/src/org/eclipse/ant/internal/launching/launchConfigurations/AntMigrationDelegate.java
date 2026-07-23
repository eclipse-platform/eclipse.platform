/*******************************************************************************
 * Copyright (c) 2006, 2025 IBM Corporation and others.
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
 *     Alexander Fedorov (ArSysOp) - API to process launch configuration attributes
 *******************************************************************************/
package org.eclipse.ant.internal.launching.launchConfigurations;

import org.eclipse.ant.internal.launching.AntLaunchingUtil;
import org.eclipse.core.externaltools.internal.IExternalToolConstants;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationMigrationDelegate;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;

/**
 * Delegate for migrating Ant launch configurations. The migration process involves a resource mapping being created such that launch configurations
 * can be filtered from the launch configuration dialog based on resource availability.
 *
 * @since 3.2
 */
public class AntMigrationDelegate implements ILaunchConfigurationMigrationDelegate {

	/**
	 * Method to get the file for the specified launch configuration that should be mapped to the launch configuration
	 *
	 * @param candidate
	 *            the launch configuration that the file will be mapped to.
	 * @return the buildfile or <code>null</code> if not in the workspace
	 */
	protected IFile getFileForCandidate(ILaunchConfiguration candidate) {
		return IExternalToolConstants.LAUNCH_ATTRIBUTE_LOCATION.probe(candidate).map(l -> AntLaunchingUtil.getFileForLocation(l, null)).orElse(null);
	}

	@Override
	public boolean isCandidate(ILaunchConfiguration candidate) throws CoreException {
		IResource[] mappedResources = candidate.getMappedResources();
		if (mappedResources != null && mappedResources.length > 0) {
			return false;
		}
		return getFileForCandidate(candidate) != null;
	}

	@Override
	public void migrate(ILaunchConfiguration candidate) throws CoreException {
		IFile file = getFileForCandidate(candidate);
		ILaunchConfigurationWorkingCopy wc = candidate.getWorkingCopy();
		wc.setMappedResources(new IResource[] { file });
		wc.doSave();
	}
}