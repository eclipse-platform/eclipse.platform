/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
package org.eclipse.core.internal.expressions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.expressions.IPropertyTester;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionDelta;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IRegistryChangeEvent;
import org.eclipse.core.runtime.IRegistryChangeListener;
import org.eclipse.core.runtime.Platform;

public class TypeExtensionManager implements IRegistryChangeListener {

	private static final String EXTENSION_NAMESPACE = "org.eclipse.core.expressions"; //$NON-NLS-1$
	private String fExtensionPoint;

	/**
	 * Tells whether this class is in debug mode.
	 *
	 * @since 3.5
	 */
	private static boolean DEBUG= "true".equalsIgnoreCase(Platform.getDebugOption("org.eclipse.core.expressions/debug/TypeExtensionManager")); //$NON-NLS-1$//$NON-NLS-2$

	private static final String TYPE= "type"; //$NON-NLS-1$

	private static final IPropertyTester[] EMPTY_PROPERTY_TESTER_ARRAY= new IPropertyTester[0];

	private static final IPropertyTester NULL_PROPERTY_TESTER= new IPropertyTester() {
		@Override
		public boolean handles(String namespace, String property) {
			return false;
		}
		@Override
		public boolean isInstantiated() {
			return true;
		}
		@Override
		public boolean isDeclaringPluginActive() {
			return true;
		}
		@Override
		public IPropertyTester instantiate() throws CoreException {
			return this;
		}
		@Override
		public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
			return false;
		}
	};

	/*
	 * Map containing all already created type extension object.
	 */
	private Map<Class<?>, TypeExtension> fTypeExtensionMap;

	/*
	 * Table containing mapping of class name to configuration element
	 */
	private Map<String, List<IConfigurationElement>> fConfigurationElementMap;

	/*
	 * A cache to give fast access to the last 1000 method invocations.
	 */
	private PropertyCache fPropertyCache;


	public TypeExtensionManager(String extensionPoint) {
		Assert.isNotNull(extensionPoint);
		fExtensionPoint= extensionPoint;
		Platform.getExtensionRegistry().addRegistryChangeListener(this);
		initializeCaches();
	}

	public Property getProperty(Object receiver, String namespace, String method) throws CoreException  {
		return getProperty(receiver, namespace, method, false);
	}

	public synchronized Property getProperty(Object receiver, String namespace, String method, boolean forcePluginActivation) throws CoreException  {
		long start= 0;
		if (Expressions.TRACING)
			start= System.currentTimeMillis();

		// if we call a static method than the receiver is the class object
		Class<?> clazz= receiver instanceof Class ? (Class<?>)receiver : receiver.getClass();
		Property result= new Property(clazz, namespace, method);
		Property cached= fPropertyCache.get(result);
		if (cached != null) {
			if (cached.isValidCacheEntry(forcePluginActivation)) {
				if (Expressions.TRACING) {
					System.out.println("[Type Extension] - method " + //$NON-NLS-1$
						clazz.getName() + "#" + method + //$NON-NLS-1$
						" found in cache: " +  //$NON-NLS-1$
						(System.currentTimeMillis() - start) + " ms."); //$NON-NLS-1$
				}
				return cached;
			}
			// The type extender isn't loaded in the cached method but can be loaded
			// now. So remove method from cache and do the normal look up so that the
			// implementation class gets loaded.
			fPropertyCache.remove(cached);
		}
		TypeExtension extension= get(clazz);
		IPropertyTester extender= extension.findTypeExtender(this, namespace, method, receiver instanceof Class, forcePluginActivation);
		if (extender == TypeExtension.CONTINUE || extender == null) {
			Throwable t= null;
			if (DEBUG) {
				//XXX: more logging for https://bugs.eclipse.org/bugs/show_bug.cgi?id=239715 :
				t= new Throwable("forcePluginActivation: " + forcePluginActivation + ", receiver: " + receiver).fillInStackTrace(); //$NON-NLS-1$ //$NON-NLS-2$
			}

			throw new CoreException(new ExpressionStatus(
				ExpressionStatus.TYPE_EXTENDER_UNKOWN_METHOD,
				Messages.format(
					ExpressionMessages.TypeExtender_unknownMethod,
					new String[] {namespace + '.' + method, clazz.toString()}),
					t));
		}
		result.setPropertyTester(extender);
		fPropertyCache.put(result);
		if (Expressions.TRACING) {
			System.out.println("[Type Extension] - method " + //$NON-NLS-1$
				clazz.getName() + "#" + method + //$NON-NLS-1$
				" not found in cache: " +  //$NON-NLS-1$
				(System.currentTimeMillis() - start) + " ms."); //$NON-NLS-1$
		}
		return result;
	}

	/*
	 * This method doesn't need to be synchronized since it is called
	 * from withing the getProperty method which is synchronized
	 */
	/* package */ TypeExtension get(Class<?> clazz) {
		TypeExtension result= fTypeExtensionMap.get(clazz);
		if (result == null) {
			result= new TypeExtension(clazz);
			fTypeExtensionMap.put(clazz, result);
		}
		return result;
	}

	/*
	 * This method doesn't need to be synchronized since it is called
	 * from withing the getProperty method which is synchronized
	 */
	/* package */ IPropertyTester[] loadTesters(Class<?> type) {
		if (fConfigurationElementMap == null) {
			fConfigurationElementMap= new HashMap<>();
			IExtensionRegistry registry= Platform.getExtensionRegistry();
			IConfigurationElement[] ces= registry.getConfigurationElementsFor(
					EXTENSION_NAMESPACE, fExtensionPoint);
			for (IConfigurationElement config : ces) {
				String typeAttr= config.getAttribute(TYPE);
				List<IConfigurationElement> typeConfigs= fConfigurationElementMap.get(typeAttr);
				if (typeConfigs == null) {
					typeConfigs= new ArrayList<>();
					fConfigurationElementMap.put(typeAttr, typeConfigs);
				}
				typeConfigs.add(config);
			}
		}
		String typeName= type.getName();
		List<IConfigurationElement> typeConfigs= fConfigurationElementMap.get(typeName);
		if (typeConfigs == null)
			return EMPTY_PROPERTY_TESTER_ARRAY;
		else {
			IPropertyTester[] result= new IPropertyTester[typeConfigs.size()];
			for (int i= 0; i < result.length; i++) {
				IConfigurationElement config= typeConfigs.get(i);
				try {
					result[i]= new PropertyTesterDescriptor(config);
				} catch (CoreException e) {
					ILog.of(TypeExtensionManager.class).log(e.getStatus());
					result[i]= NULL_PROPERTY_TESTER;
				}
			}
			fConfigurationElementMap.remove(typeName);
			return result;
		}
	}

	@Override
	public void registryChanged(IRegistryChangeEvent event) {
		IExtensionDelta[] deltas = event.getExtensionDeltas(EXTENSION_NAMESPACE, fExtensionPoint);
		if (deltas.length > 0) {
			initializeCaches();
		}
	}

	private synchronized void initializeCaches() {
		fTypeExtensionMap= new HashMap<>();
		fConfigurationElementMap= null;
		fPropertyCache= new PropertyCache(1000);
	}
}
