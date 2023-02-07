/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
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

package org.eclipse.help.ui.internal.util;

/**
 * Utility class for preparing strings for display in a FormText widget by
 * escaping the necessary characters
 */

public class EscapeUtils {

	/**
	 * Replace every occurrence of {@code &, <, >, ', "} by an escape character
	 * Replace tabs with spaces
	 * @param value the original string, may not be null
	 * @return the escaped string
	 */
	public static String escapeSpecialChars(String value) {
		return escapeSpecialChars(value, false);
	}

	/**
	 * Replace every occurrence of {@code &, <, >, ', "} by an escape character
	 * but allow <b> and </b> through
	 * Replace tabs with spaces
	 * @param value the original string, may not be null
	 * @return the escaped string
	 */
	public static String escapeSpecialCharsLeavinggBold(String value) {
		return escapeSpecialChars(value, true);
	}

	public static String escapeAmpersand(String value) {
		return value.replaceAll("&", "&amp;"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Escape any ampersands used in a label
	 */
	public static String escapeForLabel(String message) {
		// Make the most common case - i.e. no ampersand the
		// most efficient
		if (message.indexOf('&') < 0) {
			return message;
		}

		int next = 0;
		StringBuilder result = new StringBuilder();
		int index = message.indexOf('&');
		while (index >= 0) {
			result.append(message.substring(next, index + 1));
			result.append('&');
			next = index + 1;
			index = message.indexOf('&', next);
		}
		result.append(message.substring(next));
		return result.toString();
	}

	private static String escapeSpecialChars(String value, boolean leaveBold) {
		if (value == null) {
			return null;
		}
		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);

			switch (c) {
			case '&':
				buf.append("&amp;"); //$NON-NLS-1$
				break;
			case '<':
				if (leaveBold) {
					int length = value.length();
					if (i +  6 < length) {
						String tag = value.substring(i, i+7);
						if (tag.equalsIgnoreCase("</code>")) { //$NON-NLS-1$
							buf.append("</span>"); //$NON-NLS-1$
							i+= 6;
							continue;
						}
					}
					if (i +  5 < length) {
						String tag = value.substring(i, i+6);
						if (tag.equalsIgnoreCase("<code>")) { //$NON-NLS-1$
							buf.append("<span font=\"code\">"); //$NON-NLS-1$
							i+= 5;
							continue;
						}
					}
					if (i + 3 < length) {
						String tag = value.substring(i, i + 4);
						if (tag.equalsIgnoreCase("</b>")) { //$NON-NLS-1$
							buf.append(tag);
							i += 3;
							continue;
						}
						if (tag.equalsIgnoreCase("<br>")) { //$NON-NLS-1$
							buf.append("<br/>"); //$NON-NLS-1$
							i+= 3;
							continue;
						}
					}
					if (i + 2 < length) {
						String tag = value.substring(i, i + 3);
						if (tag.equalsIgnoreCase("<b>")) { //$NON-NLS-1$
							buf.append(tag);
							i += 2;
							continue;
						}
					}
				}
				buf.append("&lt;"); //$NON-NLS-1$
				break;
			case '>':
				buf.append("&gt;"); //$NON-NLS-1$
				break;
			case '\'':
				buf.append("&apos;"); //$NON-NLS-1$
				break;
			case '\"':
				buf.append("&quot;"); //$NON-NLS-1$
				break;
			case 160:
				buf.append(" "); //$NON-NLS-1$
				break;
			case '\t':
				buf.append(' ');
				break;
			default:
				buf.append(c);
				break;
			}
		}
		return buf.toString();
	}

}
