/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
package org.eclipse.ant.internal.core;

import java.io.FileDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.SocketPermission;
import java.security.Permission;
import java.util.PropertyPermission;

import org.eclipse.ant.core.AntCorePlugin;
import org.eclipse.ant.core.AntSecurityException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;

/**
 * A security manager that always throws an <code>AntSecurityException</code> if the calling thread attempts to cause the Java Virtual Machine to
 * exit/halt or if the restricted thread attempts to set a System property. Otherwise this manager just delegates to the pre-existing manager passed
 * in the constructor or mimics the default security manager behavior
 */
@SuppressWarnings("removal") // SecurityManager
public class AntSecurityManager extends SecurityManager {

	private SecurityManager fSecurityManager = null;
	private Thread fRestrictedThread = null;
	// ensure that the PropertyPermission class is loaded before we
	// start checking permissions: bug 85908
	private static final PropertyPermission fgPropertyPermission = new PropertyPermission("*", "write"); //$NON-NLS-1$ //$NON-NLS-2$

	private boolean fAllowSettingSystemProperties = true;

	public AntSecurityManager(SecurityManager securityManager, Thread restrictedThread, boolean allowSettingProperties) {
		fSecurityManager = securityManager;
		fRestrictedThread = restrictedThread;
		fAllowSettingSystemProperties = allowSettingProperties;
	}

	public AntSecurityManager(SecurityManager securityManager, Thread restrictedThread) {
		this(securityManager, restrictedThread, true);
	}

	@Override
	public void checkExit(int status) {
		// no exit allowed from the restricted thread...System.exit is being called
		// by some ant task...do not want Eclipse to exit if
		// in the same VM.
		if (Thread.currentThread() == fRestrictedThread) {
			throw new AntSecurityException();
		}
		if (fSecurityManager != null) {
			fSecurityManager.checkExit(status);
		}
	}

	@Override
	public void checkPermission(Permission perm) {
		if (!fAllowSettingSystemProperties && fgPropertyPermission.implies(perm) && fRestrictedThread == Thread.currentThread()) {
			// attempting to write a system property
			throw new AntSecurityException();
		}
		if (fSecurityManager != null) {
			fSecurityManager.checkPermission(perm);
		}
	}

	@Override
	public void checkAccept(String host, int port) {
		if (fSecurityManager != null) {
			fSecurityManager.checkAccept(host, port);
		}
	}

	@Override
	public void checkAccess(Thread t) {
		if (fSecurityManager != null) {
			fSecurityManager.checkAccess(t);
		}
	}

	@Override
	public void checkAccess(ThreadGroup g) {
		if (fSecurityManager != null) {
			fSecurityManager.checkAccess(g);
		}
	}

	@Override
	public void checkConnect(String host, int port, Object context) {
		if (fSecurityManager != null) {
			fSecurityManager.checkConnect(host, port, context);
		}
	}

	@Override
	public void checkConnect(String host, int port) {
		if (fSecurityManager != null) {
			fSecurityManager.checkConnect(host, port);
		}
	}

	@Override
	public void checkCreateClassLoader() {
		if (fSecurityManager != null) {
			fSecurityManager.checkCreateClassLoader();
		}
	}

	@Override
	public void checkDelete(String file) {
		if (fSecurityManager != null) {
			fSecurityManager.checkDelete(file);
		}
	}

	@Override
	public void checkExec(String cmd) {
		if (fSecurityManager != null) {
			fSecurityManager.checkExec(cmd);
		}
	}

	@Override
	public void checkLink(String lib) {
		if (fSecurityManager != null) {
			fSecurityManager.checkLink(lib);
		}
	}

	@Override
	public void checkListen(int port) {
		if (fSecurityManager != null) {
			fSecurityManager.checkListen(port);
		}
	}

