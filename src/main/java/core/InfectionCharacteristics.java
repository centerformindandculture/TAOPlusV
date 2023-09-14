package core;

import com.google.auto.value.AutoValue;

/**
 * The time of infectiousness, illness duration, and symptoms onset, offset at zero.
 */
@AutoValue
public abstract class InfectionCharacteristics {
  public abstract int tInfectious();

  public abstract int illnessDuration();

  public abstract int symptomsOnset();

  public abstract boolean isAsymptomatic();

  public static InfectionCharacteristics create(
      int tInfectious, int illnessDuration, int symptomsOnset, boolean isAsymptomatic) {
    return new AutoValue_InfectionCharacteristics(tInfectious, illnessDuration, symptomsOnset, isAsymptomatic);
  }
}
