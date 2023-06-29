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
package org.eclipse.debug.core.model;

/**
 * A watch expression listener is notified when an
 * <code>org.eclipse.debug.core.model.IWatchExpressionDelegate</code>
 * completes an evaluation.
 *
 * @see org.eclipse.debug.core.model.IWatchExpressionDelegate
 * @see org.eclipse.debug.core.model.IWatchExpressionResult
 * @since 3.0
 */
public interface IWatchExpressionListener {

	/**
	 * Notifies the listener that an evaluation has completed.
	 *
	 * @param result the result of the evaluation
	 */
	void watchEvaluationFinished(IWatchExpressionResult result);

}
