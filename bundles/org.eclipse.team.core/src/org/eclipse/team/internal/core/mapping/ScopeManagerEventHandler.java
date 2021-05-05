/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
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
package org.eclipse.team.internal.core.mapping;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.mapping.ISynchronizationScopeManager;
import org.eclipse.team.core.mapping.provider.SynchronizationScopeManager;
import org.eclipse.team.internal.core.BackgroundEventHandler;
import org.eclipse.team.internal.core.Messages;

public class ScopeManagerEventHandler extends BackgroundEventHandler {

	public static final int REFRESH = 10;
	private Set<ResourceMapping> toRefresh = new HashSet<>();
	private ISynchronizationScopeManager manager;

	static class ResourceMappingEvent extends Event {
		private final ResourceMapping[] mappings;
		public ResourceMappingEvent(ResourceMapping[] mappings) {
			super(REFRESH);
			this.mappings = mappings;
		}
	}

	public ScopeManagerEventHandler(SynchronizationScopeManager manager) {
		super(NLS.bind(Messages.ScopeManagerEventHandler_0, manager.getName()), NLS.bind(Messages.ScopeManagerEventHandler_1, manager.getName()));
		this.manager = manager;
	}

	@Override
	protected boolean doDispatchEvents(IProgressMonitor monitor)
			throws TeamException {
		ResourceMapping[] mappings = toRefresh.toArray(new ResourceMapping[toRefresh.size()]);
		toRefresh.clear();
		if (mappings.length > 0) {
			try {
				manager.refresh(mappings, monitor);
			} catch (CoreException e) {
				throw TeamException.asTeamException(e);
			}
		}
		return mappings.length > 0;
	}

	@Override
	protected void processEvent(Event event, IProgressMonitor monitor)
			throws CoreException {
		if (event instanceof ResourceMappingEvent) {
			ResourceMappingEvent rme = (ResourceMappingEvent) event;
			Collections.addAll(toRefresh, rme.mappings);
		}

	}

	public void refresh(ResourceMapping[] mappings) {
		queueEvent(new ResourceMappingEvent(mappings), false);
	}

	@Override
	protected Object getJobFamiliy() {
		return manager;
	}
}
