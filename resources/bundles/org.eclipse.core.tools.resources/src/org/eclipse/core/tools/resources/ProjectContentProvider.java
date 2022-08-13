/*******************************************************************************
 * Copyright (c) 2002, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tools.resources;

import java.util.ArrayList;
import java.util.Map;
import org.eclipse.core.internal.events.BuildManager;
import org.eclipse.core.internal.events.BuilderPersistentInfo;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.internal.watson.ElementTree;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.tools.AbstractTreeContentProvider;
import org.eclipse.core.tools.TreeContentProviderNode;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.Viewer;

/**
 * Constructs a tree made of <code>TreeContentProviderNode</code>s, representing
 * several details about projects.
 * Details include:
 * <ul>
 *   <li>project's name;</li>
 *   <li>the builders associated to this project. For each builder it shows:<ul>
 *       <li>the builder's name;</li>
 *       <li>the arguments;</li>
 *       <li>persistent info (interesting projects &amp; last built tree);</li>
 *   </ul></li>

 *   <li>the natures associated to this project. For each nature it shows:<ul>
 *       <li>the nature's label &amp; Id;</li>
 *       <li>the nature sets it belongs to;</li>
 *       <li>the required natures;</li>
 *   </ul></li>
 * </ul>
 *
 *
 * @see org.eclipse.core.tools.TreeContentProviderNode
 */
@SuppressWarnings("restriction")
public class ProjectContentProvider extends AbstractTreeContentProvider {

	/**
	 * Collects project info. Calls all other <code>extract...</code> methods.
	 *
	 * @param project the project object from where to extract information
	 */
	protected void extractInfo(IProject project) {
		try {
			IProjectDescription description = project.getDescription();
			extractBasicInfo(project);
			extractBuildersInfo(project, description);
			extractNaturesInfo(description);
		} catch (CoreException ce) {
			getRootNode().addChild(createNode("Error accessing project details", ce)); //$NON-NLS-1$
		}
	}

	/**
	 * Extracts basic project information.
	 *
	 * @param project the project from where to extract information
	 */

	protected void extractBasicInfo(IProject project) {
		getRootNode().addChild(createNode("Project name", project.getName())); //$NON-NLS-1$
	}

	/**
	 * Extracts builders information from the project info object, building a 'Builders'
	 * subtree in the project information tree.
	 *
	 * @param project the project from where to extract information
	 * @param description the description from where to extract information
	 */

	protected void extractBuildersInfo(IProject project, IProjectDescription description) {
		ICommand[] commands = description.getBuildSpec();
		if (commands.length == 0) {
			return;
		}
		// tries to retrieve builder persistent info for all builders in this
		// project's builder spec
		Workspace workspace = (Workspace) ResourcesPlugin.getWorkspace();
		BuildManager manager = workspace.getBuildManager();
		ArrayList<BuilderPersistentInfo> allPersistInfo = null;
		try {
			allPersistInfo = manager.getBuildersPersistentInfo(project);
		} catch (CoreException ce) {
			getRootNode().addChild(createNode("Error extracting builders info: " + ce, null)); //$NON-NLS-1$
			return;
		}

		// extracts information for each builder
		TreeContentProviderNode buildersRoot = createNode("Builders"); //$NON-NLS-1$
		getRootNode().addChild(buildersRoot);
		for (ICommand command : commands) {
			String builderName = command.getBuilderName();
			TreeContentProviderNode builderNode = createNode("Builder name", builderName); //$NON-NLS-1$
			buildersRoot.addChild(builderNode);

			// extracts information about arguments
			extractBuilderArguments(builderNode, command.getArguments());

			// extracts information from persistent info (if available)
			if (allPersistInfo != null) {
				//find persistent info for this builder
				for (BuilderPersistentInfo info : allPersistInfo) {
					if (info.getBuilderName().equals(builderName)) {
						extractBuilderPersistentInfo(builderNode, info);
						break;
					}
				}
			}
		}
	}

