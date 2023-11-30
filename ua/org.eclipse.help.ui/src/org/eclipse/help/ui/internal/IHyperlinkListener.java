/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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
package org.eclipse.help.ui.internal;

import org.eclipse.swt.widgets.*;

public interface IHyperlinkListener {
	/**
	 * @param linkLabel
	 *            org.eclipse.swt.widgets.Label
	 */
	public void linkActivated(Control linkLabel);
	/**
	 * @param linkLabel
	 *            org.eclipse.swt.widgets.Label
	 */
	public void linkEntered(Control linkLabel);
	/**
	 * @param linkLabel
	 *            org.eclipse.swt.widgets.Label
	 */
	public void linkExited(Control linkLabel);
}
