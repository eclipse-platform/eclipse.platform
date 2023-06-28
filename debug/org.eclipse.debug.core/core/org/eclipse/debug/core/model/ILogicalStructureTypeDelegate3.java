/*******************************************************************************
 *  Copyright (c) 2020 Julian Honnen
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Julian Honnen <julian.honnen@vector.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.core.model;

/**
 * Optional extension to <code>ILogicalStructureTypeDelegate</code> that allows
 * a logical structure type delegate to garbage collect its logical structures
 * once they're no longer displayed.
 *
 * @since 3.17
 * @see ILogicalStructureTypeDelegate
 */
public interface ILogicalStructureTypeDelegate3 {

	/**
	 * Called when the logical structure returned from
	 * {@link ILogicalStructureTypeDelegate#getLogicalStructure(IValue)} is no
	 * longer used and can be discarded.
	 *
	 * @param logicalStructure the logical structure value to discard
	 */
	void releaseValue(IValue logicalStructure);

}
