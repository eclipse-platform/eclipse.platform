/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.team.core.TeamException;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.IPageBookViewPage;

/**
 * A synchronize participant is a visual component that can be displayed within any
 * control (e.g. view, editor, dialog).  Typically a participant is used to show changes between
 * local resources and variant states of those resources and allows the user to perform actions
 * to manipulate the changes.
 * <p>
 * This class does not mandate how the synchronization state is displayed, but instead provides
 * the accessors that clients would use to create a visual instance of the this participant.
 * </p><p>
 * A participant can display multiple instances of its synchronization state to the user via the creation
 * of a page {@link #createPage(ISynchronizePageConfiguration)} and
 * clients can decide where to display the page. For example, the synchronize view is an example
 * of a client that displays a participant in a view. However, you can imagine that a client may
 * also want to display this state in a wizard or dialog instead.
 * </p><p>
 * When a participant is registered with the {@link ISynchronizeManager} it will automatically display
 * in the <i>Synchronize View</i> and if the participant extension point
 * enabled <code>synchronizeWizards</code> it will also appear in the global synchronize action
 * toolbar.
 * <p>
 * A participant is added to the workbench as follows:</p>
 * <ul>
 * <li>A <code>synchronizeParticipant</code> extension is contributed to
 * the team registry. This extension defines the participant id, name, icon, type, and
 * participant class.
 * <li>A user via a wizard provided by the <code>synchronizeWizards</code> extension point
 * or client code, creates a participant instance and registers it with the
 * synchronize manager. It then appears in the synchronize view.
 * <li>A synchronization can be persistent and thus re-initialized at startup.
 * <li>A pinned participant will only be removed from the synchronize manager if it is un-pinned.
 * </ul>
 * <p>
 * Once a participant is added to the synchronize manager its lifecycle will be managed. On shutdown if
 * the participant is persistable, the participant will be asked to persist state via
 * the <code>saveState()</code> method. At startup the <code>init()</code> method is called
 * with a handle to the state that was saved. The dispose method is called when the participant is
 * removed from the manager and at shutdown.
 * </p>
 * @see ISynchronizeView
 * @see ISynchronizeManager
 * @see AbstractSynchronizeParticipant
 * @since 3.0
 * @noimplement Clients are not intended to implement this interface. Instead,
 *              subclass {@link AbstractSynchronizeParticipant}.
 */
public interface ISynchronizeParticipant extends IExecutableExtension, IAdaptable {

	/**
	 * A property constant that can be used to indicate that the content of this participant
	 * has changed. This is a general event that can be used to indicate to the user that there
	 * is a change in state for the participant. In general, the values associated with the event do not have
	 * any meaning.
	 *
	 * @see #addPropertyChangeListener(IPropertyChangeListener)
	 */
	public static final String P_CONTENT = "org.eclipse.team.ui.content"; //$NON-NLS-1$

	/**
	 * Returns the unique id that identified the <i>type</i> of this
	 * synchronize participant. The synchronize manager supports registering
	 * several instances of the same participant type.
	 *
	 * @return the unique id that identified the <i>type</i> of this
	 * synchronize participant.
	 */
	public String getId();

	/**
	 * Returns the instance id that identified the unique instance of this
	 * participant. The synchronize manager supports registering
	 * several instances of the same participant type and this id is used
	 * to differentiate between them.
	 *
	 * @return  the instance id that identified the unique instance of this
	 * participant or <code>null</code> if this participant doesn't support
	 * multiple instances.
	 */
	public String getSecondaryId();

	/**
	 * Returns the name of this synchronize participant. This name is displayed to the user.
	 *
	 * @return the name of this synchronize participant
	 */
	public String getName();

	/**
	 * Returns an image descriptor for this synchronize participant, or <code>null</code>
	 * if none.
	 *
	 * @return an image descriptor for this synchronize participant, or <code>null</code>
	 * if none
	 */
	public ImageDescriptor getImageDescriptor();

	/**
	 * Returns if this participant is pinned. Pinned participants will only be removed from the
	 * synchronize manager until they are un-pinned.
	 *
	 * @return <code>true</code> if this participant is pinned and <code>false</code>
	 * otherwise.
	 */
	public boolean isPinned();

	/**
	 * Sets whether this participant is pinned.
	 *
	 * @param pinned sets if the participant is pinned.
	 */
	public void setPinned(boolean pinned);

