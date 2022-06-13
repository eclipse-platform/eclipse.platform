/*******************************************************************************
 * Copyright (c) 2010, 2013 IBM Corporation and others.
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
package org.eclipse.e4.core.di.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * This annotation indicates that injection field or method supports
 * batching of updates.
 *
 * Unlike regular updates that are processed right away, grouped updates
 * are performed at certain points, for instance, they could be processed
 * when the system gets in an idle state.
 *
 * This is a performance improvement that can be used to eliminate extra updates
 * on the injected objects that don't have to track the changes exactly, but
 * rather only need to periodically catch up to the latest state of the injected variables.
 *
 * Note that the order of batched events is unspecified and is likely different
 * from the original event order. As such, it is not recommended to use grouped updates on
 * the methods that modify other context values.
 * @since 1.3
 */
@Documented
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface GroupUpdates {
	// intentionally left empty
}
