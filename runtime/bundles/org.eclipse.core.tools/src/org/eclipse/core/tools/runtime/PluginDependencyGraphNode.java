/*******************************************************************************
 * Copyright (c) 2002, 2005 IBM Corporation and others.
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
package org.eclipse.core.tools.runtime;

import java.util.HashSet;
import java.util.Set;
import org.eclipse.core.tools.Messages;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.NLS;

/**
 *  This class is used to build up a dependency graph.  The full dependency
 *  graph is just a hash table containing PluginDependencyGraphNode's.  Each
 *  node represents one plug-in.  They are retrievable based on the plug-in
 *  id.  Each node has a list of all the plug-in ids for plug-ins that this
 *  plug-in requires (children) and another list of all the plug-in ids that
 *  require this particular plug-in (ancestors).
 */
public class PluginDependencyGraphNode {

	private BundleDescription descriptor = null;
	private Set<PluginDependencyGraphNode> children = new HashSet<>();
	private Set<PluginDependencyGraphNode> ancestors = new HashSet<>();

	/**
	 * Constructor for this class. Each node is associated with a plug-in so
	 * we accept the plug-in descriptor here and keep it around for later use.
	 */
	public PluginDependencyGraphNode(BundleDescription descriptor) {
		this.descriptor = descriptor;
	}

	/**
	 * Add the given node to this node's set of ancestors.
	 */
	public void addAncestor(PluginDependencyGraphNode ancestor) {
		ancestors.add(ancestor);
	}

	/**
	 * Add the given node to this node's set of children.
	 */
	public void addChild(PluginDependencyGraphNode child) {
		children.add(child);
	}

	/**
	 * Return the identifier for this node. It is the unique plug-in identifier
	 * for this object's plug-in descriptor.
	 *
	 * @return the plug-in id
	 */
	public String getId() {
		return descriptor.getSymbolicName();
	}

	/**
	 * Return a string representation of this object. It should be nicely formated
	 * and include the list of children and ancestor nodes.
	 */
	public String toDeepString() {
		StringBuilder buffer = new StringBuilder();

		// write ID
		writeln(buffer, 0, NLS.bind(Messages.stats_pluginid, descriptor.getSymbolicName()));

		// write ancestors
		if (ancestors.isEmpty()) {
			writeln(buffer, 1, Messages.depend_noParentPlugins);
		} else {
			writeln(buffer, 1, Messages.depend_requiredBy);
			for (PluginDependencyGraphNode ancestor : ancestors) {
				writeln(buffer, 2, ancestor.getId());
			}
		}

		// write children
		if (children.isEmpty()) {
			writeln(buffer, 1, Messages.depend_noChildrenPlugins);
		} else {
			writeln(buffer, 1, Messages.depend_requires);
			for (PluginDependencyGraphNode child : children) {
				writeln(buffer, 2, child.getId());
			}
		}
		return buffer.toString();
	}

	/**
	 * Ultility method to write a string and cr to the given buffer. Indent the
	 * text the given number of tabs.
	 */
	private void writeln(StringBuilder buffer, int indent, String text) {
		for (int i = 0; i < indent; i++)
			buffer.append('\t');
		buffer.append(text);
		buffer.append('\n');
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (!(obj instanceof PluginDependencyGraphNode))
			return false;
		PluginDependencyGraphNode other = (PluginDependencyGraphNode) obj;
		return this.getId().equals(other.getId());
	}

	@Override
	public int hashCode() {
		return getId().hashCode();
	}

	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder();
		buffer.append("PluginDependencyGraphNode("); //$NON-NLS-1$
		buffer.append(descriptor.getSymbolicName());
		buffer.append(')');
		return buffer.toString();
	}
}
