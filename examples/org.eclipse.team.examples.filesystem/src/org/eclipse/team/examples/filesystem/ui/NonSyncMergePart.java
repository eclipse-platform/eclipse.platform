/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.examples.filesystem.ui;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.IContentChangeNotifier;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.core.mapping.IMergeContext;
import org.eclipse.team.internal.ui.Policy;
import org.eclipse.team.ui.PageSaveablePart;
import org.eclipse.team.ui.mapping.ISynchronizationCompareAdapter;
import org.eclipse.team.ui.mapping.ISynchronizationCompareInput;


/**
 * This class is the compare container used by the {@link NonSyncModelMergeOperation}
 * to show a manual merge.
 */
public class NonSyncMergePart extends PageSaveablePart {
	
	private final NonSyncModelMergePage page;

	protected NonSyncMergePart(Shell shell, CompareConfiguration compareConfiguration, NonSyncModelMergePage page) {
		super(shell, compareConfiguration);
		this.page = page;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.PageSaveablePart#createPage(org.eclipse.swt.widgets.Composite, org.eclipse.jface.action.ToolBarManager)
	 */
	protected Control createPage(Composite parent, ToolBarManager toolBarManager) {
		page.createControl(parent);
		return page.getControl();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.PageSaveablePart#getSelectionProvider()
	 */
	protected ISelectionProvider getSelectionProvider() {
		return page.getViewer();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.PageSaveablePart#getCompareInput(org.eclipse.jface.viewers.ISelection)
	 */
	protected ICompareInput getCompareInput(ISelection selection) {
		ICompareInput compareInput = super.getCompareInput(selection);
		if (compareInput != null)
			return compareInput;
		Object element = ((IStructuredSelection)selection).getFirstElement();
		ISynchronizationCompareAdapter compareAdapter = getCompareAdapter(element);
		if (element instanceof ResourceMapping) {
			element = ((ResourceMapping) element).getModelObject();
		}
		if (compareAdapter != null){
			return compareAdapter.asCompareInput(page.getContext(), element);
		}
		return null;
	}
	
	protected static ISynchronizationCompareAdapter getCompareAdapter(Object element) {
		if (element instanceof ResourceMapping) {
			ResourceMapping mapping = (ResourceMapping) element;
			ModelProvider provider = mapping.getModelProvider();
			Object adapter = provider.getAdapter(ISynchronizationCompareAdapter.class);
			if (adapter instanceof ISynchronizationCompareAdapter) {
				return (ISynchronizationCompareAdapter) adapter;
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.PageSaveablePart#prepareInput(org.eclipse.compare.structuremergeviewer.ICompareInput, org.eclipse.compare.CompareConfiguration, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void prepareInput(ICompareInput input,
			CompareConfiguration configuration, IProgressMonitor monitor)
			throws InvocationTargetException {
		try {
			ISynchronizationCompareInput adapter = asSynchronizationCompareInput(input);
			if (adapter != null) {
				adapter.prepareInput(configuration, Policy.subMonitorFor(monitor, 90));
			}
		} catch (CoreException e) {
			throw new InvocationTargetException(e);
		}
	}

	/*
	 * Convert the compare input to a synchronize compare input.
	 */
	private ISynchronizationCompareInput asSynchronizationCompareInput(ICompareInput input) {
		return (ISynchronizationCompareInput)Adapters.adapt(input, ISynchronizationCompareInput.class);
	}

	public void contentChanged(IContentChangeNotifier source) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPart#getTitle()
	 */
	public String getTitle() {
		return "File System Provider Merge";
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPart#getTitleImage()
	 */
	public Image getTitleImage() {
		return null;
	}

	public IMergeContext getContext() {
		return page.getContext();
	}

}
