/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.ui.operations;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.ui.tags.TagSource;

public interface ITagOperation {
	public abstract CVSTag getTag();
	public abstract void setTag(CVSTag tag);
	public abstract void run() throws InvocationTargetException, InterruptedException;
	/**
	 * Return whether the tag operation contains any resource that would be operated on.
	 * @return whether the tag operation contains any resource that would be operated on
	 */
	public abstract boolean isEmpty();
	public abstract void moveTag();
	public abstract void doNotRecurse();
	public abstract TagSource getTagSource();
}
