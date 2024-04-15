/*******************************************************************************
 * Copyright (c) 2013, 2017 Markus Alexander Kuppe and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Markus Alexander Kuppe - initial API and implementation
 *   Lars.Vogel <Lars.Vogel@vogella.com> - Bug 472654
 ******************************************************************************/
package org.eclipse.e4.core.di.internal.extensions;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.InjectionException;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.suppliers.ExtendedObjectSupplier;
import org.eclipse.e4.core.di.suppliers.IObjectDescriptor;
import org.eclipse.e4.core.di.suppliers.IRequestor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleListener;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

@Component(service = { ExtendedObjectSupplier.class, EventHandler.class }, property = {
		"dependency.injection.annotation=org.eclipse.e4.core.di.extensions.OSGiBundle",
		"event.topics=" + IEclipseContext.TOPIC_DISPOSE })
public class OSGiObjectSupplier extends ExtendedObjectSupplier implements EventHandler {

	/**
	 * A Map of Requestor to BundleListener. Each Requestor will only ever request its own bundle and thus there is a 1:1 relationship between R and BL.
	 */
	private final Map<IRequestor, BundleListener> requestor2listener = new HashMap<>();

	private BundleContext localBundleContext;

	@Activate
	void activate(BundleContext context) {
		this.localBundleContext = context;
	}

	@Override
	public Object get(IObjectDescriptor descriptor, IRequestor requestor, boolean track, boolean group) {
		final Class<?> requestingObjectClass = requestor.getRequestingObjectClass();

		final Type desiredType = descriptor.getDesiredType();
		if (BundleContext.class.equals(desiredType)) {
			final Bundle bundle = FrameworkUtil.getBundle(requestingObjectClass);

			// Cannot use BundleListener as a BL can only be registered with a BC (which might be null)
			// Iff track is request and there is no listener yet, lets track the bundle
			if (track) {
				if (!requestor2listener.containsKey(requestor)) {
					track(bundle, requestor);
				}
				// Handlers only executed once and thus don't track the BC/Bundle.
				// Still guard to now de-register a non-existing listener.
			} else if (requestor2listener.containsKey(requestor)) {
				untrack(requestor);
			}

			final BundleContext bundleContext = bundle.getBundleContext();
			if (bundleContext != null) {
				return bundleContext;
			} else if (descriptor.getQualifier(Optional.class) != null) {
				// Do not have a bundle context but requestor has marked the parameter/field optional
				return null;
			}
			throw new InjectionException("Unable to inject BundleContext: " + bundle.getSymbolicName() + " bundle is not active or starting/stopping"); //$NON-NLS-1$  //$NON-NLS-2$
		} else if (Bundle.class.equals(desiredType)) {
			// Not tracking the Bundle's life-cycle because the B instance does
			// not change whether a bundle is ACTIVE or RESOLVED. The only
			// thing worth tracking is when a bundle switches to the INSTALLED
			// state. However, the requestor will go away along with its bundle anyway.
			return FrameworkUtil.getBundle(requestingObjectClass);
		}
		// Annotation used with unsupported type
		return null;
	}

	private void untrack(final IRequestor requestor) {
		synchronized (requestor2listener) {
			BundleListener l = requestor2listener.remove(requestor);
			localBundleContext.removeBundleListener(l);
		}
	}

	private void track(final Bundle bundle, final IRequestor requestor) {
		// A _synchronous_ BundleListener asserts that the BC is un-injected,
		// _before_ it becomes invalid (state-wise).
		SynchronousBundleListener listener = event -> {
			if (event.getBundle().equals(bundle)) {
				if (requestor.isValid()) {
					requestor.resolveArguments(false);
					requestor.execute();
				}
			}
		};
		synchronized (requestor2listener) {
			localBundleContext.addBundleListener(listener);
			requestor2listener.put(requestor, listener);
		}
	}

	@Override
	public void handleEvent(Event event) {
		for (Iterator<Entry<IRequestor, BundleListener>> it = requestor2listener.entrySet().iterator(); it.hasNext();) {
			Entry<IRequestor, BundleListener> entry = it.next();
			if (!entry.getKey().isValid()) {
				localBundleContext.removeBundleListener(entry.getValue());
				it.remove();
			}
		}
	}
}
