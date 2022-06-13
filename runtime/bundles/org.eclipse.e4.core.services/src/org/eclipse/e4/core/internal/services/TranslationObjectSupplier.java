/*******************************************************************************
 * Copyright (c) 2011, 2017 BestSolution.at and others.
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
 *     Dirk Fauth <dirk.fauth@googlemail.com> - modifications to support locale changes at runtime
 ******************************************************************************/
package org.eclipse.e4.core.internal.services;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import javax.inject.Inject;
import javax.inject.Named;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.suppliers.ExtendedObjectSupplier;
import org.eclipse.e4.core.di.suppliers.IObjectDescriptor;
import org.eclipse.e4.core.di.suppliers.IRequestor;
import org.eclipse.e4.core.services.nls.IMessageFactoryService;
import org.eclipse.e4.core.services.nls.Message;
import org.eclipse.e4.core.services.translation.ResourceBundleProvider;
import org.eclipse.e4.core.services.translation.TranslationService;
import org.eclipse.osgi.service.localization.BundleLocalization;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

@Component(service = { ExtendedObjectSupplier.class, EventHandler.class }, property = {
		"dependency.injection.annotation=org.eclipse.e4.core.services.nls.Translation",
		"event.topics=" + IEclipseContext.TOPIC_DISPOSE })
public class TranslationObjectSupplier extends ExtendedObjectSupplier implements EventHandler {

	/**
	 * The current active locale that gets injected for updating the message instances.
	 */
	private Locale locale;

	/**
	 * The service that gets {@link ResourceBundle} objects from a bundle with a given locale.
	 */
	@Inject
	private ResourceBundleProvider provider;

	/**
	 * The service that creates instances of message classes based on the current active locale, the
	 * {@link BundleLocalization} and the configuration used in the {@link Message} annotation.
	 */
	@Inject
	private IMessageFactoryService factoryService;

	/**
	 * Map that contains all {@link IRequestor} that requested an instance of a messages class. Used
	 * to inform all requestor if the instances have changed due to a locale change.
	 */
	private Map<Class<?>, Set<IRequestor>> listeners = new ConcurrentHashMap<>();

	@Override
	public Object get(IObjectDescriptor descriptor, IRequestor requestor, boolean track, boolean group) {

		Class<?> descriptorsClass = getDesiredClass(descriptor.getDesiredType());

		if (track)
			addListener(descriptorsClass, requestor);

		return getMessageInstance(descriptorsClass);
	}

	/**
	 * Setting the {@link Locale} by using this method will cause to create new instances for all
	 * message classes that were requested before. It also notifys all {@link IRequestor} that
	 * requested those messages instance which causes dynamic reinjection.
	 *
	 * @param locale
	 *            The {@link Locale} to use for creating the message instances.
	 */
	@Inject
	public void setLocale(@Optional @Named(TranslationService.LOCALE) Locale locale) {
		this.locale = locale == null ? Locale.getDefault() : locale;

		// update listener
		updateMessages();
	}

	/**
	 * Notify the {@link IRequestor}s of those instances that they need to update their message
	 * class instances.
	 */
	private void updateMessages() {
		for (Map.Entry<Class<?>, Set<IRequestor>> entry : this.listeners.entrySet()) {
			notifyRequestor(entry.getValue());
		}
	}

	/**
	 * Checks if for the specified descriptor class there is already an instance in the local cache.
	 * If not a new instance is created using the local configuration on {@link Locale},
	 * {@link BundleLocalization} and given descriptor class.
	 *
	 * @param descriptorsClass
	 *            The class for which an instance is requested.
	 * @return The instance of the requested message class
	 */
	private Object getMessageInstance(Class<?> descriptorsClass) {
		return this.factoryService.getMessageInstance(this.locale, descriptorsClass, this.provider);
	}

	/**
	 * Remember the {@link IRequestor} that requested an instance of the given descriptor class.
	 * This is needed to be able to inform all {@link IRequestor} if the {@link Locale} changes at
	 * runtime.
	 *
	 * @param descriptorsClass
	 *            The class for which an instance was requested.
	 * @param requestor
	 *            The {@link IRequestor} that requested the instance.
	 */
	private void addListener(Class<?> descriptorsClass, IRequestor requestor) {
		Set<IRequestor> registered = this.listeners.computeIfAbsent(descriptorsClass, dc -> ConcurrentHashMap.newKeySet());
		registered.add(requestor);
	}

	/**
	 * Notify all given {@link IRequestor} about changes for their injected values. This way the
	 * dynamic injection is performed.
	 *
	 * @param requestors
	 *            The {@link IRequestor} to inform about the instance changes.
	 */
	private void notifyRequestor(Collection<IRequestor> requestors) {
		if (requestors != null) {
			for (Iterator<IRequestor> it = requestors.iterator(); it.hasNext();) {
				IRequestor requestor = it.next();
				if (!requestor.isValid()) {
					it.remove();
					continue;
				}
				requestor.resolveArguments(false);
				requestor.execute();
			}
		}
	}

	private Class<?> getDesiredClass(Type desiredType) {
		if (desiredType instanceof Class<?>)
			return (Class<?>) desiredType;
		if (desiredType instanceof ParameterizedType) {
			Type rawType = ((ParameterizedType) desiredType).getRawType();
			if (rawType instanceof Class<?>)
				return (Class<?>) rawType;
		}
		return null;
	}

	@Override
	public void handleEvent(Event event) {
		for (Iterator<Entry<Class<?>, Set<IRequestor>>> it = this.listeners.entrySet().iterator(); it.hasNext();) {
			Map.Entry<Class<?>, Set<IRequestor>> entry = it.next();
			Set<IRequestor> requestors = entry.getValue();
			if (requestors != null) {
				Predicate<IRequestor> predicate = IRequestor::isValid;
				requestors.removeIf(predicate.negate());
				if (requestors.isEmpty()) {
					it.remove();
				}
			}
		}
	}
}