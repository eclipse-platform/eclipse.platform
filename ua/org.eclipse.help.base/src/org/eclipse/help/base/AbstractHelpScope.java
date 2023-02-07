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

package org.eclipse.help.base;

import java.util.Locale;
import org.eclipse.help.IIndexEntry;
import org.eclipse.help.IIndexSee;
import org.eclipse.help.IToc;
import org.eclipse.help.ITopic;
import org.eclipse.help.IUAElement;
import org.eclipse.help.internal.base.scope.ScopeUtils;

/**
 * Represents a scope which can be used to specify which topics are shown in the
 * table of contents and which entries will show in the index. The inScope() functions
 * are used by the help system to determine which elements to show. Both the table of
 * contents and index are trees and the help system reads these trees starting with the
 * root and working down through the children.
 *
 * @since 3.5
 */

public abstract class AbstractHelpScope {

	/**
	 * Determine whether a table of contents is in scope
	 */
	public abstract boolean inScope(IToc toc);

	/**
	 * Determine whether a topic is in scope
	 */
	public abstract boolean inScope(ITopic topic);

	/**
	 * Determine whether an index entry is in scope
	 */
	public abstract boolean inScope(IIndexEntry entry);

	/**
	 * a single information center instance can display content in multiple locales.
	 * This function exists to provide a name for a specific locale.
	 * @param locale a string representing the locale used for the UI
	 * @return a name for this scope appropriate for the locale which
	 * will be used in the scope selection dialog. It is recommended that
	 * the name be no more than 20 characters long.
	 */
	public abstract String getName(Locale locale);

	/**
	 * In the default implementation of this method an IndexSee element is in
	 * scope if it's target is in scope. May be overridden to exclude more
	 * IndexSee elements from the scope
	 *
	 * @param see
	 *            the {@link IIndexSee} to check
	 * @return true if the target is in scope
	 */
	public boolean inScope(IIndexSee see) {
		return hasInScopeChildren(see);
	}

	/**
	 * The help system can build the trees faster if it knows that an out of
	 * scope element cannot have child elements which are in scope. This
	 * is called a hierarchical scope. If an out of scope element can have
	 * in scope children this function should be overridden and the help
	 * system will perform a deeper search.
	 */
	public boolean isHierarchicalScope() {
		return true;
	}

	/**
	 * Convenience method to make it easier to write subclasses
	 * In the case of an IIndexSee element this method tests
	 * to see if the target is in scope. For all other elements
	 * it tests all children and if this is not a hierarchical scope
	 * all descendants are tested.
	 * @param element An element which may have children
	 * @return true if at least one child is in scope
	 */
	public final boolean hasInScopeChildren(IUAElement element) {
		return ScopeUtils.hasInScopeChildren(element, this);
	}

}

