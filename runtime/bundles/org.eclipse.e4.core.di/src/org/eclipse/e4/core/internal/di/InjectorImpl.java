/*******************************************************************************
 * Copyright (c) 2009, 2023 IBM Corporation and others.
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
 *     Lars Vogel <Lars.Vogel@gmail.com> - Bug 426754
 *     Daniel Kruegler <daniel.kruegler@gmail.com> - Bug 493697
 *******************************************************************************/
package org.eclipse.e4.core.internal.di;

import java.lang.annotation.Annotation;
import java.lang.ref.WeakReference;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.e4.core.di.IBinding;
import org.eclipse.e4.core.di.IInjector;
import org.eclipse.e4.core.di.InjectionException;
import org.eclipse.e4.core.di.annotations.Creatable;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.suppliers.ExtendedObjectSupplier;
import org.eclipse.e4.core.di.suppliers.IObjectDescriptor;
import org.eclipse.e4.core.di.suppliers.IRequestor;
import org.eclipse.e4.core.di.suppliers.PrimaryObjectSupplier;
import org.eclipse.e4.core.internal.di.AnnotationLookup.AnnotationProxy;
import org.eclipse.e4.core.internal.di.osgi.LogHelper;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * Reflection-based dependency injector.
 */
public class InjectorImpl implements IInjector {

	private static final boolean shouldDebug = Boolean.getBoolean("org.eclipse.e4.core.di.debug"); //$NON-NLS-1$

	private static final String JAVA_OBJECT = "java.lang.Object"; //$NON-NLS-1$

	private final static Boolean DEFAULT_BOOLEAN = Boolean.FALSE;
	private final static Integer DEFAULT_INTEGER = Integer.valueOf(0);
	private final static Character DEFAULT_CHAR = Character.valueOf((char) 0);
	private final static Float DEFAULT_FLOAT = Float.valueOf(0.0f);
	private final static Double DEFAULT_DOUBLE = Double.valueOf(0.0d);
	private final static Long DEFAULT_LONG = Long.valueOf(0L);
	private final static Short DEFAULT_SHORT = Short.valueOf((short) 0);
	private final static Byte DEFAULT_BYTE = Byte.valueOf((byte) 0);

	private final Map<PrimaryObjectSupplier, Set<IdentityWeakReference<?>>> injectedObjects = new WeakHashMap<>();
	private final Set<IdentityWeakReference<Class<?>>> injectedClasses = new LinkedHashSet<>();
	private final Map<Class<?>, Object> singletonCache = new HashMap<>();
	private final Map<Class<?>, Set<Binding>> bindings = new HashMap<>();
	private final Map<AnnotationProxy, Map<AnnotatedElement, Boolean>> annotationsPresent = Collections
			.synchronizedMap(new HashMap<>());

	// Performance improvement:
	private final Map<Class<?>, Method[]> methodsCache = Collections.synchronizedMap(new WeakHashMap<>());
	private final Map<Class<?>, Field[]> fieldsCache = Collections.synchronizedMap(new WeakHashMap<>());
	private final Map<Class<?>, Constructor<?>[]> constructorsCache = Collections.synchronizedMap(new WeakHashMap<>());
	private final Map<Class<?>, Map<Method, Boolean>> isOverriddenCache = Collections
			.synchronizedMap(new WeakHashMap<>());

	private final Set<Class<?>> classesBeingCreated = new HashSet<>(5);

	private volatile PrimaryObjectSupplier defaultSupplier;

	@Override
	public void inject(Object object, PrimaryObjectSupplier objectSupplier) {
		try {
			internalInject(object, objectSupplier, null);
		} catch (NoClassDefFoundError | NoSuchMethodError e) {
			throw new InjectionException(e);
		}
	}

	@Override
	public void inject(Object object, PrimaryObjectSupplier objectSupplier, PrimaryObjectSupplier staticSupplier)
			throws InjectionException {
		try {
			internalInject(object, objectSupplier, staticSupplier);
		} catch (NoClassDefFoundError | NoSuchMethodError e) {
			throw new InjectionException(e);
		}
	}

	private void internalInject(Object object, PrimaryObjectSupplier objectSupplier,
			PrimaryObjectSupplier tempSupplier) {
		// Two stages: first, go and collect {requestor, descriptor[] }
		ArrayList<Requestor<?>> requestors = new ArrayList<>();
		processClassHierarchy(object, objectSupplier, tempSupplier, true /* track */, true /* normal order */, requestors);

		// if we are not establishing any links to the injected object (nothing to inject,
		// or constructor only), create a pseudo-link to track supplier's disposal
		boolean haveLink = false;
		for (Requestor<?> requestor : requestors) {
			if (requestor.shouldTrack())
				haveLink = true;
		}
		if (!haveLink)
			requestors.add(new ClassRequestor(object.getClass(), this, objectSupplier, tempSupplier, object, true));

		// Then ask suppliers to fill actual values {requestor, descriptor[], actualvalues[] }
		resolveRequestorArgs(requestors, objectSupplier, tempSupplier, false, true, true);

		// Call requestors in order
		for (Requestor<?> requestor : requestors) {
			if (requestor.isResolved())
				requestor.execute();
		}
		rememberInjectedObject(object, objectSupplier);

		// We call @jakarta.annotation.PostConstruct after injection. This means that is
		// is called as a part of both #make() and #inject().
		processAnnotated(AnnotationLookup.POST_CONSTRUCT, object, object.getClass(), objectSupplier, tempSupplier,
				new ArrayList<>(5));

		// remove references to the temporary suppliers
		for (Requestor<?> requestor : requestors) {
			requestor.clearTempSupplier();
		}
	}

