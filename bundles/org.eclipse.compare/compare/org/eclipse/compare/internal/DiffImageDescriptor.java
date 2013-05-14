/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.internal;

import org.eclipse.swt.graphics.*;

import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;

/**
 * Combines an image with an overlay.
 */
public class DiffImageDescriptor extends CompositeImageDescriptor {

	static final int HEIGHT= 16;

	private final ImageData fBaseImageData;
	private final ImageDescriptor fOverlayImage;
	private final int fWidth;
	private final boolean fLeft;
	private final int hashCode;

	public DiffImageDescriptor(Image base, ImageDescriptor overlay, int w, boolean onLeft) {
		ImageData data = null;
		if (base != null) {
			data = base.getImageData();
			if (data == null)
				data = DEFAULT_IMAGE_DATA;
		}
		fBaseImageData = data;
		fOverlayImage= overlay;
		fWidth= w;
		fLeft= onLeft;
		hashCode = calculateHashCode();
	}

	private int calculateHashCode() {
		int h1 = 0;
		int h2 = 0;
		if (fBaseImageData != null) {
			h1 = calculateHash(fBaseImageData);
		}
		if (fOverlayImage != null) {
			h2 = fOverlayImage.hashCode();
		}
		return h1 + h2 + fWidth;
	}
	
	private int calculateHash(ImageData baseImageData) {
		byte[] data = baseImageData.data;
		int hash = baseImageData.width + baseImageData.height;
		for (int i = 0; i < data.length; i++) {
			byte b = data[i];
			hash >>>= 1;
			hash ^= b;
		}
		return hash;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.resource.CompositeImageDescriptor#getSize()
	 */
	protected Point getSize() {
		return new Point(fWidth, HEIGHT);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.resource.CompositeImageDescriptor#drawCompositeImage(int, int)
	 */
	protected void drawCompositeImage(int width, int height) {
		if (fLeft) {
			if (fBaseImageData != null) {
				drawImage(fBaseImageData, fWidth - fBaseImageData.width, 0);
			}
	
			if (fOverlayImage != null) {
				ImageData overlay= fOverlayImage.getImageData();
				if (overlay == null)
					overlay= DEFAULT_IMAGE_DATA;
				drawImage(overlay, 0, (HEIGHT - overlay.height) / 2);
			}
		} else {
			if (fBaseImageData != null) {
				drawImage(fBaseImageData, 0, 0);
			}
	
			if (fOverlayImage != null) {
				ImageData overlay= fOverlayImage.getImageData();
				if (overlay == null)
					overlay= DEFAULT_IMAGE_DATA;
				drawImage(overlay, fWidth - overlay.width, (HEIGHT - overlay.height) / 2);
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return hashCode;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj instanceof DiffImageDescriptor) {
			DiffImageDescriptor other = (DiffImageDescriptor) obj;
			return (other.hashCode == hashCode
					&& isEqual(other.fOverlayImage, fOverlayImage)
					&& other.fWidth == fWidth 
					&& other.fLeft == fLeft
					&& isEqual(other.fBaseImageData, fBaseImageData));
		}
		return false;
	}
	
	private boolean isEqual(ImageData i1, ImageData i2) {
		if (isEqual((Object) i1, (Object) i2)) {
			return true;
		}
		if (i1 == null || i2 == null)
			return false;
		return (i1.width == i2.width && i1.height == i2.height
				&& i1.depth == i2.depth && i1.scanlinePad == i2.scanlinePad
				&& i1.bytesPerLine == i2.bytesPerLine
				/* && i1.palette == i2.palette */
				&& i1.transparentPixel == i2.transparentPixel
				&& i1.maskPad == i2.maskPad
	            && i1.alpha == i2.alpha
				&& i1.type == i2.type && i1.x == i2.x && i1.y == i2.y
				&& i1.disposalMethod == i2.disposalMethod && i1.delayTime == i2.delayTime
				&& equals(i1.data,i2.data) && equals(i1.maskData, i2.maskData)
				&& equals(i1.alphaData, i2.alphaData));
	}
	
	private boolean equals(byte[] data, byte[] data2) {
		if (isEqual(data, data2))
			return true;
		if (data == null || data2 == null)
			return false;
		if (data.length != data2.length)
			return false;
		for (int i = 0; i < data2.length; i++) {
			if (data[i] != data2[i])
				return false;
		}
		return true;
	}

	private boolean isEqual(Object o1, Object o2) {
		if (o1 == o2)
			return true;
		if (o1 == null || o2 == null)
			return false;
		return o1.equals(o2);
	}
}
