/*******************************************************************************
 * Copyright (c) 2004, 2006 IBM Corporation and others.
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

package org.eclipse.team.internal.ccvs.ui;

import java.io.*;

import org.eclipse.swt.dnd.*;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteFile;
import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.resources.RemoteFile;
import org.eclipse.team.internal.ccvs.core.util.KnownRepositories;


/**
 * 
 * @author Eugene Kuleshov
 */
public final class CVSResourceTransfer extends ByteArrayTransfer {
	
	public static final String TYPE_NAME = "CVS-resource-transfer-format"; //$NON-NLS-1$

	public static int TYPE = registerType(TYPE_NAME);

	private static CVSResourceTransfer instance = new CVSResourceTransfer();

	
	private CVSResourceTransfer() {
	}

	public static CVSResourceTransfer getInstance() {
		return instance;
	}

	
	@Override
	protected int[] getTypeIds() {
		return new int[] { TYPE };
	}

	@Override
	protected String[] getTypeNames() {
		return new String[] { TYPE_NAME };
	}

	@Override
	public void javaToNative(Object object, TransferData transferData) {
		if (!isSupportedType(transferData)) {
			DND.error(DND.ERROR_INVALID_DATA);
		}

		final byte[] bytes = toByteArray((ICVSRemoteFile) object);
		if (bytes != null) {
			super.javaToNative(bytes, transferData);
		}
	}

	@Override
	protected Object nativeToJava(TransferData transferData) {
		byte[] bytes = (byte[]) super.nativeToJava(transferData);
		return fromByteArray(bytes);
	}

	
	public Object fromByteArray(byte[] bytes) {
		final DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));

		try {
			String location = in.readUTF();
			String filePath = in.readUTF();
			String fileRevision = in.readUTF();
			
			ICVSRepositoryLocation repositoryLocation = KnownRepositories.getInstance().getRepository(location);
			RemoteFile file = RemoteFile.create( filePath, repositoryLocation);
			file.setRevision(fileRevision);
			file.setReadOnly(true);
			return file;
		} catch (Exception ex) {
			return null;
		}
	}

	public byte[] toByteArray(ICVSRemoteFile file) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(bos);
		try {
			dos.writeUTF(file.getRepository().getLocation(false));
			dos.writeUTF(file.getRepositoryRelativePath());
			dos.writeUTF(file.getRevision());
			return bos.toByteArray();
		} catch (Exception ex) {
			// ex.printStackTrace();
			return null;
		}
	}

}
