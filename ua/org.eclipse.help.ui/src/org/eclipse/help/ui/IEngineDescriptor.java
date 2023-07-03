/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
package org.eclipse.help.ui;

import java.util.Dictionary;

/**
 * Descriptor of a concrete instance of a search engine. It describes the search
 * engine instance that is either loaded from the plug-in extension point contribution,
 * or created by the user in the scope settings dialog. Engines created by the
 * user are marked as such. Only user-defined engines will permit their label or
 * description changed.
 * <p>
 * This interface is not intended to be extended or implemented by clients.
 *
 * @since 3.1
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 *
 */

public interface IEngineDescriptor {
	/**
	 * Returns the unique identifier of this engine instance.
	 *
	 * @return the unique engine identifier
	 */
	String getId();

	/**
	 * Returns the unique identifier of the engine type of which this is an
	 * instance.
	 *
	 * @return the engine type identifier
	 */
	String getEngineTypeId();

	/**
	 * Returns the label of this engine for rendering in the UI.
	 *
	 * @return the engine label
	 */
	String getLabel();

	/**
	 * Changes the label of this engine. This method does nothing for engine
	 * descriptors that are not user-defined.
	 *
	 * @param label
	 *            the new engine label
	 */
	void setLabel(String label);

	/**
	 * Returns the description of this engine instance. It is initialized
	 * from the engine type description.
	 * @return the engine instance description.
	 */
	String getDescription();

	/**
	 * Changes the description of this engine. This method does nothing for
	 * engine descriptors that are not user-defined.
	 *
	 * @param desc
	 *            the new engine description
	 */
	void setDescription(String desc);

	/**
	 * Returns the parameters used to configure this engine according to the
	 * valid parameters for the associated engine type.
	 *
	 * @return the parameter dictionary
	 */
	Dictionary<String, Object> getParameters();

	/**
	 * Tests whether this engine is provided as an extension point contribution
	 * or is created by the user in the scope settings dialog. Only user-defined
	 * engine instances can have their label and/or description changed.
	 *
	 * @return <code>true</code> if the engine is user defined, or
	 *         <code>false</code> otherwise.
	 */
	boolean isUserDefined();
}
