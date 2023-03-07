package carbonconfiglib.api.buffer;

import java.util.UUID;

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
