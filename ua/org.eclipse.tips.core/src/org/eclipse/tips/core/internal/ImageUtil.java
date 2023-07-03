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
package org.eclipse.tips.core.internal;

/**
 * Image helper class.
 *
 */
public class ImageUtil {

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
}