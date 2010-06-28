/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.verifier;

import java.util.ArrayList;

import org.eclipse.update.operations.IInstallFeatureOperation;

/**
 * Verifier is responsible for checking plan sanity
 */
public abstract class PlanVerifier {
	/**
	 * Verifies provisioning operation of Classic Updater. Checks if feature can
	 * be installed in an updateable site. Checks if version of already
	 * installed plug-in is planned to downgraded or if update is safe.
	 */
	public abstract void verify(IInstallFeatureOperation installOperation,
			ArrayList currentFeatures, ArrayList featuresAfterOperation,
			ArrayList status);

}