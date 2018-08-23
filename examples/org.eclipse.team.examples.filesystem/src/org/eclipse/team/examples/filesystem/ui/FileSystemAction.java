/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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
package org.eclipse.team.examples.filesystem.ui;

import java.util.*;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.examples.filesystem.FileSystemPlugin;
import org.eclipse.team.internal.ui.actions.TeamAction;

/**
 * An abstract class that acts as a super class for FileSystemProvider actions.
 * It provides some general methods applicable to multiple actions.
 */
public abstract class FileSystemAction extends TeamAction {

	/**
	 * @see org.eclipse.team.internal.ui.actions.TeamAction#isEnabled()
	 */
	public boolean isEnabled() {
		return getSelectedMappings().length > 0;
	}

	/**
	 * Split the resources into sets associated with their project/provider
	 */
	protected Map getRepositoryProviderMapping() {
		HashMap result = new HashMap();
		IResource[] resources = getSelectedResources();
		for (int i = 0; i < resources.length; i++) {
			RepositoryProvider provider = RepositoryProvider.getProvider(resources[i].getProject());
			List list = (List) result.get(provider);
			if (list == null) {
				list = new ArrayList();
				result.put(provider, list);
			}
			list.add(resources[i]);
		}
		return result;
	}
	
	/**
	 * Return the selected resource mappings that are associated with the
	 * file system provider.
	 * @return the selected resource mappings that are associated with the
	 * file system provider.
	 */
	protected ResourceMapping[] getSelectedMappings() {
		return getSelectedResourceMappings(FileSystemPlugin.PROVIDER_ID);
	}
	
}
