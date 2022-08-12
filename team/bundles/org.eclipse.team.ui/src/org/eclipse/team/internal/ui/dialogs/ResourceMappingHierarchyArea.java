/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.dialogs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.core.mapping.ISynchronizationScope;
import org.eclipse.team.internal.ui.mapping.TeamViewerSorter;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.mapping.ITeamContentProviderManager;
import org.eclipse.ui.navigator.CommonViewer;
import org.eclipse.ui.navigator.CommonViewerSorter;
import org.eclipse.ui.navigator.INavigatorContentExtension;
import org.eclipse.ui.navigator.INavigatorContentServiceListener;

public class ResourceMappingHierarchyArea extends DialogArea implements INavigatorContentServiceListener {
	private static final String TEAM_NAVIGATOR_CONTENT = "org.eclipse.team.ui.navigatorViewer"; //$NON-NLS-1$
	private String description;
	private CommonViewer viewer;
	private final ISynchronizationScope scope;
	private final ISynchronizationContext context;


	public static ResourceMappingHierarchyArea create(ISynchronizationScope scope, ISynchronizationContext context) {
		return new ResourceMappingHierarchyArea(scope, context);
	}

	private ResourceMappingHierarchyArea(ISynchronizationScope scope, ISynchronizationContext context) {
		this.scope = scope;
		this.context = context;
	}

	@Override
	public void createArea(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout(1, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));

		if (description != null)
			createWrappingLabel(composite, description, 1);

		viewer = new CommonViewer(TEAM_NAVIGATOR_CONTENT, composite, SWT.BORDER);
		viewer.setSorter(new CommonViewerSorter());
		viewer.setSorter(new TeamViewerSorter((CommonViewerSorter)viewer.getSorter()));
		viewer.getNavigatorContentService().bindExtensions(TeamUI.getTeamContentProviderManager().getContentProviderIds(scope), true);
		viewer.getNavigatorContentService().getActivationService().activateExtensions(TeamUI.getTeamContentProviderManager().getContentProviderIds(scope), true);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.heightHint = 100;
		data.widthHint = 300;
		viewer.getControl().setLayoutData(data);
		viewer.getNavigatorContentService().addListener(this);
		viewer.setInput(getInitialInput());
		viewer.refresh();
		Object[] objects = getRootModelObjects();
		viewer.setSelection(new StructuredSelection(objects), true);
	}

	private Object getInitialInput() {
		if (context != null)
			return context;
		return scope;
	}

	private Object[] getRootModelObjects() {
		if (scope == null)
			return new Object[0];
		ResourceMapping[] mappings = scope.getMappings();
		List<Object> result = new ArrayList<>();
		for (ResourceMapping mapping : mappings) {
			result.add(mapping.getModelObject());
		}
		return result.toArray(new Object[result.size()]);
	}

	public void setDescription(String string) {
		description = string;
	}

	@Override
	public void onLoad(INavigatorContentExtension anExtension) {
		anExtension.getStateModel().setProperty(ITeamContentProviderManager.P_SYNCHRONIZATION_SCOPE, scope);
		if (context != null) {
			anExtension.getStateModel().setProperty(ITeamContentProviderManager.P_SYNCHRONIZATION_CONTEXT, context);
		}
	}
}
