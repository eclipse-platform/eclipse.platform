/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.mapping;

import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.team.core.mapping.ResourceMappingMerger;

/**
 * A default merger that delegates the merge to the merge context.
 * This is registered against ModelProvider so any model providers that
 * don't provide a custom merger will get this one.
 */
public class DefaultResourceMappingMerger extends ResourceMappingMerger {
	
	private final ModelProvider provider;

	public DefaultResourceMappingMerger(ModelProvider provider) {
		this.provider = provider;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.core.mapping.ResourceMappingMerger#getModelProvider()
	 */
	@Override
	protected ModelProvider getModelProvider() {
		return provider;
	}

}
