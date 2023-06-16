/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.unittest.internal.ui;

import java.net.URL;

import org.osgi.framework.Bundle;

import org.eclipse.unittest.internal.UnitTestPlugin;

import org.eclipse.swt.graphics.Image;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;

/**
 * Image related utilities
 */
public class Images {

	private static final IPath ICONS_PATH = IPath.fromOSString("$nl$/icons/full"); //$NON-NLS-1$

	/**
	 * Create an {@link ImageDescriptor} from a given path
	 *
	 * @param relativePath relative path to the image
	 * @return an {@link ImageDescriptor}, or <code>null</code> iff there's no image
	 *         at the given location and <code>useMissingImageDescriptor</code> is
	 *         <code>true</code>
	 */
	public static ImageDescriptor getImageDescriptor(String relativePath) {
		IPath path = ICONS_PATH.append(relativePath);
		return createImageDescriptor(UnitTestPlugin.getDefault().getBundle(), path, true);
	}

	/**
	 * Creates an {@link Image} from a given path
	 *
	 * @param path path to the image
	 * @return a new image or <code>null</code> if the image could not be created
	 */
	public static Image createImage(String path) {
		return getImageDescriptor(path).createImage();
	}

	/**
	 * Sets the three image descriptors for enabled, disabled, and hovered to an
	 * action. The actions are retrieved from the *lcl16 folders.
	 *
	 * @param action   the action
	 * @param iconName the icon name
	 */
	public static void setLocalImageDescriptors(IAction action, String iconName) {
		setImageDescriptors(action, "lcl16", iconName); //$NON-NLS-1$
	}

	private static void setImageDescriptors(IAction action, String type, String relPath) {
		ImageDescriptor id = createImageDescriptor("d" + type, relPath, false); //$NON-NLS-1$
		if (id != null)
			action.setDisabledImageDescriptor(id);

		ImageDescriptor descriptor = createImageDescriptor("e" + type, relPath, true); //$NON-NLS-1$
		action.setHoverImageDescriptor(descriptor);
		action.setImageDescriptor(descriptor);
	}

	/*
	 * Creates an image descriptor for the given prefix and name in the JDT UI
	 * bundle. The path can contain variables like $NL$. If no image could be found,
	 * <code>useMissingImageDescriptor</code> decides if either the 'missing image
	 * descriptor' is returned or <code>null</code>. or <code>null</code>.
	 */
	private static ImageDescriptor createImageDescriptor(String pathPrefix, String imageName,
			boolean useMissingImageDescriptor) {
		IPath path = ICONS_PATH.append(pathPrefix).append(imageName);
		return createImageDescriptor(UnitTestPlugin.getDefault().getBundle(), path, useMissingImageDescriptor);
	}

	/**
	 * Creates an image descriptor for the given path in a bundle. The path can
	 * contain variables like $NL$. If no image could be found,
	 * <code>useMissingImageDescriptor</code> decides if either the 'missing image
	 * descriptor' is returned or <code>null</code>.
	 *
	 * @param bundle                    a bundle
	 * @param path                      path in the bundle
	 * @param useMissingImageDescriptor if <code>true</code>, returns the shared
	 *                                  image descriptor for a missing image.
	 *                                  Otherwise, returns <code>null</code> if the
	 *                                  image could not be found
	 * @return an {@link ImageDescriptor}, or <code>null</code> iff there's no image
	 *         at the given location and <code>useMissingImageDescriptor</code> is
	 *         <code>true</code>
	 */
	private static ImageDescriptor createImageDescriptor(Bundle bundle, IPath path, boolean useMissingImageDescriptor) {
		URL url = FileLocator.find(bundle, path, null);
		if (url != null) {
			return ImageDescriptor.createFromURL(url);
		}
		if (useMissingImageDescriptor) {
			return ImageDescriptor.getMissingImageDescriptor();
		}
		return null;
	}
}
