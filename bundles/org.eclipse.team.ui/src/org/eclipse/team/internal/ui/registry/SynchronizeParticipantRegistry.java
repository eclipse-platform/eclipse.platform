/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.registry;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.team.internal.ui.TeamUIPlugin;

public class SynchronizeParticipantRegistry extends RegistryReader {

	public static final String PT_SYNCPARTICIPANTS = "synchronizeParticipants"; //$NON-NLS-1$
	private static final String TAG_SYNCPARTICIPANT = "participant"; //$NON-NLS-1$
	private Map<String, SynchronizeParticipantDescriptor> participants = new HashMap<>();

	public SynchronizeParticipantRegistry() {
		super();
	}

	@Override
	protected boolean readElement(IConfigurationElement element) {
		if (element.getName().equals(TAG_SYNCPARTICIPANT)) {
			String descText = getDescription(element);
			SynchronizeParticipantDescriptor desc;
			try {
				desc = new SynchronizeParticipantDescriptor(element, descText);
				participants.put(desc.getId(), desc);
			} catch (CoreException e) {
				TeamUIPlugin.log(e);
			}
			return true;
		}
		return false;
	}

	public SynchronizeParticipantDescriptor find(String id) {
		return participants.get(id);
	}
}
