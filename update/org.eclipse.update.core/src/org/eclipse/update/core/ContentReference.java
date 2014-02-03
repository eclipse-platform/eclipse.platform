/*******************************************************************************
 *  Copyright (c) 2000, 2012 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.osgi.util.NLS;
import org.eclipse.update.internal.core.FatalIOException;
import org.eclipse.update.internal.core.Messages;
import org.eclipse.update.internal.core.URLEncoder;
import org.eclipse.update.internal.core.UpdateManagerUtils;
import org.eclipse.update.internal.core.connection.ConnectionFactory;
import org.eclipse.update.internal.core.connection.HttpResponse;
import org.eclipse.update.internal.core.connection.IResponse;

/**
 * Content reference implements a general access wrapper 
 * to feature and site content. The reference specifies
 * a "symbolic" path identifier for the content, and the actual
 * reference as a file, or a URL.
 * <p>
 * This class may be instantiated or subclassed by clients.
 * </p> 
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 * @see org.eclipse.update.core.JarContentReference
 * @see org.eclipse.update.core.JarEntryContentReference
 * @since 2.0
 * @deprecated The org.eclipse.update component has been replaced by Equinox p2.
 * This API will be deleted in a future release. See bug 311590 for details.
 */
public class ContentReference {

	/**
	 * Unknown size indication
	 * @since 2.0
	 */
	public static final long UNKNOWN_SIZE = -1;

	/**
	 * Default executable permission when installing a content reference
	 * Will add executable bit if necessary
	 * 
	 * @since 2.0.1
	 */
	public static final int DEFAULT_EXECUTABLE_PERMISSION = -1;

	private static final String FILE_URL_PROTOCOL = "file"; //$NON-NLS-1$

	private String id;
	private URL url; // reference is either URL reference *OR*
	private File file; //    local file reference
	private IResponse response;
	private int permission; 
	private long length;
	
	// <true> if a copy of a Contentreferenec in a temp local directory
	private boolean tempLocal = false;
	
	private long lastModified;

	/**
	 * Create content reference from URL.
	 * 
	 * @param id "symbolic" path identifier
	 * @param url actual referenced URL
	 * @since 2.0
	 */
	public ContentReference(String id, URL url) {
		this.id = (id == null ? "" : id); //$NON-NLS-1$
		this.url = url; // can be null
		this.file = null;
	}

	/**
	 * Create content reference from file.
	 * 
	 * @param id "symbolic" path identifier
	 * @param file actual referenced file
	 * @since 2.0
	 */
	public ContentReference(String id, File file) {
		this.id = (id == null ? "" : id); //$NON-NLS-1$
		this.file = file; // can be null
		this.url = null;
	}

	/**
	 * A factory method to create a content reference of
	 * the same type.
	 * 
	 * @param id "symbolic" path identifier
	 * @param file actual referenced file
	 * @return content reference of the same type
	 * @since 2.0
	 */
	public ContentReference createContentReference(String id, File file) {
		return new ContentReference(id, file,true);
	}
	/**
	 * 
	 */
	private ContentReference(String id, File file, boolean b) {
		this(id,file);
		setTempLocal(b);
	}

	/**
	 * Retrieves the "symbolic" path identifier for the reference.
	 * 
	 * @return "symbolic" path identifier
	 * @since 2.0
	 */
	public String getIdentifier() {
		return id;
	}

	/**
	 * Creates an input stream for the reference.
	 * 
	 * @return input stream
	 * @exception IOException unable to create stream
	 * @since 2.0
	 */
	public InputStream getInputStream() throws IOException {
		if (file != null)
			return new FileInputStream(file);
		else if (url != null) {
			if (response == null) {
				URL resolvedURL = URLEncoder.encode(url);
				response = ConnectionFactory.get(resolvedURL);
				UpdateManagerUtils.checkConnectionResult(response,resolvedURL);
			}
			InputStream is=response.getInputStream();
			length=response.getContentLength();
			return is;
		} else
			throw new FatalIOException(NLS.bind(Messages.ContentReference_UnableToCreateInputStream, (new String[] { this.toString() })));
	}
	/**
	 * Creates an input stream for the reference.
	 * 
	 * @return input stream
	 * @exception IOException unable to create stream
	 * @since 2.0
	 */
	InputStream getPartialInputStream(long offset) throws IOException {
		if (url != null && "http".equals(url.getProtocol())) { //$NON-NLS-1$
			URL resolvedURL = URLEncoder.encode(url);
			response = ConnectionFactory.get(resolvedURL);
			if(response instanceof HttpResponse)
				((HttpResponse)response).setOffset(offset);
			UpdateManagerUtils.checkConnectionResult(response,resolvedURL);
			InputStream is = response.getInputStream();
			length=offset + response.getContentLength();
			return is;
		} else
			throw new FatalIOException(NLS.bind(Messages.ContentReference_UnableToCreateInputStream, (new String[] { this.toString() })));
	}
	
