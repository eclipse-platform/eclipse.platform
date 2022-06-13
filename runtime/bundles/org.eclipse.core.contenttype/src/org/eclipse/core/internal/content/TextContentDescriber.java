/*******************************************************************************
 * Copyright (c) 2004, 2008 IBM Corporation and others.
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

import java.io.*;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.ITextContentDescriber;

/**
 * This class provides internal basis for text-based content describers.
 *
 * <p>
 * Note: do not add protected/public members to this class if you don't intend to
 * make them public API.
 * </p>
 *
 * @see org.eclipse.core.runtime.content.XMLRootElementContentDescriber2
 * @since 3.0
 */
public class TextContentDescriber implements ITextContentDescriber {

	private final static QualifiedName[] SUPPORTED_OPTIONS = {IContentDescription.BYTE_ORDER_MARK};

	@Override
	public int describe(Reader contents, IContentDescription description) throws IOException {
		// we want to be pretty loose on detecting the text content type
		return INDETERMINATE;
	}

	@Override
	public int describe(InputStream contents, IContentDescription description) throws IOException {
		if (description == null || !description.isRequested(IContentDescription.BYTE_ORDER_MARK))
			return INDETERMINATE;
		byte[] bom = Util.getByteOrderMark(contents);
		if (bom != null)
			description.setProperty(IContentDescription.BYTE_ORDER_MARK, bom);
		// we want to be pretty loose on detecting the text content type
		return INDETERMINATE;
	}

	@Override
	public QualifiedName[] getSupportedOptions() {
		return SUPPORTED_OPTIONS;
	}
}
