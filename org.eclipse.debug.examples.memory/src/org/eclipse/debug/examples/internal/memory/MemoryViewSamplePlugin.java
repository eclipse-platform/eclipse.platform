/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
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
package org.eclipse.debug.examples.internal.memory;

import java.net.URL;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * The main plugin class for sample adapter
 */
public class MemoryViewSamplePlugin extends AbstractUIPlugin {

	public static final String PLUGIN_ID = "org.eclipse.debug.examples.memory"; //$NON-NLS-1$

	// The shared instance.
	private static MemoryViewSamplePlugin plugin;

	// Resource bundle.
	private ResourceBundle resourceBundle;

	private final static String ICONS_PATH = "icons/full/";//$NON-NLS-1$
	private final static String PATH_OBJECT = ICONS_PATH + "obj16/"; //Model object icons //$NON-NLS-1$

	public final static String IMG_OBJ_HEX_TREE = "IMB_OBJ_HEX_TREE"; //$NON-NLS-1$
	public final static String IMG_OBJ_MEMORY_SEGMENT = "IMG_OBJ_MEMORY_SEGMENT"; //$NON-NLS-1$
	public final static String IMG_OBJ_MEMORY_UNIT = "IMG_OBJ_MEMORY_UNIT"; //$NON-NLS-1$

	/**
	 * The constructor.
	 */
	public MemoryViewSamplePlugin() {
		super();
		plugin = this;
		try {
			resourceBundle = ResourceBundle.getBundle(PLUGIN_ID);
		} catch (MissingResourceException x) {
			resourceBundle = null;
		}
	}

	/**
	 * This method is called upon plug-in activation
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
	}

	/**
	 * This method is called when the plug-in is stopped
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		super.stop(context);
	}

	/**
	 * Returns the shared instance.
	 */
	public static MemoryViewSamplePlugin getDefault() {
		return plugin;
	}

	/**
	 * Returns the string from the plugin's resource bundle, or 'key' if not
	 * found.
	 */
	public static String getResourceString(String key) {
		ResourceBundle bundle = MemoryViewSamplePlugin.getDefault().getResourceBundle();
		try {
			return (bundle != null) ? bundle.getString(key) : key;
		} catch (MissingResourceException e) {
			return key;
		}
	}

	/**
	 * Returns the plugin's resource bundle,
	 */
	public ResourceBundle getResourceBundle() {
		return resourceBundle;
	}

	@Override
	protected void initializeImageRegistry(ImageRegistry reg) {
		declareImage(IMG_OBJ_HEX_TREE, PATH_OBJECT + "hex_tree.gif"); //$NON-NLS-1$
		declareImage(IMG_OBJ_MEMORY_SEGMENT, PATH_OBJECT + "memory_segment.gif"); //$NON-NLS-1$
		declareImage(IMG_OBJ_MEMORY_UNIT, PATH_OBJECT + "memory_unit.gif"); //$NON-NLS-1$
	}

	/**
	 * Declares a workbench image given the path of the image file (relative to
	 * the workbench plug-in). This is a helper method that creates the image
	 * descriptor and passes it to the main <code>declareImage</code> method.
	 *
	 * @param symbolicName the symbolic name of the image
	 * @param path the path of the image file relative to the base of the
	 *            workbench plug-ins install directory <code>false</code> if
	 *            this is not a shared image
	 */
	private void declareImage(String key, String path) {
		ImageDescriptor desc = ImageDescriptor.getMissingImageDescriptor();
		Bundle bundle = Platform.getBundle(PLUGIN_ID);
		URL url = null;
		if (bundle != null) {
			url = FileLocator.find(bundle, IPath.fromOSString(path), null);
			if (url != null) {
				desc = ImageDescriptor.createFromURL(url);
			}
		}
		getImageRegistry().put(key, desc);
	}
}
