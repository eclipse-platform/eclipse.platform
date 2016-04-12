/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize;

import java.io.*;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ui.*;
import org.eclipse.team.internal.ui.registry.*;
import org.eclipse.team.ui.synchronize.*;
import org.eclipse.ui.*;

/**
 * Manages the registered synchronize participants. It handles notification of
 * participant lifecycles, creation of <code>static</code> participants, management
 * of dynamic participants, and the re-creation of persisted participants.
 * <p>
 * A participant is defined in a plugin manifest and can have several properties:
 * - static: means that they always exist and don't have to be added to the manager
 * - dynamic: will be added to the manager at some later time
 *
 * Part (title, id, icon, composite) - described in plugin.xml (IPartInstance)
 * Can have multiple parts of the same type at runtime -> (IPart)
 *   - must acquire a part (IPartInstance.createPart())
 *   - must released to part when done (IPartInstance.releasePart())
 * Some parts can added dynamically to the registry and events are fired to listeners. Listeners can create the newly added part via
 * the #createPart() method.
 * Parts can be persisted/restored with some state
 *
 *
 *
 * Lifecycle:
 * 	startup -> registry read and stored in a participant instance
 *     createParticipant(id) ->
 * 	releaseParticipant(IParticipantDescriptor) ->
 *     getParticipantRegistry -> return IParticipantDescriptors that describe the participants
 * 	shutdown -> persist all settings
 *
 * @see ISynchronizeView
 * @see ISynchronizeParticipant
 * @since 3.0
 */
public class SynchronizeManager implements ISynchronizeManager {
	/**
	 * Synchronize participants listeners
	 */
	private ListenerList fListeners = null;

	/**
	 * Contains the participant descriptions
	 */
	private SynchronizeParticipantRegistry participantRegistry = new SynchronizeParticipantRegistry();

	/**
	 * Contains the synchronize wizard descriptions
	 */
	private SynchronizeWizardRegistry wizardRegistry = new SynchronizeWizardRegistry();

	/**
	 * Contains a table of the state saved between sessions for a participant. The set is keyed
	 * as such {String key -> ISynchronizeParticipantReference}.
	 */
	private Map participantReferences = Collections.synchronizedMap(new HashMap(10));

	// change notification constants
	private final static int ADDED = 1;
	private final static int REMOVED = 2;

	// save context constants
	private final static String CTX_PARTICIPANTS = "syncparticipants"; //$NON-NLS-1$
	private final static String CTX_PARTICIPANT = "participant"; //$NON-NLS-1$
	private final static String CTX_ID = "id"; //$NON-NLS-1$
	private final static String CTX_SECONDARY_ID = "secondary_id"; //$NON-NLS-1$
	private final static String CTX_PARTICIPANT_DISPLAY_NAME = "displayName"; //$NON-NLS-1$
	private final static String CTX_PARTICIPANT_DATA = "data"; //$NON-NLS-1$
	private final static String FILENAME = "syncParticipants.xml"; //$NON-NLS-1$

	/**
	 * Notifies a participant listeners of additions or removals of participant references.
	 */
	class SynchronizeViewPageNotifier implements ISafeRunnable {

		private ISynchronizeParticipantListener fListener;
		private int fType;
		private ISynchronizeParticipant[] fChanged;

		@Override
		public void handleException(Throwable exception) {
			TeamUIPlugin.log(IStatus.ERROR, TeamUIMessages.SynchronizeManager_7, exception);
		}

		@Override
		public void run() throws Exception {
			switch (fType) {
				case ADDED :
					fListener.participantsAdded(fChanged);
					break;
				case REMOVED :
					fListener.participantsRemoved(fChanged);
					break;
			}
		}

		/**
		 * Notifies the given listener of the adds/removes
		 * @param participants the participants that changed
		 * @param update the type of change
		 */
		public void notify(ISynchronizeParticipant[] participants, int update) {
			if (fListeners == null) {
				return;
			}
			fChanged = participants;
			fType = update;
			Object[] copiedListeners = fListeners.getListeners();
			for (int i = 0; i < copiedListeners.length; i++) {
				fListener = (ISynchronizeParticipantListener) copiedListeners[i];
				SafeRunner.run(this);
			}
			fChanged = null;
			fListener = null;
		}
	}

