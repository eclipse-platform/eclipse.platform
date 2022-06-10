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
 *******************************************************************************/
package org.eclipse.e4.core.di.suppliers;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.inject.Qualifier;

/**
 * This interface describes objects created by the dependency injection.
 * <p>
 * From the view point of the injector, objects are described by a type with a
 * set of optional qualifiers.
 * </p>
 *
 * @see Qualifier
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 1.7
 */
public interface IObjectDescriptor {

	/**
	 * The formal type that the objects should be assignable to.
	 * @return the expected object's type
	 */
	Type getDesiredType();

	/**
	 * Use this method to find out if the object descriptor has a qualifier.
	 * @param clazz qualifier
	 * @return <code>true</code> if the object descriptor has the qualifier;
	 * <code>false</code>otherwise
	 */
	boolean hasQualifier(Class<? extends Annotation> clazz);

	/**
	 * Returns an instance of the qualifier, if it is present in this descriptor,
	 * or <code>null</code>.
	 * @param <T> qualifier class
	 * @param clazz the qualifier's class
	 * @return the qualifier instance, if present, or <code>null</code>
	 */
	<T extends Annotation> T getQualifier(Class<T> clazz);

	/**
	 * Returns qualifiers specified for this object descriptor, or <code>null</code>.
	 * @return qualifiers for this descriptor, or <code>null</code> if there are
	 * no qualifiers specified
	 */
	Annotation[] getQualifiers();

}
