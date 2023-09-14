package tau;

import com.google.common.collect.ImmutableList;
import core.PlaceInfo;
import simudyne.core.rng.SeededRandom;
import tau.anylogic_code.StudentType;

import java.util.Optional;

public class Student extends UniversityAffiliate {

  public StudentType type;
  public boolean isPartTime;
  public boolean livesOnCampus;
  public String livesAtBuilding = null;
  public double attendsSportsEventPerc;

  @Override
  public void initialiseFirstPlace() {
    this.decideNextLocation();
  }

  @Override
  public PersonInitializationInfo initializationInfo() {
    UniversityConfiguration universityConfiguration = getGlobals().getUniversityConfiguration();
    SeededRandom random = getPrng();
    return PersonInitializationInfo.builderSetWithGlobalDefaults(getGlobals(), random)
        .ageSupplier(
            PersonInitializationInfo.uniform(
                getGlobals().studentAgentAgeStart, getGlobals().studentAgentAgeEnd, random))
        .build();
  }
}