	/**
	 * Creates the configuration for the participant page. The configuration controls the
	 * options for displaying the participant. The configuration used to initialize the page
	 * when {@link #createPage(ISynchronizePageConfiguration)} is called and as such
	 * can be used to pre-configure visual properties of the displayed page.
	 *
	 * @return the configuration for the participant page.
	 */
	public ISynchronizePageConfiguration createPageConfiguration();

	/**
	 * Creates and returns a new page for this synchronize participant. The
	 * page is displayed using the parameters from the configuration. For example,
	 * the configuration defines the context in which the page is shown, via the
	 * {@link ISynchronizePageSite}.
	 *
	 * @param configuration used to initialize the page
	 * @return a page book view page representation of this synchronize
	 * participant
	 */
	public IPageBookViewPage createPage(ISynchronizePageConfiguration configuration);

	/**
	 * Runs the participants action. Typically this would be some action to refresh the synchronization
	 * state of the participant. This action is run from the global synchronize drop-down.
	 *
	 * @param part the part in which the action is run or <code>null</code> if the action
	 * is not being run in a workbench part.
	 */
	public void run(IWorkbenchPart part);

	/**
	 * Initializes this participant with the given participant state.
	 * A memento is passed to the participant which contains a snapshot
	 * of the participants state from a previous session.
	 * <p>
	 * This method is automatically called by the team plugin shortly after
	 * participant construction. It marks the start of the views
	 * lifecycle. Clients must not call this method.
	 * </p>
	 * @param secondaryId the secondayId of this participant instance or <code>null</code>
	 * if this participant doesn't support multiple instances.
	 * @param memento the participant state or <code>null</code> if there
	 * is no previous saved state
	 * @exception PartInitException if this participant was not initialized
	 * successfully
	 */
	public void init(String secondaryId, IMemento memento) throws PartInitException;

	/**
	 * Disposes of this synchronize participant and is called to free the
	 * resources associated with a participant. When a participant is added
	 * to the {@link ISynchronizeManager} this method is called when the
	 * manager is shutdown or the participant is removed from the manager.
	 * <p>
	 * Within this method a participant may release any resources, fonts, images, etc.
	 * held by this part.  It is also very important to remove all listeners.
	 * </p><p>
	 * Clients should not call this method (the synchronize manager calls this
	 * method at appropriate times).
	 * </p>
	 */
	public void dispose();

	/**
	 * Saves the participants object state within the memento. This state
	 * will be available when the participant is restored via <code>init</code>.
	 * <p>
	 * This method can be called multiple times during the lifetime of the
	 * participant object.
	 * </p>
	 * @param memento a memento to receive the object state
	 */
	public void saveState(IMemento memento);

	/**
	 * Adds a listener for changes to properties of this synchronize
	 * participant. Has no effect if an identical listener is already
	 * registered.
	 * <p>
	 * The changes supported by the synchronize view are as follows:
	 * </p>
	 * <ul>
	 * <li><code>IBasicPropertyConstants.P_TEXT</code>- indicates the name
	 * of a synchronize participant has changed</li>
	 * <li><code>IBasicPropertyConstants.P_IMAGE</code>- indicates the
	 * image of a synchronize participant has changed</li>
	 * </ul>
	 * <p>
	 * Clients may define additional properties as required.
	 * </p>
	 * @param listener a property change listener
	 */
	public void addPropertyChangeListener(IPropertyChangeListener listener);

	/**
	 * Removes the given property listener from this synchronize participant.
	 * Has no effect if an identical listener is not already registered.
	 *
	 * @param listener a property listener
	 */
	public void removePropertyChangeListener(IPropertyChangeListener listener);

	/**
	 * Prepare the given element and compare configuration for use with a compare editor
	 * input.
	 * @param element the sync model element whose contents are about to be displayed to the user
	 * 		in a compare editor or compare dialog
	 * @param configuration the compare configuration that will be used to configure the compare editor or dialog
	 * @param monitor a progress monitor that can be used if contacting a server to prepare the element and configuration
	 * @throws TeamException if an error occurred that should prevent the display of the compare editor containing
	 * 		the element
	 *
	 * @since 3.1
	 */
	public void prepareCompareInput(
			ISynchronizeModelElement element,
			CompareConfiguration configuration,
			IProgressMonitor monitor)
				throws TeamException;

	/**
	 * Return the list of preference pages that are associated with this participant
	 * @return the list of preference pages that are associated with this participant
	 * @since 3.1
	 */
	public PreferencePage[] getPreferencePages();

	/**
	 * Returns the help context id of this participant.
	 *
	 * @return the help context id of this participant
	 * @since 3.5
	 */
	public String getHelpContextId();
}
