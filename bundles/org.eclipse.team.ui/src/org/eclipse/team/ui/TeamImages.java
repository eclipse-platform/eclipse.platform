/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.ui;

import org.eclipse.core.runtime.IExtension;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.team.internal.ui.TeamUIPlugin;

/**
 * TeamImages provides convenience methods for accessing shared images
 * provided by the <i>org.eclipse.team.ui</i> plug-in.
 * <p>
 * This class provides <code>ImageDescriptor</code>s for each named image in
 * {@link ISharedImages}.  All <code>Image</code> objects created from the
 * provided descriptors are managed the caller and must be disposed appropriately.
 * </p>
 * <p>
 * This class is not intended to be subclassed or instantiated by clients
 * @since 2.0
 */
public class TeamImages {
	/**
	 * Returns the image descriptor for the given image ID.
	 * Returns <code>null</code> if there is no such image.
	 *
	 * @param id  the identifier for the image to retrieve
	 * @return the image descriptor associated with the given ID
	 */
	public static ImageDescriptor getImageDescriptor(String id) {
		return TeamUIPlugin.getImageDescriptor(id);
	}
	/**
	 * Convenience method to get an image descriptor for an extension.
	 *
	 * @param extension  the extension declaring the image
	 * @param subdirectoryAndFilename the path to the image
	 * @return the image descriptor for the extension
	 */
	public static ImageDescriptor getImageDescriptorFromExtension(IExtension extension, String subdirectoryAndFilename) {
		return TeamUIPlugin.getImageDescriptorFromExtension(extension, subdirectoryAndFilename);
	}
}
