/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.mapping;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.mapping.IModelProviderDescriptor;
import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.team.internal.ui.IHelpContextIds;
import org.eclipse.team.internal.ui.SWTUtils;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.mapping.ITeamContentProviderDescriptor;
import org.eclipse.team.ui.mapping.ITeamContentProviderManager;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

public class ModelEnablementPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private Set<ITeamContentProviderDescriptor> previosulyEnabled = new HashSet<>();

	public ModelEnablementPreferencePage() {
		setTitle(TeamUIMessages.ModelEnablementPreferencePage_0);
		setPreferenceStore(TeamUIPlugin.getPlugin().getPreferenceStore());
	}

	private CheckboxTableViewer tableViewer;

	@Override
	protected Control createContents(Composite parent) {

		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));

		Label l = SWTUtils.createLabel(composite, TeamUIMessages.ModelEnablementPreferencePage_1);
		l.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		tableViewer = CheckboxTableViewer.newCheckList(composite, SWT.BORDER);
		tableViewer.getTable().setLayoutData(new GridData(GridData.FILL_BOTH));
		tableViewer.setContentProvider(new IStructuredContentProvider() {
			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
				// noting to do
			}
			@Override
			public void dispose() {
				// nothing to do
			}
			@Override
			public Object[] getElements(Object element) {
				if (element instanceof ITeamContentProviderManager) {
					ITeamContentProviderManager manager = (ITeamContentProviderManager) element;
					return manager.getDescriptors();
				}
				return new Object[0];
			}
		});
		tableViewer.setLabelProvider(new LabelProvider() {
			Map<ITeamContentProviderDescriptor, Image> images = new HashMap<>();
			@Override
			public String getText(Object element) {
				if (element instanceof ITeamContentProviderDescriptor) {
					ITeamContentProviderDescriptor desc = (ITeamContentProviderDescriptor) element;
					return getTextFor(desc);
				}
				return super.getText(element);
			}
			private String getTextFor(ITeamContentProviderDescriptor teamContentDescriptor) {
				String name = teamContentDescriptor.getName();

				if (name != null && !name.isEmpty())
					return name;

				String modelProviderID = teamContentDescriptor.getModelProviderId();
				IModelProviderDescriptor desc = ModelProvider.getModelProviderDescriptor(modelProviderID);
				if (desc != null) {
					return getLabel(desc);
				}
				return modelProviderID;
			}
			@Override
			public Image getImage(Object element) {
				if (element instanceof ITeamContentProviderDescriptor) {
					ITeamContentProviderDescriptor desc = (ITeamContentProviderDescriptor) element;
					Image image = images.get(desc);
					if (image == null) {
						ImageDescriptor idesc = desc.getImageDescriptor();
						if (idesc != null) {
							image = idesc.createImage();
							if (image != null) {
								images.put(desc, image);
							}
						}
					}
					return image;
				}
				return super.getImage(element);
			}
			@Override
			public void dispose() {
				for (Image image : images.values()) {
					image.dispose();
				}
				super.dispose();
			}
		});
		tableViewer.setComparator(new ViewerComparator() {
			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				if (e1 instanceof ITeamContentProviderDescriptor && e2 instanceof ITeamContentProviderDescriptor) {
					ITeamContentProviderDescriptor d1 = (ITeamContentProviderDescriptor) e1;
					ITeamContentProviderDescriptor d2 = (ITeamContentProviderDescriptor) e2;
					IModelProviderDescriptor md1 = ModelProvider.getModelProviderDescriptor(d1.getModelProviderId());
					IModelProviderDescriptor md2 = ModelProvider.getModelProviderDescriptor(d2.getModelProviderId());
					if (md1 != null && md2 != null)
						return getLabel(md1).compareTo(getLabel(md2));
				}
				return super.compare(viewer, e1, e2);
			}
		});
		tableViewer.setInput(TeamUI.getTeamContentProviderManager());
		updateChecks();
		applyDialogFont(composite);

		//F1
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.MODEL_PREFERENCE_PAGE);

		return composite;
	}

	private void updateChecks() {
		ITeamContentProviderDescriptor[] descriptors = TeamUI.getTeamContentProviderManager().getDescriptors();
		for (ITeamContentProviderDescriptor descriptor : descriptors) {
			if (descriptor.isEnabled()) {
				previosulyEnabled.add(descriptor);
			}
		}
		tableViewer.setCheckedElements(previosulyEnabled.toArray());
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean performOk() {
		Object[] checked = tableViewer.getCheckedElements();
		Set<ITeamContentProviderDescriptor> nowEnabled = new HashSet<>();
		nowEnabled.addAll((Collection<? extends ITeamContentProviderDescriptor>) Arrays.asList(checked));
		if (hasDescriptorEnablementChanged(checked)) {
			TeamUI.getTeamContentProviderManager().setEnabledDescriptors(
					nowEnabled.toArray(new ITeamContentProviderDescriptor[nowEnabled.size()]));
			previosulyEnabled = nowEnabled;
		}
		return true;
	}

	private boolean hasDescriptorEnablementChanged(Object[] checked) {
		ITeamContentProviderDescriptor[] descriptors = TeamUI.getTeamContentProviderManager().getDescriptors();
		for (ITeamContentProviderDescriptor descriptor : descriptors) {
			boolean enable = false;
			for (Object c : checked) {
				ITeamContentProviderDescriptor checkedDesc = (ITeamContentProviderDescriptor) c;
				if (checkedDesc.getModelProviderId().equals(descriptor.getModelProviderId())) {
					enable = true;
					break;
				}
			}
			if (descriptor.isEnabled() != enable) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected void performDefaults() {
		tableViewer.setCheckedElements(TeamUI.getTeamContentProviderManager().getDescriptors());
	}

	@Override
	public void init(IWorkbench workbench) {
		// ignore
	}

	private String getLabel(IModelProviderDescriptor desc) {
		// Only do this for the resource model since we don;t want to
		// load all model providers (see bug 133604)
		if (desc.getId().equals(ModelProvider.RESOURCE_MODEL_PROVIDER_ID))
			try {
				return Utils.getLabel(desc.getModelProvider());
			} catch (CoreException e) {
				TeamUIPlugin.log(e);
			}
		return desc.getLabel();
	}

}
