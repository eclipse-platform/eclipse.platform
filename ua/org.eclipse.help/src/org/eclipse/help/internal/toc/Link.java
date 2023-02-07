/*******************************************************************************
 * Copyright (c) 2007, 2015 IBM Corporation and others.
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
package org.eclipse.help.internal.toc;

import org.eclipse.help.ILink;
import org.eclipse.help.internal.UAElement;
import org.w3c.dom.Element;

public class Link extends UAElement implements ILink {

	public static final String NAME = "link"; //$NON-NLS-1$
	public static final String ATTRIBUTE_TOC = "toc"; //$NON-NLS-1$

	public Link(ILink src) {
		super(NAME, src);
		setToc(src.getToc());
	}

	public Link(Element src) {
		super(src);
	}

	@Override
	public String getToc() {
		return getAttribute(ATTRIBUTE_TOC);
	}

	public void setToc(String toc) {
		setAttribute(ATTRIBUTE_TOC, toc);
	}
}
