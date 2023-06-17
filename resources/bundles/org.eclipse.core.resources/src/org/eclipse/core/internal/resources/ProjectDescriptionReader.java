/*******************************************************************************
 * Copyright (c) 2000, 2022 IBM Corporation and others.
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
 *     Serge Beauchamp (Freescale Semiconductor) - [229633] Project Path Variable Support
 *     Markus Schorn (Wind River) - [306575] Save snapshot location with project
 *     James Blackburn (Broadcom Corp.) - ongoing development
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 473427
 *     Mickael Istria (Red Hat Inc.) - Bug 488937
 *     Christoph Läubrich - Issue #77 - SaveManager access the ResourcesPlugin.getWorkspace at init phase
 *******************************************************************************/
package org.eclipse.core.internal.resources;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.StringTokenizer;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.internal.events.BuildCommand;
import org.eclipse.core.internal.localstore.SafeFileInputStream;
import org.eclipse.core.internal.utils.Messages;
import org.eclipse.core.internal.utils.Policy;
import org.eclipse.core.resources.FileInfoMatcherDescription;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Reads serialized project descriptions.
 *
 * Note: Suppress warnings on whole class because of unusual use of objectStack.
 */
@SuppressWarnings({"unchecked"})
public class ProjectDescriptionReader extends DefaultHandler implements IModelObjectConstants {

	//states
	protected static final int S_BUILD_COMMAND = 0;
	protected static final int S_BUILD_COMMAND_ARGUMENTS = 1;
	protected static final int S_BUILD_COMMAND_NAME = 2;
	protected static final int S_BUILD_COMMAND_TRIGGERS = 3;
	protected static final int S_BUILD_SPEC = 4;
	protected static final int S_DICTIONARY = 5;
	protected static final int S_DICTIONARY_KEY = 6;
	protected static final int S_DICTIONARY_VALUE = 7;
	protected static final int S_INITIAL = 8;
	protected static final int S_LINK = 9;
	protected static final int S_LINK_LOCATION = 10;
	protected static final int S_LINK_LOCATION_URI = 11;
	protected static final int S_LINK_PATH = 12;
	protected static final int S_LINK_TYPE = 13;
	protected static final int S_LINKED_RESOURCES = 14;
	protected static final int S_NATURE_NAME = 15;
	protected static final int S_NATURES = 16;
	protected static final int S_PROJECT_COMMENT = 17;
	protected static final int S_PROJECT_DESC = 18;
	protected static final int S_PROJECT_NAME = 19;
	protected static final int S_PROJECTS = 20;
	protected static final int S_REFERENCED_PROJECT_NAME = 21;

	protected static final int S_FILTERED_RESOURCES = 23;
	protected static final int S_FILTER = 24;
	protected static final int S_FILTER_ID = 25;
	protected static final int S_FILTER_PATH = 26;
	protected static final int S_FILTER_TYPE = 27;

	protected static final int S_MATCHER = 28;
	protected static final int S_MATCHER_ID = 29;
	protected static final int S_MATCHER_ARGUMENTS = 30;

	protected static final int S_VARIABLE_LIST = 31;
	protected static final int S_VARIABLE = 32;
	protected static final int S_VARIABLE_NAME = 33;
	protected static final int S_VARIABLE_VALUE = 34;

	protected static final int S_SNAPSHOT_LOCATION = 35;

	/**
	 * Singleton sax parser factory
	 */
	private static SAXParserFactory singletonParserFactory;

	/**
	 * Singleton sax parser
	 */
	private static SAXParser singletonParser;

	protected final StringBuilder charBuffer = new StringBuilder();

	protected Deque<Object> objectStack;
	protected MultiStatus problems;

	/**
	 * The project we are reading the description for, or null if unknown.
	 */
	private final IProject project;
	// The project description we are creating.
	ProjectDescription projectDescription = null;

	protected int state = S_INITIAL;
	private Workspace workspace;

	/**
	 * Returns the SAXParser to use when parsing project description files.
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */
	private static synchronized SAXParser createParser(Workspace workspace)
			throws ParserConfigurationException, SAXException {
		//the parser can't be used concurrently, so only use singleton when workspace is locked
		if (workspace == null || !isWorkspaceLocked(workspace))
			return createParserFactory().newSAXParser();
		if (singletonParser == null) {
			singletonParser = createParserFactory().newSAXParser();
		}
		return singletonParser;
	}

	/**
	 * Returns the SAXParserFactory to use when parsing project description files.
	 * @throws ParserConfigurationException
	 */
	private static synchronized SAXParserFactory createParserFactory() throws ParserConfigurationException {
		if (singletonParserFactory == null) {
			singletonParserFactory = SAXParserFactory.newInstance();
			singletonParserFactory.setNamespaceAware(true);
			try {
				singletonParserFactory.setFeature("http://xml.org/sax/features/string-interning", true); //$NON-NLS-1$
			} catch (SAXException e) {
				// In case support for this feature is removed
			}
		}
		return singletonParserFactory;
	}

