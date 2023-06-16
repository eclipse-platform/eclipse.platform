/*******************************************************************************
 * Copyright (c) 2005, 2018 IBM Corporation and others.
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
 *     Bjorn Freeman-Benson - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.examples.ui.pda;

import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * The main plugin class to be used in the desktop.
 */
public class DebugUIPlugin extends AbstractUIPlugin {
	//The shared instance.
	private static DebugUIPlugin plugin;
	//Resource bundle.
	private ResourceBundle resourceBundle;

	private final static String ICONS_PATH = "icons/full/";//$NON-NLS-1$
	private final static String PATH_OBJECT = ICONS_PATH + "obj16/"; //Model object icons //$NON-NLS-1$
	private final static String PATH_ELOCALTOOL = ICONS_PATH + "elcl16/"; //Enabled local toolbar icons //$NON-NLS-1$
	private final static String PATH_DLOCALTOOL = ICONS_PATH + "dlcl16/"; //Disabled local toolbar icons //$NON-NLS-1$

	/**
	 * The id of the plugin
	 *
	 * @since 1.4.200
	 */
	public static final String PLUGIN_ID = "org.eclipse.debug.examples.ui"; //$NON-NLS-1$
	/**
	 * Toolbar action to pop data stack
	 */
	public final static String IMG_ELCL_POP = "IMG_ELCL_POP"; //$NON-NLS-1$
	public final static String IMG_DLCL_POP = "IMG_DLCL_POP"; //$NON-NLS-1$

	/**
	 * Toolbar action to push onto data stack
	 */
	public final static String IMG_ELCL_PUSH = "IMG_ELCL_PUSH"; //$NON-NLS-1$
	public final static String IMG_DLCL_PUSH = "IMG_DLCL_PUSH"; //$NON-NLS-1$

	/**
	 * PDA program image
	 */
	public final static String IMG_OBJ_PDA = "IMB_OBJ_PDA"; //$NON-NLS-1$

	/**
	 * MIDI file image
	 */
	public final static String IMG_OBJ_MIDI = "IMB_OBJ_MIDI"; //$NON-NLS-1$

	/**
	 * Keyword color
	 */
	public final static RGB KEYWORD = new RGB(0,0,255);
	public final static RGB LABEL = new RGB(128, 128, 0);

	/**
	 * Managed colors
	 */
	private final Map<RGB, Color> fColors = new HashMap<>();

	/**
	 * The constructor.
	 */
	public DebugUIPlugin() {
		super();
		plugin = this;
	}

	/**
	 * This method is called upon plug-in activation
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
//		Toggles single threaded adapter example
//		IAdapterManager adapterManager = Platform.getAdapterManager();
//		IAdapterFactory factory = new AdapterFactory();
//		adapterManager.registerAdapters(factory, PDADebugTarget.class);
	}

	/**
	 * This method is called when the plug-in is stopped
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		super.stop(context);
		plugin = null;
		resourceBundle = null;
		Iterator<Entry<RGB, Color>> colors = fColors.entrySet().iterator();
		while (colors.hasNext()) {
			Entry<RGB, Color> entry = colors.next();
			entry.getValue().dispose();
		}
	}

	/**
	 * Returns the shared instance.
	 */
	public static DebugUIPlugin getDefault() {
		return plugin;
	}

	/**
	 * Returns the string from the plugin's resource bundle,
	 * or 'key' if not found.
	 */
	public static String getResourceString(String key) {
		ResourceBundle bundle = DebugUIPlugin.getDefault().getResourceBundle();
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
		try {
			if (resourceBundle == null)
			 {
				resourceBundle = ResourceBundle.getBundle("org.eclipse.debug.examples.ui.pda.DebugUIPluginResources"); //$NON-NLS-1$
			}
		} catch (MissingResourceException x) {
			resourceBundle = null;
		}
		return resourceBundle;
	}

	@Override
	protected void initializeImageRegistry(ImageRegistry reg) {
		declareImage(IMG_OBJ_PDA, PATH_OBJECT + "pda.gif"); //$NON-NLS-1$
		declareImage(IMG_OBJ_MIDI, PATH_OBJECT + "note.gif"); //$NON-NLS-1$
		declareImage(IMG_ELCL_POP, PATH_ELOCALTOOL + "pop.gif"); //$NON-NLS-1$
		declareImage(IMG_DLCL_POP, PATH_DLOCALTOOL + "pop.gif"); //$NON-NLS-1$
		declareImage(IMG_ELCL_PUSH, PATH_ELOCALTOOL + "push.gif"); //$NON-NLS-1$
		declareImage(IMG_DLCL_PUSH, PATH_DLOCALTOOL + "push.gif"); //$NON-NLS-1$
	}

	/**
	 * Declares a workbench image given the path of the image file (relative to
	 * the workbench plug-in). This is a helper method that creates the image
	 * descriptor and passes it to the main <code>declareImage</code> method.
	 *
	 * @param symbolicName the symbolic name of the image
	 * @param path the path of the image file relative to the base of the workbench
	 * plug-ins install directory
	 * <code>false</code> if this is not a shared image
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

	/**
	 * Returns the color described by the given RGB.
	 *
	 * @param rgb
	 * @return color
	 */
	public Color getColor(RGB rgb) {
		Color color = fColors.get(rgb);
		if (color == null) {
			color= new Color(Display.getCurrent(), rgb);
			fColors.put(rgb, color);
		}
		return color;
	}

	/**
	 * Returns the active workbench window
	 *
	 * @return the active workbench window
	 */
	public static IWorkbenchWindow getActiveWorkbenchWindow() {
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow();
	}

	/**
	 * Returns the active workbench shell or <code>null</code> if none
	 *
	 * @return the active workbench shell or <code>null</code> if none
	 */
	public static Shell getActiveWorkbenchShell() {
		IWorkbenchWindow window = getActiveWorkbenchWindow();
		if (window != null) {
			return window.getShell();
		}
		return null;
	}

 }