	private void rememberInjectedObject(Object object, PrimaryObjectSupplier objectSupplier) {
		synchronized (injectedObjects) {
			injectedObjects.computeIfAbsent(objectSupplier,
					k -> new LinkedHashSet<>()).add(new IdentityWeakReference<>(object));
		}
	}

	private boolean forgetInjectedObject(Object object, PrimaryObjectSupplier objectSupplier) {
		synchronized (injectedObjects) {
			Set<IdentityWeakReference<?>> set = injectedObjects.get(objectSupplier);
			if (set != null) {
				return set.remove(new IdentityWeakReference<>(object));
			}
			return false;
		}
	}

	private Set<IdentityWeakReference<?>> forgetSupplier(PrimaryObjectSupplier objectSupplier) {
		synchronized (injectedObjects) {
			return injectedObjects.remove(objectSupplier);
		}
	}

	private Set<IdentityWeakReference<?>> getSupplierObjects(PrimaryObjectSupplier objectSupplier) {
		synchronized (injectedObjects) {
			return injectedObjects.get(objectSupplier);
		}
	}

	@Override
	public void uninject(Object object, PrimaryObjectSupplier objectSupplier) {
		try {
			if (!forgetInjectedObject(object, objectSupplier))
				return; // not injected at this time
			processAnnotated(AnnotationLookup.PRE_DESTROY, object, object.getClass(), objectSupplier, null,
					new ArrayList<>(5));

			List<Requestor<?>> requestors = new ArrayList<>();
			processClassHierarchy(object, objectSupplier, null, true /* track */, false /* inverse order */, requestors);

			for (Requestor<?> requestor : requestors) {
				// Ask suppliers to fill actual values {requestor, descriptor[], actualvalues[] }
				Object[] actualArgs = resolveArgs(requestor, null, null, true, false, false);
				int unresolved = unresolved(actualArgs);
				if (unresolved == -1) {
					requestor.setResolvedArgs(actualArgs);
					requestor.execute();
				} else if (requestor.isOptional())
					requestor.setResolvedArgs(null);
				else if (shouldDebug) {
					StringBuilder tmp = new StringBuilder();
					tmp.append("Uninjecting object \""); //$NON-NLS-1$
					tmp.append(object.toString());
					tmp.append("\": dependency on \""); //$NON-NLS-1$
					tmp.append(requestor.getDependentObjects()[unresolved].toString());
					tmp.append("\" is not optional."); //$NON-NLS-1$
					LogHelper.logError(tmp.toString(), null);
				}
			}
		} catch (NoClassDefFoundError | NoSuchMethodError e) {
			throw new InjectionException(e);
		}
	}

