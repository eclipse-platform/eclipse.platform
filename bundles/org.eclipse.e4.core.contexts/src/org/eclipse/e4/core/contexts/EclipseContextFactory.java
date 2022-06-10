/*******************************************************************************
 * Copyright (c) 2009, 2022 IBM Corporation and others.
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
 *     Christoph Läubrich - Issue #43
 *******************************************************************************/

package org.eclipse.e4.core.contexts;

import java.util.WeakHashMap;
import org.eclipse.e4.core.internal.contexts.EclipseContext;
import org.eclipse.e4.core.internal.contexts.osgi.EclipseContextOSGi;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

/**
 * This factory is used to create new context instances.
 * @see IEclipseContext
 * @since 1.3
 */
public final class EclipseContextFactory {

	/**
	 * Creates and returns a new empty context.
	 * @return A new empty context.
	 */
	static public IEclipseContext create() {
		return new EclipseContext(null);
	}

	/**
	 * Creates and returns a new empty context.
	 * @return A new empty context.
	 */
	static public IEclipseContext create(String name) {
		IEclipseContext result = create();
		result.set(EclipseContext.DEBUG_STRING, name);
		return result;
	}

	private static WeakHashMap<BundleContext, IEclipseContext> serviceContexts = new WeakHashMap<>();

	/**
	 * Returns a context that can be used to lookup OSGi services. A client must never dispose the
	 * provided context, because it may be shared by multiple callers.
	 *
	 * @param bundleContext
	 *            The bundle context to use for service lookup
	 * @return A context containing all OSGi services
	 */
	public static IEclipseContext getServiceContext(BundleContext bundleContext) {
		synchronized (serviceContexts) {
			IEclipseContext result = serviceContexts.get(bundleContext);
			if (result == null) {
				result = createServiceContext(bundleContext);
				serviceContexts.put(bundleContext, result);
			}
			return result;
		}
	}

	/**
	 * Returns a context that can be used to lookup OSGi services. A client must
	 * never dispose the provided context, because it may be shared by multiple
	 * callers.
	 *
	 * @param contextClass The class that is used as a context to use for service
	 *                     lookup
	 * @return A context containing all OSGi services
	 * @since 1.10
	 */
	public static IEclipseContext getServiceContext(Class<?> contextClass) {
		Bundle bundle = FrameworkUtil.getBundle(contextClass);
		if (bundle == null) {
			throw new IllegalArgumentException("The passed context class is not loaded by an OSGI framework!"); //$NON-NLS-1$
		}
		BundleContext bundleContext = bundle.getBundleContext();
		if (bundleContext == null) {
			throw new IllegalStateException(
					String.format(
							"The bundle %s is not started yet, either start it explicitly or add 'Bundle-ActivationPolicy: lazy' header to it before using this method!", //$NON-NLS-1$
							bundle.getSymbolicName()));
		}
		return getServiceContext(bundleContext);
	}

	/**
	 * Creates and returns a new context that can be used to lookup OSGi
	 * services. A client must dispose the provided context.
	 *
	 * @param bundleContext The bundle context to use for service lookup
	 * @return A new context containing all OSGi services
	 *
	 * @since 1.5
	 */
	public static IEclipseContext createServiceContext(BundleContext bundleContext) {
		IEclipseContext result = new EclipseContextOSGi(bundleContext);
		result.set(EclipseContext.DEBUG_STRING, "OSGi context for bundle: " + bundleContext.getBundle().getSymbolicName()); //$NON-NLS-1$
		return result;
	}

}