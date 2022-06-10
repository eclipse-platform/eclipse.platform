/*******************************************************************************
 * Copyright (c) 2014, 2018 BestSolution.at and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Tom Schindl <tom.schindl@bestsolution.at> - initial API and implementation
 *     Dirk Fauth <dirk.fauth@googlemail.com> - Bug 513563
 *     Christoph Läubrich - Bug 572476
 *******************************************************************************/
package org.eclipse.e4.core.di.internal.extensions;

import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.IInjector;
import org.eclipse.e4.core.di.extensions.Service;
import org.eclipse.e4.core.di.suppliers.ExtendedObjectSupplier;
import org.eclipse.e4.core.di.suppliers.IObjectDescriptor;
import org.eclipse.e4.core.di.suppliers.IRequestor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.log.Logger;
import org.osgi.service.log.LoggerFactory;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Supplier for {@link Service}
 */
@Component(service = { ExtendedObjectSupplier.class, EventHandler.class }, property = {
		"dependency.injection.annotation=org.eclipse.e4.core.di.extensions.Service",
		"event.topics=" + IEclipseContext.TOPIC_DISPOSE })
public class ServiceSupplier extends ExtendedObjectSupplier implements EventHandler {

	LoggerFactory factory;
	Logger logger;

	private volatile BundleTracker<ServiceSupplierContext> supplierContextTracker;

	@Activate
	void activate(BundleContext bundleContext) {
		this.supplierContextTracker = new BundleTracker<>(bundleContext,
				Bundle.RESOLVED | Bundle.STARTING | Bundle.ACTIVE | Bundle.STOPPING,
				new BundleTrackerCustomizer<ServiceSupplierContext>() {

					@Override
					public ServiceSupplierContext addingBundle(Bundle bundle, BundleEvent event) {
						return new ServiceSupplierContext(bundle, bundleContext,
								(t, e) -> logError("Injection failed", e));//$NON-NLS-1$
					}

					@Override
					public void modifiedBundle(Bundle bundle, BundleEvent event, ServiceSupplierContext object) {
						switch (event.getType()) {
						case BundleEvent.STOPPING:
							object.serviceTracker.values().forEach(ServiceTracker::close);
							break;
						case BundleEvent.STARTED:
						case BundleEvent.STOPPED:
							object.refreshServices();
							break;
						default:
							break;
						}
					}

					@Override
					public void removedBundle(Bundle bundle, BundleEvent event, ServiceSupplierContext object) {
						object.dispose();
					}
				});
		this.supplierContextTracker.open();
	}

	@Deactivate
	void deactivate(BundleContext bundleContext) {
		this.supplierContextTracker.close();
	}

	@Override
	public Object get(IObjectDescriptor descriptor, IRequestor requestor, boolean track, boolean group) {
		Type desiredType = descriptor.getDesiredType();
		Bundle bundle = FrameworkUtil.getBundle(requestor.getRequestingObjectClass());
		ServiceSupplierContext supplierContext;
		if (bundle == null || (supplierContext = supplierContextTracker.getObject(bundle)) == null
				|| supplierContext.disposed) {
			// bundle has gone...
			return IInjector.NOT_A_VALUE;
		}
		Service qualifier = descriptor.getQualifier(Service.class);
		if (desiredType instanceof ParameterizedType) {
			ParameterizedType t = (ParameterizedType) desiredType;
			if (t.getRawType() == Collections.class || t.getRawType() == List.class) {
				return handleCollection(supplierContext, t.getActualTypeArguments()[0], requestor,
						track && qualifier.dynamic(), qualifier);
			}
		}

		return handleSingle(supplierContext, desiredType, requestor, track && qualifier.dynamic(), qualifier);
	}

	private Object handleSingle(ServiceSupplierContext supplierContext, Type t, IRequestor requestor, boolean track,
			Service qualifier) {
		Class<?> cls = t instanceof ParameterizedType ? (Class<?>) ((ParameterizedType) t).getRawType() : (Class<?>) t;
		try {
			Filter filter = supplierContext.getFilter(qualifier.filterExpression());
			ServiceSupplierTracker<?> tracker = supplierContext.getTracker(cls);
			return tracker.getAndTrack(filter, track ? requestor : null, s -> {
				Optional<?> service = s.findFirst();
				if (service.isPresent()) {
					return service.get();
				}
				return IInjector.NOT_A_VALUE;
			});
		} catch (InvalidSyntaxException e) {
			logError("Invalid filter expression", e); //$NON-NLS-1$
			return IInjector.NOT_A_VALUE;
		}
	}

	private Object handleCollection(ServiceSupplierContext supplierContext, Type t, IRequestor requestor, boolean track,
			Service qualifier) {
		Class<?> cls = t instanceof ParameterizedType ? (Class<?>) ((ParameterizedType) t).getRawType() : (Class<?>) t;
		try {
			Filter filter = supplierContext.getFilter(qualifier.filterExpression());
			ServiceSupplierTracker<?> tracker = supplierContext.getTracker(cls);
			return tracker.getAndTrack(filter, track ? requestor : null, s -> s.collect(Collectors.toList()));
		} catch (InvalidSyntaxException e) {
			logError("Invalid filter expression", e); //$NON-NLS-1$
			return IInjector.NOT_A_VALUE;
		}
	}

	/**
	 * Method to log an exception.
	 *
	 * @param message The log message.
	 * @param e       The exception that should be logged.
	 */
	void logError(String message, Throwable e) {
		Logger log = this.logger;
		if (log != null) {
			log.error(message, e);
		} else {
			// fallback if no LogService is available
			e.printStackTrace();
		}
	}