	@Override
	public Object invoke(Object object, Class<? extends Annotation> qualifier, PrimaryObjectSupplier objectSupplier) {
		Object result = invokeUsingClass(object, object.getClass(), qualifier, IInjector.NOT_A_VALUE, objectSupplier,
				null, true, /* initial */ true, /* track */ false);
		if (result == IInjector.NOT_A_VALUE) {
			if (object != null && qualifier != null) {
				throw new InjectionException("Unable to find matching method to invoke. Searching for the annotation \"" + qualifier + "\" on an instance of \"" + object.getClass().getSimpleName() + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			throw new InjectionException("Unable to find matching method to invoke. One of the arguments was null."); //$NON-NLS-1$
		}
		return result;
	}

	@Override
	public Object invoke(Object object, Class<? extends Annotation> qualifier, Object defaultValue, PrimaryObjectSupplier objectSupplier) {
		return invokeUsingClass(object, object.getClass(), qualifier, defaultValue, objectSupplier, null, false,
				/* initial */ true, /* track */false);
	}

	@Override
	public Object invoke(Object object, Class<? extends Annotation> qualifier, Object defaultValue, PrimaryObjectSupplier objectSupplier, PrimaryObjectSupplier localSupplier) {
		return invokeUsingClass(object, object.getClass(), qualifier, defaultValue, objectSupplier, localSupplier,
				false, /* initial */ true, /* track */false);
	}

	/**
	 * Call the annotated method on an object, injecting the parameters from the
	 * suppliers.
	 * <p>
	 * If no matching method is found on the class, the defaultValue will be
	 * returned.
	 * </p>
	 * <p>
	 * NOTE: There is no way to turn off tracking of such a method. It continues
	 * to be re-injected on change until the object suppliers are disposed.
	 * </p>
	 * <p>
	 * This method is not intended to be used by clients and not public API and
	 * therefore might change in future releases.
	 * </p>
	 *
	 * @param object
	 *            the object on which the method should be called
	 * @param qualifier
	 *            the annotation tagging method to be called
	 * @param defaultValue
	 *            a value to be returned if the method cannot be called, might
	 *            be <code>null</code>
	 * @param objectSupplier
	 *            primary object supplier
	 * @param localSupplier
	 *            primary object supplier, values override objectSupplier
	 * @param initial
	 *            <code>true</code> true if this is the initial request from the
	 *            requestor
	 * @param track
	 *            <code>true</code> if the object suppliers should notify
	 *            requestor of changes to the returned objects;
	 *            <code>false</code> otherwise
	 * @return the return value of the method call, might be <code>null</code>
	 * @throws InjectionException
	 *             if an exception occurred while performing this operation
	 */
	public Object invoke(Object object, Class<? extends Annotation> qualifier, Object defaultValue,
			PrimaryObjectSupplier objectSupplier, PrimaryObjectSupplier localSupplier, boolean initial, boolean track) {
		return invokeUsingClass(object, object.getClass(), qualifier, defaultValue, objectSupplier, null, false,
				initial, track);
	}

	private Object invokeUsingClass(Object userObject, Class<?> currentClass, Class<? extends Annotation> qualifier,
			Object defaultValue, PrimaryObjectSupplier objectSupplier, PrimaryObjectSupplier tempSupplier,
			boolean throwUnresolved, boolean initial, boolean track) {
		Method[] methods = getDeclaredMethods(currentClass);
		for (Method method : methods) {
			if (method.getAnnotation(qualifier) == null)
				continue;
			MethodRequestor requestor = new MethodRequestor(method, this, objectSupplier, tempSupplier, userObject,
					track);

			Object[] actualArgs = resolveArgs(requestor, objectSupplier, tempSupplier, false, initial, track);
			int unresolved = unresolved(actualArgs);
			if (unresolved != -1) {
				if (throwUnresolved)
					reportUnresolvedArgument(requestor, unresolved);
				continue;
			}
			requestor.setResolvedArgs(actualArgs);
			return requestor.execute();
		}
		Class<?> superClass = currentClass.getSuperclass();
		if (superClass == null)
			return defaultValue;

		return invokeUsingClass(userObject, superClass, qualifier, defaultValue, objectSupplier, tempSupplier,
				throwUnresolved, initial, track);
	}

	@Override
	public <T> T make(Class<T> clazz, PrimaryObjectSupplier objectSupplier) {
		Class<?> implementationClass = getImplementationClass(clazz);
		return clazz.cast(internalMake(implementationClass, objectSupplier, null));
	}

	private Class<?> getImplementationClass(Class<?> clazz) {
		IObjectDescriptor descriptor = new ObjectDescriptor(clazz, null);
		Binding binding = findBinding(descriptor);
		if (binding == null)
			return getDesiredClass(descriptor.getDesiredType());
		return binding.getImplementationClass();
	}

	@Override
	public <T> T make(Class<T> clazz, PrimaryObjectSupplier objectSupplier, PrimaryObjectSupplier staticSupplier) {
		Class<?> implementationClass = getImplementationClass(clazz);
		return clazz.cast(internalMake(implementationClass, objectSupplier, staticSupplier));
	}

	public Object makeFromProvider(IObjectDescriptor descriptor, PrimaryObjectSupplier objectSupplier) {
		Binding binding = findBinding(descriptor);
		Class<?> implementationClass;
		if (binding == null)
			implementationClass = getProviderType(descriptor.getDesiredType());
		else
			implementationClass = binding.getImplementationClass();
		if (objectSupplier != null) {
			IObjectDescriptor actualClass = new ObjectDescriptor(implementationClass, null);
			Object[] actualArgs = new Object[] {IInjector.NOT_A_VALUE};
			objectSupplier.get(new IObjectDescriptor[] {actualClass}, actualArgs, null, false, true, false);
			if (actualArgs[0] != IInjector.NOT_A_VALUE)
				return actualArgs[0];
		}
		return internalMake(implementationClass, objectSupplier, null);
	}

	private Object internalMake(Class<?> clazz, PrimaryObjectSupplier objectSupplier, PrimaryObjectSupplier tempSupplier) {
		if (shouldDebug && classesBeingCreated.contains(clazz))
			LogHelper.logWarning("Possible recursive reference trying to create class \"" + clazz.getName() + "\".", null); //$NON-NLS-1$ //$NON-NLS-2$
		try {
			if (shouldDebug)
				classesBeingCreated.add(clazz);

			boolean isSingleton = isAnnotationPresent(clazz, AnnotationLookup.SINGLETON);
			if (isSingleton) {
				synchronized (singletonCache) {
					if (singletonCache.containsKey(clazz))
						return singletonCache.get(clazz);
				}
			}

			Constructor<?>[] constructors = getDeclaredConstructors(clazz);
			// Sort the constructors by descending number of constructor arguments
			ArrayList<Constructor<?>> sortedConstructors = new ArrayList<>(constructors.length);
			sortedConstructors.addAll(Arrays.asList(constructors));
			sortedConstructors.sort(Comparator.comparing(c -> c.getParameterTypes().length, Comparator.reverseOrder()));

			for (Constructor<?> constructor : sortedConstructors) {
				// skip private and protected constructors; allow public and package visibility
				int modifiers = constructor.getModifiers();
				if (((modifiers & Modifier.PRIVATE) != 0) || ((modifiers & Modifier.PROTECTED) != 0))
					continue;

				// unless this is the default constructor, it has to be tagged
				if (!isAnnotationPresent(constructor, AnnotationLookup.INJECT)
						&& constructor.getParameterTypes().length != 0) {
					continue;
				}
				ConstructorRequestor requestor = new ConstructorRequestor(constructor, this, objectSupplier, tempSupplier);
				Object[] actualArgs = resolveArgs(requestor, objectSupplier, tempSupplier, false, true, false);
				if (unresolved(actualArgs) != -1)
					continue;
				requestor.setResolvedArgs(actualArgs);

				Object newInstance = requestor.execute();
				if (newInstance != null) {
					internalInject(newInstance, objectSupplier, tempSupplier);
					if (isSingleton) {
						synchronized (singletonCache) { // TBD this is not quite right, synch the method
							singletonCache.put(clazz, newInstance);
						}
					}
					return newInstance;
				}
			}
			throw new InjectionException("Could not find satisfiable constructor in " + clazz.getName()); //$NON-NLS-1$
		} catch (NoClassDefFoundError | NoSuchMethodError e) {
			throw new InjectionException(e);
		} finally {
			if (shouldDebug)
				classesBeingCreated.remove(clazz);
		}
	}

	public void resolveArguments(IRequestor requestor, boolean initial) {
		Requestor<?> internalRequestor = ((Requestor<?>) requestor);
		Object[] actualArgs = resolveArgs(internalRequestor, internalRequestor.getPrimarySupplier(), internalRequestor.getTempSupplier(), false, initial, internalRequestor.shouldTrack());
		int unresolved = unresolved(actualArgs);
		if (unresolved == -1)
			internalRequestor.setResolvedArgs(actualArgs);
		else if (internalRequestor.isOptional())
			internalRequestor.setResolvedArgs(null);
		else {
			String msg = resolutionError(internalRequestor, unresolved);
			LogHelper.logError(msg, null);
		}
	}

	public void disposed(PrimaryObjectSupplier objectSupplier) {
		Set<IdentityWeakReference<?>> references = getSupplierObjects(objectSupplier);
		if (references == null)
			return;
		Object[] objects = new Object[references.size()];
		int count = 0;
		for (WeakReference<?> ref : references) {
			Object object = ref.get();
			if (object != null) {
				objects[count] = object;
				count++;
			}
		}
		for (int i = 0; i < count; i++) {
			Object object = objects[i];
			if (!forgetInjectedObject(object, objectSupplier))
				continue; // not injected at this time
			processAnnotated(AnnotationLookup.PRE_DESTROY, object, object.getClass(), objectSupplier, null,
					new ArrayList<>(5));
		}
		forgetSupplier(objectSupplier);
	}

	private void resolveRequestorArgs(ArrayList<Requestor<?>> requestors, PrimaryObjectSupplier objectSupplier, PrimaryObjectSupplier tempSupplier, boolean uninject, boolean initial, boolean track) {
		for (Requestor<?> requestor : requestors) {
			Object[] actualArgs = resolveArgs(requestor, objectSupplier, tempSupplier, uninject, initial, track);
			int unresolved = unresolved(actualArgs);
			if (unresolved == -1) {
				requestor.setResolvedArgs(actualArgs);
				continue;
			}

			if (requestor.isOptional())
				requestor.setResolvedArgs(null);
			else
				reportUnresolvedArgument(requestor, unresolved);
		}
	}

	private void reportUnresolvedArgument(Requestor<?> requestor, int argIndex) {
		String msg = resolutionError(requestor, argIndex);
		if (shouldDebug) {
			LogHelper.logError(msg, null);
		}
		throw new InjectionException(msg);
	}

	private String resolutionError(Requestor<?> requestor, int argIndex) {
		StringBuilder tmp = new StringBuilder();
		tmp.append("Unable to process \""); //$NON-NLS-1$
		tmp.append(requestor.toString());
		tmp.append("\": no actual value was found for the argument \""); //$NON-NLS-1$
		tmp.append(requestor.getDependentObjects()[argIndex].toString());
		tmp.append("\"."); //$NON-NLS-1$
		return tmp.toString();
	}

	private Object[] resolveArgs(Requestor<?> requestor, PrimaryObjectSupplier objectSupplier, PrimaryObjectSupplier tempSupplier, boolean uninject, boolean initial, boolean track) {
		/* Special indicator for ExtendedObjectSuppliers not having a value */
		final Object EOS_NOT_A_VALUE = new Object();

		IObjectDescriptor[] descriptors = requestor.getDependentObjects();

		// Resolution order changed in 1.4 as we now check extended suppliers first (bug 398728)

		// 0) initial fill - all values are unresolved
		Object[] actualArgs = new Object[descriptors.length];
		Arrays.fill(actualArgs, NOT_A_VALUE);

		// 1) check if we have a Provider<T>
		for (int i = 0; i < actualArgs.length; i++) {
			Class<?> providerClass = getProviderType(descriptors[i].getDesiredType());
			if (providerClass == null) {
				continue;
			}
			actualArgs[i] = AnnotationLookup.getProvider(descriptors[i], this, objectSupplier);
		}

		// 2) try extended suppliers
		for (int i = 0; i < actualArgs.length; i++) {
			if (actualArgs[i] != NOT_A_VALUE)
				continue; // already resolved
			ExtendedObjectSupplier extendedSupplier = findExtendedSupplier(descriptors[i]);
			if (extendedSupplier == null)
				continue;
			actualArgs[i] = extendedSupplier.get(descriptors[i], requestor, requestor.shouldTrack() && track, requestor.shouldGroupUpdates());
			if (actualArgs[i] == NOT_A_VALUE) {
				// Use special marker to prevent these annotated arguments from being resolved using temporary and primary suppliers
				actualArgs[i] = EOS_NOT_A_VALUE;
			}
		}

		// 3) use the temporary supplier
		if (tempSupplier != null)
			tempSupplier.get(descriptors, actualArgs, requestor, initial, false /* no tracking */, requestor.shouldGroupUpdates());

		// 4) use the primary supplier
		if (objectSupplier != null)
			objectSupplier.get(descriptors, actualArgs, requestor, initial, requestor.shouldTrack() && track, requestor.shouldGroupUpdates());

		// 5) try the bindings
		for (int i = 0; i < actualArgs.length; i++) {
			if (actualArgs[i] != NOT_A_VALUE)
				continue; // already resolved
			Binding binding = findBinding(descriptors[i]);
			if (binding != null)
				actualArgs[i] = internalMake(binding.getImplementationClass(), objectSupplier, tempSupplier);
		}

		// 5) create simple classes (implied bindings) - unless we uninject or optional
		if (!uninject && !requestor.isOptional()) {
			for (int i = 0; i < actualArgs.length; i++) {
				if (actualArgs[i] != NOT_A_VALUE)
					continue; // already resolved
				if (descriptors[i].hasQualifier(Optional.class))
					continue;
				try {
					Class<?> desiredClass = getDesiredClass(descriptors[i].getDesiredType());
					Creatable creatableAnnotation = desiredClass.getAnnotation(Creatable.class);
					if (creatableAnnotation == null)
						continue;
					actualArgs[i] = internalMake(getDesiredClass(descriptors[i].getDesiredType()), objectSupplier, tempSupplier);
				} catch (InjectionException e) {
					e.printStackTrace();
				}
			}
		}

		// 6) post process
		for (int i = 0; i < descriptors.length; i++) {
			// check that values are of a correct type
			if (actualArgs[i] != null && actualArgs[i] != IInjector.NOT_A_VALUE && actualArgs[i] != EOS_NOT_A_VALUE) {
				Class<?> descriptorsClass = getDesiredClass(descriptors[i].getDesiredType());
				if (descriptorsClass.isPrimitive()) { // support type autoboxing
					if (descriptorsClass.equals(boolean.class))
						descriptorsClass = Boolean.class;
					else if (descriptorsClass.equals(int.class))
						descriptorsClass = Integer.class;
					else if (descriptorsClass.equals(char.class))
						descriptorsClass = Character.class;
					else if (descriptorsClass.equals(float.class))
						descriptorsClass = Float.class;
					else if (descriptorsClass.equals(double.class))
						descriptorsClass = Double.class;
					else if (descriptorsClass.equals(long.class))
						descriptorsClass = Long.class;
					else if (descriptorsClass.equals(short.class))
						descriptorsClass = Short.class;
					else if (descriptorsClass.equals(byte.class))
						descriptorsClass = Byte.class;
				}
				if (!descriptorsClass.isAssignableFrom(actualArgs[i].getClass()))
					actualArgs[i] = IInjector.NOT_A_VALUE;
			}
			if (actualArgs[i] == IInjector.NOT_A_VALUE || actualArgs[i] == EOS_NOT_A_VALUE) { // still unresolved?
				if (descriptors[i].hasQualifier(Optional.class)) { // uninject or optional - fill defaults
					Class<?> descriptorsClass = getDesiredClass(descriptors[i].getDesiredType());
					if (descriptorsClass.isPrimitive()) {
						if (descriptorsClass.equals(boolean.class))
							actualArgs[i] = DEFAULT_BOOLEAN;
						else if (descriptorsClass.equals(int.class))
							actualArgs[i] = DEFAULT_INTEGER;
						else if (descriptorsClass.equals(char.class))
							actualArgs[i] = DEFAULT_CHAR;
						else if (descriptorsClass.equals(float.class))
							actualArgs[i] = DEFAULT_FLOAT;
						else if (descriptorsClass.equals(double.class))
							actualArgs[i] = DEFAULT_DOUBLE;
						else if (descriptorsClass.equals(long.class))
							actualArgs[i] = DEFAULT_LONG;
						else if (descriptorsClass.equals(short.class))
							actualArgs[i] = DEFAULT_SHORT;
						else if (descriptorsClass.equals(byte.class))
							actualArgs[i] = DEFAULT_BYTE;
					} else
						actualArgs[i] = null;
				} else if (actualArgs[i] == EOS_NOT_A_VALUE) {
					// Wasn't @Optional, so replace with NOT_A_VALUE
					actualArgs[i] = IInjector.NOT_A_VALUE;
				}
			}
		}
		return actualArgs;
	}

	private ExtendedObjectSupplier findExtendedSupplier(IObjectDescriptor descriptor) {
		Annotation[] qualifiers = descriptor.getQualifiers();
		if (qualifiers == null)
			return null;
		for (Annotation qualifier : qualifiers) {
			Class<?> type = qualifier.annotationType();
			String key = ((Class<?>) type).getName();

			ExtendedObjectSupplier supplier;
			try {
				// use qualified name to refer to a class that might be missing
				supplier = org.eclipse.e4.core.internal.di.osgi.ProviderHelper.findProvider(key, defaultSupplier);
			} catch (NoClassDefFoundError e) {
				return null; // OSGi framework not present
			}
			if (supplier != null)
				return supplier;
		}
		return null;
	}

	private int unresolved(Object[] actualArgs) {
		for (int i = 0; i < actualArgs.length; i++) {
			if (actualArgs[i] == IInjector.NOT_A_VALUE)
				return i;
		}
		return -1;
	}

	private void processClassHierarchy(Object userObject, PrimaryObjectSupplier objectSupplier, PrimaryObjectSupplier tempSupplier, boolean track, boolean normalOrder, List<Requestor<?>> requestors) {
		processClass(userObject, objectSupplier, tempSupplier, (userObject == null) ? null : userObject.getClass(), new ArrayList<>(5), track, normalOrder, requestors);
	}

	/**
	 * Make the processor visit all declared members on the given class and all superclasses
	 */
	private void processClass(Object userObject, PrimaryObjectSupplier objectSupplier, PrimaryObjectSupplier tempSupplier, Class<?> objectsClass, ArrayList<Class<?>> classHierarchy, boolean track, boolean normalOrder, List<Requestor<?>> requestors) {
		// order: superclass, fields, methods
		if (objectsClass != null) {
			Class<?> superClass = objectsClass.getSuperclass();
			if (superClass != null && !superClass.getName().equals(JAVA_OBJECT)) {
				classHierarchy.add(objectsClass);
				processClass(userObject, objectSupplier, tempSupplier, superClass, classHierarchy, track, normalOrder, requestors);
				classHierarchy.remove(objectsClass);
			}
		}
		boolean injectedStaticFields;
		boolean injectedStaticMethods;
		if (normalOrder) {
			injectedStaticFields = processFields(userObject, objectSupplier, tempSupplier, objectsClass, track, requestors);
			injectedStaticMethods = processMethods(userObject, objectSupplier, tempSupplier, objectsClass, classHierarchy, track, requestors);
		} else {
			injectedStaticMethods = processMethods(userObject, objectSupplier, tempSupplier, objectsClass, classHierarchy, track, requestors);
			injectedStaticFields = processFields(userObject, objectSupplier, tempSupplier, objectsClass, track, requestors);
		}
		if (injectedStaticFields || injectedStaticMethods)
			rememberInjectedStatic(objectsClass);
	}

	private boolean hasInjectedStatic(Class<?> objectsClass) {
		synchronized (injectedClasses) {
			return injectedClasses.contains(new IdentityWeakReference<>(objectsClass));
		}
	}

	private void rememberInjectedStatic(Class<?> objectsClass) {
		synchronized (injectedClasses) {
			injectedClasses.add(new IdentityWeakReference<>(objectsClass));
		}
	}

	/**
	 * Make the processor visit all declared fields on the given class.
	 */
	private boolean processFields(Object userObject, PrimaryObjectSupplier objectSupplier, PrimaryObjectSupplier tempSupplier, Class<?> objectsClass, boolean track, List<Requestor<?>> requestors) {
		boolean injectedStatic = false;
		Field[] fields = getDeclaredFields(objectsClass);
		for (Field field : fields) {
			if (Modifier.isStatic(field.getModifiers())) {
				if (hasInjectedStatic(objectsClass))
					continue;
				injectedStatic = true;
			}
			if (!isAnnotationPresent(field, AnnotationLookup.INJECT)) {
				continue;
			}
			requestors.add(new FieldRequestor(field, this, objectSupplier, tempSupplier, userObject, track));
		}
		return injectedStatic;
	}

	/**
	 * Make the processor visit all declared methods on the given class.
	 */
	private boolean processMethods(final Object userObject, PrimaryObjectSupplier objectSupplier,
			PrimaryObjectSupplier tempSupplier, Class<?> objectsClass, ArrayList<Class<?>> classHierarchy,
			boolean track, List<Requestor<?>> requestors) {
		boolean injectedStatic = false;
		Method[] methods = getDeclaredMethods(objectsClass);
		for (Method method : methods) {

			Boolean isOverridden = null;
			Class<?> originalClass = userObject.getClass();
			Map<Method, Boolean> methodMap = isOverriddenCache.get(originalClass);
			if (methodMap != null) {
				isOverridden = methodMap.get(method);
			}
			if (isOverridden == null) {
				isOverridden = isOverridden(method, classHierarchy);
				if (methodMap == null) {
					methodMap = Collections.synchronizedMap(new WeakHashMap<>());
					isOverriddenCache.put(originalClass, methodMap);
				}
				methodMap.put(method, isOverridden);
			}

			if (isOverridden) {
				continue; // process in the subclass
			}
			if (Modifier.isStatic(method.getModifiers())) {
				if (hasInjectedStatic(objectsClass)) {
					continue;
				}
				injectedStatic = true;
			}
			if (!isAnnotationPresent(method, AnnotationLookup.INJECT)) {
				continue;
			}
			requestors.add(new MethodRequestor(method, this, objectSupplier, tempSupplier, userObject, track));
		}
		return injectedStatic;
	}

	/**
	 * Checks if a given method is overridden with an injectable method.
	 */
	private boolean isOverridden(Method method, List<Class<?>> classHierarchy) {
		int modifiers = method.getModifiers();
		if (Modifier.isPrivate(modifiers))
			return false;
		if (Modifier.isStatic(modifiers))
			return false;
		// method is not private if we reached this line, check not(public OR protected)
		boolean isDefault = !(Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers));

		String methodName = method.getName();
		Class<?>[] methodParams = method.getParameterTypes();
		int methodParamsLength = method.getParameterTypes().length;
		for (Class<?> subClass : classHierarchy) {
			Method[] methods = getDeclaredMethods(subClass);
			Method matchingMethod = null;
			for (Method candidate : methods) {
				if (!methodName.equals(candidate.getName()))
					continue;
				Class<?>[] candidateParams = candidate.getParameterTypes();
				if (candidateParams.length != methodParamsLength)
					continue;
				boolean paramsMatch = true;
				for (int i = 0; i < methodParamsLength; i++) {
					if (candidateParams[i].equals(methodParams[i])) // strictly speaking, need to add erasures
						continue;
					paramsMatch = false;
				}
				if (!paramsMatch)
					continue;
				matchingMethod = candidate;
				break;
			}
			if (matchingMethod == null)
				continue;

			if (isDefault) { // must be in the same package to override
				Package originalPackage = method.getDeclaringClass().getPackage();
				Package overridePackage = subClass.getPackage();

				if (originalPackage == null && overridePackage == null)
					return true;
				if (originalPackage == null || overridePackage == null)
					return false;
				if (originalPackage.equals(overridePackage))
					return true;
			} else
				return true;
		}
		return false;
	}

	private Constructor<?>[] getDeclaredConstructors(Class<?> c) {
		Constructor<?>[] constructors = constructorsCache.get(c);
		if (constructors == null) {
			constructors = c.getDeclaredConstructors();
			// Sort the constructors by descending number of constructor
			// arguments
			Arrays.sort(constructors, (c1, c2) -> {
				int l1 = c1.getParameterTypes().length;
				int l2 = c2.getParameterTypes().length;
				return l2 - l1;
			});
			constructorsCache.put(c, constructors);
		}
		return constructors;
	}

	private Method[] getDeclaredMethods(Class<?> c) {
		Method[] methods = methodsCache.get(c);
		if (methods == null) {
			// filter out all bridge methods
			methods = Stream.of(c.getDeclaredMethods())
					.filter(m -> !m.isBridge()).toArray(s -> new Method[s]);
			methodsCache.put(c, methods);
		}
		return methods;
	}

	private Field[] getDeclaredFields(Class<?> c) {
		Field[] fields = fieldsCache.get(c);
		if (fields == null) {
			fields = c.getDeclaredFields();
			fieldsCache.put(c, fields);
		}
		return fields;
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

	/**
	 * Returns null if not a provider
	 */
	private Class<?> getProviderType(Type type) {
		if (!(type instanceof ParameterizedType))
			return null;
		Type rawType = ((ParameterizedType) type).getRawType();
		if (!AnnotationLookup.isProvider(rawType)) {
			return null;
		}
		Type[] actualTypes = ((ParameterizedType) type).getActualTypeArguments();
		if (actualTypes.length != 1)
			return null;
		if (!(actualTypes[0] instanceof Class<?>))
			return null;
		return (Class<?>) actualTypes[0];
	}

	@Override
	public IBinding addBinding(Class<?> clazz) {
		return addBinding(new Binding(clazz, this));
	}

	@Override
	public IBinding addBinding(IBinding binding) {
		Binding internalBinding = (Binding) binding;
		Class<?> clazz = internalBinding.getDescribedClass();
		synchronized (bindings) {
			if (bindings.containsKey(clazz)) {
				Set<Binding> collection = bindings.get(clazz);
				String desiredQualifierName = internalBinding.getQualifierName();
				for (Iterator<Binding> i = collection.iterator(); i.hasNext();) {
					Binding collectionBinding = i.next();
					if (eq(collectionBinding.getQualifierName(), desiredQualifierName)) {
						i.remove();
						break;
					}
				}
				collection.add(internalBinding);
			} else {
				Set<Binding> collection = new HashSet<>(1);
				collection.add(internalBinding);
				bindings.put(clazz, collection);
			}
		}
		return binding;
	}

	private Binding findBinding(IObjectDescriptor descriptor) {
		Class<?> desiredClass = getProviderType(descriptor.getDesiredType());
		if (desiredClass == null)
			desiredClass = getDesiredClass(descriptor.getDesiredType());
		synchronized (bindings) {
			Set<Binding> collection = bindings.get(desiredClass);
			if (collection == null) {
				return null;
			}
			String desiredQualifierName = AnnotationLookup.getQualifierValue(descriptor);
			if (desiredQualifierName == null) {
				Annotation[] annotations = descriptor.getQualifiers();
				if (annotations != null) {
					for (Annotation annotation : annotations) {
						desiredQualifierName = annotation.annotationType().getName();
						break;
					}
				}
			}

			for (Binding collectionBinding : collection) {
				if (eq(collectionBinding.getQualifierName(), desiredQualifierName))
					return collectionBinding;
			}
			desiredQualifierName = desiredClass.getName();
			for (Binding collectionBinding : collection) {
				Class<?> bindingClass = collectionBinding.getDescribedClass();
				if (bindingClass == null)
					continue;
				String simpleClassName = bindingClass.getName();
				if (eq(simpleClassName, desiredQualifierName))
					return collectionBinding;
			}
		}
		return null;
	}

	/**
	 * Are two, possibly null, string equal?
	 */
	private boolean eq(String str1, String str2) {
		if (str1 == null && str2 == null)
			return true;
		if (str1 == null || str2 == null)
			return false;
		return str1.equals(str2);
	}

	private void processAnnotated(AnnotationProxy annotation, Object userObject, Class<?> objectClass,
			PrimaryObjectSupplier objectSupplier, PrimaryObjectSupplier tempSupplier,
			List<Class<?>> classHierarchy) {
		Class<?> superClass = objectClass.getSuperclass();
		if (superClass != null && !superClass.getName().equals(JAVA_OBJECT)) {
			classHierarchy.add(objectClass);
			processAnnotated(annotation, userObject, superClass, objectSupplier, tempSupplier, classHierarchy);
			classHierarchy.remove(objectClass);
		}
		Method[] methods = getDeclaredMethods(objectClass);
		for (Method method : methods) {
			if (!isAnnotationPresent(method, annotation)) {
				if (shouldDebug) {
					for (Annotation a : method.getAnnotations()) {
						if (annotation.classes().stream().map(Class::getName)
								.anyMatch(a.annotationType().getName()::equals)) {
							StringBuilder tmp = new StringBuilder();
							tmp.append("Possbible annotation mismatch: method \""); //$NON-NLS-1$
							tmp.append(method.toString());
							tmp.append("\" annotated with \""); //$NON-NLS-1$
							tmp.append(describeClass(a.annotationType()));
							tmp.append("\" but was looking for \""); //$NON-NLS-1$
							tmp.append(annotation.classes().stream().map(InjectorImpl::describeClass)
									.collect(Collectors.joining(System.lineSeparator() + " or "))); //$NON-NLS-1$
							tmp.append("\""); //$NON-NLS-1$
							LogHelper.logWarning(tmp.toString(), null);
						}
					}
				}
				continue;
			}
			if (isOverridden(method, classHierarchy))
				continue;

			MethodRequestor requestor = new MethodRequestor(method, this, objectSupplier, tempSupplier, userObject, false);
			Object[] actualArgs = resolveArgs(requestor, objectSupplier, tempSupplier, false, false, false);
			int unresolved = unresolved(actualArgs);
			if (unresolved != -1) {
				if (isAnnotationPresent(method, AnnotationLookup.OPTIONAL)) {
					continue;
				}
				reportUnresolvedArgument(requestor, unresolved);
			}
			requestor.setResolvedArgs(actualArgs);
			requestor.execute();
		}
	}

	/** Provide a human-meaningful description of the provided class */
	private static String describeClass(Class<?> cl) {
		Bundle b = FrameworkUtil.getBundle(cl);
		if (b != null) {
			return b.getSymbolicName() + ":" + b.getVersion() + ":" + cl.getName(); //$NON-NLS-1$ //$NON-NLS-2$
		}
		CodeSource clazzCS = cl.getProtectionDomain().getCodeSource();
		if (clazzCS != null) {
			return clazzCS.getLocation() + ">" + cl.getName(); //$NON-NLS-1$
		}
		if (cl.getClassLoader() == null) {
			return cl.getName() + " [via bootstrap classloader]"; //$NON-NLS-1$
		}
		return cl.getName();
	}

	@Override
	public void setDefaultSupplier(PrimaryObjectSupplier objectSupplier) {
		defaultSupplier = objectSupplier;
	}

	private boolean isAnnotationPresent(AnnotatedElement annotatedElement, AnnotationProxy lookUp) {
		Map<AnnotatedElement, Boolean> cache = annotationsPresent.computeIfAbsent(lookUp,
				a -> Collections.synchronizedMap(new WeakHashMap<>()));
		return cache.computeIfAbsent(annotatedElement, lookUp::isPresent);
	}

	/**
	 * The IdentityWeakReference extends the {@link WeakReference} with the
	 * difference that it can be compared to another IdentityWeakReference. The
	 * compare is done on the Identity of the Referenced Object, this allows us to
	 * use this element in a Set and have unique objects in it.
	 */
	private static class IdentityWeakReference<T> extends WeakReference<T> {

		private final int hashCode;

		IdentityWeakReference(T referent) {
			super(referent);
			hashCode = System.identityHashCode(referent);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof IdentityWeakReference other) {
				return other.get() == get();
			}
			return false;
		}

		@Override
		public int hashCode() {
			return hashCode;
		}

	}
}
