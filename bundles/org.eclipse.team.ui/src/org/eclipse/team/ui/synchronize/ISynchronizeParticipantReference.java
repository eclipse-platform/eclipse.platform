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
package org.eclipse.team.ui.synchronize;

import org.eclipse.team.core.TeamException;

/**
 * A reference is a light weight handle used by the {@link ISynchronizeManager}
 * to manage registered participants. It is used to reference information
 * about a particular participant instance without requiring the participant
 * to be instantiated. Calling the {@link #getParticipant()} method will
 * cause the participant to be instantiated.
 * <p>
 * Clients are not intended to implement this interface.
 * </p>
 * @see ISynchronizeManager
 * @since 3.0
 */
public interface ISynchronizeParticipantReference {
	/**
	 * Returns the id of the participant type referenced by this handle.
	 *
	 * @return the id of the participant type references by this handle.
	 */
	public String getId();

	/**
	 * Returns the secondary id (e.g. instance id) of the participant type referenced
	 * by this handle or <code>null</code> if the participant doesn't support
	 * multiple instances.
	 *
	 * @return the secondary id of the participant type referenced
	 * by this handle or <code>null</code> if the participant doesn't support
	 * multiple instances.
	 */
	public String getSecondaryId();

	/**
	 * Returns the fully qualified name of this participant reference. This includes the
	 * secondaryId if available. This can be displayed in the user interface to allow
	 * the user to distinguish between multiple instances of a participant.
	 *
	 * @return the fully qualified name of this participant reference
	 */
	public String getDisplayName();

	/**
	 * Returns the participant referenced by this handle. This may trigger loading of the
	 * participant and and a result may be long running. The method may return <code>null</code>
	 * if the participant cannot be de-referenced.
	 *
	 * @return the participant referenced by this handle.
	 * @throws TeamException if an error occurs
	 */
	public ISynchronizeParticipant getParticipant() throws TeamException;

	/**
	 * Returns the descriptor for this participant type.
	 *
	 * @return the descriptor for this participant type.
	 */
	public ISynchronizeParticipantDescriptor getDescriptor();
}
