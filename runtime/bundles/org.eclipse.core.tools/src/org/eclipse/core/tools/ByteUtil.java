/*******************************************************************************
 * Copyright (c) 2002, 2006 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tools;

/**
 * A set of utility methods for dealing with byte arrays.
 */
public class ByteUtil {

	/**
	 * Helper method that creates a string representation for a byte array.
	 *
	 * @param byteArray a byte array to be represented as string
	 * @param max a maximum number of bytes to be considered - if zero, there is
	 * no maximum.
	 * @return a byte array string representation
	 */
	public static String byteArrayToString(byte[] byteArray, int max) {
		StringBuilder result = new StringBuilder(byteArray.length * 2 + 2);
		int bytesToBeShown = (max > 0) ? (Math.min(max, byteArray.length)) : byteArray.length;
		result.append('[');
		for (int i = 0; i < bytesToBeShown; i++) {
			result.append(byteArray[i]);
			result.append(',');
		}
		// adds an ellipsis if there is too much bytes to show
		if (max > 0 && max < byteArray.length)
			result.append("..."); //$NON-NLS-1$
		// or remove the trailing comma
		else
			result.deleteCharAt(result.length() - 1);
		result.append(']');
		return result.toString();
	}
}
