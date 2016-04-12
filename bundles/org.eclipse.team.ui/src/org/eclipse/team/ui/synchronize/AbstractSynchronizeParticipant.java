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
package org.eclipse.team.ui.synchronize;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.IBasicPropertyConstants;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.internal.ui.*;
import org.eclipse.team.internal.ui.preferences.SyncViewerPreferencePage;
import org.eclipse.team.internal.ui.registry.SynchronizeParticipantDescriptor;
import org.eclipse.team.internal.ui.synchronize.SyncInfoModelElement;
import org.eclipse.team.internal.ui.synchronize.SynchronizePageConfiguration;
import org.eclipse.team.ui.TeamImages;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PartInitException;

/**
 * This class is the abstract base class for all synchronize view participants. Clients must subclass
 * this class instead of directly implementing {@link ISynchronizeParticipant}.
 * <p>
 * This class provides lifecycle support and hooks for configuration of synchronize view pages.
 * </p>
 * @see ISynchronizeParticipant
 * @since 3.0
 */
public abstract class AbstractSynchronizeParticipant extends PlatformObject implements ISynchronizeParticipant {

	/**
	 * Property key used in the property change event fired when the pinned
	 * state of a participant changes.
	 */
	public static final String P_PINNED = "org.eclipse.team.pinned"; //$NON-NLS-1$

	/**
	 * Property key used in the property change event fired when the
	 * participants refresh schedule changes.
	 * @since 3.2
	 */
	public static final String P_SCHEDULED = "org.eclipse.team.schedule"; //$NON-NLS-1$

	// key for persisting the pinned state of a participant
	private final static String CTX_PINNED = "root"; //$NON-NLS-1$

	// property listeners
	private PropertyChangeHandler fChangeHandler;

	private String fName;
	private String fId;
	private String fSecondaryId;
	private boolean pinned;
	private ImageDescriptor fImageDescriptor;
	private String fHelpContextId;
	protected IConfigurationElement configElement;

