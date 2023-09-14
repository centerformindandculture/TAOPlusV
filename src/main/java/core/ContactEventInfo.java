package core;

import com.google.auto.value.AutoValue;

import java.util.Optional;

/**
 * Details an transmission and any relevant information.
 */
@AutoValue
public abstract class ContactEventInfo {
  public abstract long infected();

  public abstract Optional<Long> infectedBy();

  public abstract long placeId();

  public abstract boolean resultedInTransmission();

  public abstract int placeType();

  public abstract Optional<Person.PersonTransmissibilityInfo> infectedTransmiissibilityInfo();

  public abstract Optional<Person.PersonTransmissibilityInfo> infectedByTransmiissibilityInfo();

  public static ContactEventInfo create(long infected, Optional<Long> infectedBy, long placeId, boolean resultedInTransmission,
                                        int placeType,
                                        Person.PersonTransmissibilityInfo infectedTransmissibilityInfo,
                                        Person.PersonTransmissibilityInfo infectedByTransmisibilityInfo) {
    return new AutoValue_ContactEventInfo(infected, infectedBy, placeId, resultedInTransmission, placeType,
        Optional.ofNullable(infectedTransmissibilityInfo), Optional.ofNullable(infectedByTransmisibilityInfo));
  }


}
