/*******************************************************************************
 * Copyright (c) 2026 Vogella GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Vogella GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.internal.buildmonitor;

import java.util.List;

/**
 * The middle level of the tree: one project with its builders below it, or one
 * builder with its projects below it, depending on the grouping.
 */
class GroupEntry {

	private final String name;
	private final List<BuilderEntry> children;
	private final boolean groupedByBuilder;
	private final long time;
	private final int runs;
	private final long buildTotal;

	GroupEntry(String name, List<BuilderEntry> children, boolean groupedByBuilder, long buildTotal) {
		this.name = name;
		this.children = children;
		this.groupedByBuilder = groupedByBuilder;
		this.buildTotal = buildTotal;
		this.time = children.stream().mapToLong(BuilderEntry::getTime).sum();
		this.runs = children.stream().mapToInt(BuilderEntry::getRuns).sum();
	}

	/** This group's percentage of the builder time of its build. */
	double getShare() {
		return buildTotal > 0 ? 100.0 * time / buildTotal : 0;
	}

	String getName() {
		return name;
	}

	boolean isGroupedByBuilder() {
		return groupedByBuilder;
	}

	long getTime() {
		return time;
	}

	int getRuns() {
		return runs;
	}

	List<BuilderEntry> getChildren() {
		return children;
	}
}
