/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
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
package org.eclipse.compare;

import org.eclipse.core.resources.IResource;

/**
 * @since 3.1
 */
public interface IResourceProvider {

	/**
	 * Returns the corresponding resource for this object or <code>null</code>.
	 *
	 * @return the corresponding resource or <code>null</code>
	 */
	IResource getResource();
}
