/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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
package org.eclipse.ua.tests.help.webapp.service;

import java.io.File;
import java.net.URL;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.xml.sax.SAXException;

public class SchemaValidator {

	public static String testXMLSchema(String uri, String schemaFile) {
		String msg = ""; //$NON-NLS-1$
		try {
			SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema"); //$NON-NLS-1$
			Schema schema = null;
			if (schemaFile.startsWith("http") || schemaFile.startsWith("ftp"))
				schema = factory.newSchema(new URL(schemaFile));
			else
				schema = factory.newSchema(new File(schemaFile));

			Validator validator = schema.newValidator();
			Source source = new StreamSource(uri);
			try {
				validator.validate(source);
				msg = "valid"; //$NON-NLS-1$
			} catch (SAXException ex) {
				msg = "not valid. Details: " + ex.getMessage(); //$NON-NLS-1$
			}
		} catch(Exception e) {
			msg = "Exception e: " + e; //$NON-NLS-1$
		}

		return msg;
	}

	public static String testJSONSchema(String uri, String schemaFile) {
		// TODO: Not yet implemented
		return ""; //$NON-NLS-1$
	}
}
