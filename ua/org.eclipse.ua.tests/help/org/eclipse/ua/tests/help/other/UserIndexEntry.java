/*******************************************************************************
 * Copyright (c) 2009, 2016 IBM Corporation and others.
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

package org.eclipse.ua.tests.help.other;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.help.IIndexEntry;
import org.eclipse.help.IIndexEntry2;
import org.eclipse.help.IIndexSee;
import org.eclipse.help.ITopic;
import org.eclipse.help.IUAElement;

/**
 * This class is used to test topics created using the IIndexEntry2 API
 */

public class UserIndexEntry implements IIndexEntry2 {

	private List<IIndexEntry> subentries = new ArrayList<>();
	private List<IIndexSee> sees = new ArrayList<>();
	private List<ITopic> topics = new ArrayList<>();
	private boolean isEnabled;
	private String keyword;

	@Override
	public IUAElement[] getChildren() {
		IUAElement[] subentries = getSubentries();
		IUAElement[] sees = getSees();
		IUAElement[] topics = getTopics();
		IUAElement[] result = new IUAElement[subentries.length + sees.length + topics.length];
		System.arraycopy(topics, 0, result, 0, topics.length);
		System.arraycopy(subentries, 0, result, topics.length, subentries.length);
		System.arraycopy(sees, 0, result, topics.length + subentries.length, sees.length);
		return result;
	}

	@Override
	public boolean isEnabled(IEvaluationContext context) {
		return isEnabled;
	}

	public void addSee(IIndexSee child) {
		sees.add(child);
	}

	public void addEntry(IIndexEntry child) {
		subentries.add(child);
	}

	public void addTopic(ITopic child) {
		topics.add(child);
	}

	public UserIndexEntry(String keyword, boolean isEnabled) {
		this.keyword = keyword;
		this.isEnabled = isEnabled;
	}

	@Override
	public IIndexSee[] getSees() {
		return sees.toArray(new IIndexSee[0]);
	}

	@Override
	public String getKeyword() {
		return keyword;
	}

	@Override
	public IIndexEntry[] getSubentries() {
		return subentries.toArray(new IIndexEntry[0]);
	}

	@Override
	public ITopic[] getTopics() {
		return topics.toArray(new ITopic[0]);
	}

}
