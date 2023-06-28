/*******************************************************************************
 * Copyright (c) 2003, 2018 IBM Corporation and others.
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
 *     Pawel Piech - Bug 173306: When editing source lookup, new source
 *     					containers should be added at the top of the list
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 490755
 *******************************************************************************/
package org.eclipse.debug.internal.ui.sourcelookup;


import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;

/**
 * The viewer containing the source containers in the
 * SourceContainerLookupTab and the EditSourceLookupPathDialog.
 * It is a tree viewer since the containers are represented in tree form.
 *
 * @since 3.0
 */
public class SourceContainerViewer extends TreeViewer {

	/**
	 * Whether enabled/editable.
	 */
	private boolean fEnabled = true;
	/**
	 * The parent panel
	 */
	private SourceLookupPanel fPanel;
	/**
	 * The source container entries displayed in this viewer
	 */
	protected List<ISourceContainer> fEntries = new ArrayList<>();

	class ContentProvider implements ITreeContentProvider {

		@Override
		public Object[] getElements(Object inputElement) {
			return getEntries();
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			try {
				return ((ISourceContainer)parentElement).getSourceContainers();
			} catch (CoreException e) {
				return new Object[0];
			}
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			return ((ISourceContainer)element).isComposite();
		}

	}

	/**
	 * Creates a runtime classpath viewer with the given parent.
	 *
	 * @param parent the parent control
	 * @param panel the panel hosting this viewer
	 */
	public SourceContainerViewer(Composite parent, SourceLookupPanel panel) {
		super(parent);
		setContentProvider(new ContentProvider());
		SourceContainerLabelProvider lp = new SourceContainerLabelProvider();
		setLabelProvider(lp);
		fPanel = panel;
	}

	/**
	 * Sets the entries in this viewer
	 *
	 * @param entries source container entries
	 */
	public void setEntries(ISourceContainer[] entries) {
		fEntries.clear();
		for (ISourceContainer entry : entries) {
			if(entry != null) {
				fEntries.add(entry);
			}
		}
		if (getInput() == null) {
			setInput(fEntries);
			//select first item in list
			if(!fEntries.isEmpty() && fEntries.get(0)!=null) {
				setSelection(new StructuredSelection(fEntries.get(0)));
			}
		} else {
			refresh();
		}
		fPanel.setDirty(true);
		fPanel.updateLaunchConfigurationDialog();
	}

	/**
	 * Returns the entries in this viewer
	 *
	 * @return the entries in this viewer
	 */
	public ISourceContainer[] getEntries() {
		return fEntries.toArray(new ISourceContainer[fEntries.size()]);
	}

	/**
	 * Adds the given entries to the list. If there is no selection
	 * in the list, the entries are added at the end of the list,
	 * otherwise the new entries are added before the (first) selected
	 * entry. The new entries are selected.
	 *
	 * @param entries additions
	 */
	public void addEntries(ISourceContainer[] entries) {
		int index = 0;
		IStructuredSelection sel = getStructuredSelection();
		if (!sel.isEmpty()) {
			index = fEntries.indexOf(sel.getFirstElement());
		}
		for (ISourceContainer entry : entries) {
			if (!fEntries.contains(entry)) {
				fEntries.add(index, entry);
				index++;
			}
		}

		refresh();
		if(entries.length > 0) {
			setSelection(new StructuredSelection(entries));
		}
		fPanel.setDirty(true);
		fPanel.updateLaunchConfigurationDialog();
	}

	/**
	 * Enables/disables this viewer. Note the control is not disabled, since
	 * we still want the user to be able to scroll if required to see the
	 * existing entries. Just actions should be disabled.
	 */
	public void setEnabled(boolean enabled) {
		fEnabled = enabled;
		// fire selection change to upate actions
		setSelection(getSelection());
	}

	/**
	 * Returns whether this viewer is enabled
	 */
	public boolean isEnabled() {
		return fEnabled;
	}

	/**
	 * Returns the index of an equivalent entry, or -1 if none.
	 *
	 * @return the index of an equivalent entry, or -1 if none
	 */
	public int indexOf(ISourceContainer entry) {
		return fEntries.indexOf(entry);
	}

	/**
	 * Returns the source locator associated with the parent panel.
	 *
	 * @return the source locator
	 */
	public ISourceLookupDirector getSourceLocator()
	{
		return fPanel.fLocator;
	}

}
