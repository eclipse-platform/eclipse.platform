/*******************************************************************************
 * Copyright (c) 2023, 2023 Hannes Wellmann and others.
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
 *******************************************************************************/

package org.eclipse.e4.core.internal.di;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

	public static record AnnotationProxy(List<Class<? extends Annotation>> classes) {
		public AnnotationProxy {
			classes = List.copyOf(classes);
		}

		public boolean isPresent(AnnotatedElement element) {
			for (Class<? extends Annotation> annotationClass : classes) {
				if (element.isAnnotationPresent(annotationClass)) {
					return true;
				}
			}
			return false;
		}
	}

	static final AnnotationProxy INJECT = createProxyForClasses(jakarta.inject.Inject.class,
			() -> javax.inject.Inject.class);
	static final AnnotationProxy SINGLETON = createProxyForClasses(jakarta.inject.Singleton.class,
			() -> javax.inject.Singleton.class);
	static final AnnotationProxy QUALIFIER = createProxyForClasses(jakarta.inject.Qualifier.class,
			() -> javax.inject.Qualifier.class);

	static final AnnotationProxy PRE_DESTROY = createProxyForClasses(jakarta.annotation.PreDestroy.class,
			() -> javax.annotation.PreDestroy.class);
	public static final AnnotationProxy POST_CONSTRUCT = createProxyForClasses(jakarta.annotation.PostConstruct.class,
			() -> javax.annotation.PostConstruct.class);

	static final AnnotationProxy OPTIONAL = createProxyForClasses(org.eclipse.e4.core.di.annotations.Optional.class,
			null);

	private static AnnotationProxy createProxyForClasses(Class<? extends Annotation> jakartaAnnotationClass,
			Supplier<Class<? extends Annotation>> javaxAnnotationClass) {
		List<Class<?>> classes = getAvailableClasses(jakartaAnnotationClass, javaxAnnotationClass);
		@SuppressWarnings({ "rawtypes", "unchecked" })
		List<Class<? extends Annotation>> annotationClasses = (List) classes;
		return new AnnotationProxy(annotationClasses);
	}

	private static final List<Class<?>> PROVIDER_TYPES = getAvailableClasses(jakarta.inject.Provider.class,
			() -> javax.inject.Provider.class);

	static boolean isProvider(Type type) {
		for (Class<?> clazz : PROVIDER_TYPES) {
			if (clazz.equals(type)) {
				return true;
			}
		}
		return false;
	}

	@FunctionalInterface
	private interface ProviderFactory {
		Object create(IObjectDescriptor descriptor, IInjector injector, PrimaryObjectSupplier provider);
	}

	private static final ProviderFactory PROVIDER_FACTORY;
	static {
		ProviderFactory factory;
		try {
			/**
			 * This subclass solely exists for the purpose to not require the presence of
			 * the javax.inject.Provider interface in the runtime when the base-class is
			 * loaded. This can be deleted when support for javax is removed form the
			 * E4-injector.
			 */
			class JavaxCompatibilityProviderImpl<T> extends ProviderImpl<T> implements javax.inject.Provider<T> {
				public JavaxCompatibilityProviderImpl(IObjectDescriptor descriptor, IInjector injector,
						PrimaryObjectSupplier provider) {
					super(descriptor, injector, provider);
				}
			}
			factory = JavaxCompatibilityProviderImpl::new;
			// Attempt to load the class early in order to enforce an early class-loading
			// and to be able to handle the NoClassDefFoundError below in case
			// javax-Provider is not available in the runtime:
			factory.create(null, null, null);
		} catch (NoClassDefFoundError e) {
			factory = ProviderImpl::new;
		}
		PROVIDER_FACTORY = factory;
	}

	public static Object getProvider(IObjectDescriptor descriptor, IInjector injector, PrimaryObjectSupplier provider) {
		return PROVIDER_FACTORY.create(descriptor, injector, provider);
	}

	public static String getQualifierValue(IObjectDescriptor descriptor) {
		var annotations = NAMED_ANNOTATION2VALUE_GETTER.entrySet();
		for (Entry<Class<? extends Annotation>, Function<Annotation, String>> entry : annotations) {
			Class<? extends Annotation> annotationClass = entry.getKey();
			if (descriptor.hasQualifier(annotationClass)) {
				Annotation namedAnnotation = descriptor.getQualifier(annotationClass);
				return entry.getValue().apply(namedAnnotation);
			}
		}
		return null;
	}

	private static final Map<Class<? extends Annotation>, Function<Annotation, String>> NAMED_ANNOTATION2VALUE_GETTER;

	static {
		Map<Class<? extends Annotation>, Function<Annotation, String>> annotation2valueGetter = new HashMap<>();
		annotation2valueGetter.put(jakarta.inject.Named.class, a -> ((jakarta.inject.Named) a).value());
		loadJavaxClass(
				() -> annotation2valueGetter.put(javax.inject.Named.class, a -> ((javax.inject.Named) a).value()));
		NAMED_ANNOTATION2VALUE_GETTER = Map.copyOf(annotation2valueGetter);
	}

	private static List<Class<?>> getAvailableClasses(Class<?> jakartaClass, Supplier<? extends Class<?>> javaxClass) {
		List<Class<?>> classes = new ArrayList<>();
		classes.add(jakartaClass);
		if (javaxClass != null) {
			loadJavaxClass(() -> classes.add(javaxClass.get()));
		}
		return classes;
	}

	private static boolean javaxWarningPrinted = false;

	private static void loadJavaxClass(Runnable run) {
		try {
			run.run();
			if (!javaxWarningPrinted) {
				if (Boolean.parseBoolean(System.getProperty("eclipse.e4.inject.javax.warning", "true"))) { //$NON-NLS-1$//$NON-NLS-2$
					@SuppressWarnings("nls")
					String message = """
							WARNING: Annotation classes from the 'javax.inject' or 'javax.annotation' package found.
							It is recommended to migrate to the corresponding replacements in the jakarta namespace.
							The Eclipse E4 Platform will remove support for those javax-annotations in a future release.
							To suppress this warning set the VM property: -Declipse.e4.inject.javax.warning=false
							""";
					System.err.println(message);
				}
				javaxWarningPrinted = true;
			}
		} catch (NoClassDefFoundError e) {
			// Ignore exception: javax-annotation seems to be unavailable in the runtime
		}
	}

}
