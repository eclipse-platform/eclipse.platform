/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation.
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
package org.eclipse.debug.core.commands;

/**
 * A resume others handler typically resumes all suspended threads associated
 * with the same debug target, excluding the selected thread.
 * <p>
 * Clients may implement this interface. The debug platform provides a "Resume
 * Others" action that delegates to this handler interface.
 * </p>
 *
 * @since 3.24
 */
public interface IResumeOthersHandler extends IDebugCommandHandler {

}
