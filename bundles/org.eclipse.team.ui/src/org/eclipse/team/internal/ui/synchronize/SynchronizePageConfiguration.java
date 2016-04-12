/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.IThreeWayDiff;
import org.eclipse.team.core.mapping.IResourceDiff;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.mapping.CommonViewerAdvisor;
import org.eclipse.team.ui.synchronize.*;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.actions.ActionContext;

/**
 * Concrete implementation of the ISynchronizePageConfiguration. It 
 * extends SynchronizePageActionGroup in order to delegate action group
 * operations.
 * 
 * @since 3.0
 */
public class SynchronizePageConfiguration extends SynchronizePageActionGroup implements ISynchronizePageConfiguration {

	/**
	 * Property constant for the page's viewer input which is 
	 * an instance of <code>ISynchronizeModelElement</code>.
	 * This property can be queried by clients but should not be
	 * set.
	 */
	public static final String P_MODEL = TeamUIPlugin.ID  + ".P_MODEL"; //$NON-NLS-1$
	
	/**
	 * Property constant for the page's viewer advisor which is 
	 * an instance of <code>StructuredViewerAdvisor</code>.
	 * The page's viewer can be obtained from the advisor.
	 * This property can be queried by clients but should not be
	 * set.
	 */
	public static final String P_ADVISOR = TeamUIPlugin.ID  + ".P_ADVISOR"; //$NON-NLS-1$
	
	/**
	 * Property constant for the page's navigator
	 * an instance of <code>INavigable</code>.
	 * This property can be queried by clients and can be set. By default
	 * the advisors navigator will be used.
	 */
	public static final String P_NAVIGATOR = TeamUIPlugin.ID  + ".P_NAVIGATOR"; //$NON-NLS-1$
	
	/**
	 * Property constant for the compare editor inputs navigator
	 * an instance of <code>INavigable</code>.
	 * This property can be queried by clients and can be set.
	 */
	public static final String P_INPUT_NAVIGATOR = TeamUIPlugin.ID  + ".P_INPUT_NAVIGATOR"; //$NON-NLS-1$
	
	/**
	 * Property constant for the page's model  manager which is 
	 * an instance of <code>SynchronizeModelManager</code>.
	 * This property can be queried by clients but should not be
	 * set.
	 */
	public static final String P_MODEL_MANAGER = TeamUIPlugin.ID  + ".P_MODEL_MANAGER"; //$NON-NLS-1$

	/**
	 * Property that gives access to a set the
	 * contains all out-of-sync resources for the participant
	 * in the selected working set.
	 */
	public static final String P_WORKING_SET_SYNC_INFO_SET = TeamUIPlugin.ID + ".P_WORKING_SET_SYNC_INFO_SET"; //$NON-NLS-1$

	/**
	 * Property that gives access to a set the
	 * contains all out-of-sync resources for the participant
	 * before any filtering (working set or modes) is applied.
	 */
	public static final String P_PARTICIPANT_SYNC_INFO_SET = TeamUIPlugin.ID + ".P_PARTICIPANT_SYNC_INFO_SET"; //$NON-NLS-1$

	/**
	 * The hidden configuration property that opens the current selection in the
	 * page. The registered <code>IAction</code> is invoked on a single or
	 * double click depending on the open strategy chosen by the user.
	 */
	public static final String P_OPEN_ACTION = TeamUIPlugin.ID + ".P_OPEN_ACTION"; //$NON-NLS-1$

	/**
	 * Property constant for the style of the view to be used by the page.
	 */
	public static final String P_VIEWER_STYLE = TeamUIPlugin.ID + ".P_VIEWER_STYLE"; //$NON-NLS-1$

	public static final int CHECKBOX = TreeViewerAdvisor.CHECKBOX;
	
    // State flags
    private static final int UNINITIALIZED = 0;
    private static final int INITIALIZED = 1;
    private static final int DISPOSED = 2;
    
	private ISynchronizeParticipant participant;
	private ISynchronizePageSite site;
	private ListenerList propertyChangeListeners = new ListenerList(ListenerList.IDENTITY);
	private ListenerList actionContributions = new ListenerList(ListenerList.IDENTITY);
	private Map properties = new HashMap();
	private int actionState = UNINITIALIZED;
	private ISynchronizePage page;
	private IRunnableContext context;
	
