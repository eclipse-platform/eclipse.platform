/*******************************************************************************
 * Copyright (c) 2004, 2026 IBM Corporation and others.
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
 *     Brock Janiczak (brockj@tpg.com.au) - Bug 145736
 *     Andrey Loskutov - replaced SWT by Swing
 *******************************************************************************/

package org.eclipse.ant.internal.ui.antsupport.inputhandler;

/**
 * Kept only for compatibility with launch configurations explicitly specifying this class as Ant input handler (<code>-inputhandler</code>).
 *
 * @deprecated SWT is not used anymore to prompt for input in Ant builds running in a separate JVM, use {@link SwingInputHandler} instead.
 */
@Deprecated
public class SWTInputHandler extends SwingInputHandler {
	// no additional behavior
}
