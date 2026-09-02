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
	public static String BuildMonitorView_build_label;
	public static String BuildMonitorView_build_label_one;
	public static String BuildMonitorView_builder_column;
	public static String BuildMonitorView_by_builder_action;
	public static String BuildMonitorView_by_builder_action_tooltip;
	public static String BuildMonitorView_clear_action;
	public static String BuildMonitorView_copy_action;
	public static String BuildMonitorView_copy_action_tooltip;
	public static String BuildMonitorView_copy_header;
	public static String BuildMonitorView_clear_action_tooltip;
	public static String BuildMonitorView_collapse_all_action;
	public static String BuildMonitorView_expand_all_action;
	public static String BuildMonitorView_kind_auto;
	public static String BuildMonitorView_kind_clean;
	public static String BuildMonitorView_kind_full;
	public static String BuildMonitorView_kind_incremental;
	public static String BuildMonitorView_project_column;
	public static String BuildMonitorView_recorded;
	public static String BuildMonitorView_runs_column;
	public static String BuildMonitorView_share_column;
	public static String BuildMonitorView_share_column_tooltip;
	public static String BuildMonitorView_runs_column_tooltip;
	public static String BuildMonitorView_scope_project;
	public static String BuildMonitorView_scope_workspace;
	public static String BuildMonitorView_time_column;
	public static String BuildMonitorView_time_column_tooltip;
	public static String BuildMonitorView_tracing_unavailable;
	public static String BuildMonitorView_waiting;
	public static String BuildMonitorView_wall_column;
	public static String BuildMonitorView_wall_column_tooltip;

	private Messages() {
		// Do not instantiate.
	}

	static {
		NLS.initializeMessages(Messages.class.getName(), Messages.class);
	}
}
