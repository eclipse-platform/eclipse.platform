/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ant.internal.ui.launchConfigurations;

import org.eclipse.ant.internal.ui.model.IAntUIConstants;
import org.eclipse.ant.internal.ui.model.IAntUIHelpContextIds;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaJRETab;
import org.eclipse.jdt.internal.debug.ui.jres.JREDescriptor;
import org.eclipse.jdt.internal.debug.ui.launcher.VMArgumentsBlock;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.help.WorkbenchHelp;

public class AntJRETab extends JavaJRETab {

	private static final String MAIN_TYPE_NAME= "org.eclipse.ant.internal.ui.antsupport.InternalAntRunner"; //$NON-NLS-1$
	
	protected VMArgumentsBlock fVMArgumentsBlock=  new VMArgumentsBlock();
	protected AntWorkingDirectoryBlock fWorkingDirectoryBlock= new AntWorkingDirectoryBlock();

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		super.createControl(parent);
		WorkbenchHelp.setHelp(getControl(), IAntUIHelpContextIds.ANT_JRE_TAB);
		Composite comp= (Composite)fJREBlock.getControl();
		((GridData)comp.getLayoutData()).grabExcessVerticalSpace= true;
		((GridData)comp.getLayoutData()).verticalAlignment= SWT.FILL;
		
		fVMArgumentsBlock.createControl(comp);
		((GridData)fVMArgumentsBlock.getControl().getLayoutData()).horizontalSpan= 2;
						
		fWorkingDirectoryBlock.createControl(comp);		
		((GridData)fWorkingDirectoryBlock.getControl().getLayoutData()).horizontalSpan= 2;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.debug.ui.launchConfigurations.JavaJRETab#getDefaultJREDescriptor()
	 */
	protected JREDescriptor getDefaultJREDescriptor() {
		return new JREDescriptor() {
			/* (non-Javadoc)
			 * @see org.eclipse.jdt.internal.debug.ui.jres.JREDescriptor#getDescription()
			 */
			public String getDescription() {
				return AntLaunchConfigurationMessages.getString("AntJRETab.2"); //$NON-NLS-1$
			}
		};
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.debug.ui.launchConfigurations.JavaJRETab#getSpecificJREDescriptor()
	 */
	protected JREDescriptor getSpecificJREDescriptor() {
		return new JREDescriptor() {
			/* (non-Javadoc)
			 * @see org.eclipse.jdt.internal.debug.ui.jres.JREDescriptor#getDescription()
			 */
			public String getDescription() {
				return AntLaunchConfigurationMessages.getString("AntJRETab.3"); //$NON-NLS-1$
			}
		};
	}
		
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		fWorkingDirectoryBlock.setEnabled(!fJREBlock.isDefaultJRE());
		fVMArgumentsBlock.setEnabled(!fJREBlock.isDefaultJRE());
		if (fJREBlock.isDefaultJRE()) {
			configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_INSTALL_NAME, (String)null);
			configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_INSTALL_TYPE, (String)null);
			configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, (String)null);
			configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, (String)null);			
		} else {
			super.performApply(configuration);
			applySeparateVMAttributes(configuration);
			fVMArgumentsBlock.performApply(configuration);
			fWorkingDirectoryBlock.performApply(configuration);
		}
		setLaunchConfigurationWorkingCopy(configuration);
	}
	
	private void applySeparateVMAttributes(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, MAIN_TYPE_NAME);
		configuration.setAttribute(DebugPlugin.ATTR_PROCESS_FACTORY_ID, IAntUIConstants.REMOTE_ANT_PROCESS_FACTORY_ID);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public void initializeFrom(ILaunchConfiguration configuration) {
		super.initializeFrom(configuration);
		fVMArgumentsBlock.initializeFrom(configuration);
		fWorkingDirectoryBlock.initializeFrom(configuration);
		boolean separateVM= !fJREBlock.isDefaultJRE();
		fWorkingDirectoryBlock.setEnabled(separateVM);
		fVMArgumentsBlock.setEnabled(separateVM);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#isValid(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public boolean isValid(ILaunchConfiguration config) {
		return fWorkingDirectoryBlock.isValid(config);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setLaunchConfigurationDialog(org.eclipse.debug.ui.ILaunchConfigurationDialog)
	 */
	public void setLaunchConfigurationDialog(ILaunchConfigurationDialog dialog) {
		super.setLaunchConfigurationDialog(dialog);
		fWorkingDirectoryBlock.setLaunchConfigurationDialog(dialog);
		fVMArgumentsBlock.setLaunchConfigurationDialog(dialog);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getErrorMessage()
	 */
	public String getErrorMessage() {
		String m = super.getErrorMessage();
		if (m == null) {
			return fWorkingDirectoryBlock.getErrorMessage();
		}
		return m;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getMessage()
	 */
	public String getMessage() {
		String m = super.getMessage();
		if (m == null) {
			return fWorkingDirectoryBlock.getMessage();
		}
		return m;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#activated(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void activated(ILaunchConfigurationWorkingCopy workingCopy) {
		setLaunchConfigurationWorkingCopy(workingCopy);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		super.setDefaults(config);
		//by default set an Ant build to occur in a separate VM
		IVMInstall defaultInstall= null;
		try {
			defaultInstall = JavaRuntime.computeVMInstall(config);
		} catch (CoreException e) {
			//core exception thrown for non-Java project
			defaultInstall= JavaRuntime.getDefaultVMInstall();
		}
		if (defaultInstall != null) {
			String vmName = defaultInstall.getName();
			String vmTypeID = defaultInstall.getVMInstallType().getId();					
			config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_INSTALL_NAME, vmName);
			config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_INSTALL_TYPE, vmTypeID);
			applySeparateVMAttributes(config);
		}
	}
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#deactivated(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void deactivated(ILaunchConfigurationWorkingCopy workingCopy) {
	}
}