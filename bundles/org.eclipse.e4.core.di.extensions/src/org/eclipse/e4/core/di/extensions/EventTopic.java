/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
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
 *     Tom Schind<tom.schindl@bestsolution.at> - bugfix 330756
 *******************************************************************************/
package org.eclipse.e4.core.di.extensions;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.inject.Qualifier;

/**
 * This annotation can be applied to arguments and fields that want to receive notifications on the
 * specified event topic.
 * <p>
 * Normally EventTopic annotations will be marked as optional. Those annotations establish a link
 * rather then provide a value at the time of injection.
 * </p>
 * <p>
 * Example usage:
 * </p>
 * <pre>
 *   public class Car {
 *     &#064;Inject
 *     void handle(&#064;Optional &#064;EventTopic("org/eclipse/e4/some/event/topic") Payload payload);
 *     ...
 *   }
 * </pre>
 * <p>
 * The value stored under the event's "org.eclipse.e4.data" property is injected unless
 * the class of the injected variable is org.osgi.service.event.Event. In that case the whole event
 * object is injected.
 * </p>
 *
 * @since 0.16
 */
@Qualifier
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface EventTopic {
	String value() default ""; // event id
}
