/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others.
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
 *     dakshinamurthy.karra@gmail.com - bug 165371
 *     Lars Vogel <Lars.Vogel@gmail.com> - Bug 430127
 *******************************************************************************/
package org.eclipse.ui.externaltools.internal.model;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;

/**
 * The images provided by the external tools plugin.
 */
public class ExternalToolsImages {

	/**
	 * The image registry containing <code>Image</code>s.
	 */
	private static ImageRegistry imageRegistry;

	/**
	 * The registry for composite images
	 */
	private static ImageDescriptorRegistry imageDescriptorRegistry;

	/* Declare Common paths */
	private static URL ICON_BASE_URL= null;

	static {
		String pathSuffix = "icons/full/"; //$NON-NLS-1$
		ICON_BASE_URL= ExternalToolsPlugin.getDefault().getBundle().getEntry(pathSuffix);
	}

	// Use IPath and toOSString to build the names to ensure they have the slashes correct
	private final static String OBJECT= "obj16/"; //basic colors - size 16x16 //$NON-NLS-1$

	/**
	 * Declare all images
	 */
	private static void declareImages() {
		// Objects
		declareRegistryImage(IExternalToolConstants.IMG_TAB_MAIN, OBJECT + "main_tab.png"); //$NON-NLS-1$
		declareRegistryImage(IExternalToolConstants.IMG_TAB_BUILD, OBJECT + "build_tab.png"); //$NON-NLS-1$
	}

	/**
	 * Declare an Image in the registry table.
	 * @param key 	The key to use when registering the image
	 * @param path	The path where the image can be found. This path is relative to where
	 *				this plugin class is found (i.e. typically the packages directory)
	 */
	private final static void declareRegistryImage(String key, String path) {
		ImageDescriptor desc= ImageDescriptor.getMissingImageDescriptor();
		try {
			desc= ImageDescriptor.createFromURL(makeIconFileURL(path));
		} catch (MalformedURLException me) {
		}
		imageRegistry.put(key, desc);
	}

	/**
	 * Returns the ImageRegistry.
	 */
	public static ImageRegistry getImageRegistry() {
		if (imageRegistry == null) {
			initializeImageRegistry();
		}
		return imageRegistry;
	}

	/**
	 *	Initialize the image registry by declaring all of the required
	 *	graphics. This involves creating JFace image descriptors describing
	 *	how to create/find the image should it be needed.
	 *	The image is not actually allocated until requested.
	 *
	 * 	Prefix conventions
	 *		Wizard Banners			WIZBAN_
	 *		Preference Banners		PREF_BAN_
	 *		Property Page Banners	PROPBAN_
	 *		Color toolbar			CTOOL_
	 *		Enable toolbar			ETOOL_
	 *		Disable toolbar			DTOOL_
	 *		Local enabled toolbar	ELCL_
	 *		Local Disable toolbar	DLCL_
	 *		Object large			OBJL_
	 *		Object small			OBJS_
	 *		View 					VIEW_
	 *		Product images			PROD_
	 *		Misc images				MISC_
	 *
	 *	Where are the images?
	 *		The images (typically gifs) are found in the same location as this plugin class.
	 *		This may mean the same package directory as the package holding this class.
	 *		The images are declared using this.getClass() to ensure they are looked up via
	 *		this plugin class.
	 *	@see org.eclipse.jface.resource.ImageRegistry
	 */
	public static ImageRegistry initializeImageRegistry() {
		imageRegistry= new ImageRegistry(ExternalToolsPlugin.getStandardDisplay());
		declareImages();
		return imageRegistry;
	}

	/**
	 * Returns the <code>Image<code> identified by the given key,
	 * or <code>null</code> if it does not exist.
	 */
	public static Image getImage(String key) {
		return getImageRegistry().get(key);
	}

	/**
	 * Returns the <code>ImageDescriptor<code> identified by the given key,
	 * or <code>null</code> if it does not exist.
	 */
	public static ImageDescriptor getImageDescriptor(String key) {
		return getImageRegistry().getDescriptor(key);
	}

	private static URL makeIconFileURL(String iconPath) throws MalformedURLException {
		if (ICON_BASE_URL == null) {
			throw new MalformedURLException();
		}

		return new URL(ICON_BASE_URL, iconPath);
	}

	/**
	 * Sets the three image descriptors for enabled, disabled, and hovered to an action. The actions
	 * are retrieved from the *lcl16 folders.
	 */
	public static void setLocalImageDescriptors(IAction action, String iconName) {
		setImageDescriptors(action, "lcl16", iconName); //$NON-NLS-1$
	}

	private static void setImageDescriptors(IAction action, String type, String relPath) {

		try {
			ImageDescriptor id= ImageDescriptor.createFromURL(makeIconFileURL("d" + type, relPath)); //$NON-NLS-1$
			if (id != null)
				action.setDisabledImageDescriptor(id);
		} catch (MalformedURLException e) {
			ExternalToolsPlugin.getDefault().log(e);
		}

		try {
			ImageDescriptor id= ImageDescriptor.createFromURL(makeIconFileURL("c" + type, relPath)); //$NON-NLS-1$
			if (id != null)
				action.setHoverImageDescriptor(id);
		} catch (MalformedURLException e) {
			ExternalToolsPlugin.getDefault().log(e);
		}

		action.setImageDescriptor(create("e" + type, relPath)); //$NON-NLS-1$
	}

	private static URL makeIconFileURL(String prefix, String name) throws MalformedURLException {
		if (ICON_BASE_URL == null) {
			throw new MalformedURLException();
		}

		StringBuilder buffer= new StringBuilder(prefix);
		buffer.append('/');
		buffer.append(name);
		return new URL(ICON_BASE_URL, buffer.toString());
	}

	private static ImageDescriptor create(String prefix, String name) {
		try {
			return ImageDescriptor.createFromURL(makeIconFileURL(prefix, name));
		} catch (MalformedURLException e) {
			ExternalToolsPlugin.getDefault().log(e);
			return ImageDescriptor.getMissingImageDescriptor();
		}
	}

	/**
	 * Returns the image for the given composite descriptor.
	 */
	public static Image getImage(CompositeImageDescriptor imageDescriptor) {
		if (imageDescriptorRegistry == null) {
			imageDescriptorRegistry = new ImageDescriptorRegistry();
		}
		return imageDescriptorRegistry.get(imageDescriptor);
	}

	public static void disposeImageDescriptorRegistry() {
		if (imageDescriptorRegistry != null) {
			imageDescriptorRegistry.dispose();
		}
	}
}
