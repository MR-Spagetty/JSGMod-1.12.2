package tauri.dev.jsg.stargate;

import io.netty.buffer.ByteBuf;
import tauri.dev.jsg.JSG;
import tauri.dev.jsg.stargate.network.SymbolInterface;
import tauri.dev.jsg.stargate.network.SymbolTypeEnum;
import tauri.dev.jsg.util.math.*;
import tauri.dev.jsg.util.math.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Client-side class helping with the ring's rotation.
 *
 * @author MrJake222
 */
public class StargateClassicSpinHelper implements ISpinHelper {
  public StargateClassicSpinHelper() {

  }

  public static float A_ANGLE_PER_TICK = 1.8f;
  public static final float U_SPEEDUP_TIME = 35;
  public static final float S_STOP_TIME = 25;

  public SymbolTypeEnum symbolType;

  public boolean isSpinning;
  public SymbolInterface currentSymbol;
  public EnumSpinDirection direction = EnumSpinDirection.CLOCKWISE;

  private long spinStartTime;
  private SymbolInterface targetSymbol;
  private float targetRotationOffset;

  public int plusRounds;

  public boolean getIsSpinning() {
    return isSpinning;
  }

  public void setIsSpinning(boolean value) {
    isSpinning = value;
  }

  public SymbolInterface getCurrentSymbol() {
    return currentSymbol;
  }

  public void setCurrentSymbol(SymbolInterface symbol) {
    currentSymbol = symbol;
  }

  public SymbolInterface getTargetSymbol() {
    return targetSymbol;
  }

  /**
   * First phase function (with default values).
   */
  private static final MathRangedFunction SPEEDUP_PHASE_DEFAULT = getSpeedupRangedFunction(A_ANGLE_PER_TICK, U_SPEEDUP_TIME);

  /**
   * First phase ranged function generation method.
   *
   * @param a Angle per tick
   * @param u Speedup time
   *
   * @return 1st phase function
   */
  private static MathRangedFunction getSpeedupRangedFunction(float a, float u) {
    return new MathRangedFunction(new MathRange(0, u), new MathFunctionQuadratic(a / (2 * u), 0, 0));
  }

  /**
   * Second phase's function (with default values).
   */
  private static final MathFunctionLinear LINEAR_SPIN_FUNCTION_DEFAULT = getLinearSpinFunction(A_ANGLE_PER_TICK, U_SPEEDUP_TIME);


  /**
   * Second phase function generation method.
   *
   * @param a Angle per tick
   * @param u Speedup time
   *
   * @return 2nd phase function
   */
  private static MathFunctionLinear getLinearSpinFunction(float a, float u) {
    return new MathFunctionLinear(a, -a * u / 2);
  }

  private static MathFunctionQuadratic getStopFunction(float a, float u, float s, float x0) {
    return new MathFunctionQuadratic(-a / (2 * s), a + (a * x0 / s), -(a * u / 2 + a * x0 * x0 / (2 * s)));
  }

  private static float getx0(float targetAngle) {
    return targetAngle / A_ANGLE_PER_TICK + (U_SPEEDUP_TIME - S_STOP_TIME) / 2;
  }

  private static float getTargetRotation(float x0) {
    return A_ANGLE_PER_TICK * x0 + A_ANGLE_PER_TICK * (S_STOP_TIME - U_SPEEDUP_TIME) / 2;
  }

  public static float getMinimalDistance() {
    return getTargetRotation(U_SPEEDUP_TIME);
  }

  public static int getAnimationDuration(float distance) {
    return (int) (getx0(distance) + S_STOP_TIME);
  }

  public static float getAnimationDistance(int duration) {
    float distance = (duration*A_ANGLE_PER_TICK)-(U_SPEEDUP_TIME*A_ANGLE_PER_TICK)-(S_STOP_TIME*A_ANGLE_PER_TICK);
    if(distance < 15) distance = 15;
    return distance;
  }

  /**
   * {@link Map} containing the phases.
   */
  private Map<MathRange, MathFunction> phases = new HashMap<MathRange, MathFunction>(3);

  public StargateClassicSpinHelper(SymbolTypeEnum symbolType, SymbolInterface currentSymbol, EnumSpinDirection spinDirection, boolean isSpinning, SymbolInterface targetRingSymbol, long spinStartTime, int plusRounds) {
    this.symbolType = symbolType;
    this.currentSymbol = currentSymbol;
    this.direction = spinDirection;
    this.isSpinning = isSpinning;
    this.targetSymbol = targetRingSymbol;
    this.spinStartTime = spinStartTime;
    this.plusRounds = plusRounds;
  }

  public void initRotation(long totalWorldTime, SymbolInterface targetSymbol, EnumSpinDirection direction, float startOffset, int plusRounds) {
    float distance = direction.getDistance(currentSymbol, targetSymbol);
    distance += (360*plusRounds);

    float x0 = getx0(distance);
    this.targetRotationOffset = getTargetRotation(x0);

    phases.clear();

    if (x0 < U_SPEEDUP_TIME) {
      // Stop point occurs before ring reaches full speed
      // Set x0 to arithmetic mean of 0 and x0+S_STOP_TIME (halfway between start and desired stop)
      // Set u,s to x0
      x0 = (x0 + S_STOP_TIME) / 2;

      float a = distance / x0;
      MathRangedFunction speedup = getSpeedupRangedFunction(a, x0);
      phases.put(speedup.range, speedup.function);
      phases.put(new MathRange(x0, x0 + x0), getStopFunction(a, x0, x0, x0)); // x0+s = x0+x0, u=s=x0
    } else {
      phases.put(SPEEDUP_PHASE_DEFAULT.range, SPEEDUP_PHASE_DEFAULT.function);

      phases.put(new MathRange(U_SPEEDUP_TIME, x0), LINEAR_SPIN_FUNCTION_DEFAULT);
      phases.put(new MathRange(x0, x0 + S_STOP_TIME), getStopFunction(A_ANGLE_PER_TICK, U_SPEEDUP_TIME, S_STOP_TIME, x0));
    }

    this.targetSymbol = targetSymbol;
    this.direction = direction;
    this.spinStartTime = totalWorldTime;

    isSpinning = true;
  }

  private float calculate(float tick) {
    if (tick < 0) {
      //JSG.logger.warn("Negative argument");
      return 0;
    }

    for (Map.Entry<MathRange, MathFunction> phase : phases.entrySet()) {
      if (phase.getKey().test(tick)) {
        return phase.getValue().apply(tick);
      }
    }

    isSpinning = false;
    currentSymbol = targetSymbol;

    return targetRotationOffset;
  }

  public float apply(double tick) {
    return calculate((float) (tick - spinStartTime)) * direction.mul;
  }

  public void toBytes(ByteBuf buf) {
    buf.writeInt(symbolType.id);

    buf.writeBoolean(isSpinning);
    buf.writeInt(currentSymbol.getId());
    buf.writeInt(direction.id);

    buf.writeLong(spinStartTime);
    buf.writeInt(targetSymbol.getId());

    buf.writeInt(plusRounds);
  }

  public void fromBytes(ByteBuf buf) {
    symbolType = SymbolTypeEnum.valueOf(buf.readInt());

    isSpinning = buf.readBoolean();
    currentSymbol = symbolType.valueOfSymbol(buf.readInt());
    direction = EnumSpinDirection.valueOf(buf.readInt());

    spinStartTime = buf.readLong();
    targetSymbol = symbolType.valueOfSymbol(buf.readInt());

    plusRounds = buf.readInt();

    if (isSpinning) {
      initRotation(spinStartTime, targetSymbol, direction, 0, plusRounds);
    }
  }
}
