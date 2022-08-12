/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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
 *     Serge Beauchamp (Freescale Semiconductor) - [252996] add resource filtering
 *     Serge Beauchamp (Freescale Semiconductor) - [229633] Project Path Variable Support
 * Markus Schorn (Wind River) - [306575] Save snapshot location with project
 *******************************************************************************/
package org.eclipse.core.internal.resources;

public interface IModelObjectConstants {
	String ARGUMENTS = "arguments"; //$NON-NLS-1$
	String ID = "id"; //$NON-NLS-1$
	String BUILD_COMMAND = "buildCommand"; //$NON-NLS-1$
	String BUILD_SPEC = "buildSpec"; //$NON-NLS-1$
	String BUILD_TRIGGERS = "triggers"; //$NON-NLS-1$
	String TRIGGER_AUTO = "auto"; //$NON-NLS-1$
	String TRIGGER_CLEAN = "clean"; //$NON-NLS-1$
	String TRIGGER_FULL = "full"; //$NON-NLS-1$
	String TRIGGER_INCREMENTAL = "incremental"; //$NON-NLS-1$
	String COMMENT = "comment"; //$NON-NLS-1$
	String DICTIONARY = "dictionary"; //$NON-NLS-1$
	String KEY = "key"; //$NON-NLS-1$
	String LOCATION = "location"; //$NON-NLS-1$
	String LOCATION_URI = "locationURI"; //$NON-NLS-1$

	/**
	 * The project relative path is called the link name for backwards compatibility
	 */
	String NAME = "name"; //$NON-NLS-1$
	String NATURE = "nature"; //$NON-NLS-1$
	String NATURES = "natures"; //$NON-NLS-1$
	String PROJECT = "project"; //$NON-NLS-1$
	String PROJECT_DESCRIPTION = "projectDescription"; //$NON-NLS-1$
	String PROJECTS = "projects"; //$NON-NLS-1$
	String TYPE = "type"; //$NON-NLS-1$
	String VALUE = "value"; //$NON-NLS-1$
	String LINKED_RESOURCES = "linkedResources"; //$NON-NLS-1$
	String LINK = "link"; //$NON-NLS-1$
	String FILTERED_RESOURCES = "filteredResources"; //$NON-NLS-1$
	String FILTER = "filter"; //$NON-NLS-1$
	String MATCHER = "matcher"; //$NON-NLS-1$
	String VARIABLE = "variable"; //$NON-NLS-1$
	String VARIABLE_LIST = "variableList"; //$NON-NLS-1$
	String SNAPSHOT_LOCATION = "snapshotLocation"; //$NON-NLS-1$
}
