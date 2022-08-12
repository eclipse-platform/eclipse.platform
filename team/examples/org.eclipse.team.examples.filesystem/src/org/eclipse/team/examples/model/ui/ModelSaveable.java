/*******************************************************************************
 * Copyright (c) 2007, 2018 IBM Corporation and others.
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
package org.eclipse.team.examples.model.ui;

import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.team.examples.model.ModelObject;
import org.eclipse.team.examples.model.ModelObjectDefinitionFile;
import org.eclipse.ui.Saveable;

/**
 * A Saveable that represents a modified model object definition file.
 */
public class ModelSaveable extends Saveable {

	private ModelObject modelObject;
	private boolean dirty;
	private final ModelSaveablesProvider modelSaveablesProvider;

	public ModelSaveable(ModelSaveablesProvider modelSaveablesProvider, ModelObjectDefinitionFile mo) {
		this.modelSaveablesProvider = modelSaveablesProvider;
		modelObject = mo;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof ModelSaveable) {
			ModelSaveable other = (ModelSaveable) object;
			return (other.getModelObject().equals(getModelObject()));
		}
		return false;
	}

	public ModelObject getModelObject() {
		return modelObject;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return ModelWorkbenchAdapter.createImageDescriptor("obj/mod_obj.gif");
	}

	@Override
	public String getName() {
		return modelObject.getName();
	}

	@Override
	public String getToolTipText() {
		return "Saveable for " + getName();
	}

	@Override
	public int hashCode() {
		return modelObject.hashCode();
	}

	@Override
	public boolean isDirty() {
		return dirty;
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		dirty = false;
		modelSaveablesProvider.saved(this);
	}

	public void makeDirty() {
		dirty = true;
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == ResourceMapping.class) {
			return Adapters.adapt(getModelObject(), adapter);
		}
		return super.getAdapter(adapter);
	}
}
