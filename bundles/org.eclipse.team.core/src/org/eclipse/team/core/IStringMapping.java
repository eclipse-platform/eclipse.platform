/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
 * A simple interface for mappings from a string (usually a file name or a file
 * extension) and a content type (typically <code>Team.TEXT</code>,
 * <code>Team.BINARY</code> or <code>Team.UNKNOWN</code>.
 *
 * @since 3.1
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IStringMapping {

    /**
     * The string part of the mapping
     *
     * @return the string
     *
     * @since 3.1
     */
    String getString();

    /**
     * The content type associated with the string
     *
     * @return the content type
     *
     * @since 3.1
     */
    int getType();
}
