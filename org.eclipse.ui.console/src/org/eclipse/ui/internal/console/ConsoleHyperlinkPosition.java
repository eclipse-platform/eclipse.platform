/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
package org.eclipse.ui.internal.console;

import org.eclipse.jface.text.Position;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IHyperlink;

/**
 * Describes the position of a hyperlink within the Console's document.
 *
 * @since 3.1
 */
public class ConsoleHyperlinkPosition extends Position {

	public static final String HYPER_LINK_CATEGORY = ConsolePlugin.getUniqueIdentifier() + ".CONSOLE_HYPERLINK_POSITION"; //$NON-NLS-1$

	private IHyperlink fLink = null;

	public ConsoleHyperlinkPosition(IHyperlink link, int offset, int length) {
		super(offset, length);
		fLink = link;
	}

	public IHyperlink getHyperLink() {
		return fLink;
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object arg) {
		return arg instanceof ConsoleHyperlinkPosition && super.equals(arg) && getHyperLink().equals(((ConsoleHyperlinkPosition)arg).getHyperLink());
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return super.hashCode() + getHyperLink().hashCode();
	}

}