	private static boolean isWorkspaceLocked(Workspace workspace) {
		try {
			return workspace.getWorkManager().isLockAlreadyAcquired();
		} catch (CoreException e) {
			return false;
		}
	}

	public ProjectDescriptionReader(IWorkspace workspace) {
		this.workspace = (Workspace) workspace;
		this.project = null;
	}

	public ProjectDescriptionReader(IProject project) {
		this.project = project;
		this.workspace = (Workspace) project.getWorkspace();
	}

	/**
	 * @see ContentHandler#characters(char[], int, int)
	 */
	@Override
	public void characters(char[] chars, int offset, int length) {
		//accumulate characters and process them when endElement is reached
		charBuffer.append(chars, offset, length);
	}

	/**
	 * End of an element that is part of a build command
	 */
	private void endBuildCommandElement(String elementName) {
		if (elementName.equals(BUILD_COMMAND)) {
			// Pop this BuildCommand off the stack.
			BuildCommand command = (BuildCommand) objectStack.pop();
			// Add this BuildCommand to a array list of BuildCommands.
			ArrayList<BuildCommand> commandList = (ArrayList<BuildCommand>) objectStack.peek();
			commandList.add(command);
			state = S_BUILD_SPEC;
		}
	}

	/**
	 * End of an element that is part of a build spec
	 */
	private void endBuildSpecElement(String elementName) {
		if (elementName.equals(BUILD_SPEC)) {
			// Pop off the array list of BuildCommands and add them to the
			// ProjectDescription which is the next thing on the stack.
			ArrayList<ICommand> commands = (ArrayList<ICommand>) objectStack.pop();
			state = S_PROJECT_DESC;
			if (commands.isEmpty())
				return;
			ICommand[] commandArray = commands.toArray(new ICommand[commands.size()]);
			projectDescription.setBuildSpec(commandArray);
		}
	}

