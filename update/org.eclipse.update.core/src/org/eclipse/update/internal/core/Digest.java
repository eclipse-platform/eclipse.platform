/*******************************************************************************
 * Copyright (c) 2006, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.update.core.JarContentReference;
import org.eclipse.update.core.model.FeatureModel;
import org.xml.sax.SAXException;

public class Digest {
	
	private URL source;
	private File localSource;
	private JarFile digestJar;
	private InputStream inputStream;
	

	public Digest(URL source){
		this.source = source;
	}
	
	public FeatureModel[] parseDigest() throws IOException, CoreException, SAXException {
		DigestContentProvider digestContentProvider = new DigestContentProvider(source);
		localSource = digestContentProvider.asLocalReference(new JarContentReference( null, source), null).asFile();
		digestJar = new JarFile(localSource);
		
		ZipEntry digestEntry = digestJar.getEntry("digest.xml"); //$NON-NLS-1$
		
		if (digestEntry != null) {
			inputStream = digestJar.getInputStream(digestEntry);
			DigestParser digest = new DigestParser();
			digest.init(new LiteFeatureFactory());
			return digest.parse(inputStream);
		} else {
			throw new CoreException(null);
		}
	}

}
