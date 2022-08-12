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
package org.eclipse.team.internal.ui.mapping;

import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.team.core.mapping.ISynchronizationScope;
import org.eclipse.team.core.mapping.ISynchronizationScopeParticipant;
import org.eclipse.team.core.mapping.ISynchronizationScopeParticipantFactory;

public class ResourceModelScopeParticipantFactory implements
		ISynchronizationScopeParticipantFactory {

	private final ModelProvider provider;

	public ResourceModelScopeParticipantFactory(ModelProvider provider) {
		this.provider = provider;
	}

	@Override
	public ISynchronizationScopeParticipant createParticipant(
			ModelProvider provider, ISynchronizationScope scope) {
		return new ResourceModelScopeParticipant(this.provider, scope);
	}

}
