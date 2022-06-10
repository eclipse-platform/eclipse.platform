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

import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.IContentType;

public abstract class BasicDescription implements IContentDescription {

	protected IContentTypeInfo contentTypeInfo;

	public BasicDescription(IContentTypeInfo contentTypeInfo) {
		this.contentTypeInfo = contentTypeInfo;
	}

	@Override
	public IContentType getContentType() {
		ContentType contentType = contentTypeInfo.getContentType();
		//TODO performance: potential creation of garbage
		return new ContentTypeHandler(contentType, contentType.getCatalog().getGeneration());
	}

	public IContentTypeInfo getContentTypeInfo() {
		return contentTypeInfo;
	}
}
