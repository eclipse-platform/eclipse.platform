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
 ******************************************************************************/
package org.eclipse.e4.core.services.adapter;

/**
 * An adapter can adapt an object to the specified type, allowing clients to request domain-specific
 * behavior for an object.
 */
public abstract class Adapter {

	public abstract <T> T adapt(Object element, Class<T> adapterType);

}
