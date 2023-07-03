/*******************************************************************************
 * Copyright (c) 2016 Manumitting Technologies Inc and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Manumitting Technologies Inc - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.intro.quicklinks;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

/**
 * Simple label provider that knows how to load images from a URL.
 */
public class URLLabelProvider extends LabelProvider {
	private LocalResourceManager registry;

	public URLLabelProvider() {
		this.registry = new LocalResourceManager(JFaceResources.getResources());
	}

	@Override
	public Image getImage(Object element) {
		try {
			ImageDescriptor descriptor = null;
			if (element instanceof String) {
				descriptor = ImageDescriptor.createFromURL(new URL((String) element));
			} else if (element instanceof URL) {
				descriptor = ImageDescriptor.createFromURL((URL) element);
			}
			if (descriptor == null) {
				return null;
			}
			return registry.create(descriptor);
		} catch (MalformedURLException e) {
			return null;
		}
	}

	@Override
	public String getText(Object element) {
		return (String) element;
	}

	@Override
	public void dispose() {
		registry.dispose();
		super.dispose();
	}
}
