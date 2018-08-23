/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
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
package org.eclipse.team.examples.model;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

/**
 * A model object that has a corresponding resource.
 * This does not indicate that the model object only
 * consists of a single resource. It only means that at least
 * the resource of this model element makes up the model
 * element. There may be other resources included as well.
 * <p>
 * The model consists of the following:
 * <ol>
 * <li>Model object definition files (*.mod). A MOD file consists
 * of a pointer to one of more model object element (*.moe) files.</li>
 * <li>Model object element file (*.moe) contain one or more elements</li>
 * <li>Model object projects correspond to workspace projects but only show child
 * folders and MOD files when expanded.</li>
 * <li>Model object folders correspond to workspace folders but only show child
 * folders and MOD files when expanded.</li>
 * </ol>
 * 
 */
public abstract class ModelResource extends ModelObject{
	private final IResource resource;
	
	protected ModelResource(IResource resource) {
		this.resource = resource;
	}

	public IResource getResource() {
		return resource;
	}
	
	public String getName() {
		return getResource().getName();
	}
	
	public String getPath() {
		return getResource().getFullPath().makeRelative().toString();
	}
	
	public ModelObject getParent() {
		return ModelObject.create(getResource().getParent());
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (obj instanceof ModelResource) {
			ModelResource mr = (ModelResource) obj;
			return getResource().equals(mr.getResource());
		}
		return super.equals(obj);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return getResource().hashCode();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.examples.model.ModelObject#delete()
	 */
	public void delete() throws CoreException {
		getResource().delete(false, null);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.examples.model.ModelObject#getProject()
	 */
	public ModelProject getProject() {
		return (ModelProject)ModelObject.create(getResource().getProject());
	}
}
