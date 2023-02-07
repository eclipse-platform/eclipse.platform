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

package org.eclipse.ua.tests.help.scope;

import java.util.Locale;

import org.eclipse.help.IIndexEntry;
import org.eclipse.help.IIndexSee;
import org.eclipse.help.IToc;
import org.eclipse.help.ITopic;
import org.eclipse.help.base.AbstractHelpScope;

public class MockScope extends AbstractHelpScope {

	// Used for testing of scope based classes and utilities
	// Elements are in scope if they contain the letter specified
	// in the constructor

	private char letter;
	private boolean isHierarchical;

	public MockScope(char letter, boolean isHierarchical) {
		this.letter = letter;
		this.isHierarchical = isHierarchical;
	}

	@Override
	public boolean inScope(IToc toc) {
		return testForInScope(toc.getLabel());
	}

	@Override
	public boolean inScope(ITopic topic) {
		return testForInScope(topic.getLabel());
	}

	@Override
	public boolean inScope(IIndexEntry entry) {
		return testForInScope(entry.getKeyword());
	}

	@Override
	public boolean inScope(IIndexSee see) {
		return true;
	}

	@Override
	public String getName(Locale locale) {
		return null;
	}

	private boolean testForInScope(String label) {
		return label.indexOf(letter) >= 0;
	}

	@Override
	public boolean isHierarchicalScope() {
		return isHierarchical;
	}

}
