/*******************************************************************************
 * Copyright (c) 2009, 2013 IBM Corporation and others.
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

import org.osgi.framework.BundleContext;

/**
 * A context function encapsulates evaluation of some code within an
 * {@link IEclipseContext}. The result of the function must be derived purely
 * from the provided arguments and context objects, and must be free from
 * side-effects other than the function's return value. In particular, the
 * function must be idempotent - subsequent invocations of the same function
 * with the same inputs must produce the same result.
 * <p>
 * A common use for context functions is as a place holder for an object that
 * has not yet been created. These place holders can be stored as values in an
 * {@link IEclipseContext}, allowing the concrete value they represent to be
 * computed lazily only when requested.
 * </p>
 * <p>
 * Context functions can optionally be registered as OSGi services. Context
 * implementations may use such registered services to seed context instances
 * with initial values. Registering your context function as a service is a
 * signal that contexts are free to add an instance of your function to their
 * context automatically, using the key specified by the
 * {@link #SERVICE_CONTEXT_KEY} service property.
 * </p>
 *
 * @see IEclipseContext#set(String, Object)
 * @noimplement This interface is not intended to be implemented by clients.
 *              Function implementations must subclass {@link ContextFunction}
 *              instead.
 * @since 1.3
 */
public interface IContextFunction {
	/**
	 * The OSGi service name for a context function service. This name can be
	 * used to obtain instances of the service.
	 *
	 * @see BundleContext#getServiceReference(String)
	 */
	String SERVICE_NAME = IContextFunction.class.getName();

	/**
	 * An OSGi service property used to indicate the context key this function
	 * should be registered in.
	 *
	 * @see BundleContext#getServiceReference(String)
	 */
	String SERVICE_CONTEXT_KEY = "service.context.key"; //$NON-NLS-1$

	/**
	 * Evaluates the function based on the provided arguments and context to
	 * produce a consistent result.
	 *
	 * @param context
	 *            The context in which to perform the value computation.
	 * @param contextKey
	 *            The context key used to find this function; may be {@code null} such
	 *            as if invoked directly.
	 * @return The concrete value. Implementations may return {@link org.eclipse.e4.core.di.IInjector#NOT_A_VALUE}
	 * 		to cause lookup to continue up the context hierarchy.
	 */
	Object compute(IEclipseContext context, String contextKey);

	/**
	 * Recursively looks up the root {@link IEclipseContext} in the context
	 * hierarchy.
	 *
	 * @param context {@link IEclipseContext} to get the parent
	 *                {@link IEclipseContext} from
	 * @return the root {@link IEclipseContext} in the context hierarchy
	 * @since 1.8
	 */
	static IEclipseContext getRootContext(IEclipseContext context) {
		if (context.getParent() == null) {
			return context;
		}

		return getRootContext(context.getParent());
	}
}
