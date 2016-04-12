/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.history;

import org.eclipse.team.core.history.IFileRevision;

public class MessageHistoryCategory extends AbstractHistoryCategory {
	private String message;
	
	public MessageHistoryCategory(String message){
		this.message = message;
	}
	@Override
	public String getName() {
		return message;
	}

	@Override
	public boolean collectFileRevisions(IFileRevision[] fileRevisions, boolean shouldRemove) {
		return false;
	}

	@Override
	public IFileRevision[] getRevisions() {
		return new IFileRevision[0];
	}

	@Override
	public boolean hasRevisions() {
		return false;
	}

}
