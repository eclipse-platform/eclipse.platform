/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
package org.eclipse.debug.internal.core.sourcelookup;

import java.util.Comparator;

/**
 * Comparator for source locator mementos. Ignores whitespace differences.
 *
 * @since 3.0
 */
public class SourceLocatorMementoComparator implements Comparator<String> {

	@Override
	public int compare(String o1, String o2) {
		if (o1 == null && o2 == null) {
			return 0;
		}
		if (o1 == null) {
			return -1;
		}
		if (o2 == null) {
			return 1;
		}
		String m1 = o1;
		String m2 = o2;
		int i1 = 0, i2 = 0;
		while (i1 < m1.length()) {
			i1 = skipWhitespace(m1, i1);
			i2 = skipWhitespace(m2, i2);
			if (i1 < m1.length() && i2 < m2.length()) {
				if (m1.charAt(i1) != m2.charAt(i2)) {
					return m1.charAt(i1) - m2.charAt(i2);
				}
				i1++;
				i2++;
			} else {
				if (i2 < m2.length()) {
					return -1;
				}
				if (i1 < m1.length()) {
					return 1;
				}
				return 0;
			}
		}
		return 0;
	}

	private int skipWhitespace(String string, int offset) {
		int off = offset;
		while (off < string.length() && Character.isWhitespace(string.charAt(off))) {
			off++;
		}
		return off;
	}
}
