/*******************************************************************************
 * Copyright (c) 2013 Tom Schindl <tom.schindl@bestsolution.at> and others.
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
 *******************************************************************************/
package org.eclipse.e4.core.services.nls;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.inject.Inject;
import javax.inject.Qualifier;

/**
 * <p>
 * Annotation used in conjunction with {@link Inject} to mark injected values
 * </p>
 *
 * Sample:
 *
 * <pre>
 * class TestSimpleObject {
 * 	&#064;Inject
 * 	&#064;Translation
 * 	SimpleMessages simpleMessages;
 * }
 * </pre>
 *
 * @since 1.2
 */
@Qualifier
@Documented
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface Translation {

}
