/*******************************************************************************
 *  Copyright (c) 2009, 2022 QNX Software Systems and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *      QNX Software Systems - initial API and implementation
 *      Freescale Semiconductor
 *      SSI Schaefer
 *******************************************************************************/
package org.eclipse.debug.internal.core.groups;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.internal.core.DebugCoreMessages;

/**
 * Represents a single child element of a launch group.
 *
 * @since 3.11
 */
public class GroupLaunchElement {
	public static final String MODE_INHERIT = "inherit"; //$NON-NLS-1$

	/**
	 * Describes the possible post-launch actions for each
	 * {@link GroupLaunchElement}.
	 * <p>
	 * These actions get performed after the associated
	 * {@link GroupLaunchElement} has been launched, before the next one is
	 * launched (or launching is finished).
	 */
	public enum GroupElementPostLaunchAction {
		NONE(DebugCoreMessages.GroupLaunchConfigurationDelegate_None), //
		WAIT_FOR_TERMINATION(DebugCoreMessages.GroupLaunchConfigurationDelegate_Wait_until_terminated), //
		DELAY(DebugCoreMessages.GroupLaunchConfigurationDelegate_Delay), //
		OUTPUT_REGEXP(DebugCoreMessages.GroupLaunchElement_outputRegexp);

		private final String description;

		private GroupElementPostLaunchAction(String description) {
			this.description = description;
		}

		public String getDescription() {
			return description;
		}

		public static GroupLaunchElement.GroupElementPostLaunchAction valueOfDescription(String desc) {
			for (GroupLaunchElement.GroupElementPostLaunchAction e : values()) {
				if (e.description.equals(desc)) {
					return e;
				}
			}
			return NONE;
		}
	}

	public int index;
	public boolean enabled = true;
	public String mode = MODE_INHERIT;
	public GroupLaunchElement.GroupElementPostLaunchAction action = GroupElementPostLaunchAction.NONE;
	public boolean adoptIfRunning = false;
	public Object actionParam;
	public String name;
	public ILaunchConfiguration data;
}