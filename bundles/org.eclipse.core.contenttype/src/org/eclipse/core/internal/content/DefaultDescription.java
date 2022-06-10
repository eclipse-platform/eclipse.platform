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

import org.eclipse.core.runtime.QualifiedName;

/**
 * A content description for which all  properties have default values.
 */
public final class DefaultDescription extends BasicDescription {

	public DefaultDescription(IContentTypeInfo contentTypeInfo) {
		super(contentTypeInfo);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof DefaultDescription))
			return false;
		// see ContentType.equals()
		return contentTypeInfo.equals(((DefaultDescription) obj).contentTypeInfo);
	}

	@Override
	public String getCharset() {
		return (String) getProperty(CHARSET);
	}

	@Override
	public Object getProperty(QualifiedName key) {
		return contentTypeInfo.getDefaultProperty(key);
	}

	@Override
	public int hashCode() {
		return contentTypeInfo.getContentType().hashCode();
	}

	@Override
	public boolean isRequested(QualifiedName key) {
		return false;
	}

	@Override
	public void setProperty(QualifiedName key, Object value) {
		throw new IllegalStateException();
	}

	@Override
	public String toString() {
		return "{default} : " + contentTypeInfo.getContentType(); //$NON-NLS-1$
	}
}
