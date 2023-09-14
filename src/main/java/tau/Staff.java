package tau;

import simudyne.core.rng.SeededRandom;

public class Staff extends UniversityAffiliate {
  public boolean isStaffWithStudentFacingJob;

  @Override
  public void initialiseFirstPlace() {
    this.decideNextLocation();
  }

  @Override
  public PersonInitializationInfo initializationInfo() {
    SeededRandom random = getPrng();
    double suppressionPerc = 0.0;
    boolean suppressed = getPrng().uniform(0.0,1.0).sample() < suppressionPerc;
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
