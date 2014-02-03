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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.ant.core.AntCorePlugin;
import org.eclipse.ant.core.AntCorePreferences;
import org.eclipse.ant.core.Property;
import org.eclipse.ant.internal.ui.AntUIImages;
import org.eclipse.ant.internal.ui.AntUIPlugin;
import org.eclipse.ant.internal.ui.AntUtil;
import org.eclipse.ant.internal.ui.IAntUIConstants;
import org.eclipse.ant.internal.ui.IAntUIHelpContextIds;
import org.eclipse.ant.internal.ui.preferences.AntPropertiesBlock;
import org.eclipse.ant.internal.ui.preferences.IAntBlockContainer;
import org.eclipse.ant.launching.IAntLaunchConstants;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;

/**
 * Tab for setting Ant user properties per launch configuration. All properties
 * specified here will be set as user properties on the project for the
 * specified Ant build
 */
public class AntPropertiesTab extends AbstractLaunchConfigurationTab implements IAntBlockContainer {
	
	private Button fUseDefaultButton;
	private AntPropertiesBlock fAntPropertiesBlock= new AntPropertiesBlock(this);
    private boolean fSeparateJRE= true;
	
	public void createControl(Composite parent) {
		Composite top = new Composite(parent, SWT.NONE);
		top.setFont(parent.getFont());
		setControl(top);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IAntUIHelpContextIds.ANT_PROPERTIES_TAB);

		top.setLayout(new GridLayout());
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		top.setLayoutData(gridData);
		
		createChangeProperties(top);
		
		Composite propertiesBlockComposite= new Composite(top, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns= 2;
		propertiesBlockComposite.setLayout(layout);
		propertiesBlockComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		fAntPropertiesBlock.createControl(propertiesBlockComposite, AntLaunchConfigurationMessages.AntPropertiesTab__Properties__6, AntLaunchConfigurationMessages.AntPropertiesTab_Property_f_iles__7);
		
		Dialog.applyDialogFont(top);
	}
	
