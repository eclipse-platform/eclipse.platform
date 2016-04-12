/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize.actions;

import java.lang.reflect.InvocationTargetException;
import java.text.Collator;
import java.util.*;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Control;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.synchronize.*;
import org.eclipse.team.core.synchronize.FastSyncInfoFilter.SyncInfoDirectionFilter;
import org.eclipse.team.internal.core.subscribers.*;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.synchronize.*;
import org.eclipse.team.ui.synchronize.*;
import org.eclipse.ui.actions.BaseSelectionListenerAction;

/**
 * This action group contributes actions that support the management
 * of Change sets to a synchronize page.
 *
 * @since 3.1
 */
public class ChangeSetActionGroup extends SynchronizePageActionGroup {

    /**
     * Menu group that can be added to the context menu
     */
    public final static String CHANGE_SET_GROUP = "change_set_group"; //$NON-NLS-1$

	// Constants for persisting sorting options
	private static final String P_LAST_COMMENTSORT = TeamUIPlugin.ID + ".P_LAST_COMMENT_SORT"; //$NON-NLS-1$

    public static final FastSyncInfoFilter OUTGOING_RESOURCE_FILTER = new SyncInfoDirectionFilter(
            new int[] { SyncInfo.OUTGOING, SyncInfo.CONFLICTING });

	private class CreateChangeSetAction extends SynchronizeModelAction {

        public CreateChangeSetAction(ISynchronizePageConfiguration configuration) {
            super(TeamUIMessages.ChangeLogModelProvider_0, configuration);
        }

		/* (non-Javadoc)
		 * @see org.eclipse.team.ui.synchronize.SynchronizeModelAction#needsToSaveDirtyEditors()
		 */
		@Override
		protected boolean needsToSaveDirtyEditors() {
			return false;
		}

        /* (non-Javadoc)
         * @see org.eclipse.team.ui.synchronize.SynchronizeModelAction#getSyncInfoFilter()
         */
        @Override
		protected FastSyncInfoFilter getSyncInfoFilter() {
            return OUTGOING_RESOURCE_FILTER;
        }

