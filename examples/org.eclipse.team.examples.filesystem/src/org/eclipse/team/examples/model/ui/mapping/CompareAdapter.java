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
package org.eclipse.team.examples.model.ui.mapping;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.core.resources.*;
import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.Path;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.examples.model.*;
import org.eclipse.team.examples.model.mapping.ExampleModelProvider;
import org.eclipse.team.ui.mapping.SynchronizationCompareAdapter;
import org.eclipse.ui.IMemento;

/**
 * Compare adapter for use with our example model.
 */
public class CompareAdapter extends SynchronizationCompareAdapter {

	private static final String CTX_MODEL_MAPPINGS = "org.eclipse.team.examples.filesystem.modelMappings";
	
	private final ExampleModelProvider provider;

	public CompareAdapter(ExampleModelProvider provider) {
		this.provider = provider;
	}
	
	public String getName(ResourceMapping mapping) {
		Object o = mapping.getModelObject();
		if (o instanceof ModelObject) {
			return ((ModelObject) o).getName();
		}
		return super.getName(mapping);
	}
	
	public String getPathString(ResourceMapping mapping) {
		Object o = mapping.getModelObject();
		if (o instanceof ModelObject) {
			return ((ModelObject) o).getPath();
		}
		return super.getPathString(mapping);
	}

	public ICompareInput asCompareInput(ISynchronizationContext context, Object o) {
		if (o instanceof ModelObjectElementFile) {
			ModelObjectElementFile moeFile = (ModelObjectElementFile) o;
			// Use a file compare input for the model element file
			return super.asCompareInput(context, moeFile.getResource());
		}
		return super.asCompareInput(context, o);
	}
	
	public ResourceMapping[] restore(IMemento memento) {
		List result = new ArrayList();
		IMemento[] children = memento.getChildren(CTX_MODEL_MAPPINGS);
		for (int i = 0; i < children.length; i++) {
			IMemento child = children[i];
			ResourceMapping mapping = restoreMapping(child);
			if (mapping != null)
				result.add(mapping);
		}
		return (ResourceMapping[]) result.toArray(new ResourceMapping[result.size()]);
	}

	public void save(ResourceMapping[] mappings, IMemento memento) {
		for (int i = 0; i < mappings.length; i++) {
			ResourceMapping mapping = mappings[i];
			Object o = mapping.getModelObject();
			if (o instanceof ModelObject) {
				ModelObject mo = (ModelObject) o;
				save(mo, memento.createChild(CTX_MODEL_MAPPINGS));
			}
		}
		
	}

	private ResourceMapping restoreMapping(IMemento child) {
		String parent = child.getString("definition");
		String path = child.getString("resource");
		if (parent != null) {
			ModelObjectDefinitionFile modFile = (ModelObjectDefinitionFile)ModelObject.create(getResource(parent));
			if (modFile != null)
				return new ModelObjectElementFile(modFile, (IFile)getResource(path)).getAdapter(ResourceMapping.class);
		} else {
			ModelObject object = ModelObject.create(getResource(path));
			if (object != null)
				return object.getAdapter(ResourceMapping.class);
		}
		return null;
	}
	
	private IResource getResource(String path) {
		Path resourcePath = new Path(path);
		if (path.endsWith(ModelObjectDefinitionFile.MODEL_OBJECT_DEFINITION_FILE_EXTENSION) 
				|| path.endsWith(ModelObjectElementFile.MODEL_OBJECT_ELEMENTFILE_EXTENSION))
			return ResourcesPlugin.getWorkspace().getRoot().getFile(resourcePath);
		if (resourcePath.segmentCount() == 1)
			return ResourcesPlugin.getWorkspace().getRoot().getProject(resourcePath.lastSegment());
		return ResourcesPlugin.getWorkspace().getRoot().getFolder(resourcePath);
	}

	private void save(ModelObject mo, IMemento memento) {
		if (mo instanceof ModelResource) {
			ModelResource resource = (ModelResource) mo;
			memento.putString("resource", resource.getResource().getFullPath().toString());
			if (mo instanceof ModelObjectElementFile) {
				ModelObjectElementFile moeFile = (ModelObjectElementFile) mo;
				ModelObjectDefinitionFile parent = (ModelObjectDefinitionFile)moeFile.getParent();
				memento.putString("definition", parent.getResource().getFullPath().toString());
			}
		}
	}

	public ModelProvider getProvider() {
		return provider;
	}

}
