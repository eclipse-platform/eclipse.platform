/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.viewers.model.provisional;

/**
 * Clients using a {@link ViewerInputService} implement this interface to be notified of
 * the computed viewer input.
 * <p>
 * Clients may implement this interface.
 * </p>
 * @see ViewerInputService
 * @since 3.4
 */
public interface IViewerInputRequestor {

	/**
	 * Notification that a viewer input update request is complete. The given update
	 * contains the result of the request, which may have been canceled.
	 *
	 * @param update viewer input update request
	 */
	void viewerInputComplete(IViewerInputUpdate update);
}
