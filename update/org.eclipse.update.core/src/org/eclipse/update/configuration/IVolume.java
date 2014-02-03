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
package org.eclipse.update.configuration;

import java.io.File;

import org.eclipse.core.runtime.IAdaptable;

/**
 * Local Volume Info.
 * Represents local file system information for a specific volume.
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 * @see org.eclipse.update.configuration.LocalSystemInfo
 * @since 2.0
 * @deprecated The org.eclipse.update component has been replaced by Equinox p2.
 * This API will be deleted in a future release. See bug 311590 for details.
 */
public interface IVolume extends IAdaptable {

	/**
	 * Returns the available free space on this volume.
	 * Returns the amount of free space available to this
	 * user on the volume. The
	 * method takes into account any space quotas or other
	 * native mechanisms that may restrict space usage
	 * on a given volume.
	 * @return the amount of free space available (in units
	 * of Kbyte), or an indication the size is not known 
	 * @see LocalSystemInfo#SIZE_UNKNOWN
	 * @since 2.0
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public long getFreeSpace();
	
	/**
	 * returns volume label.
	 * Returns the label of the volume.
	 * @return volume label (as string), or <code>null</code> if
	 * the label cannot be determined.
	 * @since 2.0
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public String getLabel();
	
	/**
	 * Returns volume type.
	 * Returns the type of the volume.
	 * @return volume type
	 * @see LocalSystemInfo#VOLUME_UNKNOWN
	 * @see LocalSystemInfo#VOLUME_INVALID_PATH
	 * @see LocalSystemInfo#VOLUME_REMOVABLE
	 * @see LocalSystemInfo#VOLUME_FIXED
	 * @see LocalSystemInfo#VOLUME_REMOTE
	 * @see LocalSystemInfo#VOLUME_CDROM
	 * @see LocalSystemInfo#VOLUME_FLOPPY_3
	 * @see LocalSystemInfo#VOLUME_FLOPPY_5
	 * @since 2.0
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public int getType();
	
	/**
	 * Returns the volume path.
	 * Returns the path that represents the mount point of the volume.
	 * @return mount point file
	 * @since 2.0
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public File getFile();
}