	/**
	 * Returns the size of the referenced input, if it can be determined.
	 * 
	 * @return input size, or @see #UNKNOWN_SIZE if size cannot be determined.
	 * @since 2.0
	 */
	public long getInputSize() throws IOException {
		if (length>0)
			return length;
		if (file != null)
			return file.length();
		else if (url != null) {
			if (response == null) {
				URL resolvedURL = null;
				try {
					resolvedURL = URLEncoder.encode(url);
					response = ConnectionFactory.get(resolvedURL);
				} catch (IOException e) {
					return ContentReference.UNKNOWN_SIZE;
				}
				UpdateManagerUtils.checkConnectionResult(response,resolvedURL);			
			}
			long size = response.getContentLength();
			return size == -1 ? ContentReference.UNKNOWN_SIZE : size;
		} else
			return ContentReference.UNKNOWN_SIZE;
	}

	/**
	 * Indicates whether the reference is a local file reference.
	 * 
	 * @return <code>true</code> if the reference is local, 
	 * otherwise <code>false</code>
	 * @since 2.0
	 */
	public boolean isLocalReference() {
		/*if (file != null)
			return true;
		else if (url != null)
			return FILE_URL_PROTOCOL.equals(url.getProtocol());
		else
			return false;*/
		// only temp files are considered local
		return tempLocal;
	}

	/**
	 * Returns the content reference as a file. Note, that this method
	 * <b>does not</b> cause the file to be downloaded if it
	 * is not already local.
	 * 
	 * @return reference as file
	 * @exception IOException reference cannot be returned as file
	 * @since 2.0
	 */
	public File asFile() throws IOException {
		if (file != null)
			return file;

		if (url != null && FILE_URL_PROTOCOL.equals(url.getProtocol())) {
			File result = new File(url.getFile());
			if (result.exists())
				return result;
			else 
				throw new IOException(NLS.bind(Messages.ContentReference_FileDoesNotExist, (new String[] { this.toString() }))); 			
		}

		throw new IOException(NLS.bind(Messages.ContentReference_UnableToReturnReferenceAsFile, (new String[] { this.toString() }))); 
	}

	/**
	 * Returns the content reference as a URL.
	 * 
	 * @return reference as URL
	 * @exception IOException reference cannot be returned as URL
	 * @since 2.0
	 */
	public URL asURL() throws IOException {
		if (url != null)
			return url;

		if (file != null)
			return file.toURL();

		throw new FatalIOException(NLS.bind(Messages.ContentReference_UnableToReturnReferenceAsURL, (new String[] { this.toString() })));
	}

	/**
	 * Return string representation of this reference.
	 * 
	 * @return string representation
	 * @since 2.0
	 */
	public String toString() {
		if (file != null)
			return file.getAbsolutePath();
		else
			return url.toExternalForm();
	}
	/**
	 * Returns the permission for this file.
	 * 
	 * @return the content reference permission
	 * @see #DEFAULT_EXECUTABLE_PERMISSION
	 * @since 2.0.1
	 */
	public int getPermission() {
		return permission;
	}

	/**
	 * Sets the permission of this content reference.
	 * 
	 * @param permission The permission to set
	 */
	public void setPermission(int permission) {
		this.permission = permission;
	}

	/**
	 * Sets if a content reference is considered local 
	 * 
	 * @param tempLocal <code>true</code> if the file is considered local
	 */
	protected void setTempLocal(boolean tempLocal) {
		this.tempLocal = tempLocal;
	}
	
	/**
	 * Sets the timestamp the content was last modified.
	 * @param timestamp
	 * @since 3.0
	 */
	public void setLastModified(long timestamp) {
		this.lastModified = timestamp;
	}
	
	/**
	 * Returns the timestamp when the content was last modified
	 * @return the timestamp
	 * @since 3.0
	 */
	public long getLastModified() {
		if (lastModified == 0) {
			if (file != null) 
				lastModified = file.lastModified();
			else if (url != null) {
				if (response == null) {
					try {
						URL resolvedURL = URLEncoder.encode(url);
						response = ConnectionFactory.get(resolvedURL);
					} catch (MalformedURLException e) {
						// return 0
					} catch (IOException e) {
						// return 0
					}
				}
				lastModified = response.getLastModified();
			} 
		}
		return lastModified;
	}
}
