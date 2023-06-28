/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.views.launch;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.debug.internal.ui.DelegatingModelPresentation;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;

/**
 * Translates images, colors, and fonts into image descriptors, RGBs, and font
 * data for workbench adapters. Also provides labels.
 *
 * @since 3.1
 */
public class DebugElementHelper {

	// a model presentation that can provide images & labels for debug elements
	private static DelegatingModelPresentation fgPresenetation;

	// map of images to image descriptors
	private static Map<Image, ImageDescriptor> fgImages = new HashMap<>();

	/**
	 * Disposes this adapter
	 */
	public static void dispose() {
		fgImages.clear();
		if (fgPresenetation != null) {
			fgPresenetation.dispose();
			fgPresenetation = null;
		}
	}

	/**
	 * Returns an image descriptor for the given debug element.
	 *
	 * @param object object for which an image descriptor is required
	 */
	public static ImageDescriptor getImageDescriptor(Object object) {
		Image image = getPresentation().getImage(object);
		return getImageDescriptor(image);
	}

	/**
	 * Returns an image descriptor for the given debug element.
	 *
	 * @param presentation presentation to obtain image from
	 * @param object object for which an image descriptor is required
	 * @since 3.3
	 */
	public static ImageDescriptor getImageDescriptor(Object object, IDebugModelPresentation presentation) {
		Image image = presentation.getImage(object);
		return getImageDescriptor(image);
	}

	public static ImageDescriptor getImageDescriptor(Image image) {
		if (image != null) {
			ImageDescriptor descriptor = fgImages.get(image);
			if (descriptor == null) {
				descriptor = new ImageImageDescriptor(image);
				fgImages.put(image, descriptor);
			}
			return descriptor;
		}
		return null;
	}

	/**
	 * Returns a label for the given debug element.
	 *
	 * @param o object for which a label is required
	 */
	public static String getLabel(Object o) {
		return getPresentation().getText(o);
	}

	/**
	 * Returns a model presentation to use to retrieve lables & images.
	 *
	 * @return a model presentation to use to retrieve lables & images
	 */
	public static DelegatingModelPresentation getPresentation() {
		if (fgPresenetation == null) {
			fgPresenetation = new DelegatingModelPresentation();
		}
		return fgPresenetation;
	}

	/**
	 * Returns the RGB of the foreground color for the given element, or
	 * <code>null</code> if none.
	 *
	 * @param element object for which a foreground color is required
	 * @return the RGB of the foreground color for the given element, or
	 *         <code>null</code> if none
	 */
	public static RGB getForeground(Object element) {
		Color color = getPresentation().getForeground(element);
		if (color != null) {
			return color.getRGB();
		}
		return null;
	}

	/**
	 * Returns the RGB of the foreground color for the given element, or
	 * <code>null</code> if none.
	 *
	 * @param element object for which a foreground color is required
	 * @param presentation presentation to obtain color from
	 * @return the RGB of the foreground color for the given element, or
	 *         <code>null</code> if none
	 * @since 3.3
	 */
	public static RGB getForeground(Object element, IDebugModelPresentation presentation) {
		Color color = null;
		if (presentation instanceof IColorProvider) {
			IColorProvider colorProvider = (IColorProvider) presentation;
			color = colorProvider.getForeground(element);
		} else {
			color = getPresentation().getForeground(element);
		}
		if (color != null) {
			return color.getRGB();
		}
		return null;
	}

	/**
	 * Returns the RGB of the background color for the given element, or
	 * <code>null</code> if none.
	 *
	 * @param element object for which a background color is required
	 * @return the RGB of the background color for the given element, or
	 *         <code>null</code> if none
	 */
	public static RGB getBackground(Object element) {
		Color color = getPresentation().getBackground(element);
		if (color != null) {
			return color.getRGB();
		}
		return null;
	}

	/**
	 * Returns the RGB of the background color for the given element, or
	 * <code>null</code> if none.
	 *
	 * @param element object for which a background color is required
	 * @param presentation presentation to use to retrieve color
	 * @return the RGB of the background color for the given element, or
	 *         <code>null</code> if none
	 * @since 3.3
	 */
	public static RGB getBackground(Object element, IDebugModelPresentation presentation) {
		Color color = null;
		if (presentation instanceof IColorProvider) {
			IColorProvider colorProvider = (IColorProvider) presentation;
			color = colorProvider.getBackground(element);
		} else {
			color = getPresentation().getBackground(element);
		}
		if (color != null) {
			return color.getRGB();
		}
		return null;
	}

	/**
	 * Returns the font data for the given element, or <code>null</code> if
	 * none.
	 *
	 * @param element object for which font data is required
	 * @return the font data for the given element, or <code>null</code> if none
	 */
	public static FontData getFont(Object element) {
		Font font = getPresentation().getFont(element);
		if (font != null) {
			return font.getFontData()[0];
		}
		return null;
	}

	/**
	 * Returns the font data for the given element, or <code>null</code> if
	 * none.
	 *
	 * @param element object for which font data is required
	 * @param presentation presentation to obtain font from
	 * @return the font data for the given element, or <code>null</code> if none
	 * @since 3.3
	 */
	public static FontData getFont(Object element, IDebugModelPresentation presentation) {
		Font font = null;
		if (presentation instanceof IFontProvider) {
			IFontProvider provider = (IFontProvider) presentation;
			font = provider.getFont(element);
		} else {
			font = getPresentation().getFont(element);
		}
		if (font != null) {
			return font.getFontData()[0];
		}
		return null;
	}

	/**
	 * Returns whether the UI thread is required for computing the label for the
	 * given object.
	 *
	 * @param object object a label is being computed for
	 * @return whether the UI thread is required
	 */
	public static boolean requiresUIThread(Object object) {
		DelegatingModelPresentation presentation = getPresentation();
		return presentation.requiresUIThread(object);
	}
}
