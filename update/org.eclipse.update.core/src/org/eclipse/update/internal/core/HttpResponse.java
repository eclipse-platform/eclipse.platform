package org.eclipse.update.internal.core;

import java.net.URL;

/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
public class HttpResponse extends Response {
	
	/**
	 * 
	 */
	public HttpResponse(URL url) {
		//super(IStatusCodes.HTTP_OK,"", context, in);
		super(url);
	}

}
