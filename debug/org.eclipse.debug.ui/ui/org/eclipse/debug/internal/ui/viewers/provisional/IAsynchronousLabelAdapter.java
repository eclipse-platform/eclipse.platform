/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.viewers.provisional;

import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;

/**
 * Provides labels for elements. Note that implementations
 * are must provide labels asynchronously.
 * <p>
 * Clients may implement this interface.
 * </p>
 * @since 3.2
 */
public interface IAsynchronousLabelAdapter {

	/**
	 * Asynchronously retrieves the label of the given object reporting to
	 * the given monitor. If unable to retrieve label information, an exception should be
	 * reported to the monitor with an appropriate status.
	 *
	 * @param object the element for which a label is requested
	 * @param context the context in which the label has been requested
	 * @param monitor request monitor to report the result to
	 */
	void retrieveLabel(Object object, IPresentationContext context, ILabelRequestMonitor result);


}
