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

import org.eclipse.help.ICommandLink;
import org.w3c.dom.Element;

public class CommandLink extends UAElement implements ICommandLink {

	public static final String NAME = "command"; //$NON-NLS-1$
	public static final String ATTRIBUTE_LABEL = "label"; //$NON-NLS-1$
	public static final String ATTRIBUTE_SERIALIZATION = "serialization"; //$NON-NLS-1$

	public CommandLink(ICommandLink src) {
		super(NAME, src);
		setLabel(src.getLabel());
		setSerialization(src.getSerialization());
	}

	public CommandLink(Element src) {
		super(src);
	}

	@Override
	public String getLabel() {
		return getAttribute(ATTRIBUTE_LABEL);
	}

	@Override
	public String getSerialization() {
		return getAttribute(ATTRIBUTE_SERIALIZATION);
	}

	public void setLabel(String label) {
		setAttribute(ATTRIBUTE_LABEL, label);
	}

	public void setSerialization(String serialization) {
		setAttribute(ATTRIBUTE_SERIALIZATION, serialization);
	}
}