	/**
	 * Create a configuration for creating a page from the given participant.
	 * @param participant the participant whose page is being configured
	 */
	public SynchronizePageConfiguration(ISynchronizeParticipant participant) {
		this.participant = participant;
		setProperty(P_CONTEXT_MENU, DEFAULT_CONTEXT_MENU);
		setProperty(P_TOOLBAR_MENU, DEFAULT_TOOLBAR_MENU);
		setProperty(P_VIEW_MENU, DEFAULT_VIEW_MENU);
		setProperty(P_COMPARISON_TYPE, THREE_WAY);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration#getParticipant()
	 */
	@Override
	public ISynchronizeParticipant getParticipant() {
		return participant;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration#getSite()
	 */
	@Override
	public ISynchronizePageSite getSite() {
		return site;
	}
	
	/**
	 * Set the site that is associated with the page that was 
	 * configured using this configuration.
	 * @param site a synchronize page site
	 */
	public void setSite(ISynchronizePageSite site) {
		this.site = site;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration#addPropertyChangeListener(org.eclipse.jface.util.IPropertyChangeListener)
	 */
	@Override
	public void addPropertyChangeListener(IPropertyChangeListener listener) {
		synchronized(propertyChangeListeners) {
			propertyChangeListeners.add(listener);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration#removePropertyChangeListener(org.eclipse.jface.util.IPropertyChangeListener)
	 */
	@Override
	public void removePropertyChangeListener(IPropertyChangeListener listener) {
		synchronized(propertyChangeListeners) {
			propertyChangeListeners.remove(listener);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration#setProperty(java.lang.String, java.lang.Object)
	 */
	@Override
	public void setProperty(String key, Object newValue) {
		Object oldValue = properties.get(key);
		if (page == null || page.aboutToChangeProperty(this, key, newValue)) {
			properties.put(key, newValue);
			if (oldValue == null || !oldValue.equals(newValue))
				firePropertyChange(key, oldValue, newValue);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration#getProperty(java.lang.String)
	 */
	@Override
	public Object getProperty(String key) {
		return properties.get(key);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration#addActionContribution(org.eclipse.team.ui.synchronize.IActionContribution)
	 */
	@Override
	public void addActionContribution(SynchronizePageActionGroup contribution) {
        int currentActionState;
		synchronized(actionContributions) {
            // Determine the action state while locked so we handle the addition properly below
            currentActionState = actionState;
            if (currentActionState != DISPOSED)
                actionContributions.add(contribution);
		}
		if (currentActionState == INITIALIZED) {
            // This is tricky because we are doing the initialize while not locked.
            // It is possible that another thread is concurrently disposing the contributions
            // but we can't lock while calling client code. We'll change for DISPOSE after
            // we initialize and, if we are disposed, we dispose this one, just in case.
			contribution.initialize(this);
            if (actionState == DISPOSED) {
                contribution .dispose();
            }
		} else if (currentActionState == DISPOSED) {
            contribution.dispose();
        }
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration#removeActionContribution(org.eclipse.team.ui.synchronize.IActionContribution)
	 */
	@Override
	public void removeActionContribution(SynchronizePageActionGroup contribution) {
		synchronized(actionContributions) {
			actionContributions.remove(contribution);
		}
	}
	
	private void firePropertyChange(String key, Object oldValue, Object newValue) {
		Object[] listeners;
		synchronized(propertyChangeListeners) {
			listeners = propertyChangeListeners.getListeners();
		}
		final PropertyChangeEvent event = new PropertyChangeEvent(this, key, oldValue, newValue);
		for (int i = 0; i < listeners.length; i++) {
			final IPropertyChangeListener listener = (IPropertyChangeListener)listeners[i];
			SafeRunner.run(new ISafeRunnable() {
				@Override
				public void handleException(Throwable exception) {
					// Error is logged by platform
				}
				@Override
				public void run() throws Exception {
					listener.propertyChange(event);
				}
			});
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.IActionContribution#initialize(org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration)
	 */
	@Override
	public void initialize(final ISynchronizePageConfiguration configuration) {
		super.initialize(configuration);
        // need to synchronize here to ensure that actions that are added concurrently also get initialized
        final Object[] listeners;
        synchronized(actionContributions) {
            if (actionState != UNINITIALIZED) {
                // Initialization has already taken place so just return.
                return;
            }
            actionState = INITIALIZED;
            listeners = actionContributions.getListeners();
        }
		for (int i= 0; i < listeners.length; i++) {
			final SynchronizePageActionGroup contribution = (SynchronizePageActionGroup)listeners[i];
			SafeRunner.run(new ISafeRunnable() {
				@Override
				public void handleException(Throwable exception) {
					// Logged by Platform
				}
				@Override
				public void run() throws Exception {
					contribution.initialize(configuration);
				}
			});
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.actions.ActionGroup#setContext(org.eclipse.ui.actions.ActionContext)
	 */
	@Override
	public void setContext(final ActionContext context) {
		super.setContext(context);
        final Object[] listeners;
        synchronized(actionContributions) {
            listeners = actionContributions.getListeners();
        }
		for (int i= 0; i < listeners.length; i++) {
			final SynchronizePageActionGroup contribution = (SynchronizePageActionGroup)listeners[i];
			SafeRunner.run(new ISafeRunnable() {
				@Override
				public void handleException(Throwable exception) {
					// Logged by Platform
				}
				@Override
				public void run() throws Exception {
					contribution.setContext(context);
				}
			});
		}
	}
	
	/**
	 * Callback invoked from the advisor each time the context menu is
	 * about to be shown.
	 * @param manager the context menu manager
	 */
	@Override
	public void fillContextMenu(final IMenuManager manager) {
        final Object[] listeners;
        synchronized(actionContributions) {
            listeners = actionContributions.getListeners();
        }
		for (int i= 0; i < listeners.length; i++) {
			final SynchronizePageActionGroup contribution = (SynchronizePageActionGroup)listeners[i];
			SafeRunner.run(new ISafeRunnable() {
				@Override
				public void handleException(Throwable exception) {
					// Logged by Platform
				}
				@Override
				public void run() throws Exception {
					contribution.fillContextMenu(manager);
				}
			});
		}
	}

	/**
	 * Callback invoked from the page to fill the action bars.
	 * @param actionBars the action bars of the view
	 */
	@Override
	public void fillActionBars(final IActionBars actionBars) {
		if (actionState == UNINITIALIZED) {
			initialize(this);
		}
        final Object[] listeners;
        synchronized(actionContributions) {
            listeners = actionContributions.getListeners();
        }
		for (int i= 0; i < listeners.length; i++) {
			final SynchronizePageActionGroup contribution = (SynchronizePageActionGroup)listeners[i];
			SafeRunner.run(new ISafeRunnable() {
				@Override
				public void handleException(Throwable exception) {
					// Logged by Platform
				}
				@Override
				public void run() throws Exception {
					contribution.fillActionBars(actionBars);
				}
			});
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.actions.ActionGroup#updateActionBars()
	 */
	@Override
	public void updateActionBars() {
        final Object[] listeners;
        synchronized(actionContributions) {
            listeners = actionContributions.getListeners();
        }
		for (int i= 0; i < listeners.length; i++) {
			final SynchronizePageActionGroup contribution = (SynchronizePageActionGroup)listeners[i];
			SafeRunner.run(new ISafeRunnable() {
				@Override
				public void handleException(Throwable exception) {
					// Logged by Platform
				}
				@Override
				public void run() throws Exception {
					contribution.updateActionBars();
				}
			});
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.SynchronizePageActionGroup#modelChanged(org.eclipse.team.ui.synchronize.ISynchronizeModelElement)
	 */
	@Override
	public void modelChanged(final ISynchronizeModelElement root) {
        final Object[] listeners;
        synchronized(actionContributions) {
            listeners = actionContributions.getListeners();
        }
		for (int i= 0; i < listeners.length; i++) {
			final SynchronizePageActionGroup contribution = (SynchronizePageActionGroup)listeners[i];
			SafeRunner.run(new ISafeRunnable() {
				@Override
				public void handleException(Throwable exception) {
					// Logged by Platform
				}
				@Override
				public void run() throws Exception {
					contribution.modelChanged(root);
				}
			});
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.IActionContribution#dispose()
	 */
	@Override
	public void dispose() {
		super.dispose();
        final Object[] listeners;
        synchronized(actionContributions) {
            listeners = actionContributions.getListeners();
            actionState = DISPOSED;
        }
		for (int i= 0; i < listeners.length; i++) {
			final SynchronizePageActionGroup contribution = (SynchronizePageActionGroup)listeners[i];
			SafeRunner.run(new ISafeRunnable() {
				@Override
				public void handleException(Throwable exception) {
					// Logged by Platform
				}
				@Override
				public void run() throws Exception {
					contribution.dispose();
				}
			});
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration#setMenu(java.lang.String, java.lang.String[])
	 */
	@Override
	public void setMenuGroups(String menuPropertyId, String[] groups) {
		setProperty(menuPropertyId, groups);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration#appendMenu(java.lang.String, java.lang.String)
	 */
	@Override
	public void addMenuGroup(String menuPropertyId, String groupId) {
		String[] menuGroups = (String[])getProperty(menuPropertyId);
		if (menuGroups == null) {
			menuGroups = getDefault(menuPropertyId);
		}
		String[] newGroups = new String[menuGroups.length + 1];
		System.arraycopy(menuGroups, 0, newGroups, 0, menuGroups.length);
		newGroups[menuGroups.length] = groupId;
		setProperty(menuPropertyId, newGroups);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration#hasMenuGroup(java.lang.String, java.lang.String)
	 */
	@Override
	public boolean hasMenuGroup(String menuPropertyId, String groupId) {
		String[] groups = (String[])getProperty(menuPropertyId);
		if (groups == null) {
			groups = getDefault(menuPropertyId);
		}
		for (int i = 0; i < groups.length; i++) {
			String string = groups[i];
			if (string.equals(groupId)) return true;
		}
		return false;
	}
	
	protected String[] getDefault(String menuPropertyId) {
		if (menuPropertyId.equals(P_CONTEXT_MENU)) {
			return DEFAULT_CONTEXT_MENU;
		} else if (menuPropertyId.equals(P_VIEW_MENU)) {
			return DEFAULT_VIEW_MENU;
		} else if (menuPropertyId.equals(P_TOOLBAR_MENU)) {
			return DEFAULT_TOOLBAR_MENU;
		} else {
			return new String[0];
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration#addLabelDecorator(org.eclipse.jface.viewers.ILabelDecorator)
	 */
	@Override
	public void addLabelDecorator(ILabelDecorator decorator) {
		ILabelDecorator[] decorators = (ILabelDecorator[])getProperty(P_LABEL_DECORATORS);
		if (decorators == null) {
			decorators = new ILabelDecorator[0];
		}
		// Ensure we don't have it registered already
		for (int i = 0; i < decorators.length; i++) {
			ILabelDecorator d = decorators[i];
			if (d == decorator) {
				return;
			}
		}
		ILabelDecorator[] newDecorators = new ILabelDecorator[decorators.length + 1];
		System.arraycopy(decorators, 0, newDecorators, 0, decorators.length);
		newDecorators[decorators.length] = decorator;
		setProperty(P_LABEL_DECORATORS, newDecorators);
	}

	/**
	 * @param group
	 * @return the group id
	 */
	public String getGroupId(String group) {
		String id = getParticipant().getId();
		if (getParticipant().getSecondaryId() != null) {
			id += "."; //$NON-NLS-1$
			id += getParticipant().getSecondaryId();
		}
		return id + "." + group; //$NON-NLS-1$
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.subscribers.ISubscriberPageConfiguration#getMode()
	 */
	@Override
	public int getMode() {
		Object o = getProperty(P_MODE);
		if (o instanceof Integer) {
			return ((Integer)o).intValue();
		}
		return 0;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.subscribers.ISubscriberPageConfiguration#setMode(int)
	 */
	@Override
	public void setMode(int mode) {
		if (isModeSupported(mode))
			setProperty(P_MODE, new Integer(mode));
	}

	public boolean isModeSupported(int mode) {
		return (getSupportedModes() & mode) > 0;
	}

	@Override
	public int getSupportedModes() {
		Object o = getProperty(P_SUPPORTED_MODES);
		if (o instanceof Integer) {
			return ((Integer)o).intValue();
		}
		return 0;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.subscribers.ISubscriberPageConfiguration#setSupportedModes(int)
	 */
	@Override
	public void setSupportedModes(int modes) {
		setProperty(P_SUPPORTED_MODES, new Integer(modes));
	}
	
	/**
	 * @return Returns the page.
	 */
	@Override
	public ISynchronizePage getPage() {
		return page;
	}
	/**
	 * @param page The page to set.
	 */
	@Override
	public void setPage(ISynchronizePage page) {
		this.page = page;
	}

	/**
	 * @return the viewer style
	 */
	public int getViewerStyle() {
		Object o = getProperty(P_VIEWER_STYLE);
		if (o instanceof Integer) {
			return ((Integer)o).intValue();
		}
		return 0;
	}

	/**
	 * @param style
	 */
	public void setViewerStyle(int style) {
		setProperty(P_VIEWER_STYLE, new Integer(style));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration#getSyncInfoSet()
	 */
	@Override
	public SyncInfoSet getSyncInfoSet() {
		Object o = getProperty(P_SYNC_INFO_SET);
		if (o instanceof SyncInfoSet) {
			return (SyncInfoSet)o;
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration#getComparisonType()
	 */
	@Override
	public String getComparisonType() {
		return (String)getProperty(P_COMPARISON_TYPE);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration#setComparisonType(java.lang.String)
	 */
	@Override
	public void setComparisonType(String type) {
		setProperty(P_COMPARISON_TYPE,type);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration#setRunnableContext(org.eclipse.jface.operation.IRunnableContext)
	 */
	@Override
	public void setRunnableContext(IRunnableContext context) {
		this.context = context;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration#getRunnableContext()
	 */
	@Override
	public IRunnableContext getRunnableContext() {
		return context;
	}

	@Override
	public String getViewerId() {
		String viewerId = (String)getProperty(P_VIEWER_ID);
		if (viewerId != null)
			return viewerId;
		return CommonViewerAdvisor.TEAM_NAVIGATOR_CONTENT;
	}
	
	/**
	 * Return whether the given node is visible in the page based
	 * on the mode in the configuration.
	 * @param node a diff node
	 * @return whether the given node is visible in the page
	 */
	public boolean isVisible(IDiff node) {
		if (getComparisonType() == ISynchronizePageConfiguration.THREE_WAY 
				&& node instanceof IThreeWayDiff) {
			IThreeWayDiff twd = (IThreeWayDiff) node;
			return includeDirection(twd.getDirection());
		}
		return getComparisonType() == ISynchronizePageConfiguration.TWO_WAY && node instanceof IResourceDiff;
	}
	
	/**
	 * Return whether elements with the given direction should be included in
	 * the contents.
	 * 
	 * @param direction
	 *            the synchronization direction
	 * @return whether elements with the given synchronization kind should be
	 *         included in the contents
	 */
	public boolean includeDirection(int direction) {
		int mode = getMode();
		switch (mode) {
		case ISynchronizePageConfiguration.BOTH_MODE:
			return true;
		case ISynchronizePageConfiguration.CONFLICTING_MODE:
			return direction == IThreeWayDiff.CONFLICTING;
		case ISynchronizePageConfiguration.INCOMING_MODE:
			return direction == IThreeWayDiff.CONFLICTING || direction == IThreeWayDiff.INCOMING;
		case ISynchronizePageConfiguration.OUTGOING_MODE:
			return direction == IThreeWayDiff.CONFLICTING || direction == IThreeWayDiff.OUTGOING;
		default:
			break;
		}
		return true;
	}
	
	public ILabelDecorator getLabelDecorator() {
		ILabelDecorator[] decorators = (ILabelDecorator[])getProperty(ISynchronizePageConfiguration.P_LABEL_DECORATORS);
		if (decorators == null) {
			return null;
		}
		return new MultiLabelDecorator(decorators);
	}
}
