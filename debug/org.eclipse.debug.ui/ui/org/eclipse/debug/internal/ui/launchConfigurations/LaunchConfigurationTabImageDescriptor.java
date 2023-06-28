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
 *******************************************************************************/
package org.eclipse.debug.internal.ui.launchConfigurations;


import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

/**
 * A JDIImageDescriptor consists of a main icon and several adornments. The adornments
 * are computed according to flags set on creation of the descriptor.
 */
public class LaunchConfigurationTabImageDescriptor extends CompositeImageDescriptor {

	/** Flag to render the error adornment */
	public final static int ERROR= 		0x001;

	private Image fBaseImage;
	private int fFlags;
	private Point fSize;

	/**
	 * Create a new JDIImageDescriptor.
	 *
	 * @param baseImage an image descriptor used as the base image
	 * @param flags flags indicating which adornments are to be rendered
	 *
	 */
	public LaunchConfigurationTabImageDescriptor(Image baseImage, int flags) {
		setBaseImage(baseImage);
		setFlags(flags);
	}

	/**
	 * @see CompositeImageDescriptor#getSize()
	 */
	@Override
	protected Point getSize() {
		if (fSize == null) {
			CachedImageDataProvider provider = createCachedImageDataProvider(getBaseImage());
			setSize(new Point(provider.getWidth(), provider.getHeight()));
		}
		return fSize;
	}

	/**
	 * @see Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object object) {
		if (!(object instanceof LaunchConfigurationTabImageDescriptor)){
			return false;
		}

		LaunchConfigurationTabImageDescriptor other= (LaunchConfigurationTabImageDescriptor)object;
		return (getBaseImage().equals(other.getBaseImage()) && getFlags() == other.getFlags());
	}

	/**
	 * @see Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return getBaseImage().hashCode() | getFlags();
	}

	/**
	 * @see CompositeImageDescriptor#drawCompositeImage(int, int)
	 */
	@Override
	protected void drawCompositeImage(int width, int height) {
		drawImage(createCachedImageDataProvider(getBaseImage()), 0, 0);
		drawOverlays();
	}

	/**
	 * Add any overlays to the image as specified in the flags.
	 */
	protected void drawOverlays() {
		int flags= getFlags();
		CachedImageDataProvider provider;
		if ((flags & ERROR) != 0) {
			provider = createCachedImageDataProvider(DebugUITools.getImage(IDebugUIConstants.IMG_OVR_ERROR));
			drawImage(provider, 0, 0);
		}
	}

	protected Image getBaseImage() {
		return fBaseImage;
	}

	protected void setBaseImage(Image baseImage) {
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
