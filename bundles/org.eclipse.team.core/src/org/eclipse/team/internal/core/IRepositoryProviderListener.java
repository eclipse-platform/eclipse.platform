/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.core;

import org.eclipse.core.resources.IProject;
import org.eclipse.team.core.RepositoryProvider;

/**
 * Interface for listening to repository provider changes
 */
public interface IRepositoryProviderListener {
	void providerMapped(RepositoryProvider provider);
	void providerUnmapped(IProject project);
}
