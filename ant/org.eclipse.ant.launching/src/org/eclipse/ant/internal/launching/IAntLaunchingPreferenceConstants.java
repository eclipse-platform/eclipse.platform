/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.internal.launching;

/**
 * Constants used to identify user preferences.
 */
public interface IAntLaunchingPreferenceConstants {

	 /**
     * int preference identifier constant which specifies the length of time to wait
     * to connect with the socket that communicates with the separate JRE to capture the output
     */
    public static final String ANT_COMMUNICATION_TIMEOUT= "timeout"; //$NON-NLS-1$
}
