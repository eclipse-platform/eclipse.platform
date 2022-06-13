/*******************************************************************************
 *  Copyright (c) 2009, 2010 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.e4.core.services.statusreporter;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.e4.core.services.log.Logger;

/**
 * Handling warnings or errors, with the intent of making the end user aware of these. Strings are
 * expected to be translated.
 *
 * @see Logger
 */
public abstract class StatusReporter {

	/**
	 * severity OK
	 */
	public static final int OK = IStatus.OK;

	/**
	 * severity INFO
	 */
	public static final int INFO = IStatus.INFO;

	/**
	 * severity WARNING
	 */
	public static final int WARNING = IStatus.WARNING;

	/**
	 * severity ERROR
	 */
	public static final int ERROR = IStatus.ERROR;

	/**
	 * Style constant indicating that the status should not be acted on. This is used by objects
	 * such as log listeners that do not want to report a status twice. This constant is treated as
	 * a HINT.
	 */
	public static final int IGNORE = 0x10;

	/**
	 * Style constant indicating that the status should be logged only.
	 */
	public static final int LOG = 0x20;

	/**
	 * Style constant indicating that handlers should show a problem to an user without blocking the
	 * calling method while awaiting user response. This is generally done using a non modal Dialog.
	 * This constant is treated as a HINT.
	 */
	public static final int SHOW = 0x40;

	/**
	 * Style constant indicating that the handling should block the calling thread until the status
	 * has been handled. This constant is treated as a HINT, and the caller cannot rely on the fact
	 * that the call will be blocking. However, the caller needs to be prepared for the case where
	 * the call blocks, and potentially runs the event loop (which may change UI state).
	 * <p>
	 * A typical usage of this would be to give the user an opportunity to deal with the status in
	 * some manner. It is therefore likely but not required that a modal dialog is going to be used.
	 * </p>
	 * <p>
	 * The use of <code>BLOCK</code> is not recommended because of its potential to block the
	 * current thread, and the corresponding risk of causing deadlocks.
	 * </p>
	 */
	public static final int BLOCK = 0x80;

	/**
	 * Report the given status, using the given style as a hint.
	 *
	 * @param status
	 *            a status object describing an exceptional situation that should be brought to the
	 *            attention of the user.
	 * @param style
	 *            one of the handling styles (IGNORE, LOG, SHOW, BLOCK).
	 * @param information
	 *            any number of additional objects to pass along with the status in order to help
	 *            with diagnosing the problem. One possible use of these objects would be to call
	 *            their toString() methods.
	 *
	 */
	public abstract void report(IStatus status, int style, Object... information);

	/**
	 * Return a status object with the given severity, message, and optional exception.
	 *
	 * @param severity
	 *            one of the severity constants (OK, INFO, WARNING, ERROR).
	 * @param message
	 *            a non-null, non-empty localized message describing the unforeseen condition, in as
	 *            high-level a way as possible such that an end user can understand the situation
	 *            and act upon it. In the case of IGNORE or LOG, the message may be untranslated if
	 *            there is reason to believe that the message will primarily be seen by technical
	 *            support.
	 * @param exception
	 *            an optional exception, or <code>null</code>.
	 */
	public abstract IStatus newStatus(int severity, String message, Throwable exception);

	/**
	 * Convenience method, equivalent to calling
	 * <code>report(newStatus(severity, message, exception), SHOW, information).</code>
	 *
	 * @param severity
	 *            one of the severity constants (OK, INFO, WARNING, ERROR).
	 * @param message
	 *            a non-null, non-empty localized message describing the unforeseen condition, in as
	 *            high-level a way as possible such that an end user can understand the situation
	 *            and act upon it. In the case of IGNORE or LOG, the message may be untranslated if
	 *            there is reason to believe that the message will primarily be seen by technical
	 *            support.
	 * @param exception
	 *            an optional exception, or <code>null</code>.
	 * @param information
	 *            any number of additional objects to pass along in order to help with diagnosing
	 *            the problem. One possible use of these objects may be that their toString()
	 *            methods will be called.
	 *
	 */
	final public void show(int severity, String message, Throwable exception, Object... information) {
		report(newStatus(severity, message, exception), SHOW, information);
	}

}
