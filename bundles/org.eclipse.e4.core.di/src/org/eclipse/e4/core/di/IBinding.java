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
package org.eclipse.e4.core.di;

/**
 * Describes binding between object description and its implementation to be
 * used by the dependency injection.
 *
 * @see IInjector#addBinding(Class)
 * @see IInjector#addBinding(IBinding)
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 * @since 1.7
 */
public interface IBinding {

	/**
	 * Creates a binding for the specified name.
	 * @param name name for this binding
	 * @return binding for the specified name
	 */
	IBinding named(String name);

	/**
	 * Creates a binding for the specified class
	 * @param clazz class for this binding
	 * @return binding for the specified class
	 */
	IBinding implementedBy(Class<?> clazz);
}