	/**
	 * Represents a participant instance and allows lazy initialization of the instance
	 * only when the participant is required.
	 */
	private class ParticipantInstance implements ISynchronizeParticipantReference {
		private Map participants;
		private IMemento savedState;
		private SynchronizeParticipantDescriptor descriptor;
		private String secondaryId;
		private String displayName;
		private boolean dead;

		public ParticipantInstance(SynchronizeParticipantDescriptor descriptor, String secondaryId, String displayName, IMemento savedState) {
			this.participants = new HashMap();
			this.secondaryId = secondaryId;
			this.savedState = savedState;
			this.descriptor = descriptor;
			this.displayName = displayName;
		}

		public void save(IMemento memento) {
			if (dead) return;
			String key = Utils.getKey(descriptor.getId(), getSecondaryId());
			ISynchronizeParticipant ref = (ISynchronizeParticipant) participants.get(key);
			if(ref != null) {
				ref.saveState(memento);
			} else if(savedState != null) {
				memento.putMemento(savedState);
			}
		}

		@Override
		public boolean equals(Object other) {
			if(other == this) return true;
			if (! (other instanceof ISynchronizeParticipantReference)) return false;
			ISynchronizeParticipantReference otherRef = (ISynchronizeParticipantReference) other;
			String otherSecondaryId = otherRef.getSecondaryId();
			return otherRef.getId().equals(getId()) && Utils.equalObject(getSecondaryId(), otherSecondaryId);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.team.ui.synchronize.ISynchronizeParticipantReference#getId()
		 */
		@Override
		public String getId() {
			return descriptor.getId();
		}

		/* (non-Javadoc)
		 * @see org.eclipse.team.ui.synchronize.ISynchronizeParticipantReference#getSecondaryId()
		 */
		@Override
		public String getSecondaryId() {
			return secondaryId;
		}


		/* (non-Javadoc)
		 * @see org.eclipse.team.ui.synchronize.ISynchronizeParticipantReference#getDisplayName()
		 */
		@Override
		public String getDisplayName() {
			String key = Utils.getKey(descriptor.getId(), getSecondaryId());
			ISynchronizeParticipant participant = (ISynchronizeParticipant) participants.get(key);
			if(participant != null) {
				return participant.getName();
			}
			return displayName != null ? displayName : descriptor.getName();
		}

		public boolean isInstantiated() {
			String key = Utils.getKey(descriptor.getId(), getSecondaryId());
			return (ISynchronizeParticipant) participants.get(key) != null;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.team.ui.synchronize.ISynchronizeParticipantReference#createParticipant()
		 */
		@Override
		public ISynchronizeParticipant getParticipant() throws TeamException {
			if (dead) return null;
			String key = Utils.getKey(descriptor.getId(), getSecondaryId());
			try {
				ISynchronizeParticipant participant = (ISynchronizeParticipant) participants.get(key);
				if (participant == null) {
					participant = instantiate();
					if(participant != null)
						participants.put(key, participant);
				}
				return participant;
			} catch (TeamException e) {
				TeamUIPlugin.log(e);
				participantReferences.remove(key);
				throw new TeamException(TeamUIMessages.SynchronizeManager_8, e);
			}
		}

		public void setParticipant(ISynchronizeParticipant participant) {
			String key = Utils.getKey(descriptor.getId(), getSecondaryId());
			participants.put(key, participant);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.team.ui.synchronize.ISynchronizeParticipantReference#getDescriptor()
		 */
		@Override
		public ISynchronizeParticipantDescriptor getDescriptor() {
			return descriptor;
		}

		private ISynchronizeParticipant instantiate() throws TeamException {
			try {
					ISynchronizeParticipant participant = (ISynchronizeParticipant) TeamUIPlugin.createExtension(descriptor.getConfigurationElement(), SynchronizeParticipantDescriptor.ATT_CLASS);
					participant.setInitializationData(descriptor.getConfigurationElement(), null, null);
					participant.init(getSecondaryId(), savedState);
					savedState = null;
					return participant;
				} catch (PartInitException e) {
					throw new TeamException(NLS.bind(TeamUIMessages.SynchronizeManager_11, new String[] { descriptor.getName() }), e);
				} catch (CoreException e) {
					throw TeamException.asTeamException(e);
				} catch(Exception e) {
					throw new TeamException(NLS.bind(TeamUIMessages.SynchronizeManager_11, new String[] { descriptor.getName() }), e);
				}
			}

		/**
		 * Dispose of the reference
		 */
		public void dispose() {
			try {
				ISynchronizeParticipant participant = getParticipant();
				if (participant != null)
					participant.dispose();
			} catch (TeamException e) {
				// Ignore since we are disposing anyway;
			} finally {
				dead = true;
			}
		}
	}

	public SynchronizeManager() {
		init();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.team.ui.sync.ISynchronizeManager#addSynchronizeParticipantListener(org.eclipse.team.ui.sync.ISynchronizeParticipantListener)
	 */
	@Override
	public void addSynchronizeParticipantListener(ISynchronizeParticipantListener listener) {
		if (fListeners == null) {
			fListeners = new ListenerList(ListenerList.IDENTITY);
		}
		fListeners.add(listener);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.team.ui.sync.ISynchronizeManager#removeSynchronizeParticipantListener(org.eclipse.team.ui.sync.ISynchronizeParticipantListener)
	 */
	@Override
	public void removeSynchronizeParticipantListener(ISynchronizeParticipantListener listener) {
		if (fListeners != null) {
			fListeners.remove(listener);
		}
	}

	/**
	 * Creates a new participant reference with of the provided type. If the secondayId is specified it
	 * is used as the qualifier for multiple instances of the same type.
	 * <p>
	 * The returned participant reference is a light weight handle describing the participant. The plug-in
	 * defining the participant is not loaded. To instantiate a participant a client must call
	 * {@link ISynchronizeParticipantReference#createParticipant()} and must call
	 * {@link ISynchronizeParticipantReference#releaseParticipant()} when finished with the participant.
	 * </p>
	 * @param type the type of the participant
	 * @param secondaryId a unique id for multiple instance support
	 * @return a reference to a participant
	 */
	private ParticipantInstance createParticipantReference(String type, String secondaryId, String displayName) throws PartInitException {
		SynchronizeParticipantDescriptor desc = participantRegistry.find(type);
		// ensure that the view id is valid
		if (desc == null)
			throw new PartInitException(NLS.bind(TeamUIMessages.SynchronizeManager_19, new String[] { type }));
		// ensure that multiple instances are allowed if a secondary id is given
		if (secondaryId != null) {
//		    if (!desc.isMultipleInstances()) {
//				throw new PartInitException(Policy.bind("SynchronizeManager.20", type)); //$NON-NLS-1$
//		    }
		}
		String key = Utils.getKey(type, secondaryId);
		ParticipantInstance ref = (ParticipantInstance) participantReferences.get(key);
		if (ref == null) {
			ref = new ParticipantInstance(desc, secondaryId, displayName, null);
		}
		return ref;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.team.ui.sync.ISynchronizeManager#addSynchronizeParticipants(org.eclipse.team.ui.sync.ISynchronizeParticipant[])
	 */
	@Override
	public synchronized void addSynchronizeParticipants(ISynchronizeParticipant[] participants) {
		// renamed to createSynchronizeParticipant(id)
		List added = new ArrayList(participants.length);
		for (int i = 0; i < participants.length; i++) {
			ISynchronizeParticipant participant = participants[i];
			String key = Utils.getKey(participant.getId(), participant.getSecondaryId());
			if(! participantReferences.containsKey(key)) {
				try {
					ParticipantInstance ref = createParticipantReference(participant.getId(), participant.getSecondaryId(), participant.getName());
					ref.setParticipant(participant);
					removeMatchingParticipant(participant.getId());
					participantReferences.put(key, ref);
					added.add(participant);
				} catch (PartInitException e) {
					TeamUIPlugin.log(e);
					continue;
				}
			}
		}
		if (!added.isEmpty()) {
			saveState();
			fireUpdate((ISynchronizeParticipant[]) added.toArray(new ISynchronizeParticipant[added.size()]), ADDED);
		}
	}

	private void removeMatchingParticipant(String id) {
		ISynchronizeParticipantReference[] refs = get(id);
		if (refs.length > 0) {
			// Find an un-pinned participant and replace it
			for (int i = 0; i < refs.length; i++) {
				ISynchronizeParticipantReference reference = refs[i];
				ISynchronizeParticipant p;
				try {
					p = reference.getParticipant();
					if (!p.isPinned() && !isDirty(p)) {
						removeSynchronizeParticipants(new ISynchronizeParticipant[]{p});
						break;
					}
				} catch (TeamException e) {
					continue;
				}
			}
		}
	}

	private boolean isDirty(ISynchronizeParticipant p) {
		if (p instanceof ModelSynchronizeParticipant) {
			ModelSynchronizeParticipant msp = (ModelSynchronizeParticipant) p;
			Saveable s = msp.getActiveSaveable();
			if (s != null && s.isDirty()) {
				return true;
			}
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.team.ui.sync.ISynchronizeManager#removeSynchronizeParticipants(org.eclipse.team.ui.sync.ISynchronizeParticipant[])
	 */
	@Override
	public synchronized void removeSynchronizeParticipants(ISynchronizeParticipant[] participants) {
		List removed = new ArrayList(participants.length);
		for (int i = 0; i < participants.length; i++) {
			ISynchronizeParticipant participant = participants[i];
			String key = Utils.getKey(participant.getId(), participant.getSecondaryId());
			if(participantReferences.containsKey(key)) {
				ParticipantInstance ref = (ParticipantInstance)participantReferences.remove(key);
				if(ref.isInstantiated()) {
					ref.dispose();
				}
				removed.add(participant);
			}
		}
		if (!removed.isEmpty()) {
			saveState();
			fireUpdate((ISynchronizeParticipant[]) removed.toArray(new ISynchronizeParticipant[removed.size()]), REMOVED);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISynchronizeManager#get(java.lang.String)
	 */
	@Override
	public ISynchronizeParticipantReference get(String id, String secondaryId) {
		String key = Utils.getKey(id, secondaryId);
		return (ISynchronizeParticipantReference) participantReferences.get(key);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISynchronizeManager#get(java.lang.String)
	 */
	@Override
	public ISynchronizeParticipantReference[] get(String id) {
		ISynchronizeParticipantReference[] refs = getSynchronizeParticipants();
		ArrayList refsForId = new ArrayList();
		for (int i = 0; i < refs.length; i++) {
			ISynchronizeParticipantReference reference = refs[i];
			if(reference.getId().equals(id)) {
				refsForId.add(reference);
			}
		}
		return (ISynchronizeParticipantReference[]) refsForId.toArray(new ISynchronizeParticipantReference[refsForId.size()]);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.team.ui.sync.ISynchronizeManager#getSynchronizeParticipants()
	 */
	@Override
	public synchronized ISynchronizeParticipantReference[] getSynchronizeParticipants() {
		return (ISynchronizeParticipantReference[]) participantReferences.values().toArray(new ISynchronizeParticipantReference[participantReferences.values().size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISynchronizeManager#showSynchronizeViewInActivePage()
	 */
	@Override
	public ISynchronizeView showSynchronizeViewInActivePage() {
		IWorkbench workbench = TeamUIPlugin.getPlugin().getWorkbench();
		IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();

		boolean switchPerspectives = promptForPerspectiveSwitch();
		IWorkbenchPage activePage = null;
		if(switchPerspectives) {
			try {
				String pId = TeamUIPlugin.getPlugin().getPreferenceStore().getString(IPreferenceIds.SYNCVIEW_DEFAULT_PERSPECTIVE);
				activePage = workbench.showPerspective(pId, window);
			} catch (WorkbenchException e) {
				Utils.handleError(window.getShell(), e, TeamUIMessages.SynchronizeView_14, e.getMessage());
			}
		}
		try {
			if (activePage == null) {
				activePage = TeamUIPlugin.getActivePage();
				if (activePage == null)
					return null;
			}
			//IViewPart part = activePage.showView(ISynchronizeView.VIEW_ID, Long.toString(System.currentTimeMillis()), IWorkbenchPage.VIEW_ACTIVATE);
			IViewPart part = activePage.showView(ISynchronizeView.VIEW_ID);
			try {
				return (ISynchronizeView) part;
			} catch (ClassCastException e) {
				// Strange that we cannot cast the part (see bug 53671)
				TeamUIPlugin.log(IStatus.ERROR, NLS.bind(TeamUIMessages.SynchronizeManager_18, new String[] { part.getClass().getName() }), e);
				return null;
			}
		} catch (PartInitException pe) {
			Utils.handleError(window.getShell(), pe, TeamUIMessages.SynchronizeView_16, pe.getMessage());
			return null;
		}
	}

	/**
	 * Decides what action to take when switching perspectives and showing the synchronize view. Basically there are a
	 * set of user preferences that control how perspective switching.
	 */
	private boolean promptForPerspectiveSwitch() {
		// Decide if a prompt is even required
		IPreferenceStore store = TeamUIPlugin.getPlugin().getPreferenceStore();
		String option = store.getString(IPreferenceIds.SYNCHRONIZING_COMPLETE_PERSPECTIVE);
		if(option.equals(MessageDialogWithToggle.ALWAYS)) {
			return true;
		} else if(option.equals(MessageDialogWithToggle.NEVER)) {
			return false;
		}

		// Otherwise determine if a prompt is required
		IPerspectiveRegistry registry= PlatformUI.getWorkbench().getPerspectiveRegistry();
		String defaultSyncPerspectiveId = store.getString(IPreferenceIds.SYNCVIEW_DEFAULT_PERSPECTIVE);
		IPerspectiveDescriptor perspectiveDescriptor = registry.findPerspectiveWithId(defaultSyncPerspectiveId);
		IWorkbenchPage page = TeamUIPlugin.getActivePage();
		if(page != null) {
			IPerspectiveDescriptor p = page.getPerspective();
			if(p != null && p.getId().equals(defaultSyncPerspectiveId)) {
				// currently in default perspective
				return false;
			}
		}

		if(perspectiveDescriptor != null) {

			String message;;
			String desc = perspectiveDescriptor.getDescription();
			if (desc == null) {
				message = NLS.bind(TeamUIMessages.SynchronizeManager_30, new String[] { perspectiveDescriptor.getLabel() });
			} else {
				message = NLS.bind(TeamUIMessages.SynchronizeManager_32, new String[] { perspectiveDescriptor.getLabel(), desc });
			}
			MessageDialogWithToggle m = MessageDialogWithToggle.openYesNoQuestion(Utils.getShell(null),
						TeamUIMessages.SynchronizeManager_27,
						message,
						TeamUIMessages.SynchronizeManager_31,
						false /* toggle state */,
						store,
						IPreferenceIds.SYNCHRONIZING_COMPLETE_PERSPECTIVE);

			int result = m.getReturnCode();
			switch (result) {
				// yes, ok
				case IDialogConstants.YES_ID:
				case IDialogConstants.OK_ID :
					return true;
				// no
				case IDialogConstants.NO_ID :
					return false;
			}
		}
		return false;
	}

	/**
	 * Creates the participant registry and restore any saved participants.
	 * Will also instantiate any static participants.
	 */
	public void init() {
		try {
			// Initialize the participant registry - reads all participant extension descriptions.
			participantRegistry.readRegistry(Platform.getExtensionRegistry(), TeamUIPlugin.ID, SynchronizeParticipantRegistry.PT_SYNCPARTICIPANTS);
			// Initialize the wizard registry
			wizardRegistry.readRegistry(Platform.getExtensionRegistry(), TeamUIPlugin.ID, SynchronizeWizardRegistry.PT_SYNCHRONIZE_WIZARDS);

			// Instantiate and register any dynamic participants saved from a
			// previous session.
			restoreSavedParticipants();
		} catch (CoreException e) {
			TeamUIPlugin.log(new Status(IStatus.ERROR, TeamUIPlugin.ID, 1, TeamUIMessages.SynchronizeManager_8, e));
		}
	}

	/**
	 * Allow participant instances to clean-up.
	 */
	public void dispose() {
		// save state and settings for existing participants.
		saveState();
		for (Iterator it = participantReferences.values().iterator(); it.hasNext();) {
			ParticipantInstance ref = (ParticipantInstance) it.next();
			if((ref).isInstantiated()) {
				try {
					ref.getParticipant().dispose();
				} catch (TeamException e) {
					continue;
				}
			}
		}
		participantReferences = null;
	}

	/**
	 * Restores participants that have been saved between sessions.
	 */
	private void restoreSavedParticipants() throws CoreException {
		File file = getStateFile();
		Reader reader;
		try {
			reader = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e) {
			return;
		}
		IMemento memento = XMLMemento.createReadRoot(reader);
		IMemento[] participantNodes = memento.getChildren(CTX_PARTICIPANT);
		for (int i = 0; i < participantNodes.length; i++) {
			IMemento memento2 = participantNodes[i];
			String id = memento2.getString(CTX_ID);
			String secondayId = memento2.getString(CTX_SECONDARY_ID);
			if (secondayId != null) {
				String displayName = memento2.getString(CTX_PARTICIPANT_DISPLAY_NAME);
				SynchronizeParticipantDescriptor desc = participantRegistry.find(id);
				if (desc != null) {
					String key = Utils.getKey(id, secondayId);
					participantReferences.put(key, new ParticipantInstance(desc, secondayId, displayName, memento2.getChild(CTX_PARTICIPANT_DATA)));
				} else {
					TeamUIPlugin.log(new Status(IStatus.ERROR, TeamUIPlugin.ID, 1, NLS.bind(TeamUIMessages.SynchronizeManager_9, new String[] { id }), null));
				}
			}
		}
	}

	/**
	 * Saves a file containing the list of participant ids that are registered
	 * with this manager. Each initialized participant is also given the chance to save
	 * it's state.
	 */
	private void saveState() {
		XMLMemento xmlMemento = XMLMemento.createWriteRoot(CTX_PARTICIPANTS);
		for (Iterator it = participantReferences.values().iterator(); it.hasNext(); ) {
			ParticipantInstance ref = (ParticipantInstance) it.next();
			// Participants can opt out of being saved between sessions
			if(! ref.getDescriptor().isPersistent()) continue;
			// Create the state placeholder for a participant
			IMemento participantNode = xmlMemento.createChild(CTX_PARTICIPANT);
			participantNode.putString(CTX_ID, ref.getId());
			String secondaryId = ref.getSecondaryId();
			if(secondaryId != null) {
				participantNode.putString(CTX_SECONDARY_ID,secondaryId);
			}
			participantNode.putString(CTX_PARTICIPANT_DISPLAY_NAME, ref.getDisplayName());
			IMemento participantData = participantNode.createChild(CTX_PARTICIPANT_DATA);
			ref.save(participantData);
		}
		try {
			Writer writer = new BufferedWriter(new FileWriter(getStateFile()));
			try {
				xmlMemento.save(writer);
			} finally {
				writer.close();
			}
		} catch (IOException e) {
			TeamUIPlugin.log(new Status(IStatus.ERROR, TeamUIPlugin.ID, 1, TeamUIMessages.SynchronizeManager_10, e));
		}
	}

	private File getStateFile() {
		IPath pluginStateLocation = TeamUIPlugin.getPlugin().getStateLocation();
		return pluginStateLocation.append(FILENAME).toFile();
	}

	/**
	 * Fires notification.
	 *
	 * @param participants
	 *            participants added/removed
	 * @param type
	 *            ADDED or REMOVED
	 * @see SynchronizeManager#ADDED
	 * @see SynchronizeManager#REMOVED
	 */
	private void fireUpdate(ISynchronizeParticipant[] participants, int type) {
		new SynchronizeViewPageNotifier().notify(participants, type);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISynchronizeManager#getDescriptor()
	 */
	@Override
	public ISynchronizeParticipantDescriptor getParticipantDescriptor(String id) {
		return participantRegistry.find(id);
	}

	public SynchronizeWizardDescription[] getWizardDescriptors() {
		return wizardRegistry.getSynchronizeWizards();
	}
}
