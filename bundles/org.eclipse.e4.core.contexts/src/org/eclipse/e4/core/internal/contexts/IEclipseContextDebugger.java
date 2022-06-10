/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
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
package org.eclipse.e4.core.internal.contexts;

public interface IEclipseContextDebugger {

	public enum EventType {
		CONSTRUCTED, DISPOSED, LISTENER_ADDED, ACTIVATED, DEACTIVATED
	}

	void notify(EclipseContext context, EventType type, Object data);
}
