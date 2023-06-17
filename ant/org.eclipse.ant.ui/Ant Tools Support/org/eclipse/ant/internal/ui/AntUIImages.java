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
 *     John-Mason P. Shackelford (john-mason.shackelford@pearson.com) - bug 49380, 49445, 53547
 *******************************************************************************/
package org.eclipse.ant.internal.ui;

import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.osgi.framework.Bundle;

/**
 * The images provided by the external tools plugin.
 */
public class AntUIImages {

	/**
	 * The image registry containing <code>Image</code>s.
	 */
	private static ImageRegistry imageRegistry;

	/**
	 * The registry for composite images
	 */
	private static ImageDescriptorRegistry imageDescriptorRegistry;

	private static String ICONS_PATH = "$nl$/icons/full/"; //$NON-NLS-1$

	// Use IPath and toOSString to build the names to ensure they have the slashes correct
	private final static String LOCALTOOL = ICONS_PATH + "elcl16/"; //basic colors - size 16x16 //$NON-NLS-1$
	private final static String OBJECT = ICONS_PATH + "obj16/"; //basic colors - size 16x16 //$NON-NLS-1$
	private final static String OVR = ICONS_PATH + "ovr16/"; //basic colors - size 7x8 //$NON-NLS-1$
	private final static String WIZ = ICONS_PATH + "wizban/"; //$NON-NLS-1$
	private static final String T_ETOOL = ICONS_PATH + "etool16"; //$NON-NLS-1$

	/**
	 * Declare all images
	 */
	private static void declareImages() {
		// Ant Editor images
		declareRegistryImage(IAntUIConstants.IMG_PROPERTY, OBJECT + "property_obj.png"); //$NON-NLS-1$
		declareRegistryImage(IAntUIConstants.IMG_TASK_PROPOSAL, OBJECT + "task_obj.png"); //$NON-NLS-1$
		declareRegistryImage(IAntUIConstants.IMG_TEMPLATE_PROPOSAL, OBJECT + "template_obj.png"); //$NON-NLS-1$

		declareRegistryImage(IAntUIConstants.IMG_SEGMENT_EDIT, T_ETOOL + "segment_edit.png"); //$NON-NLS-1$
		declareRegistryImage(IAntUIConstants.IMG_MARK_OCCURRENCES, T_ETOOL + "mark_occurrences.png"); //$NON-NLS-1$

		// Ant View Actions
		declareRegistryImage(IAntUIConstants.IMG_ANT, OBJECT + "ant.png"); //$NON-NLS-1$
		declareRegistryImage(IAntUIConstants.IMG_REMOVE, LOCALTOOL + "remove_co.png"); //$NON-NLS-1$
		declareRegistryImage(IAntUIConstants.IMG_REMOVE_ALL, LOCALTOOL + "removeall_co.png"); //$NON-NLS-1$
		declareRegistryImage(IAntUIConstants.IMG_ADD, LOCALTOOL + "add_co.png"); //$NON-NLS-1$
		declareRegistryImage(IAntUIConstants.IMG_RUN, LOCALTOOL + "run_tool.png"); //$NON-NLS-1$
		declareRegistryImage(IAntUIConstants.IMG_SEARCH, LOCALTOOL + "search.png"); //$NON-NLS-1$

		declareRegistryImage(IAntUIConstants.IMG_FILTER_INTERNAL_TARGETS, LOCALTOOL + "filter_internal_targets.png"); //$NON-NLS-1$
		declareRegistryImage(IAntUIConstants.IMG_FILTER_IMPORTED_ELEMENTS, LOCALTOOL + "filter_imported_elements.png"); //$NON-NLS-1$
		declareRegistryImage(IAntUIConstants.IMG_FILTER_PROPERTIES, LOCALTOOL + "filter_properties.png"); //$NON-NLS-1$
		declareRegistryImage(IAntUIConstants.IMG_FILTER_TOP_LEVEL, LOCALTOOL + "filter_top_level.png"); //$NON-NLS-1$
		declareRegistryImage(IAntUIConstants.IMG_LINK_WITH_EDITOR, LOCALTOOL + "synced.png"); //$NON-NLS-1$
		declareRegistryImage(IAntUIConstants.IMG_SORT_OUTLINE, LOCALTOOL + "alpha_mode.png"); //$NON-NLS-1$
		declareRegistryImage(IAntUIConstants.IMG_REFRESH, LOCALTOOL + "refresh.png"); //$NON-NLS-1$

		// Ant View Labels
		declareRegistryImage(IAntUIConstants.IMG_ANT_PROJECT, OBJECT + "ant_buildfile.png"); //$NON-NLS-1$
		declareRegistryImage(IAntUIConstants.IMG_ANT_TARGET, OBJECT + "targetpublic_obj.png"); //$NON-NLS-1$
		declareRegistryImage(IAntUIConstants.IMG_ANT_TARGET_INTERNAL, OBJECT + "targetinternal_obj.png"); //$NON-NLS-1$
		declareRegistryImage(IAntUIConstants.IMG_ANT_DEFAULT_TARGET, OBJECT + "defaulttarget_obj.png"); //$NON-NLS-1$
		declareRegistryImage(IAntUIConstants.IMG_ANT_TARGET_ERROR, OBJECT + "ant_target_err.png"); //$NON-NLS-1$

		// ANT objects
		declareRegistryImage(IAntUIConstants.IMG_TAB_CLASSPATH, OBJECT + "classpath.png"); //$NON-NLS-1$
		declareRegistryImage(IAntUIConstants.IMG_ANT_TYPE, OBJECT + "type.png"); //$NON-NLS-1$
		declareRegistryImage(IAntUIConstants.IMG_ANT_TASKDEF, OBJECT + "taskdef_obj.png"); //$NON-NLS-1$
		declareRegistryImage(IAntUIConstants.IMG_ANT_MACRODEF, OBJECT + "macrodef_obj.png"); //$NON-NLS-1$
		declareRegistryImage(IAntUIConstants.IMG_ANT_IMPORT, OBJECT + "import_obj.png"); //$NON-NLS-1$
		declareRegistryImage(IAntUIConstants.IMG_ANT_ECLIPSE_RUNTIME_OBJECT, OBJECT + "eclipse16.png"); //$NON-NLS-1$

		declareRegistryImage(IAntUIConstants.IMG_WIZARD_BANNER, WIZ + "ant_wiz.png"); //$NON-NLS-1$
		declareRegistryImage(IAntUIConstants.IMG_EXPORT_WIZARD_BANNER, WIZ + "export_ant_wiz.png"); //$NON-NLS-1$
		declareRegistryImage(IAntUIConstants.IMG_TAB_ANT_TARGETS, LOCALTOOL + "ant_targets.png"); //$NON-NLS-1$

		// Overlays
		declareRegistryImage(IAntUIConstants.IMG_OVR_ERROR, OVR + "error_co.png"); //$NON-NLS-1$
		declareRegistryImage(IAntUIConstants.IMG_OVR_WARNING, OVR + "warning_co.png"); //$NON-NLS-1$
		declareRegistryImage(IAntUIConstants.IMG_OVR_IMPORT, OVR + "import_co.png"); //$NON-NLS-1$
	}

