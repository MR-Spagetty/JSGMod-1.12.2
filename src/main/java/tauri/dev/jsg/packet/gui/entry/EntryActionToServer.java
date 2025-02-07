package tauri.dev.jsg.packet.gui.entry;

import io.netty.buffer.ByteBuf;
import tauri.dev.jsg.item.JSGItems;
import tauri.dev.jsg.item.linkable.dialer.UniverseDialerItem;
import tauri.dev.jsg.item.linkable.dialer.UniverseDialerMode;
import tauri.dev.jsg.item.notebook.NotebookItem;
import tauri.dev.jsg.stargate.network.StargateAddress;
import tauri.dev.jsg.stargate.network.SymbolUniverseEnum;
import tauri.dev.jsg.tileentity.stargate.StargateUniverseBaseTile;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.nio.charset.StandardCharsets;

public class EntryActionToServer implements IMessage {
	public EntryActionToServer() {}
	
	private EnumHand hand;
	private EntryDataTypeEnum dataType;
	private EntryActionEnum action;
	private int index;
	private String name;
	
	public EntryActionToServer(EnumHand hand, EntryDataTypeEnum dataType, EntryActionEnum action, int index, String name) {
		this.hand = hand;
		this.dataType = dataType;
		this.action = action;
		this.index = index;
		this.name = name;
	}
	
	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(hand.ordinal());
		buf.writeInt(dataType.ordinal());
		buf.writeInt(action.ordinal());
		buf.writeInt(index);
		
		buf.writeInt(name.length());
		buf.writeCharSequence(name, StandardCharsets.UTF_8);
	}
	
	@Override
	public void fromBytes(ByteBuf buf) {
		hand = EnumHand.values()[buf.readInt()];
		dataType = EntryDataTypeEnum.values()[buf.readInt()];
		action = EntryActionEnum.values()[buf.readInt()];
		index = buf.readInt();
		
		int size = buf.readInt();
		name = buf.readCharSequence(size, StandardCharsets.UTF_8).toString();
	}
	
	
	public static class EntryActionServerHandler implements IMessageHandler<EntryActionToServer, IMessage> {

		@Override
		public IMessage onMessage(EntryActionToServer message, MessageContext ctx) {
			EntityPlayerMP player = ctx.getServerHandler().player;
			WorldServer world = player.getServerWorld();

			world.addScheduledTask(() -> {
				ItemStack stack = player.getHeldItem(message.hand);
				NBTTagCompound compound = stack.getTagCompound();
				
				if (message.dataType.page()) {
					NBTTagList list = compound.getTagList("addressList", NBT.TAG_COMPOUND);
					
					switch (message.action) {
						case RENAME:
							NotebookItem.setNameForIndex(list, message.index, message.name);
							break;
						
						case MOVE_UP:
							tagSwitchPlaces(list, message.index, message.index-1);
							break;
							
						case MOVE_DOWN:
							tagSwitchPlaces(list, message.index, message.index+1);
							break;
							
						case REMOVE:
							NBTTagCompound selectedCompound = list.getCompoundTagAt(message.index);
							list.removeTag(message.index);
							
							if (list.tagCount() == 0)
								player.setHeldItem(message.hand, ItemStack.EMPTY);
							else
								compound.setInteger("selected", Math.min(message.index, list.tagCount()-1));
							
							ItemStack pageStack = new ItemStack(JSGItems.PAGE_NOTEBOOK_ITEM, 1, 1);
							pageStack.setTagCompound(selectedCompound);
							player.addItemStackToInventory(pageStack);
							
							break;
					}
				}
				
				else if (message.dataType.universe()) {
					NBTTagList list = compound.getTagList(UniverseDialerMode.MEMORY.tagListName, NBT.TAG_COMPOUND);
					BlockPos linkedPos = BlockPos.fromLong(compound.getLong(UniverseDialerMode.MEMORY.tagPosName));
					NBTTagCompound selectedCompound = list.getCompoundTagAt(message.index);
					
					switch (message.action) {
						case RENAME:
							UniverseDialerItem.setMemoryNameForIndex(list, message.index, message.name);
							break;
							
						case MOVE_UP:
							tagSwitchPlaces(list, message.index, message.index-1);
							break;
							
						case MOVE_DOWN:
							tagSwitchPlaces(list, message.index, message.index+1);
							break;
							
						case REMOVE:
							list.removeTag(message.index);
							
							UniverseDialerMode mode = UniverseDialerMode.valueOf(compound.getByte("mode"));
							if (mode == UniverseDialerMode.MEMORY)
								compound.setByte("selected", (byte) Math.min(message.index, list.tagCount()-1));
							
							break;

						case DIAL:
							int maxSymbols = SymbolUniverseEnum.getMaxSymbolsDisplay(selectedCompound.getBoolean("hasUpgrade"));
							StargateUniverseBaseTile gateTile = (StargateUniverseBaseTile) world.getTileEntity(linkedPos);
							if(gateTile == null) break;

							if(gateTile.dialAddress(new StargateAddress(selectedCompound), maxSymbols))
								player.sendStatusMessage(new TextComponentTranslation("item.jsg.universe_dialer.dial_start"), true);

							break;
					}
				}
				
				else if (message.dataType.oc()) {
					NBTTagList list = compound.getTagList(UniverseDialerMode.OC.tagListName, NBT.TAG_COMPOUND);
					
					switch (message.action) {
						case RENAME:
							UniverseDialerItem.changeOCMessageAtIndex(list, message.index, (ocMessage) -> ocMessage.name = message.name);
							break;
						
						case MOVE_UP:
							tagSwitchPlaces(list, message.index, message.index-1);
							break;
							
						case MOVE_DOWN:
							tagSwitchPlaces(list, message.index, message.index+1);
							break;
							
						case REMOVE:
							list.removeTag(message.index);
							
							UniverseDialerMode mode = UniverseDialerMode.valueOf(compound.getByte("mode"));
							if (mode == UniverseDialerMode.OC)
								compound.setByte("selected", (byte) Math.min(message.index, list.tagCount()-1));
							
							break;
					}
				}
			});
			
			return null;
		}
		
	}
	
	private static void tagSwitchPlaces(NBTTagList list, int a, int b) {
		NBTBase tagA = list.get(a);
		list.set(a, list.get(b));
		list.set(b, tagA);
	}
}
