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

import org.eclipse.osgi.util.NLS;

final class Messages extends NLS {
	public static String BuildMonitorView_builder_column;
	public static String BuildMonitorView_clear_action;
	public static String BuildMonitorView_clear_action_tooltip;
	public static String BuildMonitorView_project_column;
	public static String BuildMonitorView_runs_column;
	public static String BuildMonitorView_slowest_run_column;
	public static String BuildMonitorView_total_time_column;
	public static String BuildMonitorView_tracing_enabled;
	public static String BuildMonitorView_tracing_unavailable;

	private Messages() {
		// Do not instantiate.
	}

	static {
		NLS.initializeMessages(Messages.class.getName(), Messages.class);
	}
}
