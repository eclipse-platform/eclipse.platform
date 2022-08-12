/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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
package org.eclipse.core.internal.localstore;

import java.io.InputStream;
import java.util.Set;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.internal.utils.UniversalUniqueIdentifier;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;

/**
 * Blob store which maps UUIDs to blobs on disk. The UUID is mapped
 * to a file in the file-system and the blob is the file contents. For scalability,
 * the blobs are split among 255 directories with the names 00 to FF.
 */
public class BlobStore {
	protected IFileStore localStore;

	/** Limits the range of directories' names. */
	protected byte mask;

	//private static short[] randomArray = {213, 231, 37, 85, 211, 29, 161, 175, 187, 3, 147, 246, 170, 30, 202, 183, 242, 47, 254, 189, 25, 248, 193, 2, 119, 133, 125, 12, 76, 213, 219, 79, 69, 133, 202, 80, 150, 190, 157, 190, 80, 190, 219, 150, 169, 117, 95, 10, 77, 214, 233, 70, 5, 188, 44, 91, 165, 149, 177, 93, 17, 112, 4, 41, 230, 148, 188, 107, 213, 31, 52, 60, 111, 246, 226, 121, 129, 197, 144, 248, 92, 133, 96, 116, 104, 67, 74, 144, 185, 141, 96, 34, 182, 90, 36, 217, 28, 205, 107, 52, 201, 14, 8, 1, 27, 216, 60, 35, 251, 194, 7, 156, 32, 5, 145, 29, 96, 61, 110, 145, 50, 56, 235, 239, 170, 138, 17, 211, 56, 98, 101, 126, 27, 57, 211, 144, 206, 207, 179, 111, 160, 50, 243, 69, 106, 118, 155, 159, 28, 57, 11, 175, 43, 173, 96, 181, 99, 169, 171, 156, 246, 243, 30, 198, 251, 81, 77, 92, 160, 235, 215, 187, 23, 71, 58, 247, 127, 56, 118, 132, 79, 188, 42, 188, 158, 121, 255, 65, 154, 118, 172, 217, 4, 47, 105, 204, 135, 27, 43, 90, 9, 31, 59, 115, 193, 28, 55, 101, 9, 117, 211, 112, 61, 55, 23, 235, 51, 104, 123, 138, 76, 148, 115, 119, 81, 54, 39, 46, 149, 191, 79, 16, 222, 69, 219, 136, 148, 181, 77, 250, 101, 223, 140, 194, 141, 44, 195, 217, 31, 223, 207, 149, 245, 115, 243, 183};
	private static byte[] randomArray = {-43, -25, 37, 85, -45, 29, -95, -81, -69, 3, -109, -10, -86, 30, -54, -73, -14, 47, -2, -67, 25, -8, -63, 2, 119, -123, 125, 12, 76, -43, -37, 79, 69, -123, -54, 80, -106, -66, -99, -66, 80, -66, -37, -106, -87, 117, 95, 10, 77, -42, -23, 70, 5, -68, 44, 91, -91, -107, -79, 93, 17, 112, 4, 41, -26, -108, -68, 107, -43, 31, 52, 60, 111, -10, -30, 121, -127, -59, -112, -8, 92, -123, 96, 116, 104, 67, 74, -112, -71, -115, 96, 34, -74, 90, 36, -39, 28, -51, 107, 52, -55, 14, 8, 1, 27, -40, 60, 35, -5, -62, 7, -100, 32, 5, -111, 29, 96, 61, 110, -111, 50, 56, -21, -17, -86, -118, 17, -45, 56, 98, 101, 126, 27, 57, -45, -112, -50, -49, -77, 111, -96, 50, -13, 69, 106, 118, -101, -97, 28, 57, 11, -81, 43, -83, 96, -75, 99, -87, -85, -100, -10, -13, 30,
			-58, -5, 81, 77, 92, -96, -21, -41, -69, 23, 71, 58, -9, 127, 56, 118, -124, 79, -68, 42, -68, -98, 121, -1, 65, -102, 118, -84, -39, 4, 47, 105, -52, -121, 27, 43, 90, 9, 31, 59, 115, -63, 28, 55, 101, 9, 117, -45, 112, 61, 55, 23, -21, 51, 104, 123, -118, 76, -108, 115, 119, 81, 54, 39, 46, -107, -65, 79, 16, -34, 69, -37, -120, -108, -75, 77, -6, 101, -33, -116, -62, -115, 44, -61, -39, 31, -33, -49, -107, -11, 115, -13, -73,};

