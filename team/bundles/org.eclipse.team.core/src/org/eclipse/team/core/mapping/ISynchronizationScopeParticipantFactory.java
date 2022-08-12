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
package org.eclipse.team.core.mapping;

import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IAdapterManager;

/**
 * Factory interface for creating a participant for use with an
 * {@link ISynchronizationScopeManager}. This factory should be
 * assocated with a {@link ModelProvider} using the {@link IAdaptable}
 * mechanism.
 * <p>
 * This interface may be implemented by clients.
 *
 * @see ModelProvider
 * @see IAdaptable
 * @see IAdapterManager
 * @see ISynchronizationScopeManager
 * @see ISynchronizationScopeParticipant
 * @since 3.2
 */
public interface ISynchronizationScopeParticipantFactory {

	/**
	 * Create a participant in the scope management process for the given model provider.
	 * @param provider the model provider
	 * @param scope the scope
	 * @return a participant in the scope management process
	 */
	ISynchronizationScopeParticipant createParticipant(ModelProvider provider, ISynchronizationScope scope);

}
