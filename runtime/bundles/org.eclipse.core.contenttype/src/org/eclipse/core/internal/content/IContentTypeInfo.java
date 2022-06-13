/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
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

import org.eclipse.core.runtime.QualifiedName;

/**
 * @since 3.1
 */
public abstract interface IContentTypeInfo {
	/**
	 * Returns a reference to the corresponding content type.
	 */
	ContentType getContentType();

	/**
	 * Returns the default value for the given property, delegating to the
	 * ancestor type if necessary.
	 */
	String getDefaultProperty(QualifiedName key);
}
