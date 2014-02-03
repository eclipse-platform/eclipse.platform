/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     John-Mason P. Shackelford (john-mason.shackelford@pearson.com) - bug 49380, 49445, 53547
 *******************************************************************************/
package org.eclipse.ant.internal.ui;


import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
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
	private final static String LOCALTOOL= ICONS_PATH + "elcl16/"; //basic colors - size 16x16 //$NON-NLS-1$
	private final static String OBJECT= ICONS_PATH + "obj16/"; //basic colors - size 16x16 //$NON-NLS-1$
	private final static String OVR= ICONS_PATH + "ovr16/"; //basic colors - size 7x8 //$NON-NLS-1$
	private final static String WIZ= ICONS_PATH + "wizban/"; //$NON-NLS-1$
	private static final String T_ETOOL= ICONS_PATH + "etool16"; 	//$NON-NLS-1$
	
	/**
	 * Declare all images
	 */
	private static void declareImages() {
		// Ant Editor images
		declareRegistryImage(IAntUIConstants.IMG_PROPERTY, OBJECT + "property_obj.gif"); //$NON-NLS-1$
		declareRegistryImage(IAntUIConstants.IMG_TASK_PROPOSAL, OBJECT + "task_obj.gif"); //$NON-NLS-1$
		declareRegistryImage(IAntUIConstants.IMG_TEMPLATE_PROPOSAL, OBJECT + "template_obj.gif"); //$NON-NLS-1$
		
		declareRegistryImage(IAntUIConstants.IMG_SEGMENT_EDIT, T_ETOOL + "segment_edit.gif"); //$NON-NLS-1$
		declareRegistryImage(IAntUIConstants.IMG_MARK_OCCURRENCES, T_ETOOL + "mark_occurrences.gif"); //$NON-NLS-1$
		
		// Ant View Actions
		declareRegistryImage(IAntUIConstants.IMG_ANT, OBJECT + "ant.gif"); //$NON-NLS-1$
		declareRegistryImage(IAntUIConstants.IMG_REMOVE, LOCALTOOL + "remove_co.gif"); //$NON-NLS-1$
		declareRegistryImage(IAntUIConstants.IMG_REMOVE_ALL, LOCALTOOL + "removeall_co.gif"); //$NON-NLS-1$
		declareRegistryImage(IAntUIConstants.IMG_ADD, LOCALTOOL + "add_co.gif"); //$NON-NLS-1$
		declareRegistryImage(IAntUIConstants.IMG_RUN, LOCALTOOL + "run_tool.gif"); //$NON-NLS-1$
		declareRegistryImage(IAntUIConstants.IMG_SEARCH, LOCALTOOL + "search.gif"); //$NON-NLS-1$
		
		declareRegistryImage(IAntUIConstants.IMG_FILTER_INTERNAL_TARGETS, LOCALTOOL + "filter_internal_targets.gif"); //$NON-NLS-1$
		declareRegistryImage(IAntUIConstants.IMG_FILTER_IMPORTED_ELEMENTS, LOCALTOOL + "filter_imported_elements.gif"); //$NON-NLS-1$
		declareRegistryImage(IAntUIConstants.IMG_FILTER_PROPERTIES, LOCALTOOL + "filter_properties.gif"); //$NON-NLS-1$
		declareRegistryImage(IAntUIConstants.IMG_FILTER_TOP_LEVEL, LOCALTOOL + "filter_top_level.gif"); //$NON-NLS-1$
		declareRegistryImage(IAntUIConstants.IMG_LINK_WITH_EDITOR, LOCALTOOL + "synced.gif"); //$NON-NLS-1$
		declareRegistryImage(IAntUIConstants.IMG_SORT_OUTLINE, LOCALTOOL + "alpha_mode.gif"); //$NON-NLS-1$
		declareRegistryImage(IAntUIConstants.IMG_REFRESH, LOCALTOOL + "refresh.gif"); //$NON-NLS-1$
		
		// Ant View Labels
		declareRegistryImage(IAntUIConstants.IMG_ANT_PROJECT, OBJECT + "ant_buildfile.gif"); //$NON-NLS-1$
		declareRegistryImage(IAntUIConstants.IMG_ANT_TARGET, OBJECT + "targetpublic_obj.gif"); //$NON-NLS-1$
		declareRegistryImage(IAntUIConstants.IMG_ANT_TARGET_INTERNAL, OBJECT + "targetinternal_obj.gif"); //$NON-NLS-1$
		declareRegistryImage(IAntUIConstants.IMG_ANT_DEFAULT_TARGET, OBJECT + "defaulttarget_obj.gif"); //$NON-NLS-1$
		declareRegistryImage(IAntUIConstants.IMG_ANT_TARGET_ERROR, OBJECT + "ant_target_err.gif"); //$NON-NLS-1$

		//ANT objects
		declareRegistryImage(IAntUIConstants.IMG_TAB_CLASSPATH, OBJECT + "classpath.gif"); //$NON-NLS-1$
		declareRegistryImage(IAntUIConstants.IMG_ANT_TYPE, OBJECT + "type.gif"); //$NON-NLS-1$
		declareRegistryImage(IAntUIConstants.IMG_ANT_TASKDEF, OBJECT + "taskdef_obj.gif"); //$NON-NLS-1$
		declareRegistryImage(IAntUIConstants.IMG_ANT_MACRODEF, OBJECT + "macrodef_obj.gif"); //$NON-NLS-1$
        declareRegistryImage(IAntUIConstants.IMG_ANT_IMPORT, OBJECT + "import_obj.gif"); //$NON-NLS-1$
        declareRegistryImage(IAntUIConstants.IMG_ANT_ECLIPSE_RUNTIME_OBJECT, OBJECT + "eclipse_obj.gif"); //$NON-NLS-1$
        
		declareRegistryImage(IAntUIConstants.IMG_WIZARD_BANNER, WIZ + "ant_wiz.png"); //$NON-NLS-1$
        declareRegistryImage(IAntUIConstants.IMG_EXPORT_WIZARD_BANNER, WIZ + "export_ant_wiz.png"); //$NON-NLS-1$
		declareRegistryImage(IAntUIConstants.IMG_TAB_ANT_TARGETS, LOCALTOOL + "ant_targets.gif"); //$NON-NLS-1$
		
		// Overlays
		declareRegistryImage(IAntUIConstants.IMG_OVR_ERROR, OVR + "error_co.gif"); //$NON-NLS-1$
		declareRegistryImage(IAntUIConstants.IMG_OVR_WARNING, OVR + "warning_co.gif"); //$NON-NLS-1$
		declareRegistryImage(IAntUIConstants.IMG_OVR_IMPORT, OVR + "import_co.gif"); //$NON-NLS-1$
	}

	/**
	 * Declare an Image in the registry table.
	 * @param key 	The key to use when registering the image
	 * @param path	The path where the image can be found. This path is relative to where
	 *				this plugin class is found (i.e. typically the packages directory)
	 */
	private final static void declareRegistryImage(String key, String path) {
        ImageDescriptor desc = ImageDescriptor.getMissingImageDescriptor();
        Bundle bundle = Platform.getBundle(AntUIPlugin.getUniqueIdentifier());
        URL url = null;
        if (bundle != null) {
            url = FileLocator.find(bundle, new Path(path), null);
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
	 * @see org.eclipse.jface.resource.ImageRegistry
	 */
	public static ImageRegistry initializeImageRegistry() {
		imageRegistry= new ImageRegistry(AntUIPlugin.getStandardDisplay());
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