	/**
	 * Default constructor is a no-op. Subclasses that are persistable must support a no-arg constructor
	 * and
	 */
	public AbstractSynchronizeParticipant() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISynchronizeParticipant#getName()
	 */
	@Override
	public String getName() {
		return fName;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISynchronizeParticipant#getImageDescriptor()
	 */
	@Override
	public ImageDescriptor getImageDescriptor() {
		return fImageDescriptor;
	}


	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISynchronizeParticipant#getId()
	 */
	@Override
	public String getId() {
		return fId;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISynchronizeParticipant#getSecondaryId()
	 */
	@Override
	public String getSecondaryId() {
		return fSecondaryId;
	}

	/**
	 * Returns the help context id of this participant or value of
	 * <code>IHelpContextIds.SYNC_VIEW</code> when no specific id has been
	 * provided.
	 *
	 * @see org.eclipse.team.ui.synchronize.ISynchronizeParticipant#getHelpContextId()
	 * @see org.eclipse.team.internal.ui.IHelpContextIds#SYNC_VIEW
	 * @since 3.5
	 * @nooverride This method is not intended to be re-implemented or extended
	 *             by clients.
	 */
	@Override
	public String getHelpContextId() {
		return fHelpContextId == null ? IHelpContextIds.SYNC_VIEW
				: fHelpContextId;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISynchronizeParticipant#setPinned(boolean)
	 */
	@Override
	public final void setPinned(boolean pinned) {
		this.pinned = pinned;
		pinned(pinned);
		firePropertyChange(this, P_PINNED, Boolean.valueOf(!pinned), Boolean.valueOf(pinned));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISynchronizeParticipant#isPinned()
	 */
	@Override
	public final boolean isPinned() {
		return pinned;
	}

	/**
	 * Called when the pinned state is changed. Allows subclasses to react to pin state changes.
	 *
	 * @param pinned whether the participant is pinned.
	 */
	protected void pinned(boolean pinned) {
		// Subclasses can re-act to changes in the pinned state
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if(obj == this) return true;
		if( ! (obj instanceof ISynchronizeParticipant)) return false;
		ISynchronizeParticipant other = (ISynchronizeParticipant)obj;
		return getId().equals(other.getId()) && Utils.equalObject(getSecondaryId(), other.getSecondaryId());
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return Utils.getKey(getId(), getSecondaryId()).hashCode();
	}

	/**
	 * Return whether this participant can be refreshed. Participants that can
	 * be refreshed may have a Synchronize menu item contributed to their context menu
	 * and can also be refreshed from the Synchronize drop-down toolbar item.
	 * When refreshed from the toolbar item, the {@link ISynchronizeParticipant#run(org.eclipse.ui.IWorkbenchPart)}
	 * method is called.
	 * @return whether this participant can be refreshed
	 */
	public boolean doesSupportSynchronize() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISynchronizeParticipant#addPropertyChangeListener(org.eclipse.jface.util.IPropertyChangeListener)
	 */
	@Override
	public synchronized void addPropertyChangeListener(IPropertyChangeListener listener) {
		if (fChangeHandler == null) {
			fChangeHandler = new PropertyChangeHandler();
		}
		fChangeHandler.addPropertyChangeListener(listener);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISynchronizeParticipant#removePropertyChangeListener(org.eclipse.jface.util.IPropertyChangeListener)
	 */
	@Override
	public void removePropertyChangeListener(IPropertyChangeListener listener) {
		if (fChangeHandler != null) {
			fChangeHandler.removePropertyChangeListener(listener);
		}
	}

	/**
	 * Notify all listeners that the given property has changed.
	 *
	 * @param source the object on which a property has changed
	 * @param property identifier of the property that has changed
	 * @param oldValue the old value of the property, or <code>null</code>
	 * @param newValue the new value of the property, or <code>null</code>
	 */
	public void firePropertyChange(Object source, String property, Object oldValue, Object newValue) {
		if (fChangeHandler == null) {
			return;
		}
		fChangeHandler.firePropertyChange(source, property, oldValue, newValue);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IExecutableExtension#setInitializationData(org.eclipse.core.runtime.IConfigurationElement, java.lang.String, java.lang.Object)
	 */
	@Override
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data) throws CoreException {
		//	Save config element.
		configElement = config;

		// Id
		fId = config.getAttribute("id"); //$NON-NLS-1$

		// Title.
		fName = config.getAttribute("name"); //$NON-NLS-1$
		if (fName == null) {
			fName = "Unknown"; //$NON-NLS-1$
		}

		// Icon.
		String strIcon = config.getAttribute("icon"); //$NON-NLS-1$
		if (strIcon != null) {
			fImageDescriptor = TeamImages.getImageDescriptorFromExtension(configElement.getDeclaringExtension(), strIcon);
		}

		// Help Context Id.
		fHelpContextId = configElement
				.getAttribute(SynchronizeParticipantDescriptor.ATT_HELP_CONTEXT_ID);
	}

	protected void setInitializationData(ISynchronizeParticipantDescriptor descriptor) throws CoreException {
		if(descriptor instanceof SynchronizeParticipantDescriptor) {
			setInitializationData(((SynchronizeParticipantDescriptor)descriptor).getConfigurationElement(), null, null);
		} else {
			throw new TeamException(TeamUIMessages.AbstractSynchronizeParticipant_4);
		}
	}

	/**
	 * Sets the name of this participant to the specified value and notifies
	 * property listeners of the change.
	 *
	 * @param name the new name
	 */
	protected void setName(String name) {
		String old = fName;
		fName = name;
		firePropertyChange(this, IBasicPropertyConstants.P_TEXT, old, name);
	}

	/**
	 * Sets the image descriptor for this participant to the specified value and
	 * notifies property listeners of the change.
	 *
	 * @param imageDescriptor the new image descriptor
	 */
	protected void setImageDescriptor(ImageDescriptor imageDescriptor) {
		ImageDescriptor old = fImageDescriptor;
		fImageDescriptor = imageDescriptor;
		firePropertyChange(this, IBasicPropertyConstants.P_IMAGE, old, imageDescriptor);
	}

	/**
	 * Sets the secondary id for this participant.
	 *
	 * @param secondaryId the secondary id for this participant.
	 */
	protected void setSecondaryId(String secondaryId) {
		this.fSecondaryId = secondaryId;
	}

	/**
	 * Classes that are persisted must override this method and perform
	 * the following initialization.
	 * <pre>
	 * 		super.init(secondaryId, memento);
	 * 		try {
	 *			ISynchronizeParticipantDescriptor descriptor = TeamUI.getSynchronizeManager().getParticipantDescriptor(PARTICIPANT_ID);
	 *			setInitializationData(descriptor);
	 *		} catch (CoreException e) {
	 *			TeamUIPlugin.log(e);
	 *		}
	 * </pre>
	 * where <code>PARTICIPANT_ID</code> is the id of the participant as defined in the plugin manifest.
	 * </p>
	 * @see org.eclipse.team.ui.synchronize.ISynchronizeParticipant#init(String, org.eclipse.ui.IMemento)
	 */
	@Override
	public void init(String secondaryId, IMemento memento) throws PartInitException {
		setSecondaryId(secondaryId);
		pinned = Boolean.valueOf(memento.getString(CTX_PINNED)).booleanValue();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISynchronizeParticipant#saveState(org.eclipse.ui.IMemento)
	 */
	@Override
	public void saveState(IMemento memento) {
		memento.putString(CTX_PINNED, Boolean.toString(pinned));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISynchronizeParticipant#createPageConfiguration()
	 */
	@Override
	public final ISynchronizePageConfiguration createPageConfiguration() {
		SynchronizePageConfiguration configuration = new SynchronizePageConfiguration(this);
		if (isViewerContributionsSupported()) {
		    configuration.setProperty(ISynchronizePageConfiguration.P_OBJECT_CONTRIBUTION_ID, getId());
		}
		initializeConfiguration(configuration);
		return configuration;
	}

    /**
	 * This method is invoked after a page configuration is created but before it is returned by the
	 * <code>createPageConfiguration</code> method. Subclasses can implement this method to
	 * tailor the configuration in ways appropriate to the participant.
	 *
	 * @param configuration the newly create page configuration
	 */
	protected abstract void initializeConfiguration(ISynchronizePageConfiguration configuration);

	/**
	 * Default implementation will update the labels in the given configuration using
	 * information from the provided element if it adapts to <code>SyncInfo</code>.
	 * It will also cache the contents for the remote and base if the element is
	 * sync info based.
	 * @param element the sync model element whose contents are about to be displayed to the user
	 * 		in a compare editor or compare dialog
	 * @param config the compare configuration that will be used to configure the compare editor or dialog
	 * @param monitor a progress monitor that can be used if contacting a server to prepare the element and configuration
	 * @throws TeamException if an error occurred that should prevent the display of the compare editor containing
	 * the element
	 *
	 * @since 3.1
	 * @see org.eclipse.team.ui.synchronize.ISynchronizeParticipant#prepareCompareInput(org.eclipse.team.ui.synchronize.ISynchronizeModelElement, org.eclipse.compare.CompareConfiguration, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void prepareCompareInput(ISynchronizeModelElement element, CompareConfiguration config, IProgressMonitor monitor) throws TeamException {
	    SyncInfo sync = getSyncInfo(element);
	    if (sync != null)
	        Utils.updateLabels(sync, config, monitor);
	    if (element instanceof SyncInfoModelElement) {
			SyncInfoModelElement node = (SyncInfoModelElement)element;
            (node).cacheContents(monitor);
	    }
	}

	/*
	 * Get the sync info node from the element using the adaptable mechanism.
	 * A <code>null</code> is returned if the element doesn't have a sync info
	 * @param element the sync model element
	 * @return the sync info for the element or <code>null</code>
	 */
	private SyncInfo getSyncInfo(ISynchronizeModelElement element) {
	    if (element instanceof IAdaptable) {
		    return (SyncInfo)((IAdaptable)element).getAdapter(SyncInfo.class);
	    }
	    return null;
	}

	/* (non-Javadoc)
     * @see org.eclipse.team.ui.synchronize.ISynchronizeParticipant#getPreferencePages()
     */
    @Override
	public PreferencePage[] getPreferencePages() {
        return new PreferencePage[] { new SyncViewerPreferencePage() };
    }

	/**
	 * Return whether this participant supports the contribution of actions to
	 * the context menu by contributing a <code>viewerContribution</code>
	 * to the <code>org.eclipse.ui.popupMenus</code> extension point. By default,
	 * <code>false</code> is returned. If a subclasses overrides to return <code>true</code>,
	 * the <code>id</code> of the participant is used as the <code>targetId</code>. Here is
	 * an extension that could be added to the plugin manifest to contribute an action to
	 * the context menu for a participant
	 *
	 * <pre>
	 *    &lt;extension point="org.eclipse.ui.popupMenus"&gt;
	 * 		&lt;viewerContribution
	 *             id="org.eclipse.team.cvs.ui.viewContributionId"
	 *             targetID="org.eclipse.team.cvs.ui.cvsworkspace-participant"&gt;
	 * 			&lt;action
	 *                label="Add"
	 *                menubarPath="additions"
	 *                tooltip="Add a file to CVS version control"
	 *                class="org.eclipse.team.internal.ccvs.ui.actions.AddAction"
	 *                helpContextId="org.eclipse.team.cvs.ui.workspace_subscriber_add"
	 *                id="org.eclipse.team.ccvs.ui.CVSWorkspaceSubscriber.add"&gt;
	 *          &lt;/action&gt;
	 * 		&lt;/viewerContribution&gt;
	 *   &lt;/extension&gt;
	 * </pre>
	 *
	 *
     * @return whether this participant supports the contribution of actions to
	 * the context menu using the <code>org.eclipse.ui.popupMenus</code> extension point
     * @since 3.1
     */
    protected boolean isViewerContributionsSupported() {
        return false;
    }
}
