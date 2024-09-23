/*******************************************************************************
 * Copyright (c) 2023, 2026 Hannes Wellmann and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Hannes Wellmann - initial API and implementation
 *     Hannes Wellmann - support multiple versions of one annotation class
 *******************************************************************************/

package org.eclipse.e4.core.internal.di;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Function;
import org.eclipse.e4.core.di.suppliers.IObjectDescriptor;
import org.eclipse.e4.core.di.suppliers.PrimaryObjectSupplier;

/**
 * A utility class to ease the look-up of jakarta/javax.inject and
 * jakarta/javax.annotation annotations and types as mutual replacements, while
 * being able to handle the absence of javax-classes in the runtime.
 *
 * If support for javax-annotations is removed, this class can be simplified to
 * only handle jakarta-annotations, then all method can be inlined and this
 * class eventually deleted, together with the entire test-project
 * org.eclipse.e4.core.tests.
 */
public class AnnotationLookup {
	private AnnotationLookup() {
	}

	public static record AnnotationProxy(Set<String> classes) {

		public boolean isPresent(AnnotatedElement element) {
			for (Annotation annotation : element.getAnnotations()) {
				if (isOfAnyTypeIn(annotation.annotationType(), classes())) {
					return true;
				}
			}
			return false;
		}
	}

	static final AnnotationProxy INJECT = createProxyForClasses( //
			"jakarta.inject.Inject", "javax.inject.Inject"); //$NON-NLS-1$ //$NON-NLS-2$
	static final AnnotationProxy SINGLETON = createProxyForClasses( //
			"jakarta.inject.Singleton", "javax.inject.Singleton"); //$NON-NLS-1$//$NON-NLS-2$
	static final AnnotationProxy QUALIFIER = createProxyForClasses( //
			"jakarta.inject.Qualifier", "javax.inject.Qualifier"); //$NON-NLS-1$//$NON-NLS-2$

	static final AnnotationProxy PRE_DESTROY = createProxyForClasses( //
			"jakarta.annotation.PreDestroy", "javax.annotation.PreDestroy"); //$NON-NLS-1$//$NON-NLS-2$
	public static final AnnotationProxy POST_CONSTRUCT = createProxyForClasses( //
			"jakarta.annotation.PostConstruct", "javax.annotation.PostConstruct"); //$NON-NLS-1$//$NON-NLS-2$

	static final AnnotationProxy OPTIONAL = createProxyForClasses("org.eclipse.e4.core.di.annotations.Optional"); //$NON-NLS-1$

	private static AnnotationProxy createProxyForClasses(String... annotationClasses) {
		return new AnnotationProxy(Set.of(annotationClasses));
	}

	private static final Set<String> PROVIDER_TYPES = Set.of( //
			"jakarta.inject.Provider", "javax.inject.Provider"); //$NON-NLS-1$//$NON-NLS-2$

	static boolean isProvider(Type type) {
		return PROVIDER_TYPES.contains(type.getTypeName());
	}

	public static Object getProvider(IObjectDescriptor descriptor, InjectorImpl injector,
			PrimaryObjectSupplier provider) {
		Class<?> providerClass;
		if ((descriptor.getDesiredType() instanceof ParameterizedType parameterizedType
				&& parameterizedType.getRawType() instanceof Class<?> desiredClass)) {
			providerClass = desiredClass;
		} else {
			// Caller must ensure that the providerClass can be extracted
			throw new IllegalArgumentException("Failed to obtain provider class from " + descriptor); //$NON-NLS-1$
		}
		return Proxy.newProxyInstance(providerClass.getClassLoader(), new Class[] { providerClass },
				(proxy, method, args) -> switch (method.getName()) {
				case "get" -> injector.makeFromProvider(descriptor, provider); //$NON-NLS-1$
				case "hashCode" -> System.identityHashCode(proxy); //$NON-NLS-1$
				case "equals" -> proxy == args[0]; //$NON-NLS-1$
				case "toString" -> "Proxy for " + descriptor; //$NON-NLS-1$ //$NON-NLS-2$
				default -> throw new UnsupportedOperationException("Unsupported method: " + method + "()"); //$NON-NLS-1$ //$NON-NLS-2$
				});
	}

	public static String getQualifierValue(IObjectDescriptor descriptor) {
		Annotation[] qualifiers = descriptor.getQualifiers();
		if (qualifiers != null) {
			for (Annotation annotation : qualifiers) {
				Class<? extends Annotation> annotationType = annotation.annotationType();
				if (isOfAnyTypeIn(annotationType, NAMED_ANNOTATIONS)) {
					Function<Annotation, String> getter = NAMED_VALUE_GETTER.computeIfAbsent(annotationType,
							AnnotationLookup::createValueGetter);
					return getter.apply(annotation);
				}
			}
		}
		return null;
	}

	private static final Set<String> NAMED_ANNOTATIONS = Set.of( //
			"jakarta.inject.Named", "javax.inject.Named"); //$NON-NLS-1$//$NON-NLS-2$

	private static final Map<Class<? extends Annotation>, Function<Annotation, String>> NAMED_VALUE_GETTER = Collections
			.synchronizedMap(new WeakHashMap<>());

	private static Function<Annotation, String> createValueGetter(Class<? extends Annotation> type) {
		MethodHandle handle;
		try {
			Lookup lookup = MethodHandles.publicLookup();
			handle = lookup.findVirtual(type, "value", MethodType.methodType(String.class)); //$NON-NLS-1$
		} catch (NoSuchMethodException | IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
		// Expect an invocation object typed as general Annotation (the specific
		// sub-class is unknown at compile-time) to allow more performant invoke exact
		MethodHandle methodHandle = handle.asType(handle.type().changeParameterType(0, Annotation.class));
		return a -> {
			try {
				return (String) methodHandle.invokeExact(a);
			} catch (Throwable e) {
				throw new IllegalStateException(e);
			}
		};
	}

	private static boolean isOfAnyTypeIn(Class<? extends Annotation> annotationType, Set<String> annotations) {
		return annotations.contains(annotationType.getName());
	}

}
