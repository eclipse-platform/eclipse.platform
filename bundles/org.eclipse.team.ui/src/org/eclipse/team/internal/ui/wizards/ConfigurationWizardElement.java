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
package org.eclipse.team.internal.ui.wizards;


import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.IConfigurationWizard;
import org.eclipse.team.ui.IConfigurationWizardExtension;
import org.eclipse.ui.*;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.WorkbenchAdapter;

/**
 * ConfigurationWizardElement represents an item in the configuration wizard table,
 * declared by an extension to the configurationWizards extension point.
 */
public class ConfigurationWizardElement extends WorkbenchAdapter implements IAdaptable, IPluginContribution  {
	private String id;
	private String name;
	private ImageDescriptor imageDescriptor;
	private IConfigurationElement configurationElement;
	
	/**
	 *	Creates a new instance of this class
	 *
	 *	@param name  the name of the element
	 */
	public ConfigurationWizardElement(String name) {
		this.name = name;
	}
	/**
	 * Create an the instance of the object described by the configuration
	 * element. That is, create the instance of the class the isv supplied in
	 * the extension point.
	 * @return the instance of the configuration wizard of type {@link IConfigurationWizard}
	 * 
	 * @throws CoreException if an error occurs creating the extension
	 */
	public Object createExecutableExtension() throws CoreException {
		return TeamUIPlugin.createExtension(configurationElement, ConfigureProjectWizard.ATT_CLASS);
	}
	
	/**
	 * Creates the instance of the wizard and initializes with the given input.
	 * @param projects the projects being shared by this wizard
	 * @return the wizard instance of type {@link IConfigurationWizard}
	 * @throws CoreException if an error occurs creating the extension 
	 */
	public IWizard createExecutableExtension(IProject[] projects) throws CoreException {
		IWorkbench workbench = PlatformUI.getWorkbench();
		IConfigurationWizard wizard = (IConfigurationWizard)createExecutableExtension();
		IConfigurationWizardExtension extension = (IConfigurationWizardExtension)Utils.getAdapter(wizard, IConfigurationWizardExtension.class);
		if (extension == null) {
			if (projects.length == 1) {
				wizard.init(workbench, projects[0]);
			} else {
				// Dispose of the created wizard, just in case
				try {
					wizard.dispose();
				} catch (RuntimeException e) {
					// If a general exception occurred here, log it and continue
					TeamUIPlugin.log(IStatus.ERROR, "An internal error occurred", e); //$NON-NLS-1$
				}
				IWizard multiWizard = new ConfigureMultipleProjectsWizard(projects, this);
				return multiWizard;
			}
		} else {
			extension.init(workbench, projects);
		}
		return wizard;
	}
	
	/*
	 * Method declared on IAdaptable.
	 */
	@Override
	public Object getAdapter(Class adapter) {
		if (adapter == IWorkbenchAdapter.class) {
			return this;
		}
		return Platform.getAdapterManager().getAdapter(this, adapter);
	}
	/**
	 * Returns the configuration element
	 * 
	 * @return the configuration element
	 */
	public IConfigurationElement getConfigurationElement() {
		return configurationElement;
	}
	/**
	 * Returns the image for the given element
	 * 
	 * @param element  the element to get the image for
	 * @return the image for the given element
	 */
	@Override
	public ImageDescriptor getImageDescriptor(Object element) {
		return imageDescriptor;
	}
	/**
	 * Returns the label for the given element
	 * 
	 * @param element  the element to get the label for
	 * @return the label for the given element
	 */
	@Override
	public String getLabel(Object element) {
		return name;
	}
	/**
	 * Returns the id as specified in the extension.
	 *
	 * @return java.lang.String
	 */
	public String getID() {
		return id;
	}
	/**
	 * Returns the image for this element.
	 * 
	 * @return the image for this element
	 */
	public ImageDescriptor getImageDescriptor() {
		return imageDescriptor;
	}
	/**
	 * Set the configuration element
	 * 
	 * @param newConfigurationElement  the new configuration element
	 */
	public void setConfigurationElement(IConfigurationElement newConfigurationElement) {
		configurationElement = newConfigurationElement;
	}
	/**
	 * Set the description parameter of this element
	 *
	 * @param value  the new description
	 */
	public void setDescription(String value) {
		// Not used
	}
	/**
	 * Sets the id parameter of this element
	 * 
	 * @param value  the new ID
	 */
	public void setID(String value) {
		id = value;
	}
	/**
	 * Sets the image for this element.
	 * 
	 * @param value  the new image
	 */
	public void setImageDescriptor(ImageDescriptor value) {
		imageDescriptor = value;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPluginContribution#getLocalId()
	 */
	@Override
	public String getLocalId() {
		return configurationElement.getAttribute(ConfigureProjectWizard.ATT_ID);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPluginContribution#getPluginId()
	 */
	@Override
	public String getPluginId() {
		return configurationElement.getNamespaceIdentifier();
	}
	
	/**
	 * Return whether the wizard created for this element has pages.
	 * Unfortunately, the only way to find this out is to create the wizard.
	 * @param projects the projects being shared
	 * @return whether the resulting wizard has pages
	 */
	public boolean wizardHasPages(IProject[] projects) {
		try {
			IWizard wizard = (IWizard)createExecutableExtension(projects);
			try {
				wizard.addPages();
				return (wizard.getPageCount() > 0);
			} finally {
				wizard.dispose();
			}
		} catch (CoreException e) {
			TeamUIPlugin.log(e);
		} catch (RuntimeException e) {
			// If a general exception occurred here, log it and continue
			TeamUIPlugin.log(IStatus.ERROR, "An internal error occurred", e); //$NON-NLS-1$
		}
		return false;
	}
}
