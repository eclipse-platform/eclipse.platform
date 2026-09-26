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

/**
 * What one builder cost on one project in one build. This is the leaf of the
 * tree either way round, so it carries both names and the grouping decides
 * which of them is shown.
 */
class BuilderEntry {

	private final String builder;
	private final String project;
	private final int runs;
	private final long time;
	private long buildTotal;

	BuilderEntry(String builder, String project, int runs, long time) {
		this.builder = builder;
		this.project = project;
		this.runs = runs;
		this.time = time;
	}

	/** The builder class name, which is what the performance event blames. */
	String getBuilder() {
		return builder;
	}

	String getProject() {
		return project;
	}

	int getRuns() {
		return runs;
	}

	long getTime() {
		return time;
	}

	/** Set once by the enclosing build, which only knows its total after all leaves exist. */
	void setBuildTotal(long buildTotal) {
		this.buildTotal = buildTotal;
	}

	/** This leaf's percentage of the builder time of its build. */
	double getShare() {
		return buildTotal > 0 ? 100.0 * time / buildTotal : 0;
	}

	/** The name of the dimension the enclosing group does not already show. */
	String getLabel(boolean groupedByBuilder) {
		return groupedByBuilder ? project : BuilderLabels.of(builder);
	}

	String getGroup(boolean groupedByBuilder) {
		return groupedByBuilder ? BuilderLabels.of(builder) : project;
	}
}
