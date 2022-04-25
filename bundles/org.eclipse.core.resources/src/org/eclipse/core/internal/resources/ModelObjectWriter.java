/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
 *     Serge Beauchamp (Freescale Semiconductor) - [229633] Group and Project Path Variable Support
 *     Markus Schorn (Wind River) - [306575] Save snapshot location with project
 *     James Blackburn (Broadcom Corp.) - ongoing development
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 473427
 *     Mickael Istria (Red Hat Inc.) - Bug 488937
 *******************************************************************************/
package org.eclipse.core.internal.resources;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.*;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.internal.events.BuildCommand;
import org.eclipse.core.internal.utils.FileUtil;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.IPath;

public class ModelObjectWriter implements IModelObjectConstants {

	/**
	 * Returns the string representing the serialized set of build triggers for
	 * the given command
	 */
	private static String triggerString(BuildCommand command) {
		StringBuilder buf = new StringBuilder();
		if (command.isBuilding(IncrementalProjectBuilder.AUTO_BUILD))
			buf.append(TRIGGER_AUTO).append(',');
		if (command.isBuilding(IncrementalProjectBuilder.CLEAN_BUILD))
			buf.append(TRIGGER_CLEAN).append(',');
		if (command.isBuilding(IncrementalProjectBuilder.FULL_BUILD))
			buf.append(TRIGGER_FULL).append(',');
		if (command.isBuilding(IncrementalProjectBuilder.INCREMENTAL_BUILD))
			buf.append(TRIGGER_INCREMENTAL).append(',');
		return buf.toString();
	}

	public ModelObjectWriter() {
		super();
	}

	protected String[] getReferencedProjects(ProjectDescription description) {
		IProject[] projects = description.getReferencedProjects();
		String[] result = new String[projects.length];
		for (int i = 0; i < projects.length; i++)
			result[i] = projects[i].getName();
		return result;
	}

	protected void write(BuildCommand command, XMLWriter writer) {
		writer.startTag(BUILD_COMMAND, null);
		if (command != null) {
			writer.printSimpleTag(NAME, command.getName());
			if (shouldWriteTriggers(command))
				writer.printSimpleTag(BUILD_TRIGGERS, triggerString(command));
			write(ARGUMENTS, command.getArguments(false), writer);
		}
		writer.endTag(BUILD_COMMAND);
	}

	/**
	 * Returns whether the build triggers for this command should be written.
	 */
	private boolean shouldWriteTriggers(BuildCommand command) {
		//only write triggers if command is configurable and there exists a trigger
		//that the builder does NOT respond to.  I.e., don't write out on the default
		//cases to avoid dirtying .project files unnecessarily.
		if (!command.isConfigurable())
			return false;
		return !command.isBuilding(IncrementalProjectBuilder.AUTO_BUILD) || !command.isBuilding(IncrementalProjectBuilder.CLEAN_BUILD) || !command.isBuilding(IncrementalProjectBuilder.FULL_BUILD) || !command.isBuilding(IncrementalProjectBuilder.INCREMENTAL_BUILD);
	}

	protected void write(LinkDescription description, XMLWriter writer) {
		writer.startTag(LINK, null);
		if (description != null) {
			writer.printSimpleTag(NAME, description.getProjectRelativePath());
			writer.printSimpleTag(TYPE, Integer.toString(description.getType()));
			//use ASCII string of URI to ensure spaces are encoded
			writeLocation(description.getLocationURI(), writer);
		}
		writer.endTag(LINK);
	}

	protected void write(IResourceFilterDescription description, XMLWriter writer) {
		writer.startTag(FILTER, null);
		if (description != null) {
			writer.printSimpleTag(ID, ((FilterDescription) description).getId());
			writer.printSimpleTag(NAME, description.getResource().getProjectRelativePath());
			writer.printSimpleTag(TYPE, Integer.toString(description.getType()));
			if (description.getFileInfoMatcherDescription() != null) {
				write(description.getFileInfoMatcherDescription(), writer);
			}
		}
		writer.endTag(FILTER);
	}

	protected void write(FileInfoMatcherDescription description, XMLWriter writer) {
		writer.startTag(MATCHER, null);
		writer.printSimpleTag(ID, description.getId());
		if (description.getArguments() != null) {
			if (description.getArguments() instanceof String) {
				writer.printSimpleTag(ARGUMENTS, description.getArguments());
			} else if (description.getArguments() instanceof FileInfoMatcherDescription[]) {
				writer.startTag(ARGUMENTS, null);
				FileInfoMatcherDescription[] array = (FileInfoMatcherDescription[]) description.getArguments();
				for (FileInfoMatcherDescription element : array) {
					write(element, writer);
				}
				writer.endTag(ARGUMENTS);
			} else
				writer.printSimpleTag(ARGUMENTS, ""); //$NON-NLS-1$
		}
		writer.endTag(MATCHER);
	}

