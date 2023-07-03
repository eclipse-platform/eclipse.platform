/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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
package org.eclipse.help.internal.base.util;

/**
 * This class provides static methods for some of the very used String
 * operations
 */
public class TString {
	// change all occurrences of oldPat to newPat
	public static String change(String in, String oldPat, String newPat) {
		if (oldPat.length() == 0)
			return in;
		if (oldPat.length() == 1 && newPat.length() == 1)
			return in.replace(oldPat.charAt(0), newPat.charAt(0));
		if (!in.contains(oldPat))
			return in;
		int lastIndex = 0;
		int newIndex = 0;
		StringBuilder newString = new StringBuilder();
		for (;;) {
			newIndex = in.indexOf(oldPat, lastIndex);
			if (newIndex != -1) {
				newString.append(in.substring(lastIndex, newIndex) + newPat);
				lastIndex = newIndex + oldPat.length();
			} else {
				newString.append(in.substring(lastIndex));
				break;
			}
		}
		return newString.toString();
	}
}
