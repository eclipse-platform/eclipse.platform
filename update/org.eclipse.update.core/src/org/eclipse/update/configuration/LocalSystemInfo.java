package org.eclipse.update.configuration;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
 
import java.io.File;
import java.util.ArrayList;

import org.eclipse.update.internal.core.UpdateManagerPlugin;
import org.eclipse.update.internal.core.Volume;
 
/**
 * Utility class providing local file system information.
 * The class attempts to load a native library implementation
 * of its methods. If successful, the method calls are delegated
 * to the native implementation. Otherwise a default non-native
 * implementation is used.  * 
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 * @see ILocalSystemInfoListener
 * @since 2.0
 */
public class LocalSystemInfo {
	
	/**
	 * Indicates the amount of available free space is not known
	 * 
	 * @see LocalSystemInfo#getFreeSpace(File)
	 * @since 2.0
	 * <p>
	 * <b>Note:</b> This field is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public static final long SIZE_UNKNOWN = -1;
	
	/**
	 * Indicates the volume type is not known
	 * 
	 * @see LocalSystemInfo#getType(File)
	 * @since 2.0
	 * <p>
	 * <b>Note:</b> This field is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public static final int VOLUME_UNKNOWN = -1;
	
	/**
	 * Indicates the volume could not be determined from path
	 * 
	 * @see LocalSystemInfo#getType(File)
	 * @since 2.0
	 * <p>
	 * <b>Note:</b> This field is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public static final int VOLUME_INVALID_PATH = -2;
	
	/**
	 * Indicates the volume is removable (other than floppy disk)
	 * 
	 * @see LocalSystemInfo#getType(File)
	 * @since 2.0
	 * <p>
	 * <b>Note:</b> This field is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public static final int VOLUME_REMOVABLE = 1;
	
	/**
	 * Indicates the volume is fixed (hard drive)
	 * 
	 * @see LocalSystemInfo#getType(File)
	 * @since 2.0
	 * <p>
	 * <b>Note:</b> This field is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public static final int VOLUME_FIXED = 2;
	
	/**
	 * Indicates a remote (network) volume
	 * 
	 * @see LocalSystemInfo#getType(File)
	 * @since 2.0
	 * <p>
	 * <b>Note:</b> This field is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public static final int VOLUME_REMOTE = 3;
	
	/**
	 * Indicates a cdrom volume (compact disc)
	 * 
	 * @see LocalSystemInfo#getType(File)
	 * @since 2.0
	 * <p>
	 * <b>Note:</b> This field is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public static final int VOLUME_CDROM = 4;	
	
	/**
	 * Indicates a ramdisk volume (memory)
	 * 
	 * @see LocalSystemInfo#getType(File)
	 * @since 2.0
	 * <p>
	 * <b>Note:</b> This field is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public static final int VOLUME_RAMDISK = 5;	

	/**
	 * Indicates the volume is removable (floppy disk 5 1/4)
	 * 
	 * @see LocalSystemInfo#getType(File)
	 * @since 2.0
	 * <p>
	 * <b>Note:</b> This field is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public static final int VOLUME_FLOPPY_5 = 6;	
	
	/**
	 * Indicates the volume is removable (floppy disk 3 1/2)
	 * 
	 * @see LocalSystemInfo#getType(File)
	 * @since 2.0
	 * <p>
	 * <b>Note:</b> This field is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public static final int VOLUME_FLOPPY_3 = 7;
	
	/**
	 * Indicates a new volume has been added
	 * 
	 * @see LocalSystemInfo#addInfoListener(File)
	 * @see LocalSystemInfo#removeInfoListener(File)
	 * @since 2.0
	 * <p>
	 * <b>Note:</b> This field is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public static final int VOLUME_ADDED = 0;
			
	/**
	 * Indicates a volume has been removed
	 * 
	 * @see LocalSystemInfo#addInfoListener(File)
	 * @see LocalSystemInfo#removeInfoListener(File)
	 * @since 2.0
	 * <p>
	 * <b>Note:</b> This field is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public static final int VOLUME_REMOVED = 1;

	/**
	 * Indicates a volume has been changed
	 * 
	 * @see LocalSystemInfo#addInfoListener(File)
	 * @see LocalSystemInfo#removeInfoListener(File)
	 * @since 2.0
	 * <p>
	 * <b>Note:</b> This field is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public static final int VOLUME_CHANGED = 2;

	
	private static ArrayList listeners = new ArrayList();	
	private static boolean hasNatives = false;	
	static {
		try {
			System.loadLibrary("update"); //$NON-NLS-1$
			hasNatives = true;
		} catch (UnsatisfiedLinkError e) {
			UpdateManagerPlugin.warn("Unable to load native library 'update'."); //$NON-NLS-1$
			hasNatives = false;
		}
	}
	
	/**
	 * Determines available free space on a volume.
	 * Returns the amount of free space available to this
	 * user on the volume containing the specified path. The
	 * method takes into account any space quotas or other
	 * native mechanisms that may restrict space usage
	 * on a given volume.
	 * @param path file path. May contain path elements beyond
	 * the volume "root"
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
	public static long getFreeSpace(File path) {
		if (hasNatives) {
			try {
				long bytes = nativeGetFreeSpace(path);
				return (bytes!=0)?bytes/1024:0;
			} catch (UnsatisfiedLinkError e) {
			}
		}
		return SIZE_UNKNOWN;
	}


	/**
	 * Lists the file system volume.
	 * @return array of volume representing mount
	 * points, or <code>null</code> if none found
	 * @since 2.0
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public static IVolume[] getVolumes() {
		String[] mountPoints = listMountPoints();
		Volume[] vol = new Volume[0];
		if (mountPoints!=null){
			vol = new Volume[mountPoints.length];
			for (int i = 0; i < mountPoints.length; i++) {
				File root = new File(mountPoints[i]);
				String label = getLabel(root);
				int type = getType(root);
				long size = getFreeSpace(root);
				vol[i] = new Volume(root,label,type,size);
				vol[i].markReadOnly();
			}
		} else {
			UpdateManagerPlugin.warn("Unable to find mount points");
			// fallback
			File [] roots = File.listRoots();
			if (roots.length == 1) {
				// just one root - skip it
				File root = roots[0];
				roots = root.listFiles();
			}
			vol = new Volume[roots.length];			
			for (int i = 0; i < roots.length; i++) {
				vol[i] = new Volume(roots[i],null,LocalSystemInfo.VOLUME_UNKNOWN,LocalSystemInfo.SIZE_UNKNOWN);
				vol[i].markReadOnly();
			}			
		}
		return vol;
	}

	
	/**
	 * Add local system change listener.
	 * Allows a listener to be added for monitoring changes
	 * in the local system information. The listener is notified
	 * each time there are relevant volume changes
	 * detected. This specifically includes changes to the
	 * list of volumes as a result of removable drive/ media
	 * operations (eg. CD insertion, removal), and changes to volume 
	 * mount structure.
	 * @param listener change listener
	 * @since 2.0
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public static void addInfoListener(ILocalSystemInfoListener listener) {
		if (!listeners.contains(listener))
			listeners.add(listener);
	}
	
	/**
	 * Remove local system change listener
	 * @param listener change listener
	 * @since 2.0
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public static void removeInfoListener(ILocalSystemInfoListener listener) {
		listeners.remove(listener);
	}
		
	/**
	 * Notify listeners of change.
	 * 
	 * @param volume the volume representing the
	 * change file system structure. Any current paths beyond
	 * the specified "root" file of the volume are assumed to be invalidated.
	 * @param changeType type of the change that occured.
	 * @see LocalSystemInfo#VOLUME_ADDED
	 * @see LocalSystemInfo#VOLUME_REMOVED
	 * @see LocalSystemInfo#VOLUME_CHANGED
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public static void fireSystemInfoChanged(IVolume volume, int changeType) {
		for (int i=0; i< listeners.size(); i++) {
			((ILocalSystemInfoListener)listeners.get(i)).systemInfoChanged(volume,changeType);
		}
	}
		
	/*
	 * Determines volume label.
	 * Returns the label of the volume containing the specified
	 * path.
	 * @param path file path. May contain path elements beyond
	 * the volume "root"
	 * @return volume label (as string), or <code>null</code> if
	 * the label cannot be determined.
	 * @since 2.0
	 */
	private static String getLabel(File path) {
		if (hasNatives) {
			try {
				return nativeGetLabel(path);
			} catch (UnsatisfiedLinkError e) {
			}
		}
		return null;
	}
	
	/*
	 * Determines volume type.
	 * Returns the type of the volume containing the specified
	 * path.
	 * @param path file path. May contain path elements beyond
	 * the volume "root"
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
	 */
	private static int getType(File path) {
		if (hasNatives) {
			try {
				return nativeGetType(path);
			} catch (UnsatisfiedLinkError e) {
			}
		}
		return VOLUME_UNKNOWN;
	}
	
	/*
	 * Lists the file system mount points.
	 * @return array of absolute file paths representing mount
	 * points, or <code>null</code> if none found
	 * @since 2.0
	 */
	private static String[] listMountPoints() {
		if (hasNatives) {
			try {
				String[] mountPoints = nativeListMountPoints();
				return mountPoints;
			} catch (UnsatisfiedLinkError e) {
			}
		}
		return null;
	}
		
	/*
	 * Native implementations.
	 */
	private static native long nativeGetFreeSpace(File path);
	private static native String nativeGetLabel(File path);
	private static native int nativeGetType(File path);
	private static native String[] nativeListMountPoints();
}
