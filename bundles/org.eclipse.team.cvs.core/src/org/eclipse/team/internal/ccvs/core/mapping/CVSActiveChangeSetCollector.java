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
package org.eclipse.team.internal.ccvs.core.mapping;

import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.internal.core.subscribers.ActiveChangeSet;
import org.eclipse.team.internal.core.subscribers.SubscriberChangeSetManager;

/**
 * The CVS Active change set manager
 */
public class CVSActiveChangeSetCollector extends SubscriberChangeSetManager {

	public CVSActiveChangeSetCollector(Subscriber subscriber) {
		super(subscriber);
	}
	
	@Override
	protected ActiveChangeSet doCreateSet(String name) {
		return new CVSActiveChangeSet(this, name);
	}

	
}
