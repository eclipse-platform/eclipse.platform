/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.registry;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.*;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.ui.mapping.ITeamContentProviderDescriptor;

public class TeamDecoratorManager {

	public static final String PT_TEAM_DECORATORS = "teamDecorators"; //$NON-NLS-1$

	private static TeamDecoratorManager instance;

	Map<String, TeamDecoratorDescription> descriptors;

	public static TeamDecoratorManager getInstance() {
		if (instance == null)
			instance = new TeamDecoratorManager();
		return instance;
	}

	public ITeamContentProviderDescriptor[] getDescriptors() {
		lazyInitialize();
		return descriptors.values().toArray(new ITeamContentProviderDescriptor[descriptors.size()]);
	}

	public TeamDecoratorDescription getDecoratorDescription(String providerId) {
		lazyInitialize();
		return descriptors.get(providerId);
	}

	protected void lazyInitialize() {
		if (descriptors != null)
			return;
		IExtensionPoint point = Platform.getExtensionRegistry().getExtensionPoint(TeamUIPlugin.ID, PT_TEAM_DECORATORS);
		IExtension[] extensions = point.getExtensions();
		descriptors = new HashMap<>(extensions.length * 2 + 1);
		for (int i = 0, imax = extensions.length; i < imax; i++) {
			TeamDecoratorDescription desc = null;
			try {
				desc = new TeamDecoratorDescription(extensions[i]);
			} catch (CoreException e) {
				TeamUIPlugin.log(e);
			}
			if (desc != null)
				descriptors.put(desc.getRepositoryId(), desc);
		}
	}
}
