/****************************************************************************
 * Copyright (c) 2017, 2018 Remain Software
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Wim Jongman <wim.jongman@remainsoftware.com> - initial API and implementation
 *****************************************************************************/
package org.eclipse.tips.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Base64;

import org.eclipse.core.runtime.Assert;
import org.eclipse.tips.core.internal.ImageUtil;

/**
 * Provides more information about the image to be used in the tip. The image
 * aspect ratio must be around 3:2 to be comfortably displayed in the Tip UI.
 *
 */
public class TipImage {

	private static final double THREE_TO_TWO = 1.5;

	/**
	 * Value to indicate that the height or width are to be determined by the Tip
	 * framework.
	 */
	public static final int UNDEFINED = -1;

	private String fExtension = null;
	private int fMaxWidth = UNDEFINED;
	private int fMaxHeight = UNDEFINED;
	private final URL fURL;
	private double fAspectRatio = THREE_TO_TWO;

	private final String fBase64Image;

	/**
	 * Creates a new TipImage with the specified URL which gets read into a base 64
	 * string.
	 *
	 * @param url
	 *            the image URL which may not be null
	 * @throws IOException
	 *             in case the stream of the passed URL could not be opened or read.
	 *
	 */
	public TipImage(URL url) throws IOException {
		Assert.isNotNull(url);
		fURL = url;
		byte[] bytes;
		try (InputStream in = url.openStream()) {
			bytes = in.readAllBytes();
		}
		fBase64Image = "data:image/" // //$NON-NLS-1$
				+ getExtension() //
				+ ";base64," // //$NON-NLS-1$
				+ Base64.getEncoder().encodeToString(bytes);
	}

	/**
	 * @return true if the URL constructor was used and not the base64 constructor.
	 */
	public boolean isURLSet() {
		return fURL != null;
	}

	/**
	 * Creates a new {@link TipImage} with the specified base64 string which must be
	 * a valid RFC-2397 string thats begins with
	 * <code>"data:image/&lt;subtype&gt;;base64"</code> where <code>subtype</code>
	 * must be a valid image type (pmg, bmp, gif, etc..).
	 *
	 * @param base64Image
	 *            the non-null base64 encoded image according to RFC-2397.
	 *
	 * @throws RuntimeException
	 *             if the string is not valid
	 * @see TipImage
	 * @see <a href="https://tools.ietf.org/search/rfc2397">RFC-2397
	 *      (https://tools.ietf.org/search/rfc2397)</a>
	 *
	 */
	public TipImage(String base64Image) {
		Assert.isNotNull(base64Image);
		fURL = null;
		if (base64Image.matches("^data:image\\/.*?;base64,.*$")) { //$NON-NLS-1$
			fBase64Image = base64Image;
			int from = base64Image.indexOf('/') + 1;
			int to = base64Image.indexOf(';');
			setExtension(base64Image.substring(from, to).trim());
			setExtension(base64Image.substring(from, to).trim());
		} else {
			int length = base64Image.length();
			throw new RuntimeException(Messages.TipImage_5 + base64Image.substring(0, length < 50 ? length : 50));
		}
	}

	/**
	 * Sets the maximum height that this image can display. For example, if you have
	 * a 32x32 image the framework will blow it up to a larger size which will not
	 * work for the image and you might want to pass 64 to indicate that the image
	 * cannot be resized passed 64 pixels. If the height is not set or set to
	 * {@link #UNDEFINED}, then it is automatically resized based on aspect ratio
	 * and maximum width.
	 *
	 * @param maxHeight
	 *            the maximum height for this image or {@link #UNDEFINED}
	 * @return this
	 * @see #setAspectRatio(double)
	 * @see #setAspectRatio(int, int, boolean)
	 * @see #setMaxWidth(int)
	 */
	public TipImage setMaxHeight(int maxHeight) {
		fMaxHeight = maxHeight;
		return this;
	}

	/**
	 * Sets the maximum width that this image can display. For example, if you have
	 * a 32x32 image the framework will blow it up to a larger size which will not
	 * work for the image and you might want to pass 64 to indicate that the image
	 * cannot be resized passed 64 pixels. If the width is not set or set to
	 * {@link #UNDEFINED}, it is automatically resized based on aspect ratio and
	 * maximum height.
	 *
	 * @param maxWidth
	 *            the maximum width for this image or {@link #UNDEFINED}
	 * @return this
	 * @see #setAspectRatio(double)
	 * @see #setAspectRatio(int, int, boolean)
	 * @see #setMaxHeight(int)
	 */
	public TipImage setMaxWidth(int maxWidth) {
		fMaxWidth = maxWidth;
		return this;
	}

