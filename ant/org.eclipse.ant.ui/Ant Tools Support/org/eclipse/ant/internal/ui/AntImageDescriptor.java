/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
 *     John-Mason P. Shackelford (john-mason.shackelford@pearson.com) - bug 49445
 *******************************************************************************/
package org.eclipse.ant.internal.ui;

import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Point;

/**
 * A image descriptor consisting of a main icon and several adornments. The adornments are computed according to flags set on creation of the
 * descriptor.
 */
public class AntImageDescriptor extends CompositeImageDescriptor {

	/** Flag to render an error adornment */
	public final static int HAS_ERRORS = 0x0001;

	/** Flag to render an imported adornment */
	public final static int IMPORTED = 0x0002;

	/** Flag to render an warning adornment */
	public final static int HAS_WARNINGS = 0x0004;

	private ImageDescriptor fBaseImage;
	private int fFlags;
	private Point fSize;

	/**
	 * Create a new AntImageDescriptor.
	 * 
	 * @param baseImage
	 *            an image descriptor used as the base image
	 * @param flags
	 *            flags indicating which adornments are to be rendered
	 */
	public AntImageDescriptor(ImageDescriptor baseImage, int flags) {
		setBaseImage(baseImage);
		setFlags(flags);
	}

	@Override
	protected Point getSize() {
		if (fSize == null) {
			CachedImageDataProvider provider = createCachedImageDataProvider(getBaseImage());
			setSize(new Point(provider.getWidth(), provider.getHeight()));
		}
		return fSize;
	}

	@Override
	public boolean equals(Object object) {
		if (!(object instanceof AntImageDescriptor)) {
			return false;
		}

		AntImageDescriptor other = (AntImageDescriptor) object;
		return (getBaseImage().equals(other.getBaseImage()) && getFlags() == other.getFlags());
	}

	@Override
	public int hashCode() {
		return getBaseImage().hashCode() | getFlags();
	}

	@Override
	protected void drawCompositeImage(int width, int height) {
		drawImage(createCachedImageDataProvider(getBaseImage()), 0, 0);
		drawOverlays();
	}

	/**
	 * Add any overlays to the image as specified in the flags.
	 */
	protected void drawOverlays() {
		int flags = getFlags();
		int y = 0;
		CachedImageDataProvider provider;
		if ((flags & IMPORTED) != 0) {
			provider = createCachedImageDataProvider(AntUIImages.getImageDescriptor(IAntUIConstants.IMG_OVR_IMPORT));
			drawImage(provider, 0, 0);
		}
		if ((flags & HAS_ERRORS) != 0) {
			y = getSize().y;
			provider = createCachedImageDataProvider(AntUIImages.getImageDescriptor(IAntUIConstants.IMG_OVR_ERROR));
			y -= provider.getHeight();
			drawImage(provider, 0, y);
		} else if ((flags & HAS_WARNINGS) != 0) {
			y = getSize().y;
			provider = createCachedImageDataProvider(AntUIImages.getImageDescriptor(IAntUIConstants.IMG_OVR_WARNING));
			y -= provider.getHeight();
			drawImage(provider, 0, y);
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
