package org.eclipse.ant.internal.core.old;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights
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

import org.eclipse.ant.core.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.apache.tools.ant.*;
import java.io.*;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * An Ant project adapted to running inside the Eclipse Platform.  Because of the class
 * loading structure of the Eclipse platform, the standard techniques for creating instances 
 * of tasks and datatypes needs to be adapted.
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under 
 * development and expected to change significantly before reaching stability. 
 * It is being made available at this early stage to solicit feedback from pioneering 
 * adopters on the understanding that any code that uses this API will almost 
 * certainly be broken (repeatedly) as the API evolves.
 * </p>
 */

public class EclipseProject extends Project {
/**
 * Creates a new project for use in running Ant inside the Eclipse Platform.
 */
public EclipseProject() {
	super();
}
/**
 * Creates and returns a new instance of the identified data type.
 * 
 * @return the new data type instance
 * @param typeName the name of the type to create
 * @exception BuildException thrown if a problem occurs during data type creation
 */
public Object createDataType(String typeName) throws BuildException {
	// First look in the types defined in the plugin extension points. If not
	// found, do the super behaviour.
	// check to see if the ant plugin is available.  If we are running from
	// the command line (i.e., no platform) it will not be.
	if (AntPlugin.getPlugin() == null)
		return internalCreateDataType(typeName);
	Map types = AntPlugin.getPlugin().getTypeExtensions();
	if (types == null)
		return internalCreateDataType(typeName);
	IConfigurationElement declaration = (IConfigurationElement) types.get(typeName);
	if (declaration == null)
		return internalCreateDataType(typeName);
	String className = declaration.getAttribute(AntPlugin.CLASS);
	try {
		Class typeClass = declaration.getDeclaringExtension().getDeclaringPluginDescriptor().getPluginClassLoader().loadClass(className);
		addDataTypeDefinition(typeName, typeClass);
	} catch (ClassNotFoundException e) {
		return internalCreateDataType(typeName);
	}
	return internalCreateDataType(typeName);
}
/**
 * Creates and returns a new instance of the identified task.
 * 
 * @return the new task
 * @param taskName the name of the task to create
 * @exception BuildException thrown if a problem occurs during task creation
 */
public Task createTask(String taskName) throws BuildException {
	// First try to find if the task is defined in a plug-in. If not, call
	// the super method.
	// check to see if the ant plugin is available.  If we are running from
	// the command line (i.e., no platform) it will not be.
	if (AntPlugin.getPlugin() == null)
		return super.createTask(taskName);
	Map tasks = AntPlugin.getPlugin().getTaskExtensions();
	if (tasks == null)
		return super.createTask(taskName);
	IConfigurationElement declaration = (IConfigurationElement) tasks.get(taskName);
	if (declaration == null)
		return super.createTask(taskName);
	String className = declaration.getAttribute(AntPlugin.CLASS);
	try {
		Class taskClass = declaration.getDeclaringExtension().getDeclaringPluginDescriptor().getPluginClassLoader().loadClass(className);
		addTaskDefinition(taskName, taskClass);
	} catch (ClassNotFoundException e) {
		return super.createTask(taskName);
	}
	return super.createTask(taskName);
}
/**
 * Sends a build finished notification to all registered listeners along with
 * the exception that caused the termination.
 * 
 * @param exception the exception to include with the notification
 */
protected void fireBuildFinished(Throwable exception) {
	super.fireBuildFinished(exception);
}
/**
 * Sends a build started notification to all registered listeners.
 */
protected void fireBuildStarted() {
	super.fireBuildStarted();
}
/**
 * Sends a target started notification to all the listeners when an execute target 
 * has just been started.
 */
protected void fireExecuteTargetStarted(Target target) {
    BuildEvent event = new BuildEvent(target);
    Vector buildListeners = getBuildListeners();
    for (int i = 0; i < buildListeners.size(); i++) {
		if (buildListeners.elementAt(i) instanceof IAntRunnerListener) {
			IAntRunnerListener listener = (IAntRunnerListener) buildListeners.elementAt(i);
			listener.executeTargetStarted(event);
		}
	}
}
/**
 * Sends a target finished notification to all the listeners when an execute target
 * has just been finished.
 */
protected void fireExecuteTargetFinished(Target target, Throwable exception) {
    BuildEvent event = new BuildEvent(target);
    event.setException(exception);
    Vector buildListeners = getBuildListeners();
    for (int i = 0; i < buildListeners.size(); i++) {
		if (buildListeners.elementAt(i) instanceof IAntRunnerListener) {
			IAntRunnerListener listener = (IAntRunnerListener) buildListeners.elementAt(i);
			listener.executeTargetFinished(event);
		}
	}
}
/**
 * Initializes the receiver.
 * 
 * @exception BuildException thrown if a problem occurs during initialization.
 */
public void init() throws BuildException {
	super.init();
	// add some additional tasks and datatypes.  Normally they would be found
	// in the plugin.xml markup for the Ant Support plugin but if we are not running 
	// the platform we need to add them here.  
	addTaskDefinition("ant", EclipseAnt.class);
	addTaskDefinition("javac", EclipseJavac.class);
	addDataTypeDefinition("commapatternset", CommaPatternSet.class);
	addDataTypeDefinition("command", CommandDataType.class);
	System.setProperty("ant.regexp.matcherimpl", "org.eclipse.ant.core.XercesRegexpMatcher");
	// initialize the datatype table with marker values so that the table contains
	// the keys (needed while parsing) but don't load the classes to prevent plugin activation.
	if (AntPlugin.getPlugin() == null)
		return;
	Map types = AntPlugin.getPlugin().getTypeExtensions();
	if (types == null)
		return;
	for (Iterator i = types.keySet().iterator(); i.hasNext();) {
		String typeName = (String)i.next();
		if (getDataTypeDefinitions().get(typeName) == null)
			addDataTypeDefinition(typeName, EclipseProject.class);
	}
}

protected Object internalCreateDataType(String typeName) throws BuildException {
    Class c = (Class) getDataTypeDefinitions().get(typeName);

    if (c == null)
        return null;

    try {
        java.lang.reflect.Constructor ctor = null;
        boolean noArg = false;
        // DataType can have a "no arg" constructor or take a single 
        // Project argument.
        try {
            ctor = c.getConstructor(new Class[0]);
            noArg = true;
        } catch (NoSuchMethodException nse) {
            ctor = c.getConstructor(new Class[] {Project.class});
            noArg = false;
        }

        Object o = null;
        if (noArg) {
             o = ctor.newInstance(new Object[0]);
        } else {
             o = ctor.newInstance(new Object[] {this});
        }
        if (o instanceof ProjectComponent) {
            ((ProjectComponent)o).setProject(this);
        }
        String msg = "   +DataType: " + typeName;
        log (msg, MSG_DEBUG);
        return o;
    } catch (java.lang.reflect.InvocationTargetException ite) {
        Throwable t = ite.getTargetException();
        String msg = "Could not create datatype of type: "
             + typeName + " due to " + t;
        throw new BuildException(msg, t);
    } catch (Throwable t) {
        String msg = "Could not create datatype of type: "
             + typeName + " due to " + t;
        throw new BuildException(msg, t);
    }
}
/**
 * Executes a target. Notification has been added: the build listener knows that a top level
 * target is being executed and when it is finished.
 * 
 * @see Project#executeTarget(String targetName)
 */
public void executeTarget(String targetName) throws BuildException {
	Target targetToExecute = (Target) getTargets().get(targetName);
	if (targetToExecute == null)
		// can happen if the user has specified a target name that is not valid
		throw new BuildException(Policy.bind("exception.targetDoesNotExist", targetName));
    try {
        fireExecuteTargetStarted(targetToExecute);
        super.executeTarget(targetName);
        fireExecuteTargetFinished(targetToExecute, null);
    } catch(RuntimeException exc) {
        fireExecuteTargetFinished(targetToExecute, exc);
        throw exc;
    }
}





}
