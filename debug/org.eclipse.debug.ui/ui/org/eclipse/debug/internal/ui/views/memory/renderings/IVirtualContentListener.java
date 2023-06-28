/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
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

package org.eclipse.debug.internal.ui.views.memory.renderings;

public interface IVirtualContentListener {

	int BUFFER_START = 0;
	int BUFFER_END = 1;

	/**
	 * Called when the viewer is at the beginning of its bufferred content
	 */
	void handledAtBufferStart();

	/**
	 * Called when viewer is at the end of its bufferred content
	 */
	void handleAtBufferEnd();

	/**
	 * @return
	 */
	int getThreshold(int bufferEndOrStart);

}
