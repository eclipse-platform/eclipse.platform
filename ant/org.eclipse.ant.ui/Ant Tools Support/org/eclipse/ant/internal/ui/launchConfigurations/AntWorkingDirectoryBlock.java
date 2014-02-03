/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Remy Chi Jian Suen <remy.suen@gmail.com> - Bug 214696 Expose WorkingDirectoryBlock as API
 *******************************************************************************/
package org.eclipse.ant.internal.ui.launchConfigurations;

import org.eclipse.ant.internal.ui.AntUIPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.internal.debug.ui.launcher.JavaWorkingDirectoryBlock;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.ui.externaltools.internal.launchConfigurations.ExternalToolsUtil;

import com.ibm.icu.text.MessageFormat;


public class AntWorkingDirectoryBlock extends JavaWorkingDirectoryBlock {
	
	private String fDefaultWorkingDirPath;

	/**
	 * Returns the default working directory path
	 * @return the default working directory path
	 */
	public String getDefaultWorkingDirPath() {
		return fDefaultWorkingDirPath;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.debug.ui.launcher.WorkingDirectoryBlock#setDefaultWorkingDir()
	 */
	protected void setDefaultWorkingDir() {
		if (fDefaultWorkingDirPath == null) {
			super.setDefaultWorkingDir();
			return;
		}
		setDefaultWorkingDirectoryText(fDefaultWorkingDirPath);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public void initializeFrom(ILaunchConfiguration configuration) {
		setLaunchConfiguration(configuration);
		try {
			try {
				fDefaultWorkingDirPath= ExternalToolsUtil.getLocation(configuration).removeLastSegments(1).toOSString();
			}
			catch(CoreException ce){}
			String wd = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_WORKING_DIRECTORY, (String)null);
			setDefaultWorkingDir();
			if (wd != null || !isSameAsDefault(wd)) {
				setOtherWorkingDirectoryText(wd);
			}
		} catch (CoreException e) {
			setErrorMessage(MessageFormat.format(AntLaunchConfigurationMessages.AntWorkingDirectoryBlock_0, new String[] {e.getStatus().getMessage()}));
			AntUIPlugin.log(e);
		}
	}
	
	private boolean isSameAsDefault(String workingDir) {
		return workingDir == null || (workingDir.equals(fDefaultWorkingDirPath) || workingDir.equals(System.getProperty("user.dir"))); //$NON-NLS-1$
	}
}
