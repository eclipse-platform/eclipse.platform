/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.update.internal.core.connection;

import java.io.IOException;
import java.net.URL;

public class ConnectionFactory {

	
	public static IResponse get(URL url) throws IOException {
		//Request request = null;
		IResponse response = null;
		
		if ("file".equals(url.getProtocol())) { //$NON-NLS-1$
			response = new FileResponse(url);
		} else if (url != null && url.getProtocol().startsWith("http")) { //$NON-NLS-1$
			response = new HttpResponse(url);
		} else {
			response = new OtherResponse(url);
		}

		return response;
	}
}
