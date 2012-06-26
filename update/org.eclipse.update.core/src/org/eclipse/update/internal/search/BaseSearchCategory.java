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
package org.eclipse.update.internal.search;

import org.eclipse.update.search.*;

public abstract class BaseSearchCategory implements IUpdateSearchCategory {
	private String id;
	
	protected BaseSearchCategory(String id) {
		setId(id);
	}
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		if (this.id==null)
			this.id = id;
	}
}
