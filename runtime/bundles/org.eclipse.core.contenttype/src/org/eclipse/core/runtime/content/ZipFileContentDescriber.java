/*******************************************************************************
 * Copyright (c) 2024 Vector Informatik GmbH and others.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: Vector Informatik GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.runtime.content;

import java.io.IOException;
import java.io.InputStream;
import org.eclipse.core.internal.content.TextContentDescriber;

/**
 * @since 3.10
 *
 */
public class ZipFileContentDescriber extends TextContentDescriber {

	@Override
	public int describe(InputStream contents, IContentDescription description) throws IOException {

		IContentType type = description.getContentType();
		if (type == null || description == null)
			return VALID;

		if (type.getId().equals("zipfile")) { //$NON-NLS-1$
			return VALID;
		}
		return INVALID;
	}
}
