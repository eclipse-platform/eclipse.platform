/*******************************************************************************
 * Copyright (c) 2023, 2024 Hannes Wellmann and others.
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
import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import org.eclipse.e4.core.di.IInjector;
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

	public static record AnnotationProxy(List<String> classes) {

		public AnnotationProxy {
			classes = List.copyOf(classes);
		}

		public boolean isPresent(AnnotatedElement element) {
			for (Annotation annotation : element.getAnnotations()) {
				if (classes.contains(annotation.annotationType().getName())) {
					return true;
				}
			}
			return false;
		}
	}

	static final AnnotationProxy INJECT = createProxyForClasses("jakarta.inject.Inject", //$NON-NLS-1$
			"javax.inject.Inject"); //$NON-NLS-1$
	static final AnnotationProxy SINGLETON = createProxyForClasses("jakarta.inject.Singleton", //$NON-NLS-1$
			"javax.inject.Singleton"); //$NON-NLS-1$
	static final AnnotationProxy QUALIFIER = createProxyForClasses("jakarta.inject.Qualifier", //$NON-NLS-1$
			"javax.inject.Qualifier"); //$NON-NLS-1$

	static final AnnotationProxy PRE_DESTROY = createProxyForClasses("jakarta.annotation.PreDestroy", //$NON-NLS-1$
			"javax.annotation.PreDestroy"); //$NON-NLS-1$
	public static final AnnotationProxy POST_CONSTRUCT = createProxyForClasses("jakarta.annotation.PostConstruct", //$NON-NLS-1$
			"javax.annotation.PostConstruct"); //$NON-NLS-1$

	static final AnnotationProxy OPTIONAL = createProxyForClasses("org.eclipse.e4.core.di.annotations.Optional", null); //$NON-NLS-1$

	private static AnnotationProxy createProxyForClasses(String jakartaAnnotationClass,
			String javaxAnnotationClass) {
		return new AnnotationProxy(getAvailableClasses(jakartaAnnotationClass, javaxAnnotationClass));
	}

	private static final Set<String> PROVIDER_TYPES = Set
			.copyOf(getAvailableClasses("jakarta.inject.Provider", "javax.inject.Provider")); //$NON-NLS-1$//$NON-NLS-2$

	static boolean isProvider(Type type) {
		return PROVIDER_TYPES.contains(type.getTypeName());
	}

	private static final Map<Class<?>, MethodHandle> PROVIDER_FACTORYS = new ConcurrentHashMap<>();

	public static Object getProvider(IObjectDescriptor descriptor, IInjector injector, PrimaryObjectSupplier provider) {

		Supplier<Object> genericProvider = () -> ((InjectorImpl) injector).makeFromProvider(descriptor, provider);

		Class<?> providerClass;
		if ((descriptor.getDesiredType() instanceof ParameterizedType parameterizedType
				&& parameterizedType.getRawType() instanceof Class<?> clazz)) {
			providerClass = clazz;
		} else {
			throw new IllegalStateException(); // The caller must ensure the providerClass can be extracted
		}
		// At runtime dynamically create a method-reference that implements the specific
		// providerClass 'foo.bar.Provider':
		// (foo.bar.Provider) genericProvider::get
		MethodHandle factory = PROVIDER_FACTORYS.computeIfAbsent(providerClass, providerType -> {
			try {
				MethodHandles.Lookup lookup = MethodHandles.lookup();
				MethodType suppliedType = MethodType.methodType(Object.class);
				CallSite callSite = LambdaMetafactory.metafactory(lookup, "get", //$NON-NLS-1$
						MethodType.methodType(providerClass, Supplier.class), suppliedType.erase(), //
						lookup.findVirtual(Supplier.class, "get", MethodType.methodType(Object.class)), //$NON-NLS-1$
						suppliedType);
				return callSite.getTarget();
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
		});
		try {
			Object providerImpl = factory.bindTo(genericProvider).invoke();
			return providerClass.cast(providerImpl);
		} catch (Throwable e) {
			throw new IllegalStateException(e);
		}
	}

	public static String getQualifierValue(IObjectDescriptor descriptor) {
		Annotation[] qualifiers = descriptor.getQualifiers();
		if (qualifiers != null) {
			for (Annotation namedAnnotation : qualifiers) {
				Class<? extends Annotation> annotationType = namedAnnotation.annotationType();
				if (NAMED_ANNOTATION_CLASSES.contains(annotationType.getName())) {
					return namedAnnotationValueGetter(annotationType).apply(namedAnnotation);
				}
			}
		}
		return null;
	}

	private static Function<Annotation, String> namedAnnotationValueGetter(
			Class<? extends Annotation> annotationType) {
		return NAMED_ANNOTATION2VALUE_GETTER2.computeIfAbsent(annotationType, type -> {
			try {
				// At runtime dynamically create the method-reference: 'foo.bar.Named::value'
				// where 'foo.bar.Named' is the passed specific annotationType. Invoking the
				// returned Function built from the method reference is much faster than using
				// reflection.
				MethodHandles.Lookup lookup = MethodHandles.lookup();
				MethodType functionApplySignature = MethodType.methodType(String.class, type);
				CallSite site = LambdaMetafactory.metafactory(lookup, "apply", //$NON-NLS-1$
						MethodType.methodType(Function.class), functionApplySignature.erase(),
						lookup.findVirtual(type, "value", MethodType.methodType(String.class)), //$NON-NLS-1$
						functionApplySignature);
				return (Function<Annotation, String>) site.getTarget().invokeExact();
			} catch (Throwable e) {
				throw new IllegalStateException(e);
			}
		});
	}

	private static final Map<Class<? extends Annotation>, Function<Annotation, String>> NAMED_ANNOTATION2VALUE_GETTER2 = new ConcurrentHashMap<>();
	private static final Set<String> NAMED_ANNOTATION_CLASSES = Set.of("jakarta.inject.Named", "javax.inject.Named"); //$NON-NLS-1$//$NON-NLS-2$
	// TODO: warn about the javax-class?

	private static List<String> getAvailableClasses(String jakartaClass, String javaxClass) {
		return javaxClass != null && canLoadJavaxClass(javaxClass) //
				? List.of(jakartaClass, javaxClass)
				: List.of(jakartaClass);
	}

	private static boolean javaxWarningPrinted = false;

	private static boolean canLoadJavaxClass(String className) {
		try {
			if (!getSystemPropertyFlag("eclipse.e4.inject.javax.disabled", false)) { //$NON-NLS-1$
				Class.forName(className); // fails if absent
				if (!javaxWarningPrinted) {
					if (getSystemPropertyFlag("eclipse.e4.inject.javax.warning", true)) { //$NON-NLS-1$
						@SuppressWarnings("nls")
						String message = """
								WARNING: Annotation classes from the 'javax.inject' or 'javax.annotation' package found.
								It is recommended to migrate to the corresponding replacements in the jakarta namespace.
								The Eclipse E4 Platform will remove support for those javax-annotations in a future release.
								To suppress this warning, set the VM property: -Declipse.e4.inject.javax.warning=false
								To disable processing of 'javax' annotations entirely, set the VM property: -Declipse.e4.inject.javax.disabled=true
								""";
						System.err.println(message);
					}
					javaxWarningPrinted = true;
				}
				return true;
			}
		} catch (NoClassDefFoundError | ClassNotFoundException e) {
			// Ignore exception: javax-annotation seems to be unavailable in the runtime
		}
		return false;
	}

	private static boolean getSystemPropertyFlag(String key, boolean defaultValue) {
		String value = System.getProperty(key);
		return value == null // assume "true" if value is empty (to allow -Dkey as shorthand for -Dkey=true)
				? defaultValue
				: (value.isEmpty() || Boolean.parseBoolean(value));
	}

}
