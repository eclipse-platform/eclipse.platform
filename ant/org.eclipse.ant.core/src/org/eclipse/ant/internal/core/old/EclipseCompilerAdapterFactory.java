package org.eclipse.ant.internal.core.old;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.ant.core.*;
import org.eclipse.core.runtime.IConfigurationElement;

import org.apache.tools.ant.*;
import org.apache.tools.ant.taskdefs.compilers.CompilerAdapterFactory;
import org.apache.tools.ant.taskdefs.compilers.CompilerAdapter;
import java.util.Map;

/**
 * Static helper functionality for finding the correct compiler adapter to use for the current
 * Javac task.  This class logically wrappers the standard Ant 
 * <code>CompilerAdapterFactory</code>.  It changes the behavior to use the
 * Ant plug-ins object class registry to find the compiler adapter class.  This is needed
 * because of the platform's class loading strategy (otherwise the Ant plug-in would likely not
 * be able to see the other classes).
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under 
 * development and expected to change significantly before reaching stability. 
 * It is being made available at this early stage to solicit feedback from pioneering 
 * adopters on the understanding that any code that uses this API will almost 
 * certainly be broken (repeatedly) as the API evolves.
 * </p>
 */

public class EclipseCompilerAdapterFactory {

private EclipseCompilerAdapterFactory() {
}
/**
 * Returns the <code>CompilerAdapter</code> to use given a compiler type and task.
 * 
 * @return the compiler adapter to use
 * @param compilerType the type of compiler
 * @param task the task
 * @exception BuildException thrown if a problem occurs while returning the adapter
 */
public static CompilerAdapter getCompiler(String compilerType, Task task) throws BuildException {
	try {
		return CompilerAdapterFactory.getCompiler(compilerType, task);
	} catch (BuildException be) {
		if (AntPlugin.getPlugin() == null)
			throw be;
		Map objects = AntPlugin.getPlugin().getObjectExtensions();
		if (objects == null)
			throw be;
		IConfigurationElement declaration = (IConfigurationElement) objects.get(compilerType);
		if (declaration == null)
			throw be;
		String className = declaration.getAttribute(AntPlugin.CLASS);
		try {
			Class clazz = declaration.getDeclaringExtension().getDeclaringPluginDescriptor().getPluginClassLoader().loadClass(className);
			return (CompilerAdapter) clazz.newInstance();
		} catch (ClassNotFoundException e) {
			throw new BuildException(Policy.bind("exception.classNotFound",className), e);
		} catch (ClassCastException e) {
			throw new BuildException(Policy.bind("exception.classNotCompiler",className), e);
		} catch (Exception e) {
			throw new BuildException(Policy.bind("exception.creationProblem",className), e);
		}
	}
}
}
