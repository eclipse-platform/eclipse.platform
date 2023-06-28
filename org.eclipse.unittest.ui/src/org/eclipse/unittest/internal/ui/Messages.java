/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
package org.eclipse.unittest.internal.ui;

import org.eclipse.osgi.util.NLS;

/**
 * Unit Test View UI Messages
 */
public final class Messages extends NLS {

	static {
		NLS.initializeMessages(Messages.class.getName(), Messages.class);
	}

	private Messages() {
		// Do not instantiate
	}

	public static String CompareResultDialog_actualLabel;
	public static String CompareResultDialog_expectedLabel;
	public static String CompareResultDialog_title;
	public static String CompareResultsAction_description;
	public static String CompareResultsAction_label;
	public static String CompareResultsAction_tooltip;

	public static String CopyFailureList_action_label;
	public static String CopyFailureList_clipboard_busy;
	public static String CopyFailureList_problem;

	public static String CopyTrace_action_label;
	public static String CopyTraceAction_clipboard_busy;
	public static String CopyTraceAction_problem;

	public static String CounterPanel_label_errors;
	public static String CounterPanel_label_failures;
	public static String CounterPanel_label_runs;
	public static String CounterPanel_runcount;
	public static String CounterPanel_runcount_assumptionsFailed;
	public static String CounterPanel_runcount_ignored;
	public static String CounterPanel_runcount_skipped;
	public static String CounterPanel_runcount_ignored_assumptionsFailed;

	public static String EnableStackFilterAction_action_description;
	public static String EnableStackFilterAction_action_label;
	public static String EnableStackFilterAction_action_tooltip;

	public static String ExpandAllAction_text;
	public static String ExpandAllAction_tooltip;

	public static String CollapseAllAction_text;
	public static String CollapseAllAction_tooltip;

	public static String RerunAction_label_debug;
	public static String RerunAction_label_run;
	public static String RerunAction_label_rerun;

	public static String ScrollLockAction_action_label;
	public static String ScrollLockAction_action_tooltip;

	public static String ShowNextFailureAction_label;
	public static String ShowNextFailureAction_tooltip;

	public static String ShowPreviousFailureAction_label;
	public static String ShowPreviousFailureAction_tooltip;

	public static String ShowStackTraceInConsoleViewAction_description;
	public static String ShowStackTraceInConsoleViewAction_label;
	public static String ShowStackTraceInConsoleViewAction_tooltip;

	public static String TestRunnerViewPart_activate_on_failure_only;
	public static String TestRunnerViewPart_cannotrerun_title;
	public static String TestRunnerViewPart_cannotrerurn_message;
	public static String TestRunnerViewPart_configName;
	public static String TestRunnerViewPart__error_cannotrun;
	public static String TestRunnerViewPart_error_cannotrerun;
	public static String TestRunnerViewPart_error_no_tests_found;

	public static String TestRunnerViewPart_jobName;

	public static String TestRunnerViewPart_label_failure;
	public static String TestRunnerViewPart_Launching;
	public static String TestRunnerViewPart_message_finish;
	public static String TestRunnerViewPart_message_stopped;
	public static String TestRunnerViewPart_message_terminated;
	public static String TestRunnerViewPart_rerunaction_label;
	public static String TestRunnerViewPart_rerunaction_tooltip;
	public static String TestRunnerViewPart_rerunfailuresaction_label;
	public static String TestRunnerViewPart_rerunfailuresaction_tooltip;
	public static String TestRunnerViewPart_rerunFailedFirstLaunchConfigName;
	public static String TestRunnerViewPart_stopaction_text;
	public static String TestRunnerViewPart_stopaction_tooltip;
	public static String TestRunnerViewPart_terminate_message;
	public static String TestRunnerViewPart_terminate_title;
	public static String TestRunnerViewPart_toggle_automatic_label;
	public static String TestRunnerViewPart_toggle_horizontal_label;
	public static String TestRunnerViewPart_toggle_vertical_label;
	public static String TestRunnerViewPart_titleToolTip;
	public static String TestRunnerViewPart_wrapperJobName;
	public static String TestRunnerViewPart_show_execution_time;
	public static String TestRunnerViewPart_show_failures_only;
	public static String TestRunnerViewPart_show_ignored_only;

	public static String TestRunnerViewPart_hierarchical_layout;
	public static String TestSessionLabelProvider_testName_elapsedTimeInSeconds;
	public static String TestSessionLabelProvider_testName_RunnerVersion;

	public static String TestSessionLabelProvider_testMethodName_className;

	public static String TestRunnerViewPart_message_stopping;
	public static String TestRunnerViewPart_PasteAction_cannotpaste_message;
	public static String TestRunnerViewPart_PasteAction_cannotpaste_title;
	public static String TestRunnerViewPart_PasteAction_label;
	public static String TestRunnerViewPart_layout_menu;
	public static String TestRunnerViewPart_editLaunchConfiguration;
	public static String TestRunnerViewPart_sortAlphabetical;
	public static String TestRunnerViewPart_sortRunner;
	public static String TestRunnerViewPart_sort;
}