	@Override
	public void handleEvent(Event event) {
		supplierContextTracker.getTracked().values().stream().flatMap(ctx -> ctx.serviceTracker.values().stream())
				.forEach(ServiceSupplierTracker::cleanup);
	}

	@Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
	void setLogger(LoggerFactory factory) {
		this.factory = factory;
		this.logger = factory.getLogger(getClass());
	}

	void unsetLogger(LoggerFactory loggerFactory) {
		if (this.factory == loggerFactory) {
			this.factory = null;
			this.logger = null;
		}
	}

	private static final class ServiceSupplierContext {

		private Bundle bundle;
		private BundleContext serviceBundleContext;
		final Map<Class<?>, ServiceSupplierTracker<?>> serviceTracker = new ConcurrentHashMap<>();
		volatile boolean disposed;
		private UncaughtExceptionHandler exceptionHandler;

		ServiceSupplierContext(Bundle bundle, BundleContext serviceBundleContext,
				UncaughtExceptionHandler exceptionHandler) {
			this.bundle = bundle;
			this.serviceBundleContext = serviceBundleContext;
			this.exceptionHandler = exceptionHandler;
		}

		public Filter getFilter(String filterExpression) throws InvalidSyntaxException {
			if (filterExpression == null || filterExpression.isEmpty() || disposed) {
				return null;
			}
			return serviceBundleContext.createFilter(filterExpression);
		}

		@SuppressWarnings("unchecked")
		public <T> ServiceSupplierTracker<T> getTracker(Class<T> serviceClass) {
			return (ServiceSupplierTracker<T>) serviceTracker.computeIfAbsent(serviceClass, cls -> {
				BundleContext bundleContext = bundle.getBundleContext();
				if (bundleContext == null || bundle.getState() == Bundle.STOPPING) {
					bundleContext = serviceBundleContext;
				}
				ServiceSupplierTracker<T> tracker = new ServiceSupplierTracker<>(bundleContext, serviceClass,
						exceptionHandler);
				tracker.open();
				return tracker;
			});

		}

		void refreshServices() {
			for (Iterator<Entry<Class<?>, ServiceSupplierTracker<?>>> iter = serviceTracker.entrySet().iterator(); iter
					.hasNext();) {
				Entry<Class<?>, ServiceSupplierTracker<?>> entry = iter.next();
				serviceTracker.compute(entry.getKey(), (k, v) -> {
					if (v != null) {
						v.close();
						v.update(null);
					}
					return null;
				});
			}
		}

		void dispose() {
			disposed = true;
			refreshServices();
		}

	}

	private static final class ServiceSupplierTracker<T> extends ServiceTracker<T, T> {

		Set<IRequestor> trackedRequestors = ConcurrentHashMap.newKeySet();
		private UncaughtExceptionHandler exceptionHandler;
		private AtomicReference<CompletableFuture<?>> pending = new AtomicReference<>();

		public ServiceSupplierTracker(BundleContext context, Class<T> clazz,
				UncaughtExceptionHandler exceptionHandler) {
			super(context, clazz, null);
			this.exceptionHandler = exceptionHandler;
		}

		public synchronized <R> R getAndTrack(Filter f, IRequestor requestor, Function<Stream<T>, R> extractor) {
			cleanup();
			Stream<T> stream = getTracked().entrySet().stream().filter(entry -> f == null || f.match(entry.getKey()))
					.map(Entry::getValue);
			R value = extractor.apply(stream);
			if (requestor != null) {
				trackedRequestors.add(requestor);
			}
			return value;
		}

		void cleanup() {
			for (Iterator<IRequestor> iterator = trackedRequestors.iterator(); iterator.hasNext();) {
				IRequestor requestor = iterator.next();
				if (!requestor.isValid()) {
					iterator.remove();
				}
			}
		}

		@Override
		public T addingService(ServiceReference<T> reference) {
			T service = super.addingService(reference);
			if (service != null) {
				update(() -> getService(reference) != null);
			}
			return service;
		}

		@Override
		public void removedService(ServiceReference<T> reference, T service) {
			super.removedService(reference, service);
			update(() -> getService(reference) == null);
		}

		@Override
		public void modifiedService(ServiceReference<T> reference, T service) {
			super.modifiedService(reference, service);
			update(null);
		}

		private void update(BooleanSupplier check) {
			IRequestor[] requestors = trackedRequestors.toArray(IRequestor[]::new);
			if (requestors.length == 0) {
				return;
			}
			CompletableFuture<Void> execution = new CompletableFuture<>();
			CompletableFuture<?> pendingExecution = pending.getAndSet(execution);
			if (pendingExecution != null) {
				pendingExecution.cancel(true);
			}
			execution.completeAsync(() -> {
				if (check != null) {
					do {
						Thread.yield();
					} while (!check.getAsBoolean() && !execution.isCancelled());
				}
				if (!execution.isCancelled()) {
					refreshRequestors(requestors);
				}
				return null;
			}).handle((v, e) -> {
				if (e instanceof CancellationException) {
					// request was canceled
					return null;
				}
				if (e != null) {
					exceptionHandler.uncaughtException(Thread.currentThread(), e);
				}
				pending.compareAndSet(execution, null);
				return null;
			});
		}

		private synchronized void refreshRequestors(IRequestor[] requestors) {
			for (IRequestor requestor : requestors) {
				if (requestor.isValid()) {
					try {
						requestor.resolveArguments(false);
						requestor.execute();
					} catch (RuntimeException e) {
						exceptionHandler.uncaughtException(Thread.currentThread(), e);
					}
				} else {
					trackedRequestors.remove(requestor);
				}

			}
		}

	}

}
