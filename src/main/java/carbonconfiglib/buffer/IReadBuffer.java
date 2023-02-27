package carbonconfiglib.buffer;

import java.util.UUID;

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
