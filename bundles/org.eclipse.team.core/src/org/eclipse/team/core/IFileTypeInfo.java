/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.core;

/**
 * A file type info specifies both the file extension and the corresponding file
 * type.
 *
 * @since 2.0
 * @deprecated Use the <code>IFileContentManager</code> API instead.
 * @noimplement This interface is not intended to be implemented by clients.
 */
@Deprecated
public interface IFileTypeInfo {
	/**
	 * Returns the string specifying the file extension
	 *
	 * @return the file extension
	 */
	public String getExtension();

	/**
	 * Returns the file type for files ending with the corresponding
	 * extension.
	 *
	 * Valid values are:
	 * Team.UNKNOWN
	 * Team.TEXT
	 * Team.BINARY
	 *
	 * @return the file type
	 */
	public int getType();
}
