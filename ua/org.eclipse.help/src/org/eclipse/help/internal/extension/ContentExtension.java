/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
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
package org.eclipse.help.internal.extension;

import org.eclipse.help.IContentExtension;
import org.eclipse.help.internal.UAElement;
import org.w3c.dom.Element;

public class ContentExtension extends UAElement implements IContentExtension {

	public static final String NAME_CONTRIBUTION = "contribution"; //$NON-NLS-1$
	public static final String NAME_CONTRIBUTION_LEGACY = "topicExtension"; //$NON-NLS-1$
	public static final String NAME_REPLACEMENT = "replacement"; //$NON-NLS-1$
	public static final String NAME_REPLACEMENT_LEGACY = "topicReplace"; //$NON-NLS-1$
	public static final String ATTRIBUTE_CONTENT = "content"; //$NON-NLS-1$
	public static final String ATTRIBUTE_PATH = "path"; //$NON-NLS-1$

	public ContentExtension(IContentExtension src) {
		super(src.getType() == IContentExtension.CONTRIBUTION ? NAME_CONTRIBUTION : NAME_REPLACEMENT, src);
		setContent(src.getContent());
		setPath(src.getPath());
	}

	public ContentExtension(Element src) {
		super(src);
	}

	@Override
	public String getContent() {
		return getAttribute(ATTRIBUTE_CONTENT);
	}

	@Override
	public String getPath() {
		return getAttribute(ATTRIBUTE_PATH);
	}

	@Override
	public int getType() {
		String name = getElementName();
		return (NAME_CONTRIBUTION.equals(name) || NAME_CONTRIBUTION_LEGACY.equals(name)) ? CONTRIBUTION : REPLACEMENT;
	}

	public void setContent(String content) {
		setAttribute(ATTRIBUTE_CONTENT, content);
	}

	public void setPath(String path) {
		setAttribute(ATTRIBUTE_PATH, path);
	}
}