	private void createChangeProperties(Composite top) {
		fUseDefaultButton= createCheckButton(top, AntLaunchConfigurationMessages.AntPropertiesTab_6);
		fUseDefaultButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				toggleUseDefaultProperties();
				updateLaunchConfigurationDialog();
			}
		});
	}		
		
	private void toggleUseDefaultProperties() {
		boolean enable= !fUseDefaultButton.getSelection();
		fAntPropertiesBlock.setEnabled(enable);
        if (!enable) {
            initializeAsGlobal(fSeparateJRE);
        }
	}
	
	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getImage()
	 */
	public Image getImage() {
		return AntUIImages.getImage(IAntUIConstants.IMG_PROPERTY);
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
	 */
	public String getName() {
		return AntLaunchConfigurationMessages.AntPropertiesTab_P_roperties_8;
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public void initializeFrom(ILaunchConfiguration configuration) {
        fSeparateJRE= AntUtil.isSeparateJREAntBuild(configuration);
		setErrorMessage(null);
		setMessage(null);
		Map properties= null;
		try {
			properties= configuration.getAttribute(IAntLaunchConstants.ATTR_ANT_PROPERTIES, (Map)null);
		} catch (CoreException ce) {
			AntUIPlugin.log(AntLaunchConfigurationMessages.AntPropertiesTab_Error_reading_configuration_9, ce);
		}
		
		String propertyFiles= null;
		try {
			propertyFiles= configuration.getAttribute(IAntLaunchConstants.ATTR_ANT_PROPERTY_FILES, (String)null);
		} catch (CoreException ce) {
			AntUIPlugin.log(AntLaunchConfigurationMessages.AntPropertiesTab_Error_reading_configuration_9, ce);
		}
		
		if (properties == null && propertyFiles == null) {
		    initializeAsGlobal(fSeparateJRE);
		} else {
			fUseDefaultButton.setSelection(false);
			fAntPropertiesBlock.populatePropertyViewer(properties);
		
			String[] files= AntUtil.parseString(propertyFiles, ","); //$NON-NLS-1$
			fAntPropertiesBlock.setPropertyFilesInput(files);
		}
		
		toggleUseDefaultProperties();
	}

    private void initializeAsGlobal(boolean separateVM) {
        AntCorePreferences prefs = AntCorePlugin.getPlugin().getPreferences();
        List prefProperties;
        if (separateVM) {
            prefProperties= prefs.getRemoteAntProperties();
        } else {
            prefProperties= prefs.getProperties();
        }
        fAntPropertiesBlock.setPropertiesInput((Property[]) prefProperties.toArray(new Property[prefProperties.size()]));
        fAntPropertiesBlock.setPropertyFilesInput(AntCorePlugin.getPlugin().getPreferences().getCustomPropertyFiles(false));
        fAntPropertiesBlock.setTablesEnabled(false);
        fUseDefaultButton.setSelection(true);
    }
	
	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		if (fUseDefaultButton.getSelection()) {
			configuration.setAttribute(IAntLaunchConstants.ATTR_ANT_PROPERTIES, (Map)null);
			configuration.setAttribute(IAntLaunchConstants.ATTR_ANT_PROPERTY_FILES, (String)null);
			return;
		}
				
		Object[] items= fAntPropertiesBlock.getProperties();
		Map properties= null;
		if (items.length > 0) {
			properties= new HashMap(items.length);
			for (int i = 0; i < items.length; i++) {
				Property property = (Property)items[i];
				properties.put(property.getName(), property.getValue(false));
			}
		}
		
		configuration.setAttribute(IAntLaunchConstants.ATTR_ANT_PROPERTIES, properties);
		
		items= fAntPropertiesBlock.getPropertyFiles();
		String files= null;
		if (items.length > 0) {
			StringBuffer buff= new StringBuffer();
			for (int i = 0; i < items.length; i++) {
				String path = (String)items[i];
				buff.append(path);
				buff.append(',');
			}
			files= buff.toString();
		}
		
		configuration.setAttribute(IAntLaunchConstants.ATTR_ANT_PROPERTY_FILES, files);
		
		fAntPropertiesBlock.saveSettings();
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.preferences.IAntBlockContainer#createPushButton(org.eclipse.swt.widgets.Composite, java.lang.String)
	 */
	public void setMessage(String message) {
		super.setMessage(message);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.preferences.IAntBlockContainer#createPushButton(org.eclipse.swt.widgets.Composite, java.lang.String)
	 */
	public void setErrorMessage(String message) {
		super.setErrorMessage(message);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.preferences.IAntBlockContainer#createPushButton(org.eclipse.swt.widgets.Composite, java.lang.String)
	 */
	public Button createPushButton(Composite parent, String buttonText) {
		return super.createPushButton(parent, buttonText, null);
	}
	
	/**
	 * @see org.eclipse.ant.internal.ui.preferences.IAntBlockContainer#update()
	 */
	public void update() {
		updateTargetsTab();
		updateLaunchConfigurationDialog();
	}
	
	private void updateTargetsTab() {
		//the properties have changed...set the targets tab to 
		//need to be recomputed
		ILaunchConfigurationTab[] tabs=  getLaunchConfigurationDialog().getTabs();
		for (int i = 0; i < tabs.length; i++) {
			ILaunchConfigurationTab tab = tabs[i];
			if (tab instanceof AntTargetsTab) {
				((AntTargetsTab)tab).setDirty(true);
				break;
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#activated(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void activated(ILaunchConfigurationWorkingCopy workingCopy) {
        if (fSeparateJRE != AntUtil.isSeparateJREAntBuild(workingCopy)) {
            //update the properties if changed whether build is in separate JRE
            initializeFrom(workingCopy);
        }
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#deactivated(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void deactivated(ILaunchConfigurationWorkingCopy workingCopy) {
	}
}
