/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
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
package org.eclipse.team.tests.ccvs.core.mappings;

import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.team.core.mapping.IResourceMappingMerger;
import org.eclipse.team.core.mapping.ResourceMappingMerger;

public class TestModelProvider extends ModelProvider {

	public Object getAdapter(Class adapter) {
		if (adapter == IResourceMappingMerger.class) {
			return new ResourceMappingMerger() {			
				protected ModelProvider getModelProvider() {
					return TestModelProvider.this;
				}	
			};
		}
		return super.getAdapter(adapter);
	}
}