	/**
	 * End a build triggers element and set the triggers for the current
	 * build command element.
	 */
	private void endBuildTriggersElement(String elementName) {
		if (elementName.equals(BUILD_TRIGGERS)) {
			state = S_BUILD_COMMAND;
			BuildCommand command = (BuildCommand) objectStack.peek();
			//presence of this element indicates the builder is configurable
			command.setConfigurable(true);
			//clear all existing values
			command.setBuilding(IncrementalProjectBuilder.AUTO_BUILD, false);
			command.setBuilding(IncrementalProjectBuilder.CLEAN_BUILD, false);
			command.setBuilding(IncrementalProjectBuilder.FULL_BUILD, false);
			command.setBuilding(IncrementalProjectBuilder.INCREMENTAL_BUILD, false);

			//set new values according to value in the triggers element
			StringTokenizer tokens = new StringTokenizer(charBuffer.toString(), ","); //$NON-NLS-1$
			while (tokens.hasMoreTokens()) {
				String next = tokens.nextToken();
				if (next.equalsIgnoreCase(TRIGGER_AUTO)) {
					command.setBuilding(IncrementalProjectBuilder.AUTO_BUILD, true);
				} else if (next.equalsIgnoreCase(TRIGGER_CLEAN)) {
					command.setBuilding(IncrementalProjectBuilder.CLEAN_BUILD, true);
				} else if (next.equalsIgnoreCase(TRIGGER_FULL)) {
					command.setBuilding(IncrementalProjectBuilder.FULL_BUILD, true);
				} else if (next.equalsIgnoreCase(TRIGGER_INCREMENTAL)) {
					command.setBuilding(IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
				}
			}
		}
	}

	/**
	 * End of a dictionary element
	 */
	private void endDictionary(String elementName) {
		if (elementName.equals(DICTIONARY)) {
			// Pick up the value and then key off the stack and add them
			// to the HashMap which is just below them on the stack.
			// Leave the HashMap on the stack to pick up more key/value
			// pairs if they exist.
			String value = (String) objectStack.pop();
			String key = (String) objectStack.pop();
			((HashMap<String, String>) objectStack.peek()).put(key, value);
			state = S_BUILD_COMMAND_ARGUMENTS;
		}
	}

	private void endDictionaryKey(String elementName) {
		if (elementName.equals(KEY)) {
			// There is a value place holder on the top of the stack and
			// a key place holder just below it.
			String value = (String) objectStack.pop();
			String oldKey = (String) objectStack.pop();
			String newKey = charBuffer.toString();
			if (oldKey != null && oldKey.length() != 0) {
				parseProblem(NLS.bind(Messages.projRead_whichKey, oldKey, newKey));
				objectStack.push(oldKey);
			} else {
				objectStack.push(newKey);
			}
			//push back the dictionary value
			objectStack.push(value);
			state = S_DICTIONARY;
		}
	}

	private void endDictionaryValue(String elementName) {
		if (elementName.equals(VALUE)) {
			String newValue = charBuffer.toString();
			// There is a value place holder on the top of the stack
			String oldValue = (String) objectStack.pop();
			if (oldValue != null && oldValue.length() != 0) {
				parseProblem(NLS.bind(Messages.projRead_whichValue, oldValue, newValue));
				objectStack.push(oldValue);
			} else {
				objectStack.push(newValue);
			}
			state = S_DICTIONARY;
		}
	}

	/**
	 * @see ContentHandler#endElement(String, String, String)
	 */
	@Override
	public void endElement(String uri, String elementName, String qname) {
		switch (state) {
			case S_PROJECT_DESC :
				// Don't think we need to do anything here.
				break;
			case S_PROJECT_NAME :
				if (elementName.equals(NAME)) {
					// Project names cannot have leading/trailing whitespace
					// as they are IResource names.
					projectDescription.setName(charBuffer.toString().trim());
					state = S_PROJECT_DESC;
				}
				break;
			case S_PROJECTS :
				if (elementName.equals(PROJECTS)) {
					endProjectsElement();
					state = S_PROJECT_DESC;
				}
				break;
			case S_DICTIONARY :
				endDictionary(elementName);
				break;
			case S_BUILD_COMMAND_ARGUMENTS :
				if (elementName.equals(ARGUMENTS)) {
					// There is a hashmap on the top of the stack with the
					// arguments (if any).
					HashMap<String, String> dictionaryArgs = (HashMap<String, String>) objectStack.pop();
					state = S_BUILD_COMMAND;
					if (dictionaryArgs.isEmpty())
						break;
					// Below the hashMap on the stack, there is a BuildCommand.
					((BuildCommand) objectStack.peek()).setArguments(dictionaryArgs);
				}
				break;
			case S_BUILD_COMMAND :
				endBuildCommandElement(elementName);
				break;
			case S_BUILD_SPEC :
				endBuildSpecElement(elementName);
				break;
			case S_BUILD_COMMAND_TRIGGERS :
				endBuildTriggersElement(elementName);
				break;
			case S_NATURES :
				endNaturesElement(elementName);
				break;
			case S_LINK :
				endLinkElement(elementName);
				break;
			case S_LINKED_RESOURCES :
				endLinkedResourcesElement(elementName);
				break;
			case S_VARIABLE :
				endVariableElement(elementName);
				break;
			case S_FILTER :
				endFilterElement(elementName);
				break;
			case S_FILTERED_RESOURCES :
				endFilteredResourcesElement(elementName);
				break;
			case S_VARIABLE_LIST :
				endVariableListElement(elementName);
				break;
			case S_PROJECT_COMMENT :
				if (elementName.equals(COMMENT)) {
					projectDescription.setComment(charBuffer.toString());
					state = S_PROJECT_DESC;
				}
				break;
			case S_REFERENCED_PROJECT_NAME :
				if (elementName.equals(PROJECT)) {
					//top of stack is list of project references
					// Referenced projects are just project names and, therefore,
					// are also IResource names and cannot have leading/trailing
					// whitespace.
					((ArrayList<String>) objectStack.peek()).add(charBuffer.toString().trim());
					state = S_PROJECTS;
				}
				break;
			case S_BUILD_COMMAND_NAME :
				if (elementName.equals(NAME)) {
					//top of stack is the build command
					// A build command name is an extension id and
					// cannot have leading/trailing whitespace.
					((BuildCommand) objectStack.peek()).setName(charBuffer.toString().trim());
					state = S_BUILD_COMMAND;
				}
				break;
			case S_DICTIONARY_KEY :
				endDictionaryKey(elementName);
				break;
			case S_DICTIONARY_VALUE :
				endDictionaryValue(elementName);
				break;
			case S_NATURE_NAME :
				if (elementName.equals(NATURE)) {
					//top of stack is list of nature names
					// A nature name is an extension id and cannot
					// have leading/trailing whitespace.
					((ArrayList<String>) objectStack.peek()).add(charBuffer.toString().trim());
					state = S_NATURES;
				}
				break;
			case S_LINK_PATH :
				endLinkPath(elementName);
				break;
			case S_LINK_TYPE :
				endLinkType(elementName);
				break;
			case S_LINK_LOCATION :
				endLinkLocation(elementName);
				break;
			case S_LINK_LOCATION_URI :
				endLinkLocationURI(elementName);
				break;
			case S_FILTER_ID :
				endFilterId(elementName);
				break;
			case S_FILTER_PATH :
				endFilterPath(elementName);
				break;
			case S_FILTER_TYPE :
				endFilterType(elementName);
				break;
			case S_MATCHER :
				endMatcherElement(elementName);
				break;
			case S_MATCHER_ID :
				endMatcherID(elementName);
				break;
			case S_MATCHER_ARGUMENTS :
				endMatcherArguments(elementName);
				break;
			case S_VARIABLE_NAME :
				endVariableName(elementName);
				break;
			case S_VARIABLE_VALUE :
				endVariableValue(elementName);
				break;
			case S_SNAPSHOT_LOCATION :
				endSnapshotLocation(elementName);
				break;
		}
		charBuffer.setLength(0);
	}

	/**
	 * End this group of linked resources and add them to the project description.
	 */
	private void endLinkedResourcesElement(String elementName) {
		if (elementName.equals(LINKED_RESOURCES)) {
			HashMap<IPath, LinkDescription> linkedResources = (HashMap<IPath, LinkDescription>) objectStack.pop();
			state = S_PROJECT_DESC;
			if (linkedResources.isEmpty())
				return;
			projectDescription.setLinkDescriptions(linkedResources);
		}
	}

	/**
	 * End this group of linked resources and add them to the project description.
	 */
	private void endFilteredResourcesElement(String elementName) {
		if (elementName.equals(FILTERED_RESOURCES)) {
			HashMap<IPath, LinkedList<FilterDescription>> filteredResources = (HashMap<IPath, LinkedList<FilterDescription>>) objectStack.pop();
			state = S_PROJECT_DESC;
			if (filteredResources.isEmpty())
				return;
			projectDescription.setFilterDescriptions(filteredResources);
		}
	}

	/**
	 * End this group of group resources and add them to the project
	 * description.
	 */
	private void endVariableListElement(String elementName) {
		if (elementName.equals(VARIABLE_LIST)) {
			HashMap<String, VariableDescription> variableList = (HashMap<String, VariableDescription>) objectStack.pop();
			state = S_PROJECT_DESC;
			if (variableList.isEmpty())
				return;
			projectDescription.setVariableDescriptions(variableList);
		}
	}

	/**
	 * End a single linked resource and add it to the HashMap.
	 */
	private void endLinkElement(String elementName) {
		if (elementName.equals(LINK)) {
			state = S_LINKED_RESOURCES;
			// Pop off the link description
			LinkDescription link = (LinkDescription) objectStack.pop();
			// Make sure that you have something reasonable
			IPath path = link.getProjectRelativePath();
			int type = link.getType();
			URI location = link.getLocationURI();
			if (location == null) {
				parseProblem(NLS.bind(Messages.projRead_badLinkLocation, path, Integer.toString(type)));
				return;
			}
			if ((path == null) || path.segmentCount() == 0) {
				parseProblem(NLS.bind(Messages.projRead_emptyLinkName, Integer.toString(type), location));
				return;
			}
			if (type == -1) {
				parseProblem(NLS.bind(Messages.projRead_badLinkType, path, location));
				return;
			}

			// The HashMap of linked resources is the next thing on the stack
			((HashMap<IPath, LinkDescription>) objectStack.peek()).put(link.getProjectRelativePath(), link);
		}
	}

	private void endMatcherElement(String elementName) {
		if (elementName.equals(MATCHER)) {
			// Pop off an array (Object[2]) containing the matcher id and arguments.
			Object[] matcher = (Object[]) objectStack.pop();
			// Make sure that you have something reasonable
			String id = (String) matcher[0];
			// the id can't be null
			if (id == null) {
				parseProblem(NLS.bind(Messages.projRead_badFilterID, null));
				return;
			}

			if (objectStack.peek() instanceof ArrayList) {
				state = S_MATCHER_ARGUMENTS;
				// The ArrayList of matchers is the next thing on the stack
				ArrayList<FileInfoMatcherDescription> list = ((ArrayList<FileInfoMatcherDescription>) objectStack.peek());
				list.add(new FileInfoMatcherDescription((String) matcher[0], matcher[1]));
			}

			if (objectStack.peek() instanceof FilterDescription) {
				state = S_FILTER;
				FilterDescription d = ((FilterDescription) objectStack.peek());
				d.setFileInfoMatcherDescription(new FileInfoMatcherDescription((String) matcher[0], matcher[1]));
			}
		}
	}

	/**
	 * End a single filtered resource and add it to the HashMap.
	 */
	private void endFilterElement(String elementName) {
		if (elementName.equals(FILTER)) {
			// Pop off the filter description
			FilterDescription filter = (FilterDescription) objectStack.pop();
			if (project != null) {
				// Make sure that you have something reasonable
				IPath path = filter.getResource().getProjectRelativePath();
				int type = filter.getType();
				// arguments can be null
				if (path == null) {
					parseProblem(NLS.bind(Messages.projRead_emptyFilterName, Integer.toString(type)));
					return;
				}
				if (type == -1) {
					parseProblem(NLS.bind(Messages.projRead_badFilterType, path));
					return;
				}

				// The HashMap of filtered resources is the next thing on the stack
				HashMap<IPath, LinkedList<FilterDescription>> map = ((HashMap<IPath, LinkedList<FilterDescription>>) objectStack.peek());
				LinkedList<FilterDescription> list = map.get(filter.getResource().getProjectRelativePath());
				if (list == null) {
					list = new LinkedList<>();
					map.put(filter.getResource().getProjectRelativePath(), list);
				}
				list.add(filter);
			} else {
				// if the project is null, that means that we're loading a project description to retrieve
				// some meta data only.
				String key = ""; // an empty key; //$NON-NLS-1$
				HashMap<String, LinkedList<FilterDescription>> map = ((HashMap<String, LinkedList<FilterDescription>>) objectStack.peek());
				LinkedList<FilterDescription> list = map.get(key);
				if (list == null) {
					list = new LinkedList<>();
					map.put(key, list);
				}
				list.add(filter);
			}
			state = S_FILTERED_RESOURCES;
		}
	}

	/**
	 * End a single group resource and add it to the HashMap.
	 */
	private void endVariableElement(String elementName) {
		if (elementName.equals(VARIABLE)) {
			state = S_VARIABLE_LIST;
			// Pop off the link description
			VariableDescription desc = (VariableDescription) objectStack.pop();
			// Make sure that you have something reasonable
			if (desc.getName().length() == 0) {
				parseProblem(NLS.bind(Messages.projRead_emptyVariableName, project.getName()));
				return;
			}

			// The HashMap of variables is the next thing on the stack
			((HashMap<String, VariableDescription>) objectStack.peek()).put(desc.getName(), desc);
		}
	}

	/**
	 * For backwards compatibility, link locations in the local file system are represented
	 * in the project description under the "location" tag.
	 * @param elementName
	 */
	private void endLinkLocation(String elementName) {
		if (elementName.equals(LOCATION)) {
			// A link location is an URI.  URIs cannot have leading/trailing whitespace
			String newLocation = charBuffer.toString().trim();
			// objectStack has a LinkDescription on it. Set the type on this LinkDescription.
			URI oldLocation = ((LinkDescription) objectStack.peek()).getLocationURI();
			if (oldLocation != null) {
				parseProblem(NLS.bind(Messages.projRead_badLocation, oldLocation, newLocation));
			} else {
				((LinkDescription) objectStack.peek())
						.setLocationURI(URIUtil.toURI(IPath.fromPortableString(newLocation)));
			}
			state = S_LINK;
		}
	}

	/**
	 * Link locations that are not stored in the local file system are represented
	 * in the project description under the "locationURI" tag.
	 * @param elementName
	 */
	private void endLinkLocationURI(String elementName) {
		if (elementName.equals(LOCATION_URI)) {
			// A link location is an URI.  URIs cannot have leading/trailing whitespace
			String newLocation = charBuffer.toString().trim();
			// objectStack has a LinkDescription on it. Set the type on this LinkDescription.
			URI oldLocation = ((LinkDescription) objectStack.peek()).getLocationURI();
			if (oldLocation != null) {
				parseProblem(NLS.bind(Messages.projRead_badLocation, oldLocation, newLocation));
			} else {
				try {
					((LinkDescription) objectStack.peek()).setLocationURI(new URI(newLocation));
				} catch (URISyntaxException e) {
					String msg = Messages.projRead_failureReadingProjectDesc;
					problems.add(new Status(IStatus.WARNING, ResourcesPlugin.PI_RESOURCES, IResourceStatus.FAILED_READ_METADATA, msg, e));
				}
			}
			state = S_LINK;
		}
	}

	private void endLinkPath(String elementName) {
		if (elementName.equals(NAME)) {
			IPath newPath = IPath.fromOSString(charBuffer.toString());
			// objectStack has a LinkDescription on it. Set the name
			// on this LinkDescription.
			IPath oldPath = ((LinkDescription) objectStack.peek()).getProjectRelativePath();
			if (oldPath.segmentCount() != 0) {
				parseProblem(NLS.bind(Messages.projRead_badLinkName, oldPath, newPath));
			} else {
				((LinkDescription) objectStack.peek()).setPath(newPath);
			}
			state = S_LINK;
		}
	}

	private void endMatcherID(String elementName) {
		if (elementName.equals(ID)) {
			// The matcher id is String.
			String newID = charBuffer.toString().trim();
			// objectStack has an array (Object[2]) on it for the matcher id and arguments.
			String oldID = (String) ((Object[]) objectStack.peek())[0];
			if (oldID != null) {
				parseProblem(NLS.bind(Messages.projRead_badID, oldID, newID));
			} else {
				((Object[]) objectStack.peek())[0] = newID;
			}
			state = S_MATCHER;
		}
	}

	private void endMatcherArguments(String elementName) {
		if (elementName.equals(ARGUMENTS)) {
			ArrayList<FileInfoMatcherDescription> matchers = (ArrayList<FileInfoMatcherDescription>) objectStack.pop();
			Object newArguments = charBuffer.toString();

			if (matchers.size() > 0)
				newArguments = matchers.toArray(new FileInfoMatcherDescription[matchers.size()]);

			// objectStack has an array (Object[2]) on it for the matcher id and arguments.
			String oldArguments = (String) ((Object[]) objectStack.peek())[1];
			if (oldArguments != null) {
				parseProblem(NLS.bind(Messages.projRead_badArguments, oldArguments, newArguments));
			} else
				((Object[]) objectStack.peek())[1] = newArguments;
			state = S_MATCHER;
		}
	}

	private void endFilterId(String elementName) {
		if (elementName.equals(ID)) {
			Long newId = Long.parseLong(charBuffer.toString());
			// objectStack has a FilterDescription on it. Set the name
			// on this FilterDescription.
			long oldId = ((FilterDescription) objectStack.peek()).getId();
			if (oldId != 0) {
				parseProblem(NLS.bind(Messages.projRead_badFilterName, oldId, newId));
			} else {
				((FilterDescription) objectStack.peek()).setId(newId.longValue());
			}
			state = S_FILTER;
		}
	}

	private void endFilterPath(String elementName) {
		if (elementName.equals(NAME)) {
			IPath newPath = IPath.fromOSString(charBuffer.toString());
			// objectStack has a FilterDescription on it. Set the name
			// on this FilterDescription.
			IResource oldResource = ((FilterDescription) objectStack.peek()).getResource();
			if (oldResource != null) {
				parseProblem(NLS.bind(Messages.projRead_badFilterName, oldResource.getProjectRelativePath(), newPath));
			} else {
				if (project != null) {
					((FilterDescription) objectStack.peek()).setResource(newPath.isEmpty() ? (IResource) project : project.getFolder(newPath));
				} else {
					// if the project is null, that means that we're loading a project description to retrieve
					// some meta data only.
					((FilterDescription) objectStack.peek()).setResource(null);
				}
			}
			state = S_FILTER;
		}
	}

	private void endFilterType(String elementName) {
		if (elementName.equals(TYPE)) {
			int newType = -1;
			try {
				// parseInt expects a string containing only numerics
				// or a leading '-'.  Ensure there is no leading/trailing
				// whitespace.
				newType = Integer.parseInt(charBuffer.toString().trim());
			} catch (NumberFormatException e) {
				log(e);
			}
			// objectStack has a FilterDescription on it. Set the type
			// on this FilterDescription.
			int oldType = ((FilterDescription) objectStack.peek()).getType();
			if (oldType != -1) {
				parseProblem(NLS.bind(Messages.projRead_badFilterType2, Integer.toString(oldType), Integer.toString(newType)));
			} else {
				((FilterDescription) objectStack.peek()).setType(newType);
			}
			state = S_FILTER;
		}
	}

	private void endVariableName(String elementName) {
		if (elementName.equals(NAME)) {
			String value = charBuffer.toString();
			// objectStack has a VariableDescription on it. Set the value
			// on this ValueDescription.
			((VariableDescription) objectStack.peek()).setName(value);
			state = S_VARIABLE;
		}
	}

	private void endVariableValue(String elementName) {
		if (elementName.equals(VALUE)) {
			String value = charBuffer.toString();
			// objectStack has a VariableDescription on it. Set the value
			// on this ValueDescription.
			((VariableDescription) objectStack.peek()).setValue(value);
			state = S_VARIABLE;
		}
	}

	private void endLinkType(String elementName) {
		if (elementName.equals(TYPE)) {
			//FIXME we should handle this case by removing the entire link
			//for now we default to a file link
			int newType = IResource.FILE;
			try {
				// parseInt expects a string containing only numerics
				// or a leading '-'.  Ensure there is no leading/trailing
				// whitespace.
				newType = Integer.parseInt(charBuffer.toString().trim());
			} catch (NumberFormatException e) {
				log(e);
			}
			// objectStack has a LinkDescription on it. Set the type
			// on this LinkDescription.
			int oldType = ((LinkDescription) objectStack.peek()).getType();
			if (oldType != -1) {
				parseProblem(NLS.bind(Messages.projRead_badLinkType2, Integer.toString(oldType), Integer.toString(newType)));
			} else {
				((LinkDescription) objectStack.peek()).setType(newType);
			}
			state = S_LINK;
		}
	}

	/**
	 * End of an element that is part of a nature list
	 */
	private void endNaturesElement(String elementName) {
		if (elementName.equals(NATURES)) {
			// Pop the array list of natures off the stack
			ArrayList<String> natures = (ArrayList<String>) objectStack.pop();
			state = S_PROJECT_DESC;
			if (natures.isEmpty())
				return;
			String[] natureNames = natures.toArray(new String[natures.size()]);
			projectDescription.setNatureIds(natureNames);
		}
	}

	/**
	 * End of an element that is part of a project references list
	 */
	private void endProjectsElement() {
		// Pop the array list that contains all the referenced project names
		ArrayList<String> referencedProjects = (ArrayList<String>) objectStack.pop();
		if (referencedProjects.isEmpty())
			// Don't bother adding an empty group of referenced projects to the
			// project descriptor.
			return;
		IWorkspaceRoot root = workspace.getRoot();
		IProject[] projects = new IProject[referencedProjects.size()];
		for (int i = 0; i < projects.length; i++) {
			projects[i] = root.getProject(referencedProjects.get(i));
		}
		projectDescription.setReferencedProjects(projects);
	}

	private void endSnapshotLocation(String elementName) {
		if (elementName.equals(SNAPSHOT_LOCATION)) {
			String location = charBuffer.toString().trim();
			try {
				projectDescription.setSnapshotLocationURI(new URI(location));
			} catch (URISyntaxException e) {
				String msg = NLS.bind(Messages.projRead_badSnapshotLocation, location);
				problems.add(new Status(IStatus.WARNING, ResourcesPlugin.PI_RESOURCES, IResourceStatus.FAILED_READ_METADATA, msg, e));
			}
			state = S_PROJECT_DESC;
		}
	}

	/**
	 * @see ErrorHandler#error(SAXParseException)
	 */
	@Override
	public void error(SAXParseException error) {
		log(error);
	}

	/**
	 * @see ErrorHandler#fatalError(SAXParseException)
	 */
	@Override
	public void fatalError(SAXParseException error) throws SAXException {
		// ensure a null value is not passed as message to Status constructor (bug 42782)
		String message = error.getMessage();
		if (project != null)
			message = NLS.bind(Messages.resources_readMeta, project.getName());
		problems.add(new Status(IStatus.ERROR, ResourcesPlugin.PI_RESOURCES, IResourceStatus.FAILED_READ_METADATA, message == null ? "" : message, error)); //$NON-NLS-1$
		throw error;
	}

	protected void log(Exception ex) {
		String message = ex.getMessage();
		if (project != null)
			message = NLS.bind(Messages.resources_readMeta, project.getName());
		problems.add(new Status(IStatus.WARNING, ResourcesPlugin.PI_RESOURCES, IResourceStatus.FAILED_READ_METADATA, message == null ? "" : message, ex)); //$NON-NLS-1$
	}

	private void parseProblem(String errorMessage) {
		problems.add(new Status(IStatus.WARNING, ResourcesPlugin.PI_RESOURCES, IResourceStatus.FAILED_READ_METADATA, errorMessage, null));
	}

	private void parseProjectDescription(String elementName) {
		if (elementName.equals(NAME)) {
			state = S_PROJECT_NAME;
			return;
		}
		if (elementName.equals(COMMENT)) {
			state = S_PROJECT_COMMENT;
			return;
		}
		if (elementName.equals(PROJECTS)) {
			state = S_PROJECTS;
			// Push an array list on the object stack to hold the name
			// of all the referenced projects.  This array list will be
			// popped off the stack, massaged into the right format
			// and added to the project description when we hit the
			// end element for PROJECTS.
			objectStack.push(new ArrayList<>());
			return;
		}
		if (elementName.equals(BUILD_SPEC)) {
			state = S_BUILD_SPEC;
			// Push an array list on the object stack to hold the build commands
			// for this build spec.  This array list will be popped off the stack,
			// massaged into the right format and added to the project's build
			// spec when we hit the end element for BUILD_SPEC.
			objectStack.push(new ArrayList<>());
			return;
		}
		if (elementName.equals(NATURES)) {
			state = S_NATURES;
			// Push an array list to hold all the nature names.
			objectStack.push(new ArrayList<>());
			return;
		}
		if (elementName.equals(LINKED_RESOURCES)) {
			// Push a HashMap to collect all the links.
			objectStack.push(new HashMap<>());
			state = S_LINKED_RESOURCES;
			return;
		}
		if (elementName.equals(FILTERED_RESOURCES)) {
			// Push a HashMap to collect all the filters.
			objectStack.push(new HashMap<>());
			state = S_FILTERED_RESOURCES;
			return;
		}
		if (elementName.equals(VARIABLE_LIST)) {
			// Push a HashMap to collect all the variables.
			objectStack.push(new HashMap<>());
			state = S_VARIABLE_LIST;
			return;
		}
		if (elementName.equals(SNAPSHOT_LOCATION)) {
			state = S_SNAPSHOT_LOCATION;
			return;
		}
	}

	public ProjectDescription read(InputSource input) {
		problems = new MultiStatus(ResourcesPlugin.PI_RESOURCES, IResourceStatus.FAILED_READ_METADATA, Messages.projRead_failureReadingProjectDesc, null);
		objectStack = new ArrayDeque<>();
		state = S_INITIAL;
		try {
			createParser(workspace).parse(input, this);
		} catch (ParserConfigurationException | IOException | SAXException e) {
			log(e);
		}

		if (projectDescription != null && projectDescription.getName() == null)
			parseProblem(Messages.projRead_missingProjectName);

		switch (problems.getSeverity()) {
			case IStatus.ERROR :
				Policy.log(problems);
				return null;
			case IStatus.WARNING :
			case IStatus.INFO :
				Policy.log(problems);
			case IStatus.OK :
			default :
				return projectDescription;
		}
	}

	/**
	 * Reads and returns a project description stored at the given location
	 */
	public ProjectDescription read(IPath location) throws IOException {
		try (
			BufferedInputStream file = new BufferedInputStream(new FileInputStream(location.toFile()));
		) {
			return read(new InputSource(file));
		}
	}

	/**
	 * Reads and returns a project description stored at the given location, or
	 * temporary location.
	 */
	public ProjectDescription read(IPath location, IPath tempLocation) throws IOException {
		try (
			SafeFileInputStream file = new SafeFileInputStream(location.toOSString(), tempLocation.toOSString());
		) {
			return read(new InputSource(file));
		}
	}

	/**
	 * @see ContentHandler#startElement(String, String, String, Attributes)
	 */
	@Override
	public void startElement(String uri, String elementName, String qname, Attributes attributes) throws SAXException {
		//clear the character buffer at the start of every element
		charBuffer.setLength(0);
		switch (state) {
			case S_INITIAL :
				if (elementName.equals(PROJECT_DESCRIPTION)) {
					state = S_PROJECT_DESC;
					projectDescription = new ProjectDescription();
				} else {
					throw (new SAXException(NLS.bind(Messages.projRead_notProjectDescription, elementName)));
				}
				break;
			case S_PROJECT_DESC :
				parseProjectDescription(elementName);
				break;
			case S_PROJECTS :
				if (elementName.equals(PROJECT)) {
					state = S_REFERENCED_PROJECT_NAME;
				}
				break;
			case S_BUILD_SPEC :
				if (elementName.equals(BUILD_COMMAND)) {
					state = S_BUILD_COMMAND;
					objectStack.push(new BuildCommand());
				}
				break;
			case S_BUILD_COMMAND :
				if (elementName.equals(NAME)) {
					state = S_BUILD_COMMAND_NAME;
				} else if (elementName.equals(BUILD_TRIGGERS)) {
					state = S_BUILD_COMMAND_TRIGGERS;
				} else if (elementName.equals(ARGUMENTS)) {
					state = S_BUILD_COMMAND_ARGUMENTS;
					// Push a HashMap to hold all the key/value pairs which
					// will become the argument list.
					objectStack.push(new HashMap<>());
				}
				break;
			case S_BUILD_COMMAND_ARGUMENTS :
				if (elementName.equals(DICTIONARY)) {
					state = S_DICTIONARY;
					// Push 2 strings for the key/value pair to be read
					objectStack.push(""); // key //$NON-NLS-1$
					objectStack.push(""); // value //$NON-NLS-1$
				}
				break;
			case S_DICTIONARY :
				if (elementName.equals(KEY)) {
					state = S_DICTIONARY_KEY;
				} else if (elementName.equals(VALUE)) {
					state = S_DICTIONARY_VALUE;
				}
				break;
			case S_NATURES :
				if (elementName.equals(NATURE)) {
					state = S_NATURE_NAME;
				}
				break;
			case S_LINKED_RESOURCES :
				if (elementName.equals(LINK)) {
					state = S_LINK;
					// Push place holders for the name, type and location of
					// this link.
					objectStack.push(new LinkDescription());
				}
				break;
			case S_VARIABLE_LIST :
				if (elementName.equals(VARIABLE)) {
					state = S_VARIABLE;
					// Push place holders for the name, type and location of
					// this link.
					objectStack.push(new VariableDescription());
				}
				break;
			case S_LINK :
				if (elementName.equals(NAME)) {
					state = S_LINK_PATH;
				} else if (elementName.equals(TYPE)) {
					state = S_LINK_TYPE;
				} else if (elementName.equals(LOCATION)) {
					state = S_LINK_LOCATION;
				} else if (elementName.equals(LOCATION_URI)) {
					state = S_LINK_LOCATION_URI;
				}
				break;
			case S_FILTERED_RESOURCES :
				if (elementName.equals(FILTER)) {
					state = S_FILTER;
					// Push place holders for the name, type, id and arguments of
					// this filter.
					objectStack.push(new FilterDescription());
				}
				break;
			case S_FILTER :
				if (elementName.equals(ID)) {
					state = S_FILTER_ID;
				} else if (elementName.equals(NAME)) {
					state = S_FILTER_PATH;
				} else if (elementName.equals(TYPE)) {
					state = S_FILTER_TYPE;
				} else if (elementName.equals(MATCHER)) {
					state = S_MATCHER;
					// Push an array for the matcher id and arguments
					objectStack.push(new Object[2]);
				}
				break;
			case S_MATCHER :
				if (elementName.equals(ID)) {
					state = S_MATCHER_ID;
				} else if (elementName.equals(ARGUMENTS)) {
					state = S_MATCHER_ARGUMENTS;
					objectStack.push(new ArrayList<>());
				}
				break;
			case S_MATCHER_ARGUMENTS :
				if (elementName.equals(MATCHER)) {
					state = S_MATCHER;
					// Push an array for the matcher id and arguments
					objectStack.push(new Object[2]);
				}
				break;
			case S_VARIABLE :
				if (elementName.equals(NAME)) {
					state = S_VARIABLE_NAME;
				} else if (elementName.equals(VALUE)) {
					state = S_VARIABLE_VALUE;
				}
				break;
		}
	}

	/**
	 * @see ErrorHandler#warning(SAXParseException)
	 */
	@Override
	public void warning(SAXParseException error) {
		log(error);
	}
}
