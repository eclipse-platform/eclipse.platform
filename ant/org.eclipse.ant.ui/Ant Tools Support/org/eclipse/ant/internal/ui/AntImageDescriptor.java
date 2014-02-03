/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     John-Mason P. Shackelford (john-mason.shackelford@pearson.com) - bug 49445
 *******************************************************************************/
package org.eclipse.ant.internal.ui;

import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;

/**
 * A image descriptor consisting of a main icon and several adornments. The adornments
 * are computed according to flags set on creation of the descriptor.
 */
public class AntImageDescriptor extends CompositeImageDescriptor {
	
	/** Flag to render an error adornment */
	public final static int HAS_ERRORS= 0x0001;

	/** Flag to render an imported adornment */
	public final static int IMPORTED= 0x0002;
	
	/** Flag to render an warning adornment */
	public final static int HAS_WARNINGS= 0x0004;
	
	private ImageDescriptor fBaseImage;
	private int fFlags;
	private Point fSize;

	/**
	 * Create a new AntImageDescriptor.
	 * 
	 * @param baseImage an image descriptor used as the base image
	 * @param flags flags indicating which adornments are to be rendered
	 * 
	 */
	public AntImageDescriptor(ImageDescriptor baseImage, int flags) {
		setBaseImage(baseImage);
		setFlags(flags);
	}
	
	/**
	 * @see CompositeImageDescriptor#getSize()
	 */
	protected Point getSize() {
		if (fSize == null) {
			ImageData data= getBaseImage().getImageData();
			setSize(new Point(data.width, data.height));
		}
		return fSize;
	}
	
	/**
	 * @see Object#equals(java.lang.Object)
	 */
	public boolean equals(Object object) {
		if (!(object instanceof AntImageDescriptor)){
			return false;
		}
			
		AntImageDescriptor other= (AntImageDescriptor)object;
		return (getBaseImage().equals(other.getBaseImage()) && getFlags() == other.getFlags());
	}
	
	/**
	 * @see Object#hashCode()
	 */
	public int hashCode() {
		return getBaseImage().hashCode() | getFlags();
	}
	
	/**
	 * @see CompositeImageDescriptor#drawCompositeImage(int, int)
	 */
	protected void drawCompositeImage(int width, int height) {
		ImageData bg= getBaseImage().getImageData();
		if (bg == null) {
			bg= DEFAULT_IMAGE_DATA;
		}
		drawImage(bg, 0, 0);
		drawOverlays();
	}

	/**
	 * Add any overlays to the image as specified in the flags.
	 */
	protected void drawOverlays() {
		int flags= getFlags();
		int y= 0;
		ImageData data= null;
		if ((flags & IMPORTED) != 0) {
			data= AntUIImages.getImageDescriptor(IAntUIConstants.IMG_OVR_IMPORT).getImageData();
			drawImage(data, 0, 0);
		}
		if ((flags & HAS_ERRORS) != 0) {
			y= getSize().y;
			data= AntUIImages.getImageDescriptor(IAntUIConstants.IMG_OVR_ERROR).getImageData();
			y -= data.height;
			drawImage(data, 0, y);
		} else if ((flags & HAS_WARNINGS) != 0) {
			y= getSize().y;
			data= AntUIImages.getImageDescriptor(IAntUIConstants.IMG_OVR_WARNING).getImageData();
			y -= data.height;
			drawImage(data, 0, y);
		}
	}
	
	protected ImageDescriptor getBaseImage() {
		return fBaseImage;
	}

	protected void setBaseImage(ImageDescriptor baseImage) {
		fBaseImage = baseImage;
	}

	protected int getFlags() {
		return fFlags;
	}

	protected void setFlags(int flags) {
		fFlags = flags;
	}

	protected void setSize(Point size) {
		fSize = size;
	}
}
