package tau;

import simudyne.core.rng.SeededRandom;

public class Faculty extends UniversityAffiliate {

  @Override
  public void initialiseFirstPlace() {
    this.decideNextLocation();
  }

  @Override
  public PersonInitializationInfo initializationInfo() {
    SeededRandom random = getPrng();
    return PersonInitializationInfo.builderSetWithGlobalDefaults(getGlobals(), random)
        .ageSupplier(
            PersonInitializationInfo.truncNormal(
                getGlobals().facultyStaffAgentAgeStart,
                getGlobals().facultyStaffAgentAgeEnd,
                getGlobals().facultyStaffAgentAgeMean,
                getGlobals().facultyStaffAgentAgeSD,
                random))
        .build();
  }
}
