package org.eclipse.debug.internal.ui.launchConfigurations;

/**********************************************************************
Copyright (c) 2002 IBM Corp. and others. All rights reserved.
This file is made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html
 
Contributors:
**********************************************************************/
 
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.jface.resource.ImageDescriptor;


/**
 * Proxy to a launch group extension
 */
public class LaunchGroupExtension {
	
	/**
	 * The configuration element defining this launch group.
	 */
	private IConfigurationElement fConfig;
	
	/**
	 * The image for this group
	 */
	private ImageDescriptor fImageDescriptor;
	
	/**
	 * The banner image for this group	 */
	private ImageDescriptor fBannerImageDescriptor;
	
	/**
	 * Constructs a launch group extension based on the given configuration
	 * element
	 * 
	 * @param element the configuration element defining the
	 *  attribtues of this launch group extension
	 * @return a new launch group extension
	 */
	public LaunchGroupExtension(IConfigurationElement element) {
		setConfigurationElement(element);
	}
	
	/**
	 * Sets the configuration element that defines the attributes
	 * for this launch group extension.
	 * 
	 * @param element configuration element
	 */
	private void setConfigurationElement(IConfigurationElement element) {
		fConfig = element;
	}
	
	/**
	 * Returns the configuration element that defines the attributes
	 * for this launch group extension.
	 * 
	 * @param configuration element that defines the attributes
	 *  for this launch group extension
	 */
	protected IConfigurationElement getConfigurationElement() {
		return fConfig;
	}
	
	/**
	 * Returns the image for this launch group, or <code>null</code> if none
	 * 
	 * @return the image for this launch group, or <code>null</code> if none
	 */
	public ImageDescriptor getImageDescriptor() {
		if (fImageDescriptor == null) {
			fImageDescriptor = createImageDescriptor("image");
		}
		return fImageDescriptor;
	}
	
	/**
	 * Returns the banner image for this launch group, or <code>null</code> if
	 * none
	 * 
	 * @return the banner image for this launch group, or <code>null</code> if
	 * none
	 */
	public ImageDescriptor getBannerImageDescriptor() {
		if (fBannerImageDescriptor == null) {
			fBannerImageDescriptor = createImageDescriptor("bannerImage");
		}
		return fBannerImageDescriptor;
	}	
	
	/**
	 * Returns the label for this launch group
	 * 
	 * @return the label for this launch group
	 */
	public String getLabel() {
		return getConfigurationElement().getAttribute("label");
	}	
	
	/**
	 * Returns the id for this launch group
	 * 
	 * @return the id for this launch group
	 */
	public String getIdentifier() {
		return getConfigurationElement().getAttribute("id");
	}	
	
	/**
	 * Returns the category for this launch group, possibly <code>null</code>
	 * 
	 * @return the category for this launch group, possibly <code>null</code>
	 */
	public String getCategory() {
		return getConfigurationElement().getAttribute("category");
	}
	
	/**
	 * Returns the mode for this launch group
	 * 
	 * @return the mode for this launch group
	 */
	public String getMode() {
		return getConfigurationElement().getAttribute("mode");
	}					
	
	/**
	 * Creates an image descriptor based on the given attribute name
	 * 
	 * @param attribute	 * @return ImageDescriptor	 */
	protected ImageDescriptor createImageDescriptor(String attribute) {
		URL iconURL = getConfigurationElement().getDeclaringExtension().getDeclaringPluginDescriptor().getInstallURL();
		String iconPath = getConfigurationElement().getAttribute(attribute);
		if (iconPath != null) {
			try {
				iconURL = new URL(iconURL, iconPath);
				return ImageDescriptor.createFromURL(iconURL);
			} catch (MalformedURLException e) {
				DebugUIPlugin.log(e);
			}
		}
		return null;
	}
}

