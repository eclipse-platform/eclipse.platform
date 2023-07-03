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

import org.eclipse.debug.core.DebugPlugin;

/**
 * A step filters handler typically toggles the use of step filters
 * in a debug session based on the user preference setting. To determine if step filters
 * should be enabled use the method <code>isUseStepFilters()</code> in
 * {@link DebugPlugin}.
 * <p>
 * Clients may implement this interface. The debug platform provides a
 * toggle step filters action that delegates to this handler interface. As well, the
 * debug platform provides an implementation of the step filters handler registered
 * as an adapter on objects that implement
 * {@link org.eclipse.debug.core.model.IStepFilters}.
 * </p>
 * @since 3.3
 */
public interface IStepFiltersHandler extends IDebugCommandHandler {

}
