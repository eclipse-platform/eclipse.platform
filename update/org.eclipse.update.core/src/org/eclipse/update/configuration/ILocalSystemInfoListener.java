package org.eclipse.update.configuration;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
 


/**
 * Local system change listener interface.
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 * @see LocalSystemInfo#addInfoListener(ILocalSystemInfoListener)
 * @see LocalSystemInfo#removeInfoListener(ILocalSystemInfoListener)
 * @see LocalSystemInfo#fireSystemInfoChanged(IVolume,int)
 * @since 2.0
 */
public interface ILocalSystemInfoListener {
	
	/**
	 * Volume change notification.
	 * Called each time there are relevant volume changes
	 * detected. This specifically includes changes to the
	 * file system structure as a result of removable drive/ media
	 * operations (eg. CD insertion), and changes to volume 
	 * mount structure.
	 * @param volume volume of the changed file
	 * system structure. Any current paths beyond
	 * the specified 'root' file of the volume are assumed to be invalidated.
	 * @param changeType type of the change that occured.
	 * @see LocalSystemInfo#VOLUME_ADDED
	 * @see LocalSystemInfo#VOLUME_REMOVED
	 * @see LocalSystemInfo#VOLUME_CHANGED
	 * @since 2.0
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public void systemInfoChanged(IVolume volume, int changeType);
}