	/**
	 * @deprecated Use {@link #checkPermission(java.security.Permission)} instead
	 */
	@Deprecated
	@Override
	public void checkMulticast(InetAddress maddr, byte ttl) {
		if (fSecurityManager != null) {
			String host = maddr.getHostAddress();
			if (!host.startsWith("[") && host.indexOf(':') != -1) { //$NON-NLS-1$
				host = "[" + host + "]"; //$NON-NLS-1$ //$NON-NLS-2$
			}
			checkPermission(new SocketPermission(host, "accept,connect")); //$NON-NLS-1$
		}
	}

	@Override
	public void checkMulticast(InetAddress maddr) {
		if (fSecurityManager != null) {
			fSecurityManager.checkMulticast(maddr);
		}
	}

	@Override
	public void checkPackageAccess(String pkg) {
		if (fSecurityManager != null) {
			fSecurityManager.checkPackageAccess(pkg);
		}
	}

	@Override
	public void checkPackageDefinition(String pkg) {
		if (fSecurityManager != null) {
			fSecurityManager.checkPackageDefinition(pkg);
		}
	}

	@Override
	public void checkPermission(Permission perm, Object context) {
		if (fSecurityManager != null) {
			fSecurityManager.checkPermission(perm, context);
		}
	}

	@Override
	public void checkPrintJobAccess() {
		if (fSecurityManager != null) {
			fSecurityManager.checkPrintJobAccess();
		}
	}

	@Override
	public void checkPropertiesAccess() {
		if (fSecurityManager != null) {
			fSecurityManager.checkPropertiesAccess();
		}
		super.checkPropertiesAccess();
	}

	@Override
	public void checkPropertyAccess(String key) {
		if (fSecurityManager != null) {
			fSecurityManager.checkPropertyAccess(key);
		}
	}

	@Override
	public void checkRead(FileDescriptor fd) {
		if (fSecurityManager != null) {
			fSecurityManager.checkRead(fd);
		}
	}

	@Override
	public void checkRead(String file, Object context) {
		if (fSecurityManager != null) {
			fSecurityManager.checkRead(file, context);
		}
	}

	@Override
	public void checkRead(String file) {
		if (fSecurityManager != null) {
			fSecurityManager.checkRead(file);
		}
	}

	@Override
	public void checkSecurityAccess(String target) {
		if (fSecurityManager != null) {
			fSecurityManager.checkSecurityAccess(target);
		}
	}

	@Override
	public void checkSetFactory() {
		if (fSecurityManager != null) {
			fSecurityManager.checkSetFactory();
		}
	}

	@Override
	public void checkWrite(FileDescriptor fd) {
		if (fSecurityManager != null) {
			fSecurityManager.checkWrite(fd);
		}
	}

	@Override
	public void checkWrite(String file) {
		if (fSecurityManager != null) {
			fSecurityManager.checkWrite(file);
		}
	}

	@Override
	public Object getSecurityContext() {
		if (fSecurityManager != null) {
			return fSecurityManager.getSecurityContext();
		}
		return super.getSecurityContext();
	}

	@Override
	public ThreadGroup getThreadGroup() {
		if (fSecurityManager != null) {
			fSecurityManager.getThreadGroup();
		}
		return super.getThreadGroup();
	}

	// --------------------------------------------------------------------------------
	// Below are SecurityManager methods deprecated in Java 9 and removed in Java 10.
	// They are accessed through reflections to support Java 8 and 11 at the same time.
	// XXX: This also means you must not add @Override annotations even if Eclipse try to add them.
	// --------------------------------------------------------------------------------

	/**
	 * @deprecated super class method has been removed in JDK 10
	 */
	@Deprecated
	public void checkAwtEventQueueAccess() {
		if (fSecurityManager != null) {
			try {
				final Method m = fSecurityManager.getClass().getMethod("checkAwtEventQueueAccess"); //$NON-NLS-1$
				m.invoke(fSecurityManager);
			}
			catch (NoSuchMethodException e) {
				logDeprecatedAccess(e);
			}
			catch (InvocationTargetException e) {
				if (e.getTargetException() instanceof RuntimeException) {
					throw (RuntimeException) e.getTargetException();
				}
				logException(e);
			}
			catch (IllegalAccessException | IllegalArgumentException e) {
				logException(e);
			}
		}
	}

