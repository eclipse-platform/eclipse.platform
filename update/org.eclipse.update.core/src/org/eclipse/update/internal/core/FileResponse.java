package org.eclipse.update.internal.core;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
import java.io.InputStream;

//import org.eclipse.update.internal.core.net.http.IContext;
//import org.eclipse.update.internal.core.net.http.client.IStatusCodes;
//import org.eclipse.update.internal.core.net.http.client.Response;

/**
 * A File repsonse message
 */
public class FileResponse extends Response {

	/**
	 * 
	 */
	public FileResponse(InputStream in) {
		//super(IStatusCodes.HTTP_OK,"", context, in);
		super(in);
	}

}
