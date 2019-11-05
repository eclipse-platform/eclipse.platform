/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Atsuhiko Yamanaka, JCraft,Inc. - initial API and implementation.
 *     IBM Corporation - ongoing maintenance
 *******************************************************************************/
package org.eclipse.jsch.internal.ui;

import java.net.URL;
import java.util.Hashtable;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jsch.core.IJSchService;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

public class JSchUIPlugin extends AbstractUIPlugin{
	public static final String ID="org.eclipse.jsch.ui"; //$NON-NLS-1$
	public static final String DECORATOR_ID="org.eclipse.jsch.ui.decorator"; //$NON-NLS-1$

	private static Hashtable<String, ImageDescriptor> imageDescriptors = new Hashtable<>(20);
	/**
	 * The singleton plug-in instance
	 */
	private static JSchUIPlugin plugin;
	private ServiceTracker<?, ?> tracker;

	public JSchUIPlugin(){
		super();
		plugin=this;
	}

	/**
	 * Returns the singleton plug-in instance.
	 *
	 * @return the plugin instance
	 */
	public static JSchUIPlugin getPlugin(){
		return plugin;
	}

	/**
	 * Returns the image descriptor for the given image ID.
	 * Returns null if there is no such image.
	 * @param id the id of the image descriptor
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String id){
		return imageDescriptors.get(id);
	}

	/**
	 * Creates an image and places it in the image registry.
	 */
	protected void createImageDescriptor(String id){
		URL url=FileLocator.find(JSchUIPlugin.getPlugin().getBundle(), new Path(
				IUIConstants.ICON_PATH+id), null);
		ImageDescriptor desc=ImageDescriptor.createFromURL(url);
		imageDescriptors.put(id, desc);
	}

	/**
	 * Convenience method to get an image descriptor for an extension
	 *
	 * @param extension  the extension declaring the image
	 * @param subdirectoryAndFilename  the path to the image
	 * @return the image
	 */
	public static ImageDescriptor getImageDescriptorFromExtension(IExtension extension, String subdirectoryAndFilename) {
		URL fullPathString = FileLocator.find(Platform.getBundle(extension.getNamespaceIdentifier()), new Path(subdirectoryAndFilename), null);
		return ImageDescriptor.createFromURL(fullPathString);
	}

	/**
	 * Initializes the table of images used in this plugin.
	 */
	private void initializeImages() {
		createImageDescriptor(IUIConstants.IMG_KEY_LOCK);
	}

	@Override
public void start(BundleContext context) throws Exception {
		super.start(context);

		initializeImages();

		IPreferenceStore store = getPreferenceStore();
		if (store.getBoolean(IUIConstants.PREF_FIRST_STARTUP)) {
			store.setValue(IUIConstants.PREF_FIRST_STARTUP, false);
		}

		tracker = new ServiceTracker<>(getBundle().getBundleContext(), IJSchService.class.getName(), null);
		tracker.open();
	}

	@Override
public void stop(BundleContext context) throws Exception{
		super.stop(context);
		tracker.close();
	}

	public IJSchService getJSchService() {
		return (IJSchService)tracker.getService();
	}

	public URL getImageUrl(String relative){
		return FileLocator.find(Platform.getBundle(ID), new Path(IUIConstants.ICON_PATH + relative), null);
	}
}
