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
package org.eclipse.team.internal.ccvs.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.ui.mapping.*;
import org.eclipse.team.ui.synchronize.SubscriberTeamStateProvider;

public class CVSTeamStateProvider extends SubscriberTeamStateProvider {

	public CVSTeamStateProvider(Subscriber subscriber) {
		super(subscriber);
	}

	public ITeamStateDescription getStateDescription(Object element, final int requestedStateMask, String[] properties, IProgressMonitor monitor) throws CoreException {
		if (properties != null && properties.length == 0) {
			return new CVSTeamStateDescription(getSynchronizationState(element, requestedStateMask, monitor));
		}
		CVSDecoration d = CVSLightweightDecorator.decorate(element, new SynchronizationStateTester() {
			public int getState(Object element, int stateMask, IProgressMonitor monitor) throws CoreException {
				if (requestedStateMask != USE_DECORATED_STATE_MASK) {
					stateMask = requestedStateMask & stateMask;
				}
				return getSynchronizationState(element, requestedStateMask & stateMask, monitor);
			}
		});
		return d.asTeamStateDescription(properties);
	}
}