        /* (non-Javadoc)
         * @see org.eclipse.team.ui.synchronize.SynchronizeModelAction#getSubscriberOperation(org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration, org.eclipse.compare.structuremergeviewer.IDiffElement[])
         */
        @Override
		protected SynchronizeModelOperation getSubscriberOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
            return new SynchronizeModelOperation(configuration, elements) {
                @Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    syncExec(new Runnable() {
                        @Override
						public void run() {
		                    ActiveChangeSet set = createChangeSet(getDiffs(getSyncInfoSet().getResources()));
		                    if (set != null) {
		                        getActiveChangeSetManager().add(set);
		                    }
                        }
                    });
                }
            };
        }
	}

	private abstract class ChangeSetAction extends BaseSelectionListenerAction {

        public ChangeSetAction(String title, ISynchronizePageConfiguration configuration) {
            super(title);
        }

        /* (non-Javadoc)
         * @see org.eclipse.ui.actions.BaseSelectionListenerAction#updateSelection(org.eclipse.jface.viewers.IStructuredSelection)
         */
        @Override
		protected boolean updateSelection(IStructuredSelection selection) {
            return getSelectedSet() != null;
        }

        protected ActiveChangeSet getSelectedSet() {
            IStructuredSelection selection = getStructuredSelection();
            if (selection.size() == 1) {
                Object first = selection.getFirstElement();
                if (first instanceof IAdaptable) {
	                Object adapter = ((IAdaptable)first).getAdapter(ChangeSet.class);
	                if (adapter instanceof ActiveChangeSet) {
	                    return (ActiveChangeSet)adapter;
	                }
                }
            }
            return null;
        }
	}

	private class EditChangeSetAction extends ChangeSetAction {

        public EditChangeSetAction(ISynchronizePageConfiguration configuration) {
            super(TeamUIMessages.ChangeLogModelProvider_6, configuration);
        }

        @Override
		public void run() {
            ActiveChangeSet set = getSelectedSet();
            if (set == null) return;
    		editChangeSet(set);
        }
	}

	private class RemoveChangeSetAction extends ChangeSetAction {

        public RemoveChangeSetAction(ISynchronizePageConfiguration configuration) {
            super(TeamUIMessages.ChangeLogModelProvider_7, configuration);
        }

        @Override
		public void run() {
            ActiveChangeSet set = getSelectedSet();
            if (set == null) return;
            if (MessageDialog.openConfirm(getConfiguration().getSite().getShell(), TeamUIMessages.ChangeSetActionGroup_0, NLS.bind(TeamUIMessages.ChangeSetActionGroup_1, new String[] { LegacyActionTools.escapeMnemonics(set.getTitle()) }))) { //
                getActiveChangeSetManager().remove(set);
            }
        }
	}

	private class MakeDefaultChangeSetAction extends ChangeSetAction {

		public MakeDefaultChangeSetAction(
				ISynchronizePageConfiguration configuration) {
			super(TeamUIMessages.ChangeLogModelProvider_9, configuration);
		}

		@Override
		protected boolean updateSelection(IStructuredSelection selection) {
			if (getSelectedSet() != null) {
				setText(TeamUIMessages.ChangeLogModelProvider_9);
				setChecked(getSelectedSet().equals(
						getActiveChangeSetManager().getDefaultSet()));
			} else {
				setText(TeamUIMessages.ChangeLogModelProvider_10);
				setChecked(false);
			}
			return true;
		}

		@Override
		public void run() {
			getActiveChangeSetManager().makeDefault(
					isChecked() ? getSelectedSet() : null);
			if (getSelectedSet() == null) {
				setChecked(false); // keep unchecked
			}
		}

	}

	private class AddToChangeSetAction extends SynchronizeModelAction {

        private final ActiveChangeSet set;

        public AddToChangeSetAction(ISynchronizePageConfiguration configuration, ActiveChangeSet set, ISelection selection) {
            super(set == null ? TeamUIMessages.ChangeSetActionGroup_2 : LegacyActionTools.escapeMnemonics(set.getTitle()), configuration);
            this.set = set;
            selectionChanged(selection);
        }

        /* (non-Javadoc)
         * @see org.eclipse.team.ui.synchronize.SynchronizeModelAction#getSyncInfoFilter()
         */
        @Override
		protected FastSyncInfoFilter getSyncInfoFilter() {
            return OUTGOING_RESOURCE_FILTER;
        }

		@Override
		protected boolean needsToSaveDirtyEditors() {
			return false;
		}

        /* (non-Javadoc)
         * @see org.eclipse.team.ui.synchronize.SynchronizeModelAction#getSubscriberOperation(org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration, org.eclipse.compare.structuremergeviewer.IDiffElement[])
         */
        @Override
		protected SynchronizeModelOperation getSubscriberOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
            return new SynchronizeModelOperation(configuration, elements) {
                @Override
				public void run(IProgressMonitor monitor)
                        throws InvocationTargetException, InterruptedException {
                	IResource[] resources = getSyncInfoSet().getResources();
                    if (set != null) {
                    	IDiff[] diffArray = getDiffs(resources);
						set.add(diffArray);
                    } else {
                        ChangeSet[] sets = getActiveChangeSetManager().getSets();
                        for (int i = 0; i < sets.length; i++) {
                            ActiveChangeSet activeSet = (ActiveChangeSet)sets[i];
							activeSet.remove(resources);
                        }
                    }
                }
            };
        }
	}

	/* *****************************************************************************
	 * Action that allows changing the model providers sort order.
	 */
	private class ToggleSortOrderAction extends Action {
		private int criteria;
		protected ToggleSortOrderAction(String name, int criteria) {
			super(name, IAction.AS_RADIO_BUTTON);
			this.criteria = criteria;
			update();
		}

		@Override
		public void run() {
			if (isChecked() && sortCriteria != criteria) {
			    sortCriteria = criteria;
				String key = getSettingsKey();
				IDialogSettings pageSettings = getConfiguration().getSite().getPageSettings();
				if(pageSettings != null) {
					pageSettings.put(key, criteria);
				}
				update();
				provider.setViewerSorter(getViewerSorter());
			}
		}

		public void update() {
		    setChecked(criteria == sortCriteria);
		}

		protected String getSettingsKey() {
		    return P_LAST_COMMENTSORT;
		}
	}

	/*
	 * The model provider for this action group
	 */
	private ChangeSetModelProvider provider;

	/*
	 * The actions created by this group
	 */
	private MenuManager sortByComment;
	private CreateChangeSetAction createChangeSet;
	private MenuManager addToChangeSet;
    private EditChangeSetAction editChangeSet;
    private RemoveChangeSetAction removeChangeSet;
    private MakeDefaultChangeSetAction makeDefault;

    private SynchronizePageActionGroup subActions;

    /*
     * The currently chosen sort criteria
     */
    private int sortCriteria = ChangeSetModelSorter.DATE;

    public static int getSortCriteria(ISynchronizePageConfiguration configuration) {
        int sortCriteria = ChangeSetModelSorter.DATE;
		try {
			IDialogSettings pageSettings = configuration.getSite().getPageSettings();
			if(pageSettings != null) {
				sortCriteria = pageSettings.getInt(P_LAST_COMMENTSORT);
			}
		} catch(NumberFormatException e) {
			// ignore and use the defaults.
		}
		switch (sortCriteria) {
        case ChangeSetModelSorter.COMMENT:
        case ChangeSetModelSorter.DATE:
        case ChangeSetModelSorter.USER:
            break;
        default:
            sortCriteria = ChangeSetModelSorter.DATE;
            break;
        }
		return sortCriteria;
    }

    public ChangeSetActionGroup(ChangeSetModelProvider provider) {
        this.provider = provider;
    }

	@Override
	public void initialize(ISynchronizePageConfiguration configuration) {
		super.initialize(configuration);

		if (getChangeSetCapability().supportsCheckedInChangeSets()) {
		    sortCriteria = getSortCriteria(configuration);
			sortByComment = new MenuManager(TeamUIMessages.ChangeLogModelProvider_0a);
			sortByComment.add(new ToggleSortOrderAction(TeamUIMessages.ChangeLogModelProvider_1a, ChangeSetModelSorter.COMMENT));
			sortByComment.add(new ToggleSortOrderAction(TeamUIMessages.ChangeLogModelProvider_2a, ChangeSetModelSorter.DATE));
			sortByComment.add(new ToggleSortOrderAction(TeamUIMessages.ChangeLogModelProvider_3a, ChangeSetModelSorter.USER));
		}

		if (getChangeSetCapability().supportsActiveChangeSets()) {
			addToChangeSet = new MenuManager(TeamUIMessages.ChangeLogModelProvider_12);
			addToChangeSet.setRemoveAllWhenShown(true);
			addToChangeSet.addMenuListener(new IMenuListener() {
	            @Override
				public void menuAboutToShow(IMenuManager manager) {
	                addChangeSets(manager);
	            }
	        });
			createChangeSet = new CreateChangeSetAction(configuration);
			addToChangeSet.add(createChangeSet);
			addToChangeSet.add(new Separator());
			editChangeSet = new EditChangeSetAction(configuration);
			makeDefault = new MakeDefaultChangeSetAction(configuration);
			removeChangeSet = new RemoveChangeSetAction(configuration);
		}

		subActions = getChangeSetCapability().getActionGroup();
		if (subActions != null) {
		    subActions.initialize(configuration);
		}
	}

	/* (non-Javadoc)
     * @see org.eclipse.team.ui.synchronize.SynchronizePageActionGroup#fillContextMenu(org.eclipse.jface.action.IMenuManager)
     */
    @Override
	public void fillContextMenu(IMenuManager menu) {
        if (getChangeSetCapability().enableCheckedInChangeSetsFor(getConfiguration())) {
            appendToGroup(menu, ISynchronizePageConfiguration.SORT_GROUP, sortByComment);
        }
        if (getChangeSetCapability().enableActiveChangeSetsFor(getConfiguration())) {
			appendToGroup(
					menu,
					CHANGE_SET_GROUP,
					addToChangeSet);
			appendToGroup(
					menu,
					CHANGE_SET_GROUP,
					editChangeSet);
			appendToGroup(
					menu,
					CHANGE_SET_GROUP,
					removeChangeSet);
			appendToGroup(
					menu,
					CHANGE_SET_GROUP,
					makeDefault);
        }
		if (subActions != null) {
		    subActions.fillContextMenu(menu);
		}
    }

    protected void addChangeSets(IMenuManager manager) {
        ChangeSet[] sets = getActiveChangeSetManager().getSets();
        Arrays.sort(sets, new Comparator() {
        	private Collator collator = Collator.getInstance();
        	@Override
			public int compare(Object o1, Object o2) {
        		return collator.compare(((ChangeSet) o1).getName(), ((ChangeSet) o2).getName());
        	}
        });
        ISelection selection = getContext().getSelection();
        createChangeSet.selectionChanged(selection);
		addToChangeSet.add(createChangeSet);
		addToChangeSet.add(new Separator());
        for (int i = 0; i < sets.length; i++) {
            ActiveChangeSet set = (ActiveChangeSet)sets[i];
            AddToChangeSetAction action = new AddToChangeSetAction(getConfiguration(), set, selection);
            manager.add(action);
        }
        addToChangeSet.add(new Separator());
        // Action that removes change set resources
        addToChangeSet.add(new AddToChangeSetAction(getConfiguration(), null, selection));
    }

    /**
     * Return the change set manager for the current page.
     * @return the change set manager for the current page
     */
    protected ActiveChangeSetManager getActiveChangeSetManager() {
        return getChangeSetCapability().getActiveChangeSetManager();
    }

    /* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.SynchronizePageActionGroup#dispose()
	 */
	@Override
	public void dispose() {
	    if (addToChangeSet != null) {
			addToChangeSet.dispose();
			addToChangeSet.removeAll();
	    }
	    if (sortByComment != null) {
			sortByComment.dispose();
			sortByComment.removeAll();
	    }
	    if (subActions != null) {
	        subActions.dispose();
	    }
		super.dispose();
	}


    @Override
	public void updateActionBars() {
        if (editChangeSet != null)
	        editChangeSet.selectionChanged((IStructuredSelection)getContext().getSelection());
        if (removeChangeSet != null)
            removeChangeSet.selectionChanged((IStructuredSelection)getContext().getSelection());
        if (makeDefault != null)
	        makeDefault.selectionChanged((IStructuredSelection)getContext().getSelection());
        super.updateActionBars();
    }

    private void syncExec(final Runnable runnable) {
		final Control ctrl = getConfiguration().getPage().getViewer().getControl();
		if (ctrl != null && !ctrl.isDisposed()) {
			ctrl.getDisplay().syncExec(new Runnable() {
				@Override
				public void run() {
					if (!ctrl.isDisposed()) {
					    runnable.run();
					}
				}
			});
		}
    }

    /**
     * Return a viewer sorter that utilizes the sort criteria
     * selected by the user.
     * @return a sorter
     */
	public ViewerSorter getViewerSorter() {
		return new ChangeSetModelSorter(provider, sortCriteria);
	}

    private ActiveChangeSet createChangeSet(IDiff[] diffs) {
        return getChangeSetCapability().createChangeSet(getConfiguration(), diffs);
    }

    private void editChangeSet(ActiveChangeSet set) {
        getChangeSetCapability().editChangeSet(getConfiguration(), set);
    }

    private ChangeSetCapability getChangeSetCapability() {
        return provider.getChangeSetCapability();
    }

	private IDiff[] getDiffs(IResource[] resources) {
		List diffs = new ArrayList();
		Subscriber s = ((SubscriberParticipant)getConfiguration().getParticipant()).getSubscriber();
		for (int i = 0; i < resources.length; i++) {
			IResource resource = resources[i];
			try {
				IDiff diff = s.getDiff(resource);
				if (diff != null)
					diffs.add(diff);
			} catch (CoreException e) {
				TeamUIPlugin.log(e);
			}
		}
		IDiff[] diffArray = (IDiff[]) diffs
				.toArray(new IDiff[diffs.size()]);
		return diffArray;
	}
}
