/*
 * Copyright (c) 2002 IBM Corp.  All rights reserved.
 * This file is made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 */
package org.eclipse.update.internal.ui.model;

import org.eclipse.update.core.*;

/**
 * @version 	1.0
 * @author
 */
public interface IConfiguredFeatureAdapter extends IConfiguredSiteContext {
	public boolean isConfigured();
	public boolean isUpdated();
}
