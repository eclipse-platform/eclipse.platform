/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
package org.eclipse.help.ui.internal;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.osgi.framework.Bundle;
/**
 * Uses a resource bundle to load images and strings from a property file. This
 * class needs to properly use the desired locale.
 */
public class HelpUIResources {

	private static final String LEAF = "_leaf"; //$NON-NLS-1$
	private static final String CLOSED = "_closed"; //$NON-NLS-1$
	private static final String OPEN = "_open"; //$NON-NLS-1$
	private static final String EXT_PT = "org.eclipse.help.toc"; //$NON-NLS-1$
	private static final String TOC_ICON_ELEMENT = "tocIcon"; //$NON-NLS-1$
	private static final String TOC_ICON_ID = "id"; //$NON-NLS-1$
	private static final String OPEN_ICON_PATH = "openIcon"; //$NON-NLS-1$
	private static final String CLOSED_ICON_PATH = "closedIcon"; //$NON-NLS-1$
	private static final String LEAF_ICON_PATH= "leafIcon"; //$NON-NLS-1$
	private static boolean iconsInitialized = false;

	/**
	 * WorkbenchResources constructor comment.
	 */
	public HelpUIResources() {
		super();
	}
	/**
	 * Returns a string from a property file
	 */
	public static URL getImagePath(String name) {
		IPath path = IPath.fromOSString("$nl$/icons/").append(name); //$NON-NLS-1$
		return FileLocator.find(HelpUIPlugin.getDefault().getBundle(), path, null);
	}

	/**
	 * Returns an image descriptor from a property file
	 * @param name simple image file name
	 * @return the descriptor
	 */

	public static ImageDescriptor getImageDescriptor(String name) {
		URL imagePath = getImagePath(name);
		ImageRegistry registry = HelpUIPlugin.getDefault().getImageRegistry();
		ImageDescriptor desc = registry.getDescriptor(name);
		if (desc==null) {
			desc = ImageDescriptor.createFromURL(imagePath);
			registry.put(name, desc);
		}
		return desc;
	}

	public static ImageDescriptor getImageDescriptor(String bundleId, String name) {
		ImageRegistry registry = HelpUIPlugin.getDefault().getImageRegistry();
		ImageDescriptor desc = registry.getDescriptor(name);
		if (desc==null) {
			Bundle bundle = Platform.getBundle(bundleId);
			if (bundle==null) return null;
			URL url = FileLocator.find(bundle, IPath.fromOSString(name), null);
			desc = ImageDescriptor.createFromURL(url);
			registry.put(name, desc);
		}
		return desc;
	}

	public static ImageDescriptor getIconImageDescriptor(String bundleId, String path, String key) {
		ImageRegistry registry = HelpUIPlugin.getDefault().getImageRegistry();
		ImageDescriptor desc = registry.getDescriptor(key);
		if (desc==null) {
			Bundle bundle = Platform.getBundle(bundleId);
			if (bundle == null) return null;
			URL url = FileLocator.find(bundle, IPath.fromOSString(path), null);
			desc = ImageDescriptor.createFromURL(url);
			registry.put(key, desc);
		}
		return desc;
	}
	/**
	 * Returns an icon image from a property file
	 * @param name simple image file name
	 * @return the new image or <code>null</code> if image
	 * could not be created
	 */

	public static Image getIconImage(String key) {
		ImageRegistry registry = HelpUIPlugin.getDefault().getImageRegistry();
		return registry.get(key);
	}

	/**
	 * Get the image for an icon based upon the id
	 * @param iconId The id of the icon, may be null, if so a null image is returned
	 * @param isOpen true if this is an expanded container
	 * @param isLeaf true if this node has no children
	 * @return a valid image or null if no image found
	 */
	public static Image getImageFromId(String iconId, boolean isOpen, boolean isLeaf) {
		if (iconId == null) {
			return null;
		}
		initializeTocIcons();
		String suffix;
		if (isOpen) {
			suffix = OPEN;
		} else if (isLeaf) {
			suffix = LEAF;
		} else {
			suffix = CLOSED;
		}
		Image result = lookupImage(iconId + suffix);
		if (result != null || isOpen) {
			return result;
		}
		return lookupImage(iconId + OPEN);
	}

	private static Image lookupImage(String name) {
		ImageRegistry registry = HelpUIPlugin.getDefault().getImageRegistry();
		return registry.get(name);
	}

	private static void initializeTocIcons(){
		if (iconsInitialized) {
			return;
		}
		iconsInitialized = true;
		//Get extension points that contribute products
		IExtension[] extensionsFound = Platform.getExtensionRegistry().getExtensionPoint(EXT_PT).getExtensions();

		for(int i=0; i < extensionsFound.length; i++){

			IConfigurationElement[] configElements = extensionsFound[i].getConfigurationElements();
			for(int j=0; j < configElements.length; j++){
				if (configElements[j].getName().equals(TOC_ICON_ELEMENT)){
					IConfigurationElement iconElem = configElements[j];
					String attrs[] = iconElem.getAttributeNames();
					String contributorID = iconElem.getContributor().getName();

					for (int k=0; k < attrs.length; k++){
						if(attrs[k].equals(OPEN_ICON_PATH))
							HelpUIResources.getIconImageDescriptor(contributorID, iconElem.getAttribute(OPEN_ICON_PATH), iconElem.getAttribute(TOC_ICON_ID) + OPEN);
						if(attrs[k].equals(CLOSED_ICON_PATH))
							HelpUIResources.getIconImageDescriptor(contributorID, iconElem.getAttribute(CLOSED_ICON_PATH), iconElem.getAttribute(TOC_ICON_ID) + CLOSED);
						if(attrs[k].equals(LEAF_ICON_PATH))
							HelpUIResources.getIconImageDescriptor(contributorID, iconElem.getAttribute(LEAF_ICON_PATH), iconElem.getAttribute(TOC_ICON_ID) + LEAF);
					}

				}

			}
		}
	}

	/**
	 * Returns an image from a property file
	 * @param name simple image file name
	 * @return the new image or <code>null</code> if image
	 * could not be created
	 */

	public static Image getImage(String name) {
		ImageRegistry registry = HelpUIPlugin.getDefault().getImageRegistry();
		//Ensure we have the descriptor
		getImageDescriptor(name);
		return registry.get(name);
	}

	public static Image getImage(URL url) {
		ImageRegistry registry = HelpUIPlugin.getDefault().getImageRegistry();
		String name = url.toString();
		ImageDescriptor desc = registry.getDescriptor(name);
		if (desc==null) {
			desc = ImageDescriptor.createFromURL(url);
			registry.put(name, desc);
		}
		return registry.get(name);
	}
}
