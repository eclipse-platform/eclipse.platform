/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 * Matt McCutchen <hashproduct+eclipse@gmail.com> - Bug 128429 [Change Sets] Change Sets with / in name do not get persited
 *******************************************************************************/
package org.eclipse.team.internal.core.subscribers;

import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.diff.*;
import org.eclipse.team.core.mapping.IChangeGroupingRequestor;
import org.eclipse.team.core.mapping.IResourceDiffTree;
import org.eclipse.team.internal.core.Messages;
import org.eclipse.team.internal.core.TeamPlugin;
import org.eclipse.team.internal.core.mapping.CompoundResourceTraversal;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/**
 * A change set manager that contains sets that represent collections of
 * related local changes.
 */
public abstract class ActiveChangeSetManager extends ChangeSetManager implements IDiffChangeListener, IChangeGroupingRequestor {

    private static final String CTX_DEFAULT_SET = "defaultSet"; //$NON-NLS-1$

    private ActiveChangeSet defaultSet;

	/**
     * Return the Change Set whose sync info set is the
     * one given.
     * @param tree a diff tree
     * @return the change set for the given diff tree
     */
    protected ChangeSet getChangeSet(IResourceDiffTree tree) {
        ChangeSet[] sets = getSets();
        for (int i = 0; i < sets.length; i++) {
			ChangeSet changeSet = sets[i];
            if (((DiffChangeSet)changeSet).getDiffTree() == tree) {
                return changeSet;
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.core.subscribers.ChangeSetManager#add(org.eclipse.team.internal.core.subscribers.ChangeSet)
     */
    public void add(ChangeSet set) {
        Assert.isTrue(set instanceof ActiveChangeSet);
        super.add(set);
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.core.subscribers.AbstractChangeSetCollector#handleSetAdded(org.eclipse.team.internal.core.subscribers.ChangeSet)
     */
    protected void handleSetAdded(ChangeSet set) {
    	Assert.isTrue(set instanceof ActiveChangeSet);
    	((DiffChangeSet)set).getDiffTree().addDiffChangeListener(getDiffTreeListener());
    	super.handleSetAdded(set);
    	handleAddedResources(set, ((ActiveChangeSet)set).internalGetDiffTree().getDiffs());
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.core.subscribers.AbstractChangeSetCollector#handleSetRemoved(org.eclipse.team.internal.core.subscribers.ChangeSet)
     */
    protected void handleSetRemoved(ChangeSet set) {
    	((DiffChangeSet)set).getDiffTree().removeDiffChangeListener(getDiffTreeListener());
    	super.handleSetRemoved(set);
    }

    /**
     * Return the listener that is registered with the diff trees associated with
     * the sets for this manager.
     * @return the listener that is registered with the diff trees associated with
     * the sets for this manager
     */
    protected IDiffChangeListener getDiffTreeListener() {
        return this;
    }

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.diff.IDiffChangeListener#diffsChanged(org.eclipse.team.core.diff.IDiffChangeEvent, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void diffsChanged(IDiffChangeEvent event, IProgressMonitor monitor) {
	    IResourceDiffTree tree = (IResourceDiffTree)event.getTree();
	    handleSyncSetChange(tree, event.getAdditions(), getAllResources(event));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.diff.IDiffChangeListener#propertyChanged(org.eclipse.team.core.diff.IDiffTree, int, org.eclipse.core.runtime.IPath[])
	 */
	public void propertyChanged(IDiffTree tree, int property, IPath[] paths) {
		// ignore
	}

	public boolean isModified(IFile file) throws CoreException {
		IDiff diff = getDiff(file);
		if (diff != null)
			return isModified(diff);
		return false;
	}

	/**
	 * Return whether the given diff represents a local change.
	 * @param diff the diff
	 * @return whether the given diff represents a local change
	 */
    public boolean isModified(IDiff diff) {
        if (diff != null) {
        	if (diff instanceof IThreeWayDiff) {
				IThreeWayDiff twd = (IThreeWayDiff) diff;
				int dir = twd.getDirection();
				return dir == IThreeWayDiff.OUTGOING || dir == IThreeWayDiff.CONFLICTING;
			} else {
				return diff.getKind() != IDiff.NO_CHANGE;
			}
        }
        return false;
    }

	/**
	 * Return the set with the given name.
	 * @param name the name of the set
	 * @return the set with the given name
	 */
	public ActiveChangeSet getSet(String name) {
		ChangeSet[] sets = getSets();
		for (int i = 0; i < sets.length; i++) {
			ChangeSet set = sets[i];
			if (set.getName().equals(name) && set instanceof ActiveChangeSet) {
				return (ActiveChangeSet)set;
			}
		}
		return null;
	}

    /**
     * Create a change set containing the given files if
     * they have been modified locally.
     * @param title the title of the commit set
     * @param files the files contained in the set
     * @return the created set
     * @throws CoreException
     */
    public ActiveChangeSet createSet(String title, IFile[] files) throws CoreException {
        List infos = new ArrayList();
        for (int i = 0; i < files.length; i++) {
            IFile file = files[i];
            IDiff diff = getDiff(file);
            if (diff != null) {
                infos.add(diff);
            }
        }
        return createSet(title, (IDiff[]) infos.toArray(new IDiff[infos.size()]));
    }

    /**
     * Create a commit set with the given title and files. The created
     * set is not added to the control of the commit set manager
     * so no events are fired. The set can be added using the
     * <code>add</code> method.
     * @param title the title of the commit set
     * @param diffs the files contained in the set
     * @return the created set
     */
    public ActiveChangeSet createSet(String title, IDiff[] diffs) {
        ActiveChangeSet commitSet = doCreateSet(title);
        if (diffs != null && diffs.length > 0) {
            commitSet.add(diffs);
        }
        return commitSet;
    }

    /**
     * Create a change set with the given name.
     * @param name the name of the change set
     * @return the created change set
     */
	protected ActiveChangeSet doCreateSet(String name) {
		return new ActiveChangeSet(this, name);
	}

	public abstract IDiff getDiff(IResource resource) throws CoreException;

	/**
	 * Return whether the manager allows a resource to
	 * be in multiple sets. By default, a resource
	 * may only be in one set.
	 * @return whether the manager allows a resource to
	 * be in multiple sets.
	 */
	protected boolean isSingleSetPerResource() {
	    return true;
	}

	private IPath[] getAllResources(IDiffChangeEvent event) {
		Set allResources = new HashSet();
		IDiff[] addedResources = event.getAdditions();
		for (int i = 0; i < addedResources.length; i++) {
			IDiff diff = addedResources[i];
			allResources.add(diff.getPath());
		}
		IDiff[] changedResources = event.getChanges();
		for (int i = 0; i < changedResources.length; i++) {
			IDiff diff = changedResources[i];
			allResources.add(diff.getPath());
		}
		IPath[] removals = event.getRemovals();
		for (int i = 0; i < removals.length; i++) {
			IPath path = removals[i];
			allResources.add(path);
		}
	    return (IPath[]) allResources.toArray(new IPath[allResources.size()]);
	}

	/**
	 * React to the given diffs being added to the given set.
	 * @param set the set
	 * @param diffs the diffs
	 */
	protected void handleAddedResources(ChangeSet set, IDiff[] diffs) {
	    if (isSingleSetPerResource() && ((ActiveChangeSet)set).isUserCreated()) {
	        IResource[] resources = new IResource[diffs.length];
	        for (int i = 0; i < resources.length; i++) {
				resources[i] = ((DiffChangeSet)set).getDiffTree().getResource(diffs[i]);
			}
	        // Remove the added files from any other set that contains them
	        ChangeSet[] sets = getSets();
	        for (int i = 0; i < sets.length; i++) {
	            ChangeSet otherSet = sets[i];
				if (otherSet != set && ((ActiveChangeSet)otherSet).isUserCreated()) {
	                otherSet.remove(resources);
	            }
	        }
	    }
	}

	private void handleSyncSetChange(IResourceDiffTree tree, IDiff[] addedDiffs, IPath[] allAffectedResources) {
	    ChangeSet changeSet = getChangeSet(tree);
	    if (tree.isEmpty() && changeSet != null) {
	        remove(changeSet);
	    }
	    fireResourcesChangedEvent(changeSet, allAffectedResources);
	    handleAddedResources(changeSet, addedDiffs);
	}

	/**
	 * Make the given set the default set into which all new modifications that
	 * are not already in another set go.
	 *
	 * @param set
	 *            the set which is to become the default set or
	 *            <code>null</code> to unset the default set
	 */
	public void makeDefault(ActiveChangeSet set) {
	    // The default set must be an active set
		if (set != null && !contains(set)) {
			add(set);
		}
	    ActiveChangeSet oldSet = defaultSet;
	    defaultSet = set;
	    fireDefaultChangedEvent(oldSet, defaultSet);
	}

	/**
	 * Return whether the given set is the default set into which all
	 * new modifications will be placed.
	 * @param set the set to test
	 * @return whether the set is the default set
	 */
	public boolean isDefault(ActiveChangeSet set) {
	    return set == defaultSet;
	}

    /**
     * Return the set which is currently the default or
     * <code>null</code> if there is no default set.
     * @return the default change set
     */
    public ActiveChangeSet getDefaultSet() {
        return defaultSet;
    }

	/**
	 * If the given traversals contain any resources in the active change sets, ensure
	 * that the traversals cover all the resources in the overlapping change set.
	 * @param traversals the traversals
	 * @return the traversals adjusted to contain all the resources of intersecting change sets
	 */
	public ResourceTraversal[] adjustInputTraversals(ResourceTraversal[] traversals) {
		CompoundResourceTraversal traversal = new CompoundResourceTraversal();
		traversal.addTraversals(traversals);
		ChangeSet[] sets = getSets();
		for (int i = 0; i < sets.length; i++) {
			ChangeSet set = sets[i];
			handleIntersect(traversal, set);
		}
		return traversal.asTraversals();
	}

	private void handleIntersect(CompoundResourceTraversal traversal, ChangeSet set) {
		IResource[] resources = set.getResources();
		for (int i = 0; i < resources.length; i++) {
			IResource resource = resources[i];
			if (traversal.isCovered(resource, IResource.DEPTH_ZERO)) {
				traversal.addResources(resources, IResource.DEPTH_ZERO);
				return;
			}
		}
	}

	/**
	 * Save the state of this manager including all its contained sets
	 * into the given preferences node.
	 * @param prefs a preferences node
	 */
	protected void save(Preferences prefs) {
		// No need to save the sets if the manager has never been initialized
		if (!isInitialized())
			return;
        // Clear the persisted state before saving the new state
        try {
            String[] oldSetNames = prefs.childrenNames();
            for (int i = 0; i < oldSetNames.length; i++) {
                String string = oldSetNames[i];
                prefs.node(string).removeNode();
            }
        } catch (BackingStoreException e) {
            TeamPlugin.log(IStatus.ERROR, NLS.bind(Messages.SubscriberChangeSetCollector_5, new String[] { getName() }), e);
        }
        ChangeSet[] sets = getSets();
        for (int i = 0; i < sets.length; i++) {
            ChangeSet set = sets[i];
			if (set instanceof ActiveChangeSet && !set.isEmpty()) {
			    // Since the change set title is stored explicitly, the name of
			    // the child preference node doesn't matter as long as it
			    // doesn't contain / and no two change sets get the same name.
			    String childPrefName = escapePrefName(((ActiveChangeSet)set).getTitle());
			    Preferences child = prefs.node(childPrefName);
			    ((ActiveChangeSet)set).save(child);
			}
		}
		if (getDefaultSet() != null) {
		    prefs.put(CTX_DEFAULT_SET, getDefaultSet().getTitle());
		} else {
			// unset default changeset
			prefs.remove(CTX_DEFAULT_SET);
		}
		try {
            prefs.flush();
        } catch (BackingStoreException e) {
            TeamPlugin.log(IStatus.ERROR, NLS.bind(Messages.SubscriberChangeSetCollector_3, new String[] { getName() }), e);
        }
    }

    /**
	 * Escape the given string for safe use as a preference node name by
	 * translating / to \s (so it's a single path component) and \ to \\ (to
	 * preserve uniqueness).
	 *
	 * @param string
	 *            Input string
	 * @return Escaped output string
	 */
	private static String escapePrefName(String string) {
		StringBuffer out = new StringBuffer();
		for (int i = 0; i < string.length(); i++) {
			char c = string.charAt(i);
			switch (c) {
			case '/':
				out.append("\\s"); //$NON-NLS-1$
				break;
			case '\\':
				out.append("\\\\"); //$NON-NLS-1$
				break;
			default:
				out.append(c);
			}
		}
		return out.toString();
	}

	/**
	 * Load the manager's state from the given preferences node.
	 *
	 * @param prefs
	 *            a preferences node
	 */
	protected void load(Preferences prefs) {
		String defaultSetTitle = prefs.get(CTX_DEFAULT_SET, null);
        try {
			String[] childNames = prefs.childrenNames();
			for (int i = 0; i < childNames.length; i++) {
			    String string = childNames[i];
			    Preferences childPrefs = prefs.node(string);
			    ActiveChangeSet set = createSet(childPrefs);
			    if (!set.isEmpty()) {
			    	if (getDefaultSet() == null && defaultSetTitle != null && set.getTitle().equals(defaultSetTitle)) {
			    	    makeDefault(set);
			    	}
			    	add(set);
			    }
			}
		} catch (BackingStoreException e) {
			TeamPlugin.log(IStatus.ERROR, NLS.bind(Messages.SubscriberChangeSetCollector_4, new String[] { getName() }), e);
		}
    }

	/**
	 * Return the name of this change set manager.
	 * @return the name of this change set manager
	 */
	protected abstract String getName();

    /**
     * Create a change set from the given preferences that were
     * previously saved.
     * @param childPrefs the previously saved preferences
     * @return the created change set
     */
    protected ActiveChangeSet createSet(Preferences childPrefs) {
        // Don't specify a title when creating the change set; instead, let the
        // change set read its title from the preferences.
        ActiveChangeSet changeSet = doCreateSet(null);
        changeSet.init(childPrefs);
        return changeSet;
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.core.IChangeGroupingRequestor#ensureChangesGrouped(org.eclipse.core.resources.IProject, org.eclipse.core.resources.IFile[], java.lang.String)
     */
    public void ensureChangesGrouped(IProject project, IFile[] files,
    		String name) throws CoreException {
		ActiveChangeSet set = getSet(name);
		if (set == null) {
			set = createSet(name, files);
			set.setUserCreated(false);
			add(set);
		} else {
			set.setUserCreated(false);
			set.add(files);
		}
    }
}
