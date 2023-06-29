/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.launchConfigurations;


import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.ui.ILaunchGroup;
import org.eclipse.jface.resource.ImageDescriptor;


/**
 * Proxy to a launch group extension
 */
public class LaunchGroupExtension implements ILaunchGroup {

	/**
	 * The configuration element defining this launch group.
	 */
	private IConfigurationElement fConfig;

	/**
	 * The image for this group
	 */
	private ImageDescriptor fImageDescriptor;

	/**
	 * The banner image for this group
	 */
	private ImageDescriptor fBannerImageDescriptor;

	/**
	 * Constructs a launch group extension based on the given configuration
	 * element
	 *
	 * @param element the configuration element defining the
	 *  attributes of this launch group extension
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
	@Override
	public ImageDescriptor getImageDescriptor() {
		if (fImageDescriptor == null) {
			fImageDescriptor = createImageDescriptor("image"); //$NON-NLS-1$
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
	@Override
	public ImageDescriptor getBannerImageDescriptor() {
		if (fBannerImageDescriptor == null) {
			fBannerImageDescriptor = createImageDescriptor("bannerImage"); //$NON-NLS-1$
		}
		return fBannerImageDescriptor;
	}

	/**
	 * Returns the label for this launch group
	 *
	 * @return the label for this launch group
	 */
	@Override
	public String getLabel() {
		return getConfigurationElement().getAttribute("label"); //$NON-NLS-1$
	}

	/**
	 * Returns the id for this launch group
	 *
	 * @return the id for this launch group
	 */
	@Override
	public String getIdentifier() {
		return getConfigurationElement().getAttribute("id"); //$NON-NLS-1$
	}

	/**
	 * Returns the category for this launch group, possibly <code>null</code>
	 *
	 * @return the category for this launch group, possibly <code>null</code>
	 */
	@Override
	public String getCategory() {
		return getConfigurationElement().getAttribute("category"); //$NON-NLS-1$
	}

	/**
	 * Returns the mode for this launch group
	 *
	 * @return the mode for this launch group
	 */
	@Override
	public String getMode() {
		return getConfigurationElement().getAttribute("mode"); //$NON-NLS-1$
	}

	/**
	 * Creates an image descriptor based on the given attribute name
	 *
	 * @param attribute
	 * @return ImageDescriptor
	 */
	protected ImageDescriptor createImageDescriptor(String attribute) {
		return DebugUIPlugin.getImageDescriptor(getConfigurationElement(), attribute);
	}

	/**
	 * Returns whether this launch group is public
	 *
	 * @return boolean
	 */
	@Override
	public boolean isPublic() {
		String string = getConfigurationElement().getAttribute("public"); //$NON-NLS-1$
		if (string == null) {
			return true;
		}
		return string.equals("true"); //$NON-NLS-1$
	}

}

