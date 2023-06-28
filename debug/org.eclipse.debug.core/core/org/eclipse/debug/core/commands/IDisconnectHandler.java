/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
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

/**
 * A disconnect handler disconnects the debug user interface from
 * a debug session. Typically a disconnect handler is supported by remote
 * debuggers allowing the debug user interface to disconnect and the
 * remote process to continue.
 * <p>
 * Clients may implement this interface. The debug platform provides a
 * disconnect action that delegates to this handler interface. As well, the
 * debug platform provides an implementation of the disconnect handler registered
 * as an adapter on objects that implement {@link org.eclipse.debug.core.model.IDisconnect}.
 * </p>
 * @since 3.3
 */
public interface IDisconnectHandler extends IDebugCommandHandler {

}
