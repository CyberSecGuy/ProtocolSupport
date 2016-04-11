package protocolsupport.protocol.transformer.utils.chunk.blockstorage;

import io.netty.buffer.ByteBuf;

public class NibbleBlockStorage extends BlockStorage {

	private final byte[] blocks;

	protected NibbleBlockStorage(int[] palette, int dataLength) {
		super(palette, -1);
		blocks = new byte[dataLength << 3];
	}

	@Override
	public void readFromStream(ByteBuf stream) {
		stream.readBytes(blocks);
	}

	@Override
	public int getPaletteIndex(int blockIndex) {
		int sIndex = blockIndex >> 1;
		int index = (((sIndex >> 3) << 3) | (7 - (sIndex & 7)));
		if ((blockIndex & 1) == 0) {
			return blocks[index] & 0xF;
		} else {
			return (blocks[index] & 0xFF) >> 4;
		}
	}

}