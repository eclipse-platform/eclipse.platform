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

/**
 * Manages synchronization view participants. Clients can programmatically add
 * or remove participant instances from this manager. Managed participants are available to
 * clients whereas un-managed participants can still exist but won't be available
 * generally available to clients until explicitly added to the manager.
 * <p>
 * Participants added to the manager will benefit from the manager's lifecycle
 * support. The participants will automatically have their <code>init</code> method and <code>dispose</code>
 * called when the manager starts and is shutdown and if persistable will
 * be allowed to save their state on shutdown.
 * </p><p>
 * Clients are not intended to implement this interface.
 * </p>
 * @see ISynchronizeParticipant
 * @see org.eclipse.team.ui.TeamUI#getSynchronizeManager()
 * @since 3.0
 */
public interface ISynchronizeManager {

	/**
	 * Constant identifying the job family identifier for a background job that affects the
	 * synchronization state of resources. All clients
	 * that schedule background jobs that affect synchronization state should include this job
	 * family in their implementation of <code>belongsTo</code>.
	 *
	 * @see org.eclipse.core.runtime.jobs.Job#belongsTo(java.lang.Object)
	 */
	public static final Object FAMILY_SYNCHRONIZE_OPERATION = new Object();

	/**
	 * Registers the given listener for participant notifications. Has
	 * no effect if an identical listener is already registered.
	 *
	 * @param listener listener to register
	 */
	public void addSynchronizeParticipantListener(ISynchronizeParticipantListener listener);

	/**
	 * Removes the given listener for participant notifications. Has
	 * no effect if an identical listener is not already registered.
	 *
	 * @param listener listener to remove
	 */
	public void removeSynchronizeParticipantListener(ISynchronizeParticipantListener listener);

	/**
	 * Adds the given participants to the synchronize manager. Has no effect for
	 * equivalent participants are already registered. The participants will be added
	 * to any existing synchronize views.
	 *
	 * @param participants participants to add
	 */
	public void addSynchronizeParticipants(ISynchronizeParticipant[] participants);

	/**
	 * Removes the given participants from the synchronize manager. If the participants are
	 * being displayed in any synchronize views, their associated pages will be closed.
	 *
	 * @param participants participants to remove
	 */
	public void removeSynchronizeParticipants(ISynchronizeParticipant[] participants);

	/**
	 * Returns a collection of synchronize participant references registered with the synchronize manager.
	 *
	 * @return a collection of synchronize participants registered with the synchronize manager.
	 */
	public ISynchronizeParticipantReference[] getSynchronizeParticipants();

	/**
	 * Returns the registered synchronize participants with the given type id. It is
	 * possible to have multiple instances of the same participant type.
	 *
	 * @param id the type identifier for the participant
	 * @return the registered synchronize participants with the given id, or
	 * an empty list if there are none with that id registered.
	 */
	public ISynchronizeParticipantReference[] get(String id);

	/**
	 * Returns the registered synchronize participants with the given type id and instance id.
	 *
	 * @param id the type identifier for the participant
	 * @param secondaryId the instance identifier for this participant type or <code>null</code>
	 * if this participant doesn't support multiple instances.
	 * @return the registered synchronize participants with the given id, or
	 * <code>null</code> if none with that id is not registered.
	 */
	public ISynchronizeParticipantReference get(String id, String secondaryId);

	/**
	 * Opens the synchronize view in the perspective defined by the user in the team synchronize
	 * preferences.
	 *
	 * @return the opened synchronize view or <code>null</code> if it can't be opened.
	 */
	public ISynchronizeView showSynchronizeViewInActivePage();

	/**
	 * Returns the participant descriptor for the given participant type id or
	 * <code>null</code> if a descriptor is not found for that id.
	 * @param id the participant id
	 *
	 * @return the participant descriptor for the given participant id or
	 * <code>null</code> if a descriptor is not found for that id.
	 */
	public ISynchronizeParticipantDescriptor getParticipantDescriptor(String id);
}
