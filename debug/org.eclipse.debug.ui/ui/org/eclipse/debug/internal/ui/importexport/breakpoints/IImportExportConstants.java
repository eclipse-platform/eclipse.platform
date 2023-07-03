/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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

package org.eclipse.debug.internal.ui.importexport.breakpoints;

/**
 *
 * XML tag constants for importing and exporting breakpoints
 */
public interface IImportExportConstants {

	/**
	 * <p>
	 * The name for the top level node in the XMLMemento for storing/restoring breakpoint information.</br>
	 *
	 * General formulation of the XMLMemento is as follows:</br>
	 *
	 * breakpoints := (breakpoint)*</br>
	 *
	 * breakpoint := resource</br>
	 *
	 * resource := (marker)+</br>
	 *
	 * marker := (attribs)+</br>
	 * </p>
	 * @see WizardExportBreakpointsPage
	 * @see WizardImportBreakpointsPage
	 *
	 */
	String IE_NODE_BREAKPOINTS = "breakpoints"; //$NON-NLS-1$

	/**
	 * The name of the node type for each of the imported or exported breakpoints
	 *
	 * @see WizardExportBreakpointsPage
	 * @see WizardImportBreakpointsPage
	 */
	String IE_NODE_BREAKPOINT = "breakpoint"; //$NON-NLS-1$

	/**
	 * <p>
	 * The generalized ID for each of the values stored in a markers' attribute map.
	 * Since a marker can have any number of attributes and or values, we use a
	 * (name, value) paring in the XMLmemento to store them, without having a dependence upon what the attribute
	 * is or what type it is.
	 * </p>
	 * @see WizardExportBreakpointsPage
	 * @see WizardImportBreakpointsPage
	 */
	String IE_NODE_VALUE = "value"; //$NON-NLS-1$

	/**
	 * Each breakpoint has an associated resource, which is described with this element
	 * name
	 *
	 * @see WizardExportBreakpointsPage
	 * @see WizardImportBreakpointsPage
	 */
	String IE_NODE_RESOURCE = "resource"; //$NON-NLS-1$

	/**
	 * To validate the resource when filtering importable breakpoints we need to know its path.
	 * This is the the name of XMLMemento node that stores that path.
	 *
	 * @see WizardExportBreakpointsPage
	 * @see WizardImportBreakpointsPage
	 */
	String IE_NODE_PATH = "path"; //$NON-NLS-1$

	/**
	 * To filter the type of path searched for within the workspace to allow for the filtering of
	 * breakpoints for import, we need to know the type to filter for.
	 *
	 * This is the name of the XMLMemento node that stores the type of the resource
	 *
	 * @see WizardExportBreakpointsPage
	 * @see WizardImportBreakpointsPage
	 */
	String IE_NODE_TYPE = "type"; //$NON-NLS-1$

	/**
	 * The name for the marker node in the corresponding XMLMemento
	 *
	 * @see WizardExportBreakpointsPage
	 * @see WizardImportBreakpointsPage
	 */
	String IE_NODE_MARKER = "marker"; //$NON-NLS-1$

	/**
	 * The name for a marker attribute node in the corresponding XMLMemento
	 *
	 * @see WizardExportBreakpointsPage
	 * @see WizardImportBreakpointsPage
	 */
	String IE_NODE_ATTRIB = "attrib"; //$NON-NLS-1$

	/**
	 * The generalized name for the "name" node used in marker attribute nodes.
	 * This is the other ID in the (name, value) pairing used to describe attributes
	 * of markers.
	 *
	 * @see WizardExportBreakpointsPage
	 * @see WizardImportBreakpointsPage
	 */
	String IE_NODE_NAME = "name"; //$NON-NLS-1$

	/**
	 * The name of the enabled attribute, which is part of the breakpoint node information
	 *
	 * @see WizardExportBreakpointsPage
	 * @see WizardImportBreakpointsPage
	 */
	String IE_BP_ENABLED = "enabled"; //$NON-NLS-1$

	/**
	 * The name of the registered attribute, which is part of the breakpoint node information
	 *
	 * @see WizardExportBreakpointsPage
	 * @see WizardImportBreakpointsPage
	 */
	String IE_BP_REGISTERED = "registered"; //$NON-NLS-1$

	/**
	 * The name of the persistent attribute for breakpoint information
	 */
	String IE_BP_PERSISTANT = "persistant"; //$NON-NLS-1$

	/**
	 * The default file extension for breakpoint export files
	 */
	String EXTENSION = "bkpt";  //$NON-NLS-1$

	/**
	 * the charstart attribute from a marker
	 */
	String CHARSTART = "charStart"; //$NON-NLS-1$

	/**
	 * The delimiter for the listing of working sets that a marker belongs to
	 */
	String DELIMITER = "<;#>"; //$NON-NLS-1$
}
