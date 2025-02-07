package tauri.dev.jsg.stargate;

import tauri.dev.jsg.tileentity.transportrings.TransportRingsAbstractTile;

import java.util.HashMap;
import java.util.Map;

public enum EnumScheduledTask {
  STARGATE_OPEN_SOUND(0, 1, false),
  STARGATE_ENGAGE(1, 40),
  STARGATE_CLOSE(2, -1),
  STARGATE_SPIN_FINISHED(3, -1),
  STARGATE_CHEVRON_OPEN(4, 19, false),
  STARGATE_CHEVRON_OPEN_SECOND(5, 38, false),
  STARGATE_CHEVRON_CLOSE(6, 15, false),

  HORIZON_FLASH(7, -1, false),

  STARGATE_ORLIN_OPEN(8, 144 + 25), // 8.93s(duration of dial sound) * 20(tps) + 25(fix bug with sound sync)
  STARGATE_ORLIN_SPARK(9, 27, false),
  STARGATE_ORLIN_BROKE_SOUND(32, -1),

  STARGATE_HORIZON_LIGHT_BLOCK(10, -1),
  STARGATE_HORIZON_WIDEN(25, -1, false),
  STARGATE_HORIZON_SHRINK(12, -1, false),

  RINGS_START_ANIMATION(13, 20),
  RINGS_FADE_OUT(14, TransportRingsAbstractTile.TIMEOUT_FADE_OUT),
  RINGS_TELEPORT(15, TransportRingsAbstractTile.TIMEOUT_TELEPORT),
  RINGS_CLEAR_OUT(15, TransportRingsAbstractTile.RINGS_CLEAR_OUT),
  RINGS_SOLID_BLOCKS(16, 20),
  RINGS_SYMBOL_DEACTIVATE(35, 7),

  STARGATE_ACTIVATE_CHEVRON(17, 10),
  STARGATE_CLEAR_DHD_SYMBOLS(17, -1),
  STARGATE_CHEVRON_LIGHT_UP(18, -1),
  STARGATE_CHEVRON_DIM(19, -1),
  STARGATE_CHEVRON_FAIL(20, -1),
  STARGATE_LIGHTING_UPDATE_CLIENT(22, 5),
  STARGATE_FAILED_SOUND(23, -1),
  STARGATE_FAIL(24, -1),
  STARGATE_GIVE_PAGE(25, -1),
  STARGATE_DIAL_NEXT(26, -1),
  BEAMER_TOGGLE_SOUND(27, -1),
  STARGATE_DIAL_FINISHED(28, -1),
  STARGATE_CLEAR_CHEVRONS(29, 10),

  GATE_RING_ROLL(30, -1),
  LIGHT_UP_CHEVRONS(31, -1),

  STARGATE_FAST_DIAL_SPIN_FINISHED(32, -1),
  STARGATE_RESET(33, 20), // used only for uni gates
  BEGIN_SPIN(34, -1);

    public final int id;
  public final int waitTicks;

  /**
   * Should the task be called on nearest occasion
   * even when the scheduled wait time exceeded?
   */
  public final boolean overtime;

  EnumScheduledTask(int id, int waitTicks) {
    this(id, waitTicks, true);
  }

  EnumScheduledTask(int id, int waitTicks, boolean overtime) {
    this.id = id;
    this.waitTicks = waitTicks;
    this.overtime = overtime;
  }

  @Override
  public String toString() {
    return this.name() + "[time=" + this.waitTicks + "]";
  }

  private static Map<Integer, EnumScheduledTask> idMap = new HashMap<>();

  static {
    for (EnumScheduledTask task : EnumScheduledTask.values())
      idMap.put(task.id, task);
  }

  public static EnumScheduledTask valueOf(int id) {
    return idMap.get(id);
  }
}
