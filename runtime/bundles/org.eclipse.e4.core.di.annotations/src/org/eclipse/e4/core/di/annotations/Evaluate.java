/*******************************************************************************
 * Copyright (c) 2016 vogella GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Simon Scholz <simon.scholz@vogella.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.e4.core.di.annotations;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Use this annotation to tag methods that determine if MUIElements (e.g.,
 * MMenu, MToolbar and it's items etc.) should be visible or not. The tagged
 * method must return Boolean value.
 * <p>
 * This annotation must not be applied to more than one method per class. If
 * several class methods are tagged with this annotation, only one of them will
 * be called.
 * </p>
 *
 * @since 1.6
 */
@Documented
@Target(METHOD)
@Retention(RUNTIME)
public @interface Evaluate {
    // intentionally left empty
}