	/**
	 * Sets the aspect ratio of this image. If the image is 300 wide and 600 high
	 * then the aspect ratio is 300/600 = 0,5 (1:2). If your image is 1200 wide and
	 * 250 high then the aspect ratio (1200/250) = 4,8. With the supplied values the
	 * best dimensions for the image can be calculated give the available space in
	 * the UI.
	 * <p>
	 * In case you pass true for <code>pSetAsMax</code> then the image can not be
	 * up-scaled beyond the specified size. So if your image is 200x200 and you want
	 * a maximum up-scale of 2 then pass 400x400 to this method to maintain the
	 * aspect ratio but allow the image to be resized to maximum it's double size.
	 * <p>
	 * The recommended aspect ratio is around 3:2 (1.5) to be comfortably displayed
	 * in the Tip UI.
	 *
	 * @param width
	 *            the width of the image, must be greater than 0
	 * @param height
	 *            the height of the image, must be greater than 0
	 * @param setAsMax
	 *            true to set the passed width and height as the maximum width and
	 *            height for the image
	 * @return this
	 * @see #setAspectRatio(double)
	 * @see #getIMGAttributes(int, int)
	 * @see #setMaxHeight(int)
	 * @see #setMaxWidth(int)
	 */
	public TipImage setAspectRatio(int width, int height, boolean setAsMax) {
		Assert.isTrue(width > 0);
		Assert.isTrue(height > 0);
		fAspectRatio = (double) width / (double) height;
		if (setAsMax) {
			setMaxHeight(height);
			setMaxWidth(width);
		}
		return this;
	}

	/**
	 * Sets the aspect ratio of your image which is defined by width divided by
	 * height.
	 * <p>
	 * The recommended aspect ratio is around 3:2 (1.5) to be comfortably displayed
	 * in the Tip UI.
	 *
	 *
	 * @param aspectRatio
	 *            the aspect ration
	 * @return this
	 * @see #setAspectRatio(int, int, boolean)
	 * @see #getIMGAttributes(int, int)
	 */
	public TipImage setAspectRatio(double aspectRatio) {
		fAspectRatio = aspectRatio;
		return this;
	}

	/**
	 * Changes the default value "null" to the passed value which commonly is "png",
	 * "gif" and such.
	 *
	 * @param extension
	 *            the extension of this file
	 * @return this
	 * @see #getExtension()
	 */
	public TipImage setExtension(String extension) {
		fExtension = extension;
		return this;
	}

	/**
	 * Returns the base64 encoded image string according to RFC-2397 or null. The
	 * recommended aspect ratio is around 3:2.
	 *
	 * @return the base64 encoded image string according to RFC-2397 or null
	 */
	public String getBase64Image() {
		return fBase64Image;
	}

	/**
	 * Returns the width and height attributes of the HTML IMG tag.
	 *
	 * <pre>
	 *  &lt;img src="smiley.gif" height="42" width="42"&gt;
	 * </pre>
	 *
	 * The available space in the UI is passed and with it the best size of the
	 * image will be calculated based on the aspect ratio of this image.
	 *
	 * Clients may override if they can provide better information.
	 *
	 * @param widthHint
	 *            the available width which must be greater than 0
	 * @param heightHint
	 *            the available height which must be greater than 0
	 * @return the attributes in the HTML img tag
	 * @see TipImage#setAspectRatio(double)
	 * @see TipImage#setAspectRatio(int, int, boolean)
	 * @see TipImage#setMaxHeight(int)
	 * @see TipImage#setMaxWidth(int)
	 */
	public String getIMGAttributes(int widthHint, int heightHint) {

		int myWidthHint = (fMaxWidth == UNDEFINED) ? widthHint : Math.min(widthHint, fMaxWidth);
		int myHeightHint = (fMaxHeight == UNDEFINED) ? heightHint : Math.min(heightHint, fMaxHeight);

		int width = ImageUtil.getWidth(fAspectRatio, myWidthHint, myHeightHint);
		int height = ImageUtil.getHeight(fAspectRatio, myWidthHint, myHeightHint);

		String result = ""; //$NON-NLS-1$
		if (fMaxWidth == UNDEFINED) {
			result += " width=\"" + width + "\""; //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			result += " width=\"" + Math.min(fMaxWidth, width) + "\""; //$NON-NLS-1$ //$NON-NLS-2$
		}

		if (fMaxHeight == UNDEFINED) {
			result += " height=\"" + height + "\""; //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			result += " height=\"" + Math.min(fMaxHeight, height) + "\""; //$NON-NLS-1$ //$NON-NLS-2$
		}
		return result;
	}

	/**
	 * Returns the image extension for use in the IMG tag for the data attribute
	 * (<code>data:image/???</code>). If the extension is not set in this object,
	 * then the URL is examined to find the extension. If that can not be determined
	 * then "png" is returned.
	 *
	 * @return the extension
	 */
	private String getExtension() {
		if (fExtension != null) {
			return fExtension;
		}
		String[] split = fURL.getPath().split("\\."); //$NON-NLS-1$
		if (split.length > 1) {
			fExtension = split[split.length - 1];
		} else {
			fExtension = "png"; //$NON-NLS-1$
		}
		return fExtension;
	}
}