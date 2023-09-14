package core;

import com.google.auto.value.AutoValue;

/**
 * Intended for a specific demographic of person, a uniform range of illness trajectory
 * distributions.
 */
@AutoValue
public abstract class InfectionTrajectoryDistribution {
  public abstract int infectiousRangeStart();

  public abstract int infectiousRangeEnd();

  public abstract int illnessDurationNonSevereRangeStart();

  public abstract int illnessDurationNonSevereRangeEnd();

  public abstract int illnessDurationSevereRangeStart();

  public abstract int illnessDurationSevereRangeEnd();

  public abstract int symptomsOnsetRangeStart();

  public abstract int symptomsOnsetRangeEnd();

  public abstract double percentageAsymptomaticCases();

  public abstract double percentageNonSevereSymptomaticCases();

  public abstract double percentageSevereCases();

  public static Builder dummyBuilder() {
    return new AutoValue_InfectionTrajectoryDistribution.Builder()
        .percentageAsymptomaticCases(0)
        .percentageNonSevereSymptomaticCases(0)
        .percentageSevereCases(0)
        .infectiousRangeStart(0)
        .infectiousRangeEnd(0)
        .illnessDurationNonSevereRangeStart(0)
        .illnessDurationNonSevereRangeEnd(0)
        .symptomsOnsetRangeStart(0)
        .symptomsOnsetRangeEnd(0)
        .illnessDurationSevereRangeStart(0)
        .illnessDurationSevereRangeEnd(0);
  }

  public static Builder builder() {
    return new AutoValue_InfectionTrajectoryDistribution.Builder()
        .percentageAsymptomaticCases(0.5)
        .percentageNonSevereSymptomaticCases(0.45)
        .percentageSevereCases(0.05)
        .infectiousRangeStart(2)
        .infectiousRangeEnd(3)
        .illnessDurationNonSevereRangeStart(7)
        .illnessDurationNonSevereRangeEnd(14)
        .symptomsOnsetRangeStart(3)
        .symptomsOnsetRangeEnd(5)
        .illnessDurationSevereRangeStart(14)
        .illnessDurationSevereRangeEnd(30);
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder infectiousRangeStart(int infectiousRangeStart);

    public abstract Builder infectiousRangeEnd(int infectiousRangeEnd);

    public abstract Builder illnessDurationNonSevereRangeStart(
        int illnessDurationNonSevereRangeStart);

    public abstract Builder illnessDurationNonSevereRangeEnd(int illnessDurationNonSevereRangeEnd);

    public abstract Builder illnessDurationSevereRangeStart(int illnessDurationSevereRangeStart);

    public abstract Builder illnessDurationSevereRangeEnd(int illnessDurationSevereRangeEnd);

    public abstract Builder symptomsOnsetRangeStart(int symptomsOnsetRangeStart);

    public abstract Builder symptomsOnsetRangeEnd(int symptomsOnsetRangeEnd);

    public abstract Builder percentageAsymptomaticCases(double percentageAsymptomaticCases);

    public abstract Builder percentageNonSevereSymptomaticCases(
        double percentageNonSevereSymptomaticCases);

    public abstract Builder percentageSevereCases(double percentageSevereCases);

    public abstract InfectionTrajectoryDistribution build();
  }
}
