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
package org.eclipse.team.internal.ccvs.ui.operations;

import org.eclipse.core.resources.IProject;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.subscribers.SubscriberResourceMappingContext;

public final class SingleProjectSubscriberContext extends SubscriberResourceMappingContext {
	private final IProject project;

	public SingleProjectSubscriberContext(Subscriber subscriber, boolean refresh, IProject project) {
		super(subscriber, refresh);
		this.project = project;
	}

	public IProject[] getProjects() {
		return new IProject[] { project };
	}
}