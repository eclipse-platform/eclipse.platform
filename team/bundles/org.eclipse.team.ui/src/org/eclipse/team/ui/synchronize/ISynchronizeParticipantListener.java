/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
package org.eclipse.team.ui.synchronize;

/**
 * A synchronize participant listener is notified when participants are added or
 * removed from the synchronize manager.
 * <p>
 * Clients may implement this interface.
 * </p>
 * @see ISynchronizeManager
 * @since 3.0
 */
public interface ISynchronizeParticipantListener {
	/**
	 * Notification the given participants have been added to the synchronize
	 * manager.
	 *
	 * @param participants added participants
	 */
	public void participantsAdded(ISynchronizeParticipant[] participants);

	/**
	 * Notification the given participants have been removed from the
	 * synchronize manager.
	 *
	 * @param participants removed participants
	 */
	public void participantsRemoved(ISynchronizeParticipant[] participants);
}