	/**
	 * Extracts information related to builder arguments for a given builder.
	 *
	 * @param builderNode the node where to add builder arguments nodes
	 * @param builderArgs a map containing arguments for a builder
	 */
	protected void extractBuilderArguments(TreeContentProviderNode builderNode, Map<String, String> builderArgs) {
		if (builderArgs == null || builderArgs.isEmpty()) {
			return;
		}
		TreeContentProviderNode builderArgsRoot = createNode("Builder args"); //$NON-NLS-1$
		builderNode.addChild(builderArgsRoot);
		for (Map.Entry<String, String> entry : builderArgs.entrySet()) {
			TreeContentProviderNode builderArgNode = createNode(entry.getKey(), entry.getValue());
			builderArgsRoot.addChild(builderArgNode);
		}
	}

	/**
	 * Extracts builder persistent info, if any.
	 *
	 * @param builderNode the node where to add persistent info nodes
	 * @param persistInfo the persistent info
	 */
	protected void extractBuilderPersistentInfo(TreeContentProviderNode builderNode, BuilderPersistentInfo persistInfo) {

		if (persistInfo == null) {
			return;
		}
		// extracts information about interesting projects
		IProject[] interestingProjects = persistInfo.getInterestingProjects();
		if (interestingProjects.length > 0) {
			TreeContentProviderNode interestingRoot = createNode("Interesting projects"); //$NON-NLS-1$
			for (IProject interestingProject : interestingProjects) {
				interestingRoot.addChild(createNode(interestingProject.getName()));
			}
			builderNode.addChild(interestingRoot);
		}

		// extracts information about last built tree (just a String rep for now)
		ElementTree lastBuiltTree = persistInfo.getLastBuiltTree();
		builderNode.addChild(createNode("Last built tree", lastBuiltTree.toString())); //$NON-NLS-1$
	}

	/**
	 * Extracts from project description object all information regarding natures,
	 * building a 'Natures' subtree in the projects information tree.
	 *
	 * @param description a project description
	 */
	protected void extractNaturesInfo(IProjectDescription description) {
		String[] natureIds = description.getNatureIds();
		if (natureIds.length == 0) {
			return;
		}
		// extracts information about each nature assigned to the project
		TreeContentProviderNode naturesRoot = createNode("Natures"); //$NON-NLS-1$
		getRootNode().addChild(naturesRoot);
		Workspace workspace = (Workspace) ResourcesPlugin.getWorkspace();
		for (String natureId : natureIds) {
			IProjectNatureDescriptor descriptor = workspace.getNatureDescriptor(natureId);
			String nodeName = (descriptor == null ? "Missing" : descriptor.getLabel()) + " (" + natureId + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			TreeContentProviderNode natureNode = createNode(nodeName);
			naturesRoot.addChild(natureNode);

			// we may not have the descriptor for this nature if (for instance) the
			// project was created in a different workspace with a plug-in that we don't have
			if (descriptor != null) {
				// lists nature sets to which the current nature belongs
				String[] natureSets = descriptor.getNatureSetIds();
				if (natureSets.length > 0) {
					TreeContentProviderNode setsNode = createNode("Nature sets"); //$NON-NLS-1$
					for (String natureSet : natureSets) {
						setsNode.addChild(createNode(natureSet));
					}
					natureNode.addChild(setsNode);
				}

				// lists nature sets which the current nature requires
				String[] requiredNatures = descriptor.getRequiredNatureIds();
				if (requiredNatures.length > 0) {
					TreeContentProviderNode requiredNaturesNode = createNode("Required natures", null); //$NON-NLS-1$
					for (String requiredNature : requiredNatures) {
						requiredNaturesNode.addChild(createNode(requiredNature, null));
					}
					natureNode.addChild(requiredNaturesNode);
				}
			}
		}
	}

	/**
	 * Reconstructs this content provider data model upon the provided input object.
	 *
	 * @param input the new input object - must not be null
	 */
	@Override
	protected void rebuild(Viewer viewer, final Object input) {
		SafeRunner.run(new SafeRunnable() {
			@Override
			public void run() throws Exception {
				extractInfo(((IResource) input).getProject());
			}
		});
	}

	/**
	 * Returns true if the input is a resource.
	 *
	 * @param input an input object
	 * @return true if the provided input is a resource
	 * @see org.eclipse.core.tools.AbstractTreeContentProvider#acceptInput(java.lang.Object)
	 */
	@Override
	protected boolean acceptInput(Object input) {
		return input instanceof IResource;
	}

}
