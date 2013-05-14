/*******************************************************************************
 *  Copyright (c) 2000, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.internal.patch;

import java.util.Arrays;

import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.swt.graphics.*;

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

    public boolean equals(Object o) {
        if (!(o instanceof DecoratorOverlayIcon)) {
			return false;
		}
        DecoratorOverlayIcon other = (DecoratorOverlayIcon) o;
        return base.equals(other.base)
                && Arrays.equals(overlays, other.overlays);
    }

    public int hashCode() {
        int code = base.hashCode();
        for (int i = 0; i < overlays.length; i++) {
            if (overlays[i] != null) {
				code ^= overlays[i].hashCode();
			}
        }
        return code;
    }

    protected void drawCompositeImage(int width, int height) {
        ImageDescriptor underlay = overlays[IDecoration.UNDERLAY];
        if (underlay != null) {
			drawImage(underlay.getImageData(), 0, 0);
		}
        drawImage(base.getImageData(), 0, 0);
        drawOverlays(overlays);
    }

    protected Point getSize() {
        return size;
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.jface.resource.CompositeImageDescriptor#getTransparentPixel()
     */
    protected int getTransparentPixel() {
    	return base.getImageData().transparentPixel;
    }

}
