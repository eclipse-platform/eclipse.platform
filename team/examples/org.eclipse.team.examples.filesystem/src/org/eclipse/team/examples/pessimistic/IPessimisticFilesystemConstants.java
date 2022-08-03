/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
package org.eclipse.team.examples.pessimistic;

/**
 * Preference constants for the <code>PessimisticFilesystemProvider</code>.
 */
public interface IPessimisticFilesystemConstants {
	/**
	 * Preference name's prefix
	 */
	String PREFIX = "org.eclipse.team.examples.pessimistic.";

	/**
	 * Preference name for when checked in files are saved.
	 */
	String PREF_CHECKED_IN_FILES_SAVED = PREFIX + "WhenCheckedInFilesAreSaved";
	/**
	 * Preference name for when checked in files are edited with a UI context.
	 */
	String PREF_CHECKED_IN_FILES_EDITED = PREFIX + "WhenCheckedInFilesAreEdited";
	/**
	 * Preference name for when checked in files are edited without a UI context.
	 */
	String PREF_CHECKED_IN_FILES_EDITED_NOPROMPT = PREFIX + "WhenCheckedInFilesAreEditedNoPrompt";
	/**
	 * Preference name for the option to always fail validate edit.
	 */
	String PREF_FAIL_VALIDATE_EDIT= PREFIX + "FailValidateEdit";
	/**
	 * Preference name for the option to touch files during validate edit calls
	 */
	String PREF_TOUCH_DURING_VALIDATE_EDIT= PREFIX + "ChangeFileContents";
	/**
	 * Preference name for the option to add files to the repository provider.
	 */
	String PREF_ADD_TO_CONTROL= PREFIX + "AddToControl";

	/**
	 * Preference option indicating that the user should be prompted.
	 */
	int OPTION_PROMPT = 1;
	/**
	 * Preference option indicating that the action should happen automatically.
	 */
	int OPTION_AUTOMATIC = 2;
	/**
	 * Preference option indicating that the action should not occur.
	 */
	int OPTION_DO_NOTHING = 4;

	/**
	 * Status flag indicating that resources are ready to be edited.
	 */
	int STATUS_OK_TO_EDIT = 1;
	/**
	 * Status flag indicating that resources need to be reloaded.
	 */
	int STATUS_PROMPT_FOR_RELOAD = 2;
}
