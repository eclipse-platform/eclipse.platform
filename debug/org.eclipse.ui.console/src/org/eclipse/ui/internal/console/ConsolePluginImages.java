/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
 *     vogella GmbH - Bug 287303 - [patch] Add Word Wrap action to Console View
 *******************************************************************************/
package org.eclipse.ui.internal.console;


import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsoleConstants;

/**
 * The images provided by the debug plugin.
 */
public class ConsolePluginImages {

	/**
	 * The image registry containing <code>Image</code>s and the <code>ImageDescriptor</code>s.
	 */
	private static ImageRegistry imageRegistry;

	/* Declare Common paths */
	private static URL ICON_BASE_URL= null;

	static {
		String pathSuffix = "icons/full/"; //$NON-NLS-1$
		ICON_BASE_URL= ConsolePlugin.getDefault().getBundle().getEntry(pathSuffix);
	}

	// Use IPath and toOSString to build the names to ensure they have the slashes correct
	private final static String LOCALTOOL= "clcl16/"; //basic colors - size 16x16 //$NON-NLS-1$
	private final static String DLCL= "dlcl16/"; //disabled - size 16x16 //$NON-NLS-1$
	private final static String ELCL= "elcl16/"; //enabled - size 16x16 //$NON-NLS-1$
	private final static String VIEW= "cview16/"; // views //$NON-NLS-1$

	/**
	 * Declare all images
	 */
	private static void declareImages() {
		// Actions

		//local toolbars
		declareRegistryImage(IConsoleConstants.IMG_LCL_CLEAR, LOCALTOOL + "clear_co.png"); //$NON-NLS-1$
		declareRegistryImage(IInternalConsoleConstants.IMG_LCL_PIN, LOCALTOOL + "pin.png"); //$NON-NLS-1$
		declareRegistryImage(IInternalConsoleConstants.IMG_LCL_LOCK, LOCALTOOL + "lock_co.png"); //$NON-NLS-1$
		declareRegistryImage(IInternalConsoleConstants.IMG_LCL_WRAP, LOCALTOOL + "wordwrap.png"); //$NON-NLS-1$

		// disabled local toolbars
		declareRegistryImage(IInternalConsoleConstants.IMG_DLCL_CLEAR, DLCL + "clear_co.png"); //$NON-NLS-1$
		declareRegistryImage(IInternalConsoleConstants.IMG_DLCL_PIN, DLCL + "pin.png"); //$NON-NLS-1$
		declareRegistryImage(IInternalConsoleConstants.IMG_DLCL_LOCK, DLCL + "lock_co.png"); //$NON-NLS-1$
		declareRegistryImage(IInternalConsoleConstants.IMG_DLCL_WRAP, DLCL + "wordwrap.png"); //$NON-NLS-1$
		declareRegistryImage(IInternalConsoleConstants.IMG_DLCL_CLOSE, DLCL + "rem_co.png"); //$NON-NLS-1$
		declareRegistryImage(IInternalConsoleConstants.IMG_DLCL_NEW_CON, DLCL + "new_con.png"); //$NON-NLS-1$

		// enabled local toolbars
		declareRegistryImage(IInternalConsoleConstants.IMG_ELCL_CLEAR, ELCL + "clear_co.png"); //$NON-NLS-1$
		declareRegistryImage(IInternalConsoleConstants.IMG_ELCL_PIN, ELCL + "pin.png"); //$NON-NLS-1$
		declareRegistryImage(IInternalConsoleConstants.IMG_ELCL_LOCK, ELCL + "lock_co.png"); //$NON-NLS-1$
		declareRegistryImage(IInternalConsoleConstants.IMG_ELCL_WRAP, ELCL + "wordwrap.png"); //$NON-NLS-1$
		declareRegistryImage(IInternalConsoleConstants.IMG_ELCL_CLOSE, ELCL + "rem_co.png"); //$NON-NLS-1$
		declareRegistryImage(IInternalConsoleConstants.IMG_ELCL_NEW_CON, ELCL + "new_con.png"); //$NON-NLS-1$

		// Views
		declareRegistryImage(IConsoleConstants.IMG_VIEW_CONSOLE, VIEW + "console_view.png"); //$NON-NLS-1$
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
			ConsolePlugin.log(me);
		}
		imageRegistry.put(key, desc);
	}

	/**
	 * Returns the ImageRegistry.
	 *
	 * @return the ImageRegistry
	 */
	public static ImageRegistry getImageRegistry() {
		if (imageRegistry == null) {
			initializeImageRegistry();
		}
		return imageRegistry;
	}

	/**
	 * Initialize the image registry by declaring all of the required graphics. This
	 * involves creating JFace image descriptors describing how to create/find the
	 * image should it be needed. The image is not actually allocated until
	 * requested.
	 *
	 * <pre>
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
	 *		The images (typically pngs) are found in the same location as this plugin class.
	 *		This may mean the same package directory as the package holding this class.
	 *		The images are declared using this.getClass() to ensure they are looked up via
	 *		this plugin class.
	 * </pre>
	 *
	 * @return the initialized ImageRegistry
	 * @see org.eclipse.jface.resource.ImageRegistry
	 */
	public static ImageRegistry initializeImageRegistry() {
		imageRegistry = new ImageRegistry(ConsolePlugin.getStandardDisplay());
		declareImages();
		return imageRegistry;
	}

	/**
	 * Returns the <code>Image</code> identified by the given key, or
	 * <code>null</code> if it does not exist.
	 *
	 * @param key the image's key
	 * @return the <code>Image</code> identified by the given key, or
	 *         <code>null</code> if it does not exist
	 */
	public static Image getImage(String key) {
		return getImageRegistry().get(key);
	}

	/**
	 * Returns the <code>ImageDescriptor</code> identified by the given key, or
	 * <code>null</code> if it does not exist.
	 *
	 * @param key the image's key
	 * @return the <code>ImageDescriptor</code> identified by the given key, or
	 *         <code>null</code> if it does not exist
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
}