	protected void write(VariableDescription description, XMLWriter writer) {
		writer.startTag(VARIABLE, null);
		if (description != null) {
			writer.printSimpleTag(NAME, description.getName());
			writer.printSimpleTag(VALUE, description.getValue());
		}
		writer.endTag(VARIABLE);
	}

	/**
	 * Writes a location to the XML writer.  For backwards compatibility,
	 * local file system locations are written and read using a different tag
	 * from non-local file systems.
	 * @param location
	 * @param writer
	 */
	private void writeLocation(URI location, XMLWriter writer) {
		if (EFS.SCHEME_FILE.equals(location.getScheme())) {
			writer.printSimpleTag(LOCATION, FileUtil.toPath(location).toPortableString());
		} else {
			writer.printSimpleTag(LOCATION_URI, location.toASCIIString());
		}
	}

	/**
	 * The OutputStream is closed in this method.
	 */
	public void write(Object object, OutputStream output, String lineSeparator) throws IOException {
		try (
			XMLWriter writer = new XMLWriter(output, lineSeparator);
		) {
			write(object, writer);
			writer.flush();
			if (writer.checkError())
				throw new IOException();
		} finally {
			FileUtil.safeClose(output);
		}
	}

	protected void write(Object obj, XMLWriter writer) throws IOException {
		if (obj instanceof BuildCommand) {
			write((BuildCommand) obj, writer);
			return;
		}
		if (obj instanceof ProjectDescription) {
			write((ProjectDescription) obj, writer);
			return;
		}
		if (obj instanceof WorkspaceDescription) {
			throw new IOException("Workspace description format is not supported anymore."); //$NON-NLS-1$
		}
		if (obj instanceof LinkDescription) {
			write((LinkDescription) obj, writer);
			return;
		}
		if (obj instanceof IResourceFilterDescription) {
			write((IResourceFilterDescription) obj, writer);
			return;
		}
		if (obj instanceof VariableDescription) {
			write((VariableDescription) obj, writer);
			return;
		}
		writer.printTabulation();
		writer.println(obj.toString());
	}

	protected void write(ProjectDescription description, XMLWriter writer) throws IOException {
		writer.startTag(PROJECT_DESCRIPTION, null);
		if (description != null) {
			writer.printSimpleTag(NAME, description.getName());
			String comment = description.getComment();
			writer.printSimpleTag(COMMENT, comment == null ? "" : comment); //$NON-NLS-1$
			URI snapshotLocation = description.getSnapshotLocationURI();
			if (snapshotLocation != null) {
				writer.printSimpleTag(SNAPSHOT_LOCATION, snapshotLocation.toString());
			}
			write(PROJECTS, PROJECT, getReferencedProjects(description), writer);
			write(BUILD_SPEC, Arrays.asList(description.getBuildSpec(false)), writer);
			write(NATURES, NATURE, description.getNatureIds(false), writer);
			HashMap<IPath, LinkDescription> links = description.getLinks();
			if (links != null) {
				// ensure consistent order of map elements
				List<LinkDescription> sorted = new ArrayList<>(links.values());
				sorted.sort(null);
				write(LINKED_RESOURCES, sorted, writer);
			}
			HashMap<IPath, LinkedList<FilterDescription>> filters = description.getFilters();
			if (filters != null) {
				List<FilterDescription> sorted = new ArrayList<>();
				for (LinkedList<FilterDescription> linkedList : filters.values()) {
					List<FilterDescription> list = linkedList;
					sorted.addAll(list);
				}
				sorted.sort(null);
				write(FILTERED_RESOURCES, sorted, writer);
			}
			HashMap<String, VariableDescription> variables = description.getVariables();
			if (variables != null) {
				List<VariableDescription> sorted = new ArrayList<>(variables.values());
				sorted.sort(null);
				write(VARIABLE_LIST, sorted, writer);
			}
		}
		writer.endTag(PROJECT_DESCRIPTION);
	}

	protected void write(String name, Collection<?> collection, XMLWriter writer) throws IOException {
		writer.startTag(name, null);
		for (Object o : collection)
			write(o, writer);
		writer.endTag(name);
	}

	/**
	 * Write maps of (String, String).
	 */
	protected void write(String name, Map<String, String> table, XMLWriter writer) {
		writer.startTag(name, null);
		if (table != null) {
			// ensure consistent order of map elements
			List<String> sorted = new ArrayList<>(table.keySet());
			sorted.sort(null);

			for (String key : sorted) {
				Object value = table.get(key);
				writer.startTag(DICTIONARY, null);
				writer.printSimpleTag(KEY, key);
				writer.printSimpleTag(VALUE, value);
				writer.endTag(DICTIONARY);
			}
		}
		writer.endTag(name);
	}

	protected void write(String name, String elementTagName, String[] array, XMLWriter writer) {
		writer.startTag(name, null);
		for (String element : array)
			writer.printSimpleTag(elementTagName, element);
		writer.endTag(name);
	}

}