	/**
	 * The limit is the maximum number of directories managed by this store.
	 * This number must be power of 2 and do not exceed 256. The location
	 * should be an existing valid directory.
	 */
	public BlobStore(IFileStore store, int limit) {
		Assert.isNotNull(store);
		localStore = store;
		Assert.isTrue(localStore.fetchInfo().isDirectory());
		Assert.isTrue(limit == 256 || limit == 128 || limit == 64 || limit == 32 || limit == 16 || limit == 8 || limit == 4 || limit == 2 || limit == 1);
		mask = (byte) (limit - 1);
	}

	public UniversalUniqueIdentifier addBlob(IFileStore target, boolean moveContents) throws CoreException {
		UniversalUniqueIdentifier uuid = new UniversalUniqueIdentifier();
		folderFor(uuid).mkdir(EFS.NONE, null);
		IFileStore destination = fileFor(uuid);
		if (moveContents)
			target.move(destination, EFS.NONE, null);
		else
			target.copy(destination, EFS.NONE, null);
		return uuid;
	}

	/**
	 * @see UniversalUniqueIdentifier#appendByteString(StringBuilder, byte)
	 */
	@SuppressWarnings("javadoc")
	private void appendByteString(StringBuilder buffer, byte value) {
		String hexString;
		if (value < 0)
			hexString = Integer.toHexString(256 + value);
		else
			hexString = Integer.toHexString(value);
		if (hexString.length() == 1)
			buffer.append("0"); //$NON-NLS-1$
		buffer.append(hexString);
	}

	/**
	 * Converts an array of bytes into a String.
	 *
	 * @see UniversalUniqueIdentifier#toString()
	 */
	private String bytesToHexString(byte[] b) {
		StringBuilder buffer = new StringBuilder();
		for (byte element : b)
			appendByteString(buffer, element);
		return buffer.toString();
	}

	/**
	 * Deletes a blobFile.
	 */
	public void deleteBlob(UniversalUniqueIdentifier uuid) {
		Assert.isNotNull(uuid);
		try {
			fileFor(uuid).delete(EFS.NONE, null);
		} catch (CoreException e) {
			//ignore
		}
	}

	/**
	 * Delete all of the blobs in the given set.
	 */
	public void deleteBlobs(Set<UniversalUniqueIdentifier> set) {
		for (UniversalUniqueIdentifier id : set)
			deleteBlob(id);
	}

	public IFileStore fileFor(UniversalUniqueIdentifier uuid) {
		IFileStore root = folderFor(uuid);
		return root.getChild(bytesToHexString(uuid.toBytes()));
	}

	/**
	 * Find out the name of the directory that fits better to this UUID.
	 */
	public IFileStore folderFor(UniversalUniqueIdentifier uuid) {
		byte hash = hashUUIDbytes(uuid);
		hash &= mask; // limit the range of the directory
		String dirName = Integer.toHexString(hash + (128 & mask)); // +(128 & mask) makes sure 00h is the lower value
		return localStore.getChild(dirName);
	}

	public InputStream getBlob(UniversalUniqueIdentifier uuid) throws CoreException {
		IFileStore blobFile = fileFor(uuid);
		return blobFile.openInputStream(EFS.NONE, null);
	}

	/**
	 * Converts a byte array into a byte hash representation. It is used to
	 * get a directory name.
	 */
	protected byte hashUUIDbytes(UniversalUniqueIdentifier uuid) {
		byte[] bytes = uuid.toBytes();
		byte hash = 0;
		for (byte b : bytes)
			hash ^= randomArray[b + 128]; // +128 makes sure the index is >0
		return hash;
	}
}
