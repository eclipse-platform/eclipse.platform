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
package org.eclipse.ui.externaltools.internal.launchConfigurations;

import java.util.Comparator;

public class IgnoreWhiteSpaceComparator implements Comparator<String> {

	@Override
	public int compare(String o1, String o2) {
		if (o1 == null || o2 == null) {
			if (o1 == o2) {
				return 0;
			}
			return -1;
		}
		int i1 = 0;
		int i2 = 0;
		int l1 = o1.length();
		int l2 = o2.length();
		char ch1 = ' ';
		char ch2 = ' ';
		while (i1 < l1 && i2 < l2) {
			while (i1 < l1 && Character.isWhitespace(ch1 = o1.charAt(i1))) {
				i1++;
			}
			while (i2 < l2 && Character.isWhitespace(ch2 = o2.charAt(i2))) {
				i2++;
			}
			if (i1 == l1 && i2 == l2) {
				return 0;
			}
			if (ch1 != ch2) {
				return -1;
			}
			i1++;
			i2++;
		}
		return 0;
	}
}
