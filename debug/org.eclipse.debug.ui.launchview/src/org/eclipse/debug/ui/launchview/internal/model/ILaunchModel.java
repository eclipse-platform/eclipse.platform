/*******************************************************************************
 * Copyright (c) 2017, 2019 SSI Schaefer IT Solutions GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SSI Schaefer IT Solutions GmbH
 *******************************************************************************/
package org.eclipse.debug.ui.launchview.internal.model;

/**
 * Service which controls the lifecycle of the model which the view is based on.
 */
public interface ILaunchModel {

	/**
	 * @return the current model. Never <code>null</code>. Always created from
	 *         the current state.
	 */
	public LaunchObjectContainerModel getModel();

}
