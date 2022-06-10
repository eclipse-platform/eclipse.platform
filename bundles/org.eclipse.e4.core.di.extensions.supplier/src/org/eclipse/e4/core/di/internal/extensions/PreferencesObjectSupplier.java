/*******************************************************************************
 * Copyright (c) 2010, 2017 IBM Corporation and others.
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
 *     Steven Spungin <steven@spungin.tv> - Bug 391061
 *     Lars.Vogel <Lars.Vogel@vogella.com> - Bug 472654
 *     Alex Blewitt <alex.blewitt@gmail.com> - Bug 476364
 *******************************************************************************/
package org.eclipse.e4.core.di.internal.extensions;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.IInjector;
import org.eclipse.e4.core.di.extensions.Preference;
import org.eclipse.e4.core.di.suppliers.ExtendedObjectSupplier;
import org.eclipse.e4.core.di.suppliers.IObjectDescriptor;
import org.eclipse.e4.core.di.suppliers.IRequestor;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

/**
 * Note: we do not support byte arrays in preferences at this time. This class
 * is instantiated and wired by declarative services.
 */
@Component(service = { ExtendedObjectSupplier.class, EventHandler.class }, property = {
		"dependency.injection.annotation=org.eclipse.e4.core.di.extensions.Preference",
		"event.topics=" + IEclipseContext.TOPIC_DISPOSE }, immediate = true)
public class PreferencesObjectSupplier extends ExtendedObjectSupplier implements EventHandler {

	private IPreferencesService preferencesService;

	public IPreferencesService getPreferencesService() {
		return preferencesService;
	}

	@Reference
	public void setPreferencesService(IPreferencesService preferenceService) {
		this.preferencesService = preferenceService;
	}

	static private class PrefInjectionListener implements IPreferenceChangeListener {

		final private IRequestor requestor;
		final private IEclipsePreferences node;
		final private String key;

		public PrefInjectionListener(IEclipsePreferences node, String key, IRequestor requestor) {
			this.node = node;
			this.key = key;
			this.requestor = requestor;
		}

		@Override
		public void preferenceChange(final PreferenceChangeEvent event) {
			if (!requestor.isValid()) {
				node.removePreferenceChangeListener(this);
				return;
			}

			if (!event.getKey().equals(key)) {
				return;
			}

			requestor.resolveArguments(false);
			requestor.execute();
		}

		public IRequestor getRequestor() {
			return requestor;
		}

		public void stopListening() {
			node.removePreferenceChangeListener(this);
		}
	}

	// Hash (nodePath -> Hash (key -> list))
	private Map<String, HashMap<String, List<PrefInjectionListener>>> listenerCache = new HashMap<>();

