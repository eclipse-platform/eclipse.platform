/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ant.internal.core.ant;

/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999, 2000 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectComponent;

/**
 * A subclass of Project to facilitate "faster" parsing with
 * less garbage generated. This class is not used on Ant 1.6 and newer
 * due to the improvements in lazy loading of these Ant versions.
 * 
 * Only three tasks are loaded (property, taskdef and 
 * typedef: three tasks that can be defined outside of a target on Ant 1.5.1 or older).
 *
 * Datatypes are loaded if requested.
 * 
 * Derived from the original Ant Project class
 */
public class InternalProject extends Project {

	private Hashtable typeNameToClass = null;
	
	public InternalProject() {
		super();
	}

	/* (non-Javadoc)
	 * @see org.apache.tools.ant.Project#init()
	 */
	public void init() throws BuildException {
		setJavaVersionProperty();

		try {
			Class taskClass = Class.forName("org.apache.tools.ant.taskdefs.Property"); //$NON-NLS-1$
			addTaskDefinition("property", taskClass); //$NON-NLS-1$
			taskClass = Class.forName("org.apache.tools.ant.taskdefs.Typedef"); //$NON-NLS-1$
			addTaskDefinition("typedef", taskClass); //$NON-NLS-1$
			taskClass = Class.forName("org.apache.tools.ant.taskdefs.Taskdef"); //$NON-NLS-1$
			addTaskDefinition("taskdef", taskClass); //$NON-NLS-1$
		} catch (NoClassDefFoundError e) {
			throw new BuildException(InternalAntMessages.getString("InternalAntRunner.Missing_Class"), e); //$NON-NLS-1$
		} catch (ClassNotFoundException c) {
			throw new BuildException(InternalAntMessages.getString("InternalAntRunner.Missing_Class"), c); //$NON-NLS-1$
		}

		setSystemProperties();
	}

	/* (non-Javadoc)
	 * @see org.apache.tools.ant.Project#createDataType(java.lang.String)
	 */
	public Object createDataType(String typeName) throws BuildException {
		if (typeNameToClass == null) {
			initializeTypes();
		}
		Class typeClass = (Class) typeNameToClass.get(typeName);

		if (typeClass == null) {
			return null;
		}

		try {
			Constructor ctor = null;
			boolean noArg = false;
			// DataType can have a "no arg" constructor or take a single
			// Project argument.
			try {
				ctor = typeClass.getConstructor(new Class[0]);
				noArg = true;
			} catch (NoSuchMethodException nse) {
				ctor = typeClass.getConstructor(new Class[] { Project.class });
				noArg = false;
			}

			Object o = null;
			if (noArg) {
				o = ctor.newInstance(new Object[0]);
			} else {
				o = ctor.newInstance(new Object[] { this });
			}
			if (o instanceof ProjectComponent) {
				((ProjectComponent) o).setProject(this);
			}
			return o;
		} catch (InvocationTargetException ite) {
			return null;
		} catch (Throwable t) {
			return null;
		}
	}

	/**
	 * Initialize the mapping of data type name to data type classname
	 */
	private void initializeTypes() {
		typeNameToClass = new Hashtable(18);
		String dataDefs = "/org/apache/tools/ant/types/defaults.properties"; //$NON-NLS-1$
		try {
			Properties props = new Properties();
			InputStream in = Project.class.getResourceAsStream(dataDefs);
			if (in == null) {
				return;
			}
			props.load(in);
			in.close();

			Enumeration enumeration = props.propertyNames();
			while (enumeration.hasMoreElements()) {
				String typeName = (String) enumeration.nextElement();
				String className = props.getProperty(typeName);
				try {
					Class typeClass= Class.forName(className);
					typeNameToClass.put(typeName, typeClass);
				} catch (NoClassDefFoundError e) {
					//ignore
				} catch (ClassNotFoundException c) {
					//ignore
				}
			}
		} catch (IOException ioe) {
			return;
		}

	}
	
	/* (non-Javadoc)
	 * @see org.apache.tools.ant.Project#getDataTypeDefinitions()
	 */
	public Hashtable getDataTypeDefinitions() {
		if (typeNameToClass == null) {
			initializeTypes();
		}
		return typeNameToClass;
	}
	
	/* (non-Javadoc)
	 * @see org.apache.tools.ant.Project#addDataTypeDefinition(java.lang.String, java.lang.Class)
	 */
	public void addDataTypeDefinition(String typeName, Class typeClass) {
		getDataTypeDefinitions();
		typeNameToClass.put(typeName, typeClass);
	}
}
