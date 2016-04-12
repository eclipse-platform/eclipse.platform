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
package org.eclipse.team.internal.ui.synchronize;

/**
 * Interface that can appear of a synchronize participant if that participant supports
 * change sets. This was not made API in 3.1 because it was felt that the API was not
 * ready at that point.
 */
public interface IChangeSetProvider {

    /**
     * Return the change set capability for this participant or <code>null</code>
     * if change sets are not supported.
     * @return the change set capability for this participant
     * @since 3.1
     */
    public ChangeSetCapability getChangeSetCapability();
}
