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

import org.eclipse.debug.internal.ui.viewers.model.provisional.IStatusMonitor;

/**
 * A request monitor that collects whether an element contains
 * children.
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 * @since 3.2
 */
public interface IContainerRequestMonitor extends IStatusMonitor {

	/**
	 * Sets whether an element contains children.
	 *
	 * @param container whether an element contains children
	 */
	void setIsContainer(boolean container);

}
