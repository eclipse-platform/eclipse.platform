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

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One build invocation, with the time each builder spent on each project in it.
 * The same leaves are grouped either by project or by builder.
 */
class BuildRun {

	private final String label;
	private final LocalTime started;
	private final long wallTime;
	private final List<BuilderEntry> leaves;
	private final long builderTime;
	private final int runs;
	private final int projects;

	private List<GroupEntry> byProject;
	private List<GroupEntry> byBuilder;

	BuildRun(String label, LocalTime started, long wallTime, List<BuilderEntry> leaves) {
		this.label = label;
		this.started = started;
		this.wallTime = wallTime;
		this.leaves = leaves;
		this.builderTime = leaves.stream().mapToLong(BuilderEntry::getTime).sum();
		this.runs = leaves.stream().mapToInt(BuilderEntry::getRuns).sum();
		this.projects = (int) leaves.stream().map(BuilderEntry::getProject).distinct().count();
		leaves.forEach(leaf -> leaf.setBuildTotal(builderTime));
	}

	/** How many projects actually ran a builder in this build. */
	int getProjects() {
		return projects;
	}

	/** How much of the build was spent inside builders rather than around them. */
	double getShare() {
		return wallTime > 0 ? 100.0 * builderTime / wallTime : 0;
	}

	String getLabel() {
		return label;
	}

	LocalTime getStarted() {
		return started;
	}

	/** Time from the start of the build until every builder has finished. */
	long getWallTime() {
		return wallTime;
	}

	/** Time spent inside builders, which is at most {@link #getWallTime()}. */
	long getTime() {
		return builderTime;
	}

	int getRuns() {
		return runs;
	}

	/** How many rows this build contributes to the tree once fully expanded. */
	int getLeafCount() {
		return leaves.size();
	}

	synchronized List<GroupEntry> getGroups(boolean groupedByBuilder) {
		if (groupedByBuilder) {
			if (byBuilder == null) {
				byBuilder = group(true);
			}
			return byBuilder;
		}
		if (byProject == null) {
			byProject = group(false);
		}
		return byProject;
	}

	private List<GroupEntry> group(boolean groupedByBuilder) {
		Map<String, List<BuilderEntry>> children = new LinkedHashMap<>();
		for (BuilderEntry leaf : leaves) {
			children.computeIfAbsent(leaf.getGroup(groupedByBuilder), name -> new ArrayList<>()).add(leaf);
		}
		List<GroupEntry> groups = new ArrayList<>();
		children.forEach((name, entries) -> groups.add(new GroupEntry(name, entries, groupedByBuilder, builderTime)));
		return groups;
	}
}
