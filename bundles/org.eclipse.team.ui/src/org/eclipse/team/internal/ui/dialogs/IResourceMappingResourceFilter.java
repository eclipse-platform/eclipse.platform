/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.dialogs;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;

/**
 * A filter for selecting resources of a resource mapping for
 * inclusion in a view.
 */
public interface IResourceMappingResourceFilter {

    /**
     * Return whether the resource should be included in this filter.
     * @param resource the resource
     * @param mapping the mapping chiehc containes the resource
     * @param traversal the traversal from which the resource was obtained
     * @return whether the resource passes the filter
     * @throws CoreException
     */
    boolean select(IResource resource, ResourceMapping mapping, ResourceTraversal traversal) throws CoreException;
}
