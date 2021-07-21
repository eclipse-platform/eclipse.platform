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
package org.eclipse.team.internal.ui.synchronize;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.ui.synchronize.ISynchronizeScope;
import org.eclipse.team.ui.synchronize.ResourceScope;
import org.eclipse.team.ui.synchronize.WorkingSetScope;
import org.eclipse.team.ui.synchronize.WorkspaceScope;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.navigator.ResourceComparator;

/**
 * Page that allows the user to select a set of resources that are managed by a subscriber
 * participant. Callers can provide a scope hint to determine the initial selection for the
 * resource list. By default, the resources in the current selection are checked, otherwise
 * all resources are checked.
 *
 * @since 3.0
 */
public class GlobalRefreshResourceSelectionPage extends GlobalRefreshElementSelectionPage {

	private List resources;

	/**
	 * Content provider that accepts a <code>SubscriberParticipant</code> as input and
	 * returns the participants root resources.
	 */
	class MyContentProvider extends BaseWorkbenchContentProvider {
		@Override
		public Object[] getChildren(Object element) {
			if(element instanceof List) {
				return ((List<?>)element).toArray(new IResource[((List)element).size()]);
			}
			return super.getChildren(element);
		}
	}

	/**
	 * Label decorator that will display the full path for participant roots that are folders. This
	 * is useful for participants that have non-project roots.
	 */
	class MyLabelProvider extends LabelProvider {
		private LabelProvider workbenchProvider = new WorkbenchLabelProvider();
		@Override
		public String getText(Object element) {
			if(element instanceof IContainer) {
				IContainer c = (IContainer)element;
				if(c.getType() != IResource.PROJECT && resources.contains(c)) {
					return c.getFullPath().toString();
				}
			}
			return workbenchProvider.getText(element);
		}
		@Override
		public Image getImage(Object element) {
			return workbenchProvider.getImage(element);
		}
	}

	/**
	 * Create a new page for the given participant. The scope hint will determine the initial selection.
	 *
	 * @param resources the resources to synchronize
	 */
	public GlobalRefreshResourceSelectionPage(IResource[] resources) {
		super(TeamUIMessages.GlobalRefreshResourceSelectionPage_1);
		// Caching the roots so that the decorator doesn't have to recompute all the time.
		this.resources = Arrays.asList(resources);
		setDescription(TeamUIMessages.GlobalRefreshResourceSelectionPage_2);
		setTitle(TeamUIMessages.GlobalRefreshResourceSelectionPage_3);
	}

	@Override
	protected ContainerCheckedTreeViewer createViewer(Composite top) {
		GridData data;
		ContainerCheckedTreeViewer fViewer = new ContainerCheckedTreeViewer(top, SWT.BORDER);
		data = new GridData(GridData.FILL_BOTH);
		//data.widthHint = 200;
		data.heightHint = 100;
		fViewer.getControl().setLayoutData(data);
		fViewer.setContentProvider(new MyContentProvider());
		fViewer.setLabelProvider(new DecoratingLabelProvider(new MyLabelProvider(), PlatformUI.getWorkbench().getDecoratorManager().getLabelDecorator()));
		fViewer.addCheckStateListener(event -> updateOKStatus());
		fViewer.setComparator(new ResourceComparator(ResourceComparator.NAME));
		fViewer.setInput(resources);
		return fViewer;
	}

	@Override
	protected void checkAll() {
		getViewer().setCheckedElements(resources.toArray());
	}

	@Override
	protected boolean checkWorkingSetElements() {
		List<IResource> allWorkingSetResources = new ArrayList<>();
		IWorkingSet[] workingSets = getWorkingSets();
		for (IWorkingSet set : workingSets) {
			allWorkingSetResources.addAll(IDE.computeSelectedResources(new StructuredSelection(set.getElements())));
		}
		getViewer().setCheckedElements(allWorkingSetResources.toArray(new IResource[allWorkingSetResources.size()]));
		return !allWorkingSetResources.isEmpty();
	}

	public IResource[] getRootResources() {
		Object[] objects = getRootElement();
		IResource[] resources = new IResource[objects.length];
		for (int i = 0; i < resources.length; i++) {
			resources[i] = (IResource)objects[i];

		}
		return resources;
	}

	public ISynchronizeScope getSynchronizeScope() {
		if (isWorkingSetSelected()) {
			return new WorkingSetScope(getWorkingSets());
		}
		if (isWorkspaceSelected()) {
			return new WorkspaceScope();
		}
		return new ResourceScope(getRootResources());
	}

}
