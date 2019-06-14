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
package org.eclipse.team.ui.synchronize;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;

/**
 * A synchronize scope whose roots are defined by a working set.
 *
 * @since 3.0
 * @noextend This class is not intended to be subclassed by clients.
 */
public class WorkingSetScope extends AbstractSynchronizeScope implements IPropertyChangeListener {

	/*
	 * Constants used to save and restore this scope
	 */
	private final static String CTX_SETS = "workingset_scope_sets"; //$NON-NLS-1$
	private final static String CTX_SET_NAME = "workingset_scope_name"; //$NON-NLS-1$

	/*
	 * The working sets associated with this scope
	 */
	private IWorkingSet[] sets;

	/**
	 * Create the scope for the working sets
	 *
	 * @param sets the working sets that defines this scope
	 */
	public WorkingSetScope(IWorkingSet[] sets) {
		setWorkingSets(sets);
	}

	/**
	 * Create this scope from it's previously saved state
	 *
	 * @param memento the memento containing a previous scope information
	 * that is used to initialize this scope.
	 */
	protected WorkingSetScope(IMemento memento) {
		super(memento);
	}

	/**
	 * Initialize this working set scope with the provided working sets.
	 *
	 * @since 3.1
	 */
	protected void setWorkingSets(IWorkingSet[] sets) {
		this.sets = sets;
		PlatformUI.getWorkbench().getWorkingSetManager().addPropertyChangeListener(this);
	}

	@Override
	public String getName() {
		if (sets.length == 0) {
			return TeamUIMessages.WorkingSetScope_0;
		}
		StringBuilder name = new StringBuilder();
		for (int i = 0; i < sets.length; i++) {
			IWorkingSet set = sets[i];
			name.append(set.getName());
			if (i < sets.length - 1) {
				name.append(", "); //$NON-NLS-1$
			}
		}
		return name.toString();
	}

	@Override
	public IResource[] getRoots() {
		if (sets.length == 0) {
			return null;
		}
		HashSet<IResource> roots = new HashSet<>();
		for (IWorkingSet set : sets) {
			IResource[] resources = Utils.getResources(set.getElements());
			addNonOverlapping(roots, resources);
		}
		return roots.toArray(new IResource[roots.size()]);
	}

	private void addNonOverlapping(HashSet<IResource> roots, IResource[] resources) {
		for (IResource newResource : resources) {
			boolean add = true;
			for (Iterator iter = roots.iterator(); iter.hasNext();) {
				IResource existingResource = (IResource) iter.next();
				if (existingResource.equals(newResource)) {
					// No need to add it since it is already there
					add = false;
					break;
				}
				if (existingResource.getFullPath().isPrefixOf(newResource.getFullPath())) {
					// No need to add it since a parent is already there
					add = false;
					break;
				}
				if (newResource.getFullPath().isPrefixOf(existingResource.getFullPath())) {
					// Remove existing and continue
					iter.remove();
				}
			}
			if (add) {
				roots.add(newResource);
			}
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty() == IWorkingSetManager.CHANGE_WORKING_SET_CONTENT_CHANGE) {
			IWorkingSet newSet = (IWorkingSet) event.getNewValue();
			for (IWorkingSet set : sets) {
				if (newSet == set) {
					fireRootsChanges();
					return;
				}
			}
		} else if(event.getProperty() == IWorkingSetManager.CHANGE_WORKING_SET_NAME_CHANGE) {
			firePropertyChangedEvent(new PropertyChangeEvent(this, NAME, null, event.getNewValue()));
		}
	}

	@Override
	public void dispose() {
		super.dispose();
		if (PlatformUI.isWorkbenchRunning())
			PlatformUI.getWorkbench().getWorkingSetManager().removePropertyChangeListener(this);
	}

	@Override
	public void saveState(IMemento memento) {
		super.saveState(memento);
		for (IWorkingSet set : sets) {
			IMemento rootNode = memento.createChild(CTX_SETS);
			rootNode.putString(CTX_SET_NAME, set.getName());
		}
	}

	@Override
	protected void init(IMemento memento) {
		super.init(memento);
		IMemento[] rootNodes = memento.getChildren(CTX_SETS);
		if (rootNodes != null) {
			List<IWorkingSet> sets = new ArrayList<>();
			for (IMemento rootNode : rootNodes) {
				String setName = rootNode.getString(CTX_SET_NAME);
				IWorkingSet set = PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSet(setName);
				if (set != null) {
					sets.add(set);
				}
			}
			setWorkingSets(sets.toArray(new IWorkingSet[sets.size()]));
		}
	}
}
