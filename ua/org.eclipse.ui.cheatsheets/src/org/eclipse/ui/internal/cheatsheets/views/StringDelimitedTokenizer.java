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
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.internal.cheatsheets.views;

public class StringDelimitedTokenizer {

	private String str;
	private String delimiter;
	private int delimiterLength;
	private int currentPosition;
	private int maxPosition;

	public StringDelimitedTokenizer(String str, String delim) {
		currentPosition = 0;
		this.str = str;
		this.delimiter = delim;
		maxPosition = this.str.length();
		delimiterLength = this.delimiter.length();
	}

	public int countTokens() {
		int count = 0;
		int startPosition = 0;

		while (startPosition < maxPosition && startPosition != -1) {
			startPosition = str.indexOf(delimiter, startPosition);
			if (startPosition != -1) {
				startPosition += delimiterLength;
			}
			count++;
		}

		return count;
	}

	public boolean endsWithDelimiter() {
		return str.endsWith(delimiter);
	}

	public boolean hasMoreTokens() {
		return (currentPosition < maxPosition);
	}

	public String nextToken() {
		int position = str.indexOf(delimiter, currentPosition);
		String token = null;
		if (position == -1) {
			token = str.substring(currentPosition);
			currentPosition = maxPosition;
		} else {
			token = str.substring(currentPosition, position);
			currentPosition = position + delimiterLength;
		}
		return token;
	}
}
