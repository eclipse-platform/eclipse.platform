/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
package org.eclipse.unittest.internal.ui;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;

/**
 * Manages a set of images that can show progress in the image itself.
 */
public class ProgressIcons {
	private final ImageData initialImageData;

	private static final class ProgressIconKey {
		private final int pixels;
		private final RGB color;

		public ProgressIconKey(int pixels, RGB color) {
			this.pixels = pixels;
			this.color = color;
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof ProgressIconKey)) {
				return false;
			}
			ProgressIconKey other = (ProgressIconKey) obj;
			return this.pixels == other.pixels && Objects.equals(this.color, other.color);
		}

		@Override
		public int hashCode() {
			return Objects.hash(Integer.valueOf(pixels), color);
		}
	}

	private final Map<ProgressIconKey, Image> progressIcons = new HashMap<>();
	private Device display;

	/**
	 * Constructs a progress icons object
	 *
	 * @param sourceImage a source image object
	 */
	public ProgressIcons(Image sourceImage) {
		this.initialImageData = sourceImage.getImageData();
		this.display = sourceImage.getDevice();
	}

	/**
	 * Disposes a progress icons object
	 */
	public void dispose() {
		this.progressIcons.values().forEach(Image::dispose);
		this.progressIcons.clear();
	}

	/**
	 * Returns an image added with counters
	 *
	 * @param current     a current test run number
	 * @param total       a total test count
	 * @param hasFailures a flag indicating if a test has failures
	 * @return an image object instance
	 */
	public Image getImage(int current, Integer total, boolean hasFailures) {
		int totalAsInt = total != null ? total.intValue() : current + 1;
		int pixelsToDraw = initialImageData.width * current / totalAsInt;
		RGB color = display.getSystemColor(hasFailures ? SWT.COLOR_RED : SWT.COLOR_GREEN).getRGB();
		ProgressIconKey key = new ProgressIconKey(pixelsToDraw, color);
		return progressIcons.computeIfAbsent(key, progressKey -> {
			ImageData imageData = (ImageData) initialImageData.clone();
			int pixelColorCode = imageData.palette.getPixel(color);
			for (int line = 4 * imageData.height / 5; line < imageData.height; line++) {
				for (int column = 0; column < pixelsToDraw && column < imageData.width; column++) {
					imageData.setAlpha(column, line, 255);
					imageData.setPixel(column, line, pixelColorCode);
				}
			}
			return new Image(display, imageData);
		});
	}

}
