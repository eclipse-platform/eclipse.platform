/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others.
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
package org.eclipse.e4.core.contexts;

/**
 * The base class for all computed value implementations. Clients may subclass
 * this class. See the class comment of {@link IContextFunction} for specific
 * rules that must be followed by function implementations.
 * <p>
 * This class is intended to be subclassed by clients.
 * </p>
 * @see IContextFunction
 * @since 1.3
 */
public abstract class ContextFunction implements IContextFunction {

	/**
	 * Constructs a new instance of the context function
	 */
	public ContextFunction() {
		// placeholder
	}

	/**
	 * @deprecated {@link IContextFunction}'s compute() was changed to take the context key
	 */
	@Deprecated
	public Object compute(IEclipseContext context) {
		return null;
	}

	@Override
	public Object compute(IEclipseContext context, String contextKey) {
		// call into now-deprecated method to maintain backwards compatibility
		return compute(context);
	}

}
