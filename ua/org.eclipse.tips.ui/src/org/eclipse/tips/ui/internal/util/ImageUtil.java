/*******************************************************************************
 * Copyright (c) 2018 Remain Software
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     wim.jongman@remainsoftware.com - initial API and implementation
 *******************************************************************************/
package org.eclipse.tips.ui.internal.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;

/**
 * Image helper class.
 *
 */
public class ImageUtil {

	private static final String COMMA = ","; //$NON-NLS-1$

	/**
	 * Calculate a height that will fit in the proposed rectangle given the passed
	 * aspect ratio.
	 *
	 * @param aspectRatio
	 *            the aspect ration, e.g. 1.5 for a 3/2 ratio (width/height).
	 * @param widthHint
	 *            the available width in the viewport
	 * @param heightHint
	 *            the available height in the viewport
	 * @return the maximum height that will fit in the passed rectangle.
	 */
	public static int getHeight(double aspectRatio, int widthHint, int heightHint) {
		return (int) Math.min(heightHint, widthHint / aspectRatio);
	}

	/**
	 * Calculate a width that will fit in the proposed rectangle given the passed
	 * aspect ratio.
	 *
	 * @param aspectRatio
	 *            the aspect ration, e.g. 1.5 for a 3/2 ratio (width/height).
	 * @param widthHint
	 *            the available width in the viewport
	 * @param heightHint
	 *            the available height in the viewport
	 * @return the maximum width that will fit in the rectangle.
	 */
	public static int getWidth(double aspectRatio, int widthHint, int heightHint) {
		return (int) Math.min(widthHint, heightHint * aspectRatio);
	}

	/**
	 * Converts a base 64 encoded image into and ImageData object.
	 *
	 * @param base64Image
	 *            the images as a base64 string.
	 * @return the image decoded as base64
	 * @throws IOException
	 *             in case of IO problems
	 */
	public static ImageData decodeToImage(String base64Image) throws IOException {
		if (base64Image == null) {
			return null;
		}
		String image = processB64String(base64Image);
		Decoder decoder = Base64.getDecoder();
		byte[] imageByte = decoder.decode(image);
		try (ByteArrayInputStream bis = new ByteArrayInputStream(imageByte)) {
			return new ImageData(bis);
		}
	}

	/**
	 * Transforms the passed image to a Base64 String.
	 *
	 * @param image
	 *            the image to convert, null in is null out.
	 * @param format
	 *            see {@link ImageLoader#save(java.io.OutputStream, int)}
	 * @return The encoded image in base 64 or null if the passed image was null.
	 */
	public static String decodeFromImage(Image image, int format) {
		if (image == null) {
			return null;
		}
		ImageLoader loader = new ImageLoader();
		loader.data = new ImageData[] { image.getImageData() };
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		loader.save(bos, format);
		Encoder encoder = Base64.getEncoder();
		return getHeader(format) + encoder.encodeToString(bos.toByteArray());
	}

	private static String getHeader(int format) {
		return "data:image/" + getExtension(format) + ";base64,"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	private static String getExtension(int format) {
		switch (format) {
		case SWT.IMAGE_BMP:
			return "bmp"; //$NON-NLS-1$
		case SWT.IMAGE_BMP_RLE:
			return "bmp"; //$NON-NLS-1$
		case SWT.IMAGE_GIF:
			return "gif"; //$NON-NLS-1$
		case SWT.IMAGE_ICO:
			return "ico"; //$NON-NLS-1$
		case SWT.IMAGE_JPEG:
			return "jpeg"; //$NON-NLS-1$
		default:
			return "png"; //$NON-NLS-1$
		}
	}

	private static String processB64String(String base64Image) {
		int index = base64Image.substring(0, 50).indexOf(COMMA) + 1;
		return base64Image.substring(index);
	}
}