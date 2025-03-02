package tauri.dev.jsg.packet.transportrings;

import io.netty.buffer.ByteBuf;
import tauri.dev.jsg.packet.PositionedPacket;
import tauri.dev.jsg.tileentity.transportrings.controller.TRControllerAbstractTile;
import tauri.dev.jsg.tileentity.transportrings.TransportRingsAbstractTile;
import tauri.dev.jsg.transportrings.SymbolTypeTransportRingsEnum;
import tauri.dev.jsg.transportrings.TransportResult;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.Objects;

public class TRControllerActivatedToServer extends PositionedPacket {
	public TRControllerActivatedToServer() {}
	
	public int symbol;
	public int symbolType;
	
	public TRControllerActivatedToServer(BlockPos pos, int angleIndex, SymbolTypeTransportRingsEnum symbolType) {
		super(pos);

		int i = angleIndex - 1;

		this.symbolType = symbolType.id;
		this.symbol = Objects.requireNonNull(SymbolTypeTransportRingsEnum.valueOf(this.symbolType).getSymbolByAngleIndex(i)).getId();
	}
	
	@Override
	public void toBytes(ByteBuf buf) {
		super.toBytes(buf);
		
		buf.writeInt(symbol);
		buf.writeInt(symbolType);
	}

	
	@Override
	public void fromBytes(ByteBuf buf) {
		super.fromBytes(buf);
		
		symbol = buf.readInt();
		symbolType = buf.readInt();
	}

	
	public static class TRControllerActivatedServerHandler implements IMessageHandler<TRControllerActivatedToServer, IMessage> {

		@Override
		public IMessage onMessage(TRControllerActivatedToServer message, MessageContext ctx) {
			EntityPlayerMP player = ctx.getServerHandler().player;
			WorldServer world = player.getServerWorld();
			
			world.addScheduledTask(() -> {
				TRControllerAbstractTile controllerTile = (TRControllerAbstractTile) world.getTileEntity(message.pos);
				if(controllerTile != null) {
					TransportRingsAbstractTile ringsTile = controllerTile.getLinkedRingsTile(world);

					if (ringsTile != null) {
						TransportResult result = ringsTile.addSymbolToAddressInternal(SymbolTypeTransportRingsEnum.valueOf(message.symbolType).getSymbol(message.symbol), false);
						result.sendMessageIfFailed(player);
					} else
						player.sendStatusMessage(new TextComponentTranslation("tile.jsg.transportrings_controller_block.not_linked"), true);
				}
			});
			
			return null;
		}
		
	}
}
