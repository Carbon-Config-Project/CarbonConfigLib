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
public interface IWriteBuffer {
	void writeBoolean(boolean value);
	void writeByte(byte value);
	void writeShort(short value);
	void writeMedium(int value);
	void writeInt(int value);
	void writeVarInt(int value);
	void writeFloat(float value);
	void writeDouble(double value);
	void writeLong(long value);
	void writeChar(char value);
	void writeEnum(Enum<?> value);
	void writeString(String value);
	void writeBytes(byte[] value);
	void writeUUID(UUID value);
}
