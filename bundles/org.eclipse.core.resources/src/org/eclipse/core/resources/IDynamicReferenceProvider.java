/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
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
package org.eclipse.core.resources;

import java.util.List;
import org.eclipse.core.runtime.CoreException;

/**
 * Implementations of this interface are capable of determining a set
 * of projects which a given project depends upon. Unless otherwise stated,
 * all arguments and return values are non-null.
 *
 * @since 3.12
 */
public interface IDynamicReferenceProvider {
	/**
	 * Returns the set of projects which the given project depends upon. If the return
	 * value of a previous call to this method ever changes, it will fire an event to
	 * the listeners. This method my be invoked from any thread and may be invoked
	 * in parallel by multiple threads.
	 *
	 * @param buildConfiguration the build configuration being queried.
	 * @return the set of projects which the given projects depends upon.
	 */
	List<IProject> getDependentProjects(IBuildConfiguration buildConfiguration) throws CoreException;
}
