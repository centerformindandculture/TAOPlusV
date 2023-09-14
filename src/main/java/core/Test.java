package core;

import com.google.auto.value.AutoValue;
import simudyne.core.rng.SeededRandom;

@AutoValue
public abstract class Test {
  public abstract long testedPersonID();
  public abstract int tStepReturn();
  public abstract boolean positive();

  public static Test create(long testedPersonID, int tStepReturn, boolean positive) {
    return new AutoValue_Test(testedPersonID, tStepReturn, positive);
  }

}
