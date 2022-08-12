/*******************************************************************************
 *  Copyright (c) 2000, 2011 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.internal.patch;

import java.util.Arrays;

import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;

/**
 * An DecoratorOverlayIcon consists of a main icon and several adornments.
 * Copied until bug 164394 is resolved.
 */
class DecoratorOverlayIcon extends CompositeImageDescriptor {
	// the base image
	private Image base;

	// the overlay images
	private ImageDescriptor[] overlays;

	// the size
	private Point size;


	/**
	 * OverlayIcon constructor.
	 *
	 * @param baseImage the base image
	 * @param overlaysArray the overlay images
	 * @param sizeValue the size
	 */
	public DecoratorOverlayIcon(Image baseImage,
			ImageDescriptor[] overlaysArray, Point sizeValue) {
		this.base = baseImage;
		this.overlays = overlaysArray;
		this.size = sizeValue;
	}

	/**
	 * Draw the overlays for the receiver.
	 * @param overlaysArray the overlay images
	 */
	protected void drawOverlays(ImageDescriptor[] overlaysArray) {

		for (int i = 0; i < overlays.length; i++) {
			ImageDescriptor overlay = overlaysArray[i];
			if (overlay == null) {
				continue;
			}
			ImageData overlayData = overlay.getImageData();
			//Use the missing descriptor if it is not there.
			if (overlayData == null) {
				overlayData = ImageDescriptor.getMissingImageDescriptor()
						.getImageData();
			}
			switch (i) {
			case IDecoration.TOP_LEFT:
				drawImage(overlayData, 0, 0);
				break;
			case IDecoration.TOP_RIGHT:
				drawImage(overlayData, size.x - overlayData.width, 0);
				break;
			case IDecoration.BOTTOM_LEFT:
				drawImage(overlayData, 0, size.y - overlayData.height);
				break;
			case IDecoration.BOTTOM_RIGHT:
				drawImage(overlayData, size.x - overlayData.width, size.y
						- overlayData.height);
				break;
			}
		}
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof DecoratorOverlayIcon)) {
			return false;
		}
		DecoratorOverlayIcon other = (DecoratorOverlayIcon) o;
		return base.equals(other.base)
				&& Arrays.equals(overlays, other.overlays);
	}

	@Override
	public int hashCode() {
		int code = base.hashCode();
		for (ImageDescriptor overlay : overlays) {
			if (overlay != null) {
				code ^= overlay.hashCode();
			}
		}
		return code;
	}

	@Override
	protected void drawCompositeImage(int width, int height) {
		ImageDescriptor underlay = overlays[IDecoration.UNDERLAY];
		if (underlay != null) {
			drawImage(underlay.getImageData(), 0, 0);
		}
		drawImage(base.getImageData(), 0, 0);
		drawOverlays(overlays);
	}

	@Override
	protected Point getSize() {
		return size;
	}

	@Override
	protected int getTransparentPixel() {
		return base.getImageData().transparentPixel;
	}

}
