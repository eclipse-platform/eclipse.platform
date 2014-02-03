/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.internal.ui.model;

public interface IProblemRequestor {

	/**
	 * Notification of a Ant buildfile problem.
	 *
	 * @param problem IProblem - The discovered Ant buildfile problem.
	 */
	void acceptProblem(IProblem problem);

	/**
	 * Notification sent before starting the problem detection process.
	 * Typically, this would tell a problem collector to clear previously recorded problems.
	 */
	void beginReporting();

	/**
	 * Notification sent after having completed problem detection process.
	 * Typically, this would tell a problem collector that no more problems should be expected in this
	 * iteration.
	 */
	void endReporting();

}
