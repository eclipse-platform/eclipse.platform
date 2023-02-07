/*******************************************************************************
 * Copyright (c) 2010, 2015 IBM Corporation and others.
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

package org.eclipse.help.internal.base.scope;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.help.IIndexEntry;
import org.eclipse.help.IIndexEntry2;
import org.eclipse.help.IIndexSee;
import org.eclipse.help.IToc;
import org.eclipse.help.ITopic;
import org.eclipse.help.IUAElement;
import org.eclipse.help.base.AbstractHelpScope;
import org.eclipse.help.internal.UAElement;
import org.eclipse.help.internal.index.Index;
import org.eclipse.help.internal.index.IndexSee;

/**
 * Utilities to test for enabled topics, index entries etc.
 */

public class ScopeUtils {


	/*
	 * Function to determine whether a topic should be shown in the toc.
	 * For hierarchical scopes the element itself must be in scope.
	 * For non hierarchical scopes if any child is in scope the element should show.
	 * A toc with no in scope topics does not display
	 */
	public static boolean showInTree(IToc toc, AbstractHelpScope scope) {
		if (scope.isHierarchicalScope() && !scope.inScope(toc)) {
			return false;
		}
		return hasInScopeDescendent(toc, scope);

	}

	/*
	 * Function to determine whether a topic should be shown in the toc.
	 * For hierarchical scopes the element itself must be in scope.
	 * For non hierarchical scopes if any child is in scope the element should show.
	 * Leaf topics with no href do not show in the toc
	 */
	public static boolean showInTree(ITopic topic, AbstractHelpScope scope) {
		if (scope.inScope(topic)) {
			return (topic.getHref() != null) || hasInScopeDescendent(topic, scope);
		}
		return !scope.isHierarchicalScope() && hasInScopeDescendent(topic, scope);
	}

	/*
	 * Function to determine whether an entry should be shown in the index.
	 * For hierarchical scopes the element itself must be in scope.
	 * For non hierarchical scopes if any child is in scope the element should show.
	 * An entry with no topic descendants does not display
	 */
	public static boolean showInTree(IIndexEntry entry, AbstractHelpScope scope) {
		if (scope.isHierarchicalScope() && !scope.inScope(entry)) {
			return false;
		}
		return hasInScopeDescendent(entry, scope);
	}

	/*
	 * Returns true if one of the children meets the conditions
	 * necessary to be shown in the Toc tree
	 */
	public static boolean hasInScopeDescendent(ITopic topic, AbstractHelpScope scope) {
		ITopic[] subtopics = topic.getSubtopics();
		for (ITopic subtopic : subtopics) {
			if (showInTree(subtopic, scope)) {
				return true;
			}
		}
		return false;
	}

	/*
	 * Returns true if one of the children meets the conditions
	 * necessary to be shown in the Toc tree
	 */
	public static boolean hasInScopeDescendent(IToc toc, AbstractHelpScope scope) {
		ITopic[] topics = toc.getTopics();
		for (ITopic topic : topics) {
			if (showInTree(topic, scope)) {
				return true;
			}
		}
		return false;
	}

	/*
	 * Returns true if one of the children meets the conditions
	 * necessary to be shown in the Index
	 */
	public static boolean hasInScopeDescendent(IIndexEntry entry,
			AbstractHelpScope scope) {
		ITopic[] topics = entry.getTopics();
		for (ITopic topic : topics) {
			if (showInTree(topic, scope)) {
				return true;
			}
		}
		IIndexEntry[] entries = entry.getSubentries();
		for (IIndexEntry innerEntry : entries) {
			if (showInTree(innerEntry, scope)) {
				return true;
			}
		}
		if (entry instanceof IIndexEntry2) {
			IIndexSee[] sees = ((IIndexEntry2)entry).getSees();
			for (IIndexSee see : sees) {
				if (showInTree(see, scope)) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean hasInScopeTarget(IIndexSee see, AbstractHelpScope scope) {
		if (see instanceof IndexSee) {
			IndexSee indexSee = (IndexSee)see;
			UAElement ancestor = indexSee.getParentElement();
			while (!(ancestor instanceof Index)) {
				if (ancestor == null) {
					return true;
				}
				ancestor = ancestor.getParentElement();
			}
			IIndexEntry target = ((Index)ancestor).getSeeTarget(indexSee);
			if (target == null) {
				return false;
			}
			return showInTree(target, scope);
		}
		return false;
	}

	public static boolean showInTree(IIndexSee see, AbstractHelpScope scope) {
		if (scope.isHierarchicalScope() && !scope.inScope(see)) {
			return false;
		}
		if (see instanceof IndexSee) {
			IndexSee indexSee = (IndexSee)see;
			UAElement ancestor = indexSee.getParentElement();
			while (!(ancestor instanceof Index)) {
				if (ancestor == null) {
					return true;
				}
				ancestor = ancestor.getParentElement();
			}
			IIndexEntry target = ((Index)ancestor).getSeeTarget(indexSee);
			if (target == null) {
				return false;
			}
			return showInTree(target, scope);
		}
		return false;
	}

	/**
	 * Filter out any disabled entries from an array
	 * @param entries an array of entries
	 * @param scope
	 * @return an array containing only those entries which are enabled
	 */
	public static IIndexEntry[] inScopeEntries(IIndexEntry[] entries, AbstractHelpScope scope) {
		for (int i=0;i<entries.length;++i) {
			if (!scope.inScope(entries[i])) {
				List<IIndexEntry> list = new ArrayList<>(entries.length);
				for (int j=0;j<entries.length;++j) {
					if (j < i || scope.inScope(entries[j])) {
						list.add(entries[j]);
					}
				}
				return list.toArray(new IIndexEntry[list.size()]);
			}
		}
		return entries;
	}

	/**
	 * Filter out any disabled topics from an array
	 * @param topics an array of topics
	 * @param scope
	 * @return an array containing only those topics which are enabled
	 */
	public static ITopic[] inScopeTopics(ITopic[] topics, AbstractHelpScope scope) {
		for (int i=0;i<topics.length;++i) {
			if (!scope.inScope(topics[i])) {
				List<ITopic> list = new ArrayList<>(topics.length);
				for (int j=0;j<topics.length;++j) {
					if (j < i || scope.inScope(topics[j])) {
						list.add(topics[j]);
					}
				}
				return list.toArray(new ITopic[list.size()]);
			}
		}
		return topics;
	}

	public static boolean hasInScopeChildren(IUAElement element,
			AbstractHelpScope scope) {
		if (element instanceof IToc) {
			return hasInScopeDescendent((IToc)element, scope);
		}
		if (element instanceof ITopic) {
			return hasInScopeDescendent((ITopic)element, scope);
		}
		if (element instanceof IIndexEntry) {
			return hasInScopeDescendent((IIndexEntry) element, scope);
		}
		if (element instanceof IIndexSee) {
			return hasInScopeTarget((IIndexSee) element, scope);
		}
		return false;
	}

}
