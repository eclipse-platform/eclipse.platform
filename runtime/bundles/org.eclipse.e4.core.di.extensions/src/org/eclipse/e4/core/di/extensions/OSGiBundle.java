/*******************************************************************************
 * Copyright (c) 2013 Markus Alexander Kuppe and others.
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
 ******************************************************************************/
package org.eclipse.e4.core.di.extensions;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * A method or field of type {@link BundleContext} and annotated with
 * {@link OSGiBundle} will be injected with the BundleContext from the bundle
 * containing the class if the bundle's state is {@link Bundle#ACTIVE}.
 * <p>
 * If the method or field type is of {@link Bundle}, the bundle containing the
 * class will be injected even for bundles in the {@link Bundle#RESOLVED} state.
 * </p>
 * <p>
 * The {@link Bundle} injected will be the {@link Bundle} that supplied the
 * class of the object instance. For example, in:
 * </p>
 *
 * <pre>
 * class A {
 * 	&#64;Inject
 * 	&#64;OSGiBundle
 * 	BundleContext context;
 * }
 *
 * class B extends A {
 * }
 * </pre>
 *
 * <code>ContextInjectionFactory.make(B.class)</code> will inject the
 * {@link BundleContext} for the bundle that supplies class B, not class A.
 *
 * @since 0.16
 */
@jakarta.inject.Qualifier
@Documented
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface OSGiBundle {
	// Nop
}
