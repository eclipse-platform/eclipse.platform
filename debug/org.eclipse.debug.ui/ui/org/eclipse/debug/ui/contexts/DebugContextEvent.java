/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
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
package org.eclipse.debug.ui.contexts;

import java.util.EventObject;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.ISelection;

/**
 * A debug context event. Debug context events are generated by debug context
 * providers. A debug context is represented by a selection and flags
 * (bit mask) describing how the context has changed.
 * <p>
 * Clients may instantiate this class.
 * </p>
 * @see IDebugContextListener
 * @see IDebugContextProvider
 * @since 3.3
 * @noextend This class is not intended to be subclassed by clients.
 */
public class DebugContextEvent extends EventObject {

	/**
	 * The context
	 */
	private final ISelection fContext;

	/**
	 * Change flags.
	 */
	private final int fFlags;

	/**
	 * Change constant (bit mask) indicating a context has been activated.
	 */
	public static final int ACTIVATED = 0x01;

	/**
	 * Change constant (bit mask) indicating the state of a context has changed.
	 * State changes are only broadcast for previously activated contexts.
	 */
	public static final int STATE = 0x10;

	/**
	 * Generated serial version UID for this class.
	 */
	private static final long serialVersionUID = 3395172504615255524L;

	/**
	 * Constructs a new debug context event.
	 *
	 * @param source source of the event - a debug context provider
	 * @param context the relevant context
	 * @param flags bit mask indicating how the context has changed - see change constants
	 * 	defined in this class
	 */
	public DebugContextEvent(IDebugContextProvider source, ISelection context, int flags) {
		super(source);
		Assert.isNotNull(context, "DebugContextEvent context must not be null"); //$NON-NLS-1$
		fContext = context;
		fFlags = flags;
	}

	/**
	 * Returns the debug context associated with this event.
	 *
	 * @return debug context, possible an empty selection
	 */
	public ISelection getContext() {
		return fContext;
	}

	/**
	 * Returns flags which describe in more detail how a context has changed.
	 * See change constants defined in this class.
	 *
	 * @return event flags
	 */
	public int getFlags() {
		return fFlags;
	}

	/**
	 * Returns the context provider that initiated this event.
	 *
	 * @return context provider
	 */
	public IDebugContextProvider getDebugContextProvider() {
		return (IDebugContextProvider) getSource();
	}
}