	/**
	 * Declare an Image in the registry table.
	 * 
	 * @param key
	 *            The key to use when registering the image
	 * @param path
	 *            The path where the image can be found. This path is relative to where this plugin class is found (i.e. typically the packages
	 *            directory)
	 */
	private final static void declareRegistryImage(String key, String path) {
		ImageDescriptor desc = ImageDescriptor.getMissingImageDescriptor();
		Bundle bundle = Platform.getBundle(AntUIPlugin.getUniqueIdentifier());
		URL url = null;
		if (bundle != null) {
			url = FileLocator.find(bundle, IPath.fromOSString(path), null);
			desc = ImageDescriptor.createFromURL(url);
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
	 * Initialize the image registry by declaring all of the required graphics. This involves creating JFace image descriptors describing how to
	 * create/find the image should it be needed. The image is not actually allocated until requested.
	 *
	 * Prefix conventions
	 * 
	 * <pre>
	 *      Wizard Banners          WIZBAN_
	 *      Preference Banners      PREF_BAN_
	 *      Property Page Banners   PROPBAN_
	 *      Color toolbar           CTOOL_
	 *      Enable toolbar          ETOOL_
	 *      Disable toolbar         DTOOL_
	 *      Local enabled toolbar   ELCL_
	 *      Local Disable toolbar   DLCL_
	 *      Object large            OBJL_
	 *      Object small            OBJS_
	 *      View                    VIEW_
	 *      Product images          PROD_
	 *      Misc images             MISC_
	 * </pre>
	 *
	 * Where are the images?
	 * <p>
	 * The images (typically gifs) are found in the same location as this plugin class. This may mean the same package directory as the package
	 * holding this class. The images are declared using this.getClass() to ensure they are looked up via this plugin class.
	 * </p>
	 * 
	 * @see org.eclipse.jface.resource.ImageRegistry
	 */
	public static ImageRegistry initializeImageRegistry() {
		imageRegistry = new ImageRegistry(AntUIPlugin.getStandardDisplay());
		declareImages();
		return imageRegistry;
	}

	/**
	 * Returns the <code>Image</code> identified by the given key, or <code>null</code> if it does not exist.
	 */
	public static Image getImage(String key) {
		return getImageRegistry().get(key);
	}

	/**
	 * Returns the <code>ImageDescriptor</code> identified by the given key, or <code>null</code> if it does not exist.
	 */
	public static ImageDescriptor getImageDescriptor(String key) {
		return getImageRegistry().getDescriptor(key);
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

	/**
	 * Returns whether the images have been initialized.
	 * 
	 * @return whether the images have been initialized
	 */
	public synchronized static boolean isInitialized() {
		return imageDescriptorRegistry != null;
	}
}