	/**
	 * @deprecated super class method has been removed in JDK 10
	 */
	@Deprecated
	public void checkMemberAccess(Class<?> clazz, int which) {
		if (fSecurityManager != null) {
			try {
				final Method m = fSecurityManager.getClass().getMethod("checkMemberAccess", Class.class, int.class); //$NON-NLS-1$
				m.invoke(fSecurityManager, clazz, which);
			}
			catch (NoSuchMethodException e) {
				logDeprecatedAccess(e);
			}
			catch (InvocationTargetException e) {
				if (e.getTargetException() instanceof RuntimeException) {
					throw (RuntimeException) e.getTargetException();
				}
				logException(e);
			}
			catch (IllegalAccessException | IllegalArgumentException e) {
				logException(e);
			}
		}
	}

	/**
	 * @deprecated super class method has been removed in JDK 10
	 */
	@Deprecated
	public void checkSystemClipboardAccess() {
		if (fSecurityManager != null) {
			try {
				final Method m = fSecurityManager.getClass().getMethod("checkSystemClipboardAccess"); //$NON-NLS-1$
				m.invoke(fSecurityManager);
			}
			catch (NoSuchMethodException e) {
				logDeprecatedAccess(e);
			}
			catch (InvocationTargetException e) {
				if (e.getTargetException() instanceof RuntimeException) {
					throw (RuntimeException) e.getTargetException();
				}
				logException(e);
			}
			catch (IllegalAccessException | IllegalArgumentException e) {
				logException(e);
			}
		}
	}

	/**
	 * @deprecated super class method has been removed in JDK 10
	 */
	@Deprecated
	public boolean checkTopLevelWindow(Object window) {
		try {
			if (fSecurityManager != null) {
				final Method m = fSecurityManager.getClass().getMethod("checkTopLevelWindow", Object.class); //$NON-NLS-1$
				return (boolean) m.invoke(fSecurityManager, window);
			}
			final Method m = SecurityManager.class.getMethod("checkTopLevelWindow", Object.class); //$NON-NLS-1$
			return (boolean) m.invoke(new SecurityManager(), window);
		}
		catch (NoSuchMethodException e) {
			logDeprecatedAccess(e);
		}
		catch (InvocationTargetException e) {
			if (e.getTargetException() instanceof RuntimeException) {
				throw (RuntimeException) e.getTargetException();
			}
			logException(e);
		}
		catch (IllegalAccessException | IllegalArgumentException e) {
			logException(e);
		}
		return false;
	}

	/**
	 * @deprecated super class method has been removed in JDK 10
	 */
	@Deprecated
	public boolean getInCheck() {
		try {
			if (fSecurityManager != null) {
				final Method m = fSecurityManager.getClass().getMethod("getInCheck"); //$NON-NLS-1$
				return (boolean) m.invoke(fSecurityManager);
			}
			final Method m = SecurityManager.class.getMethod("getInCheck"); //$NON-NLS-1$
			return (boolean) m.invoke(new SecurityManager());
		}
		catch (NoSuchMethodException e) {
			logDeprecatedAccess(e);
		}
		catch (InvocationTargetException e) {
			if (e.getTargetException() instanceof RuntimeException) {
				throw (RuntimeException) e.getTargetException();
			}
			logException(e);
		}
		catch (IllegalAccessException | IllegalArgumentException e) {
			logException(e);
		}
		return false;
	}

	private static void logDeprecatedAccess(Throwable e) {
		Platform.getLog(AntCorePlugin.getPlugin().getBundle()).log(new Status(IStatus.WARNING, AntCorePlugin.PI_ANTCORE, InternalCoreAntMessages.AntSecurityManager_0, e));
	}

	private static void logException(Throwable e) {
		Platform.getLog(AntCorePlugin.getPlugin().getBundle()).log(new Status(IStatus.ERROR, AntCorePlugin.PI_ANTCORE, e.getLocalizedMessage(), e));
	}
}
