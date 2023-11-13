/**
 * 
 */
package org.eclipse.pki.util;

//import org.eclipse.core.runtime.Platform;


/**
 * A logging utility class for {@link AuthenticationPlugin}
 */
public class LogUtil {
	public static void logError(String message, Throwable t) {
		StackWalker stackWalker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
		final var pluginName = stackWalker.getClass();
		//Platform.getLog(pluginName).info(pluginName.getCanonicalName() + ":" + message); //$NON-NLS-1$
	}
	
	
}
