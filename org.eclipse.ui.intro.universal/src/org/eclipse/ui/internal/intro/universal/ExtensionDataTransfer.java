/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
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
package org.eclipse.ui.internal.intro.universal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TransferData;


public class ExtensionDataTransfer extends ByteArrayTransfer {

	private static final String MYTYPENAME = "ExtensionData"; //$NON-NLS-1$
	private static final int MYTYPEID = registerType(MYTYPENAME);
	private static ExtensionDataTransfer _instance = new ExtensionDataTransfer();

	public static ExtensionDataTransfer getInstance() {
		return _instance;
	}

	@Override
	protected String[] getTypeNames() {
		return new String[] { MYTYPENAME };
	}

	@Override
	protected int[] getTypeIds() {
		return new int[] { MYTYPEID };
	}

	@Override
	public void javaToNative(Object object, TransferData transferData) {
		if (!checkMyType(object) || !isSupportedType(transferData)) {
			DND.error(DND.ERROR_INVALID_DATA);
		}
		BaseData[] myTypes = (BaseData[]) object;
		try {
			// write data to a byte array and then ask super to convert to pMedium
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			DataOutputStream writeOut = new DataOutputStream(out);
			for (int i = 0, length = myTypes.length; i < length; i++) {
				BaseData bd = myTypes[i];
				boolean separator = bd instanceof SeparatorData;
				writeOut.writeBoolean(separator);
				byte[] buffer = bd.getId().getBytes();
				writeOut.writeInt(bd.getId().length());
				writeOut.write(buffer);
				if (bd instanceof ExtensionData) {
					ExtensionData ed = (ExtensionData)bd;
					writeOut.writeInt(ed.getName().length());
					buffer = ed.getName().getBytes();
					writeOut.write(buffer);
					writeOut.writeInt(ed.getImportance());
				}
			}
			byte[] buffer = out.toByteArray();
			writeOut.close();
			super.javaToNative(buffer, transferData);
		} catch (IOException e) {
		}
	}

	@Override
	public Object nativeToJava(TransferData transferData) {
		if (isSupportedType(transferData)) {
			byte[] buffer = (byte[]) super.nativeToJava(transferData);
			if (buffer == null)
				return null;

			BaseData[] myData = new BaseData[0];
			try {
				ByteArrayInputStream in = new ByteArrayInputStream(buffer);
				DataInputStream readIn = new DataInputStream(in);
				while (readIn.available() > 4) {
					boolean separator;
					int importance=0;
					String id;
					String name=null;
					separator = readIn.readBoolean();
					int size = readIn.readInt();
					byte[] buff = new byte[size];
					readIn.read(buff);
					id = new String(buff);
					if (!separator) {
						size = readIn.readInt();
						buff = new byte[size];
						readIn.read(buff);
						name = new String(buff);
						importance = readIn.readInt();
					}

					BaseData[] newMyData = new BaseData[myData.length + 1];
					System.arraycopy(myData, 0, newMyData, 0, myData.length);
					if (separator)
						newMyData[myData.length] = new SeparatorData(id);
					else
						newMyData[myData.length] = new ExtensionData(id, name, importance);
					myData = newMyData;
				}
				readIn.close();
			} catch (IOException ex) {
				return null;
			}
			return myData;
		}

		return null;
	}


	boolean checkMyType(Object object) {
		if (object == null || !(object instanceof BaseData[]) || ((BaseData[]) object).length == 0) {
			return false;
		}
		BaseData[] myTypes = (BaseData[]) object;
		for (int i = 0; i < myTypes.length; i++) {
			if (myTypes[i] == null || myTypes[i].getId() == null || myTypes[i] instanceof ExtensionData && ((ExtensionData)myTypes[i]).getName() == null)
				return false;
		}
		return true;
	}

	@Override
	protected boolean validate(Object object) {
		return checkMyType(object);
	}
}
