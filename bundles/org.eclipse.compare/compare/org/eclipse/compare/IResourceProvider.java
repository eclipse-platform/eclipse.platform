/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
