/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
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
package org.eclipse.debug.core.commands;

import org.eclipse.debug.core.IRequest;

/**
 * A request to execute a command on specific elements. A debug command request is
 * passed to a {@link IDebugCommandHandler} when a command is invoked.
 * <p>
 * Clients that invoke command handlers may implement this interface.
 * </p>
 * @since 3.3
 */
public interface IDebugCommandRequest extends IRequest {

	/**
	 * Returns the elements to execute a command on.
	 *
	 * @return elements to execute a command on
	 */
	Object[] getElements();
}