	@Override
	public Object get(IObjectDescriptor descriptor, IRequestor requestor, boolean track, boolean group) {
		if (descriptor == null)
			return null;
		Class<?> descriptorsClass = getDesiredClass(descriptor.getDesiredType());
		String nodePath = getNodePath(descriptor, requestor.getRequestingObjectClass());
		if (IEclipsePreferences.class.equals(descriptorsClass)) {
			return InstanceScope.INSTANCE.getNode(nodePath);
		}

		String key = getKey(descriptor);
		if (key == null || nodePath == null || key.isEmpty() || nodePath.isEmpty())
			return IInjector.NOT_A_VALUE;
		if (track)
			addListener(nodePath, key, requestor);

		if (descriptorsClass.isPrimitive()) {
			if (descriptorsClass.equals(boolean.class))
				return getPreferencesService().getBoolean(nodePath, key, false, null);
			else if (descriptorsClass.equals(int.class))
				return getPreferencesService().getInt(nodePath, key, 0, null);
			else if (descriptorsClass.equals(double.class))
				return getPreferencesService().getDouble(nodePath, key, 0.0d, null);
			else if (descriptorsClass.equals(float.class))
				return getPreferencesService().getFloat(nodePath, key, 0.0f, null);
			else if (descriptorsClass.equals(long.class))
				return getPreferencesService().getLong(nodePath, key, 0L, null);
		}

		if (String.class.equals(descriptorsClass))
			return getPreferencesService().getString(nodePath, key, null, null);
		else if (Boolean.class.equals(descriptorsClass))
			return getPreferencesService().getBoolean(nodePath, key, false, null);
		else if (Integer.class.equals(descriptorsClass))
			return getPreferencesService().getInt(nodePath, key, 0, null);
		else if (Double.class.equals(descriptorsClass))
			return getPreferencesService().getDouble(nodePath, key, 0.0d, null);
		else if (Float.class.equals(descriptorsClass))
			return getPreferencesService().getFloat(nodePath, key, 0.0f, null);
		else if (Long.class.equals(descriptorsClass))
			return getPreferencesService().getLong(nodePath, key, 0L, null);

		return getPreferencesService().getString(nodePath, key, null, null);
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

	private String getKey(IObjectDescriptor descriptor) {
		if (descriptor == null)
			return null;
		Preference qualifier = descriptor.getQualifier(Preference.class);
		return qualifier.value();
	}

	private String getNodePath(IObjectDescriptor descriptor, Class<?> requestingObject) {
		if (descriptor == null)
			return null;
		Preference qualifier = descriptor.getQualifier(Preference.class);
		String nodePath = qualifier.nodePath();

		if (nodePath == null || nodePath.isEmpty()) {
			if (requestingObject == null)
				return null;
			nodePath = FrameworkUtil.getBundle(requestingObject).getSymbolicName();
		}
		return nodePath;
	}

	private void addListener(String nodePath, String key, final IRequestor requestor) {
		if (requestor == null)
			return;
		synchronized (listenerCache) {
			if (listenerCache.containsKey(nodePath)) {
				HashMap<String, List<PrefInjectionListener>> map = listenerCache.get(nodePath);
				if (map.containsKey(key)) {
					for (PrefInjectionListener listener : map.get(key)) {
						IRequestor previousRequestor = listener.getRequestor();
						if (previousRequestor.equals(requestor))
							return; // avoid adding duplicate listeners
					}
				}
			}
		}
		final IEclipsePreferences node = InstanceScope.INSTANCE.getNode(nodePath);
		PrefInjectionListener listener = new PrefInjectionListener(node, key, requestor);
		node.addPreferenceChangeListener(listener);

		synchronized (listenerCache) {
			HashMap<String, List<PrefInjectionListener>> map = listenerCache.get(nodePath);
			if (map == null) {
				map = new HashMap<>();
				listenerCache.put(nodePath, map);
			}
			List<PrefInjectionListener> listeningRequestors = map.get(key);
			if (listeningRequestors == null) {
				listeningRequestors = new ArrayList<>();
				map.put(key, listeningRequestors);
			}
			listeningRequestors.add(listener);
		}
	}

	@Deactivate
	public void removeAllListeners() {
		synchronized (listenerCache) {
			for (HashMap<String, List<PrefInjectionListener>> map : listenerCache.values()) {
				for (List<PrefInjectionListener> listeners : map.values()) {
					if (listeners == null)
						continue;
					for (PrefInjectionListener listener : listeners) {
						listener.stopListening();
					}
				}
			}
			listenerCache.clear();
		}
	}

	@Override
	public void handleEvent(Event event) {
		synchronized (listenerCache) {
			for (Iterator<Map.Entry<String, HashMap<String, List<PrefInjectionListener>>>> nodesIterator = listenerCache
					.entrySet().iterator(); nodesIterator.hasNext();) {
				HashMap<String, List<PrefInjectionListener>> map = nodesIterator.next().getValue();
				for (Iterator<HashMap.Entry<String, List<PrefInjectionListener>>> valuesIterator = map.entrySet()
						.iterator(); valuesIterator.hasNext();) {
					List<PrefInjectionListener> listeners = valuesIterator.next().getValue();
					if (listeners != null) {
						for (Iterator<PrefInjectionListener> listenerIterator = listeners.iterator(); listenerIterator
								.hasNext();) {
							PrefInjectionListener listener = listenerIterator.next();
							if (!listener.getRequestor().isValid()) {
								listener.stopListening();
								listenerIterator.remove();
							}
						}

						if (listeners.isEmpty()) {
							valuesIterator.remove();
						}
					}
				}

				if (map.isEmpty()) {
					nodesIterator.remove();
				}
			}
		}
	}

}
