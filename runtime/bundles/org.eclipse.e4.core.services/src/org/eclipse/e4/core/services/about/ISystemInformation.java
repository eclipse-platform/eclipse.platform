/*******************************************************************************
 *  Copyright (c) 2019 ArSysOp and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *      Alexander Fedorov <alexander.fedorov@arsysop.ru> - initial API and implementation
 *******************************************************************************/
package org.eclipse.e4.core.services.about;

import java.io.PrintWriter;

/**
 * Collects the system information for the "about"-related functionality.
 *
 * @noextend This interface is not intended to be extended by clients.
 * @since 2.2
 */
public interface ISystemInformation {

	/**
	 * Appends the information to the system summary.
	 *
	 * @param writer consumes the information to the system summary, should not be
	 *               closed by implementor
	 */
	void append(PrintWriter writer);

}
