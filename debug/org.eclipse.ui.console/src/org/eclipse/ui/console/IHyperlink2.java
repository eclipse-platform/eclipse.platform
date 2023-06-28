/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
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
package org.eclipse.ui.console;

import org.eclipse.swt.widgets.Event;

/**
 * Optional extension to {@link IHyperlink}.
 * <p>
 * Clients implementing {@link IHyperlink} may also implement this interface.
 * When implemented, the method <code>linkActivated(Event)</code> is called instead of
 * <code>linkActivated()</code>.
 * </p>
 * @since 3.2
 */
public interface IHyperlink2 extends IHyperlink {

	/**
	 * Notification that this link has been activated. Performs
	 * context specific linking.
	 *
	 * @param event the SWT event which triggered this hyperlink
	 */
	void linkActivated(Event event);
}
