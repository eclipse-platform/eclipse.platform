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
package org.eclipse.help.internal;

import org.eclipse.help.IAnchor;
import org.w3c.dom.Element;

public class Anchor extends UAElement implements IAnchor {

	public static final String NAME = "anchor"; //$NON-NLS-1$
	public static final String ATTRIBUTE_ID = "id"; //$NON-NLS-1$

	public Anchor(IAnchor src) {
		super(NAME, src);
		setId(src.getId());
	}

	public Anchor(Element src) {
		super(src);
	}

	@Override
	public String getId() {
		return getAttribute(ATTRIBUTE_ID);
	}

	public void setId(String id) {
		setAttribute(ATTRIBUTE_ID, id);
	}
}
