/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
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
package org.eclipse.core.internal.content;

import org.eclipse.core.runtime.content.IContentType;

/**
 * Provides a uniform representation for file specifications, such
 * as file names, file extensions and regular expressions.
 */
class FileSpec {
	final static int BASIC_TYPE = IContentType.FILE_EXTENSION_SPEC | IContentType.FILE_NAME_SPEC;
	private String text;
	private int type;

	public FileSpec(String text, int type) {
		this.text = text;
		this.type = type;
	}

	public String getText() {
		return text;
	}

	public int getType() {
		return type;
	}

	public static int getBasicType(int type) {
		return BASIC_TYPE & type;
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof FileSpec))
			return false;
		FileSpec otherFileSpec = (FileSpec) other;
		return equals(text, otherFileSpec.getType(), false);
	}

	public boolean equals(final String text, final int otherType, final boolean strict) {
		return ((!strict && getBasicType(type) == getBasicType(otherType)) || type == otherType) && this.text.equalsIgnoreCase(text);
	}

	@Override
	public int hashCode() {
		return text.hashCode();
	}

	public static String getMappingKeyFor(String fileSpecText) {
		return fileSpecText.toLowerCase();
	}

	@Override
	public String toString() {
		return getText();
	}
}
