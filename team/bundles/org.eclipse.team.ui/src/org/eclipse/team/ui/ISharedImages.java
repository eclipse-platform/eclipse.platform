/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
package org.eclipse.team.ui;

/**
 * Images that are available for providers to re-use. They include
 * common overlays and wizard images. A provider can use their own
 * custom images, these shared images are only available for
 * convenience.
 *
 * @see TeamImages
 * @since 2.0
 */
public interface ISharedImages {

	/*====================================================================
	 * Constants defining overlays
	 *====================================================================*/

	/**
	 * Overlay identifies a locally changed file.
	 */
	public final String IMG_DIRTY_OVR = "ovr/dirty_ov.gif"; //$NON-NLS-1$

	/**
	 * Overlay identified a version controlled file.
	 */
	public final String IMG_CHECKEDIN_OVR = "ovr/version_controlled.gif"; //$NON-NLS-1$

	/**
	 * Overlay identifies a checked-out file.
	 */
	public final String IMG_CHECKEDOUT_OVR = "ovr/checkedout_ov.gif"; //$NON-NLS-1$

	/**
	 * Overlay identifies a conflicting element.
	 */
	public final String IMG_CONFLICT_OVR = "ovr/confchg_ov.gif"; //$NON-NLS-1$

	/**
	 * Overlay identifies an error in the element.
	 */
	public final String IMG_ERROR_OVR = "ovr/error_co.gif"; //$NON-NLS-1$

	/**
	 * Overlay identifies an error in the element.
	 */
	public final String IMG_WARNING_OVR = "ovr/warning_co.gif"; //$NON-NLS-1$

	/**
	 * Overlay identifies an element that is being worked on (e.g. is busy).
	 */
	public final String IMG_HOURGLASS_OVR = "ovr/waiting_ovr.gif"; //$NON-NLS-1$
}
