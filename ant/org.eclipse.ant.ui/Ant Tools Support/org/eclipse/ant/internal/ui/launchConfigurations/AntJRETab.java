/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ant.internal.ui.launchConfigurations;

import org.eclipse.ant.internal.ui.AntUIPlugin;
import org.eclipse.ant.internal.ui.IAntUIConstants;
import org.eclipse.ant.internal.ui.IAntUIHelpContextIds;
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
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.externaltools.internal.launchConfigurations.ExternalToolsUtil;
import org.eclipse.ui.externaltools.internal.model.IExternalToolConstants;

public class AntJRETab extends JavaJRETab {

	private static final String MAIN_TYPE_NAME= "org.eclipse.ant.internal.ui.antsupport.InternalAntRunner"; //$NON-NLS-1$
	
	private VMArgumentsBlock fVMArgumentsBlock=  new VMArgumentsBlock();
	private AntWorkingDirectoryBlock fWorkingDirectoryBlock= new AntWorkingDirectoryBlock();

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		super.createControl(parent);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IAntUIHelpContextIds.ANT_JRE_TAB);
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
				return AntLaunchConfigurationMessages.AntJRETab_2;
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
				return AntLaunchConfigurationMessages.AntJRETab_3;
			}
		};
	}
		
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		boolean isDefaultJRE = fJREBlock.isDefaultJRE();
        fWorkingDirectoryBlock.setEnabled(!isDefaultJRE);
		fVMArgumentsBlock.setEnabled(!isDefaultJRE);
		if (isDefaultJRE) {
			configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_INSTALL_NAME, (String)null);
			configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_INSTALL_TYPE, (String)null);
			configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_JRE_CONTAINER_PATH, (String)null);
			configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, (String)null);
			configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, (String)null);
			configuration.setAttribute(IAntUIConstants.ATTR_DEFAULT_VM_INSTALL, false);
		} else {
			super.performApply(configuration);
			
			if (useDefaultSeparateJRE(configuration)) {
				configuration.setAttribute(IAntUIConstants.ATTR_DEFAULT_VM_INSTALL, true);
			} else {
				configuration.setAttribute(IAntUIConstants.ATTR_DEFAULT_VM_INSTALL, false);
			}
			
			applySeparateVMAttributes(configuration);
			fVMArgumentsBlock.performApply(configuration);
			fWorkingDirectoryBlock.performApply(configuration);
		}
		setLaunchConfigurationWorkingCopy(configuration);
	}
	
	private boolean useDefaultSeparateJRE(ILaunchConfigurationWorkingCopy configuration) {
		boolean deflt= false;
		String vmInstallType= null;
        String jreContainerPath= null;
		try {
			vmInstallType= configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_INSTALL_TYPE, (String)null);
            jreContainerPath= configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_JRE_CONTAINER_PATH, (String)null);
		} catch (CoreException e) {
		}
		if (vmInstallType != null) {
			configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_INSTALL_TYPE, (String)null);
		}
        if (jreContainerPath != null) {
            configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_JRE_CONTAINER_PATH, (String)null);
        }
		IVMInstall defaultVMInstall= getDefaultVMInstall(configuration);
		if (defaultVMInstall != null) {
			IVMInstall vm= fJREBlock.getJRE();
			deflt= defaultVMInstall.equals(vm);
		}
		
		if (vmInstallType != null) {
			configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_INSTALL_TYPE, vmInstallType);
		}
        if (jreContainerPath != null) {
            configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_JRE_CONTAINER_PATH, jreContainerPath);
        }
		return deflt;
	}
	
	private void applySeparateVMAttributes(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, MAIN_TYPE_NAME);
		//only set to use the remote ant process factory if the user
		//has not set to use a logger...bug 84608
        boolean userLogger= false;
        try {
    		String args = configuration.getAttribute(IExternalToolConstants.ATTR_TOOL_ARGUMENTS, (String) null);
    		if (args != null) {
    			String[] arguments = ExternalToolsUtil.parseStringIntoList(args);
    			if (arguments != null) {
                    for (int i = 0; i < arguments.length; i++) {
                        String arg = arguments[i];
                        if (arg.equals("-logger")) { //$NON-NLS-1$
                            userLogger= true;
                            break;
                        }
                    }
                }
    		}
        } catch (CoreException e) {
        }
        if (userLogger) {
            configuration.setAttribute(DebugPlugin.ATTR_PROCESS_FACTORY_ID, (String) null);
        } else {
            configuration.setAttribute(DebugPlugin.ATTR_PROCESS_FACTORY_ID, IAntUIConstants.REMOTE_ANT_PROCESS_FACTORY_ID);
        }
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public void initializeFrom(ILaunchConfiguration configuration) {
		try {
			 boolean isDefaultVMInstall= configuration.getAttribute(IAntUIConstants.ATTR_DEFAULT_VM_INSTALL, false);
			 if (isDefaultVMInstall) {
			 	ILaunchConfigurationWorkingCopy copy = null;
			 	if (configuration instanceof ILaunchConfigurationWorkingCopy) {
			 		copy= (ILaunchConfigurationWorkingCopy) configuration;
			 	} else {
			 		copy= configuration.getWorkingCopy();
			 	}
			 	
			 	//null out the vm type and jre container path to get the default vm install from JavaRuntime
			 	copy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_INSTALL_TYPE, (String)null);
			 	copy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_JRE_CONTAINER_PATH, (String)null);
			 	IVMInstall defaultVMInstall= getDefaultVMInstall(copy);
			 	if (defaultVMInstall != null) {
			 		//update if required
			 		setDefaultVMInstallAttributes(defaultVMInstall, copy);
			 	}
				if (copy.isDirty() && !copy.isReadOnly()) {
					configuration= copy.doSave();
				}
			 }
        } catch (CoreException ce) {
        	AntUIPlugin.log(ce);
        }
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
		return super.isValid(config) && fWorkingDirectoryBlock.isValid(config);
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
		config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_SOURCE_PATH_PROVIDER, "org.eclipse.ant.ui.AntClasspathProvider"); //$NON-NLS-1$
		IVMInstall defaultVMInstall= getDefaultVMInstall(config);
		if (defaultVMInstall != null) {
			config.setAttribute(IAntUIConstants.ATTR_DEFAULT_VM_INSTALL, true);
			setDefaultVMInstallAttributes(defaultVMInstall, config);
			applySeparateVMAttributes(config);
		}
		
	}

	private IVMInstall getDefaultVMInstall(ILaunchConfiguration config) {
		IVMInstall defaultVMInstall;
		try {
			defaultVMInstall = JavaRuntime.computeVMInstall(config);
		} catch (CoreException e) {
			//core exception thrown for non-Java project
			defaultVMInstall= JavaRuntime.getDefaultVMInstall();
		}
		return defaultVMInstall;
	}

	private void setDefaultVMInstallAttributes(IVMInstall defaultVMInstall, ILaunchConfigurationWorkingCopy config) {
		String vmName = defaultVMInstall.getName();
		String vmTypeID = defaultVMInstall.getVMInstallType().getId();
		config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_INSTALL_NAME, vmName);
		config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_INSTALL_TYPE, vmTypeID);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#deactivated(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void deactivated(ILaunchConfigurationWorkingCopy workingCopy) {
	}
}
