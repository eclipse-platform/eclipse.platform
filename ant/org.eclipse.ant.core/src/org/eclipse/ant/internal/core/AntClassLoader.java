package org.eclipse.ant.internal.core;

import java.net.URL;
import java.net.URLClassLoader;

public class AntClassLoader extends URLClassLoader {

	protected ClassLoader[] pluginLoaders;

public AntClassLoader(URL[] urls, ClassLoader[] pluginLoaders, ClassLoader parent) {
	super(urls, parent);
	this.pluginLoaders = pluginLoaders;
}

public Class loadClass(String name) {
	Class result = loadClassParent(name);
	if (result == null)
		result = loadClassURLs(name);
	if (result == null)
		result = loadClassPlugins(name);
	return result;
}

protected Class loadClassParent(String name) {
	try {
		ClassLoader parent = getParent();
		if (parent != null)
			return parent.loadClass(name);
	} catch (ClassNotFoundException e) {
	}
	return null;
}

protected Class loadClassURLs(String name) {
	try {
		return super.loadClass(name);
	} catch (ClassNotFoundException e) {
	}
	return null;
}

protected Class loadClassPlugins(String name) {
	Class result = null;
	if (pluginLoaders != null) {
		for (int i = 0; (i < pluginLoaders.length) && (result == null); i++) {
			try {
				result = pluginLoaders[i].loadClass(name);
			} catch (ClassNotFoundException e) {
			}
		}
	}
	return result;
}
}