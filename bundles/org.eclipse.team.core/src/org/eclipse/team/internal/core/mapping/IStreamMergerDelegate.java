/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.core.mapping;

import org.eclipse.core.resources.IStorage;
import org.eclipse.team.core.mapping.DelegatingStorageMerger;
import org.eclipse.team.core.mapping.IStorageMerger;

/**
 * Interface that allows the {@link DelegatingStorageMerger} to
 * delegate to IStreamMergers. We need an interface so the UI can
 * provide the implementation.
 */
public interface IStreamMergerDelegate {
	/**
	 * Find a storage merger for the given target.
	 * A storage merger will only be returned if
	 * there is a stream merger that matches the
	 * targets content type or extension.
	 *
	 * @param target the input storage
	 * @return a storage merger for the given target
	 */
	IStorageMerger findMerger(IStorage target);
}