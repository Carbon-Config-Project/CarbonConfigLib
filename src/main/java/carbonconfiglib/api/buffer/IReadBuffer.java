package carbonconfiglib.api.buffer;

import java.util.UUID;

/**
 * Copyright 2023 Speiger, Meduris
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public interface IReadBuffer {
	boolean readBoolean();
	byte readByte();
	short readShort();
	int readMedium();
	int readInt();
	int readVarInt();
	float readFloat();
	double readDouble();
	long readLong();
	char readChar();
	<T extends Enum<T>> T readEnum(Class<T> clz);
	byte[] readBytes();
	String readString();
	UUID readUUID();
}
