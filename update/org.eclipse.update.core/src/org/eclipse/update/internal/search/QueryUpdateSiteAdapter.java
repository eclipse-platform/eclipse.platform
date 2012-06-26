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

import java.net.*;

import org.eclipse.update.search.*;

public class QueryUpdateSiteAdapter extends UpdateSiteAdapter implements IQueryUpdateSiteAdapter {
	private String mappingId;

	public QueryUpdateSiteAdapter(String label, URL url, String mappingId) {
		super(label, url);
		this.mappingId = mappingId;
	}
	
	public String getMappingId() {
		return mappingId;
	}
}
