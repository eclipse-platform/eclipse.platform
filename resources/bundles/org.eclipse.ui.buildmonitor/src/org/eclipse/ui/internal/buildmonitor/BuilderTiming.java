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
 * Accumulated build times of one builder on one project.
 */
class BuilderTiming {

	private final String builder;
	private final String project;
	private int runs;
	private long totalTime;
	private long slowestRun;

	BuilderTiming(String builder, String project) {
		this.builder = builder;
		this.project = project;
	}

	void addRun(long duration) {
		runs++;
		totalTime += duration;
		slowestRun = Math.max(slowestRun, duration);
	}

	String getBuilder() {
		return builder;
	}

	String getProject() {
		return project;
	}

	int getRuns() {
		return runs;
	}

	long getTotalTime() {
		return totalTime;
	}

	long getSlowestRun() {
		return slowestRun;
	}
}
