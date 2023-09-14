package core;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import simudyne.core.abm.testkit.TestKit;
import simudyne.core.rng.SeededRandom;

import java.util.Set;

import static com.google.common.truth.Truth.assertThat;

public class DefaultModulesImplTest {

  private TestKit<Globals> testKit;
  private final DefaultModulesImpl defaultModules = DefaultModulesImpl.getInstance();

  @Before
  public void setUp() throws Exception {
    testKit = TestKit.create(Globals.class);
  }

  @Test
  public void testGetPeopleToTest_firstPerson() {
    testKit.getGlobals().testsPerDay = 1;
    testKit.getGlobals().tOneDay = 1;

    Set<Long> agentsToTest =
        defaultModules.getAgentsToTest(
            ImmutableSet.of(),
            ImmutableMap.of(1L, 1.0, 2L, 0.0, 3L, 0.0),
            SeededRandom.create(1),
            testKit.getGlobals());

    assertThat(agentsToTest).containsExactly(1L);
  }

  @Test
  public void testGetPeopleToTest_middlePerson() {
    testKit.getGlobals().testsPerDay = 1;
    testKit.getGlobals().tOneDay = 1;

    Set<Long> agentsToTest =
        defaultModules.getAgentsToTest(
            ImmutableSet.of(),
            ImmutableMap.of(1L, 0.0, 2L, 2.0, 3L, 0.0),
            SeededRandom.create(1),
            testKit.getGlobals());

    assertThat(agentsToTest).containsExactly(2L);
  }

  @Test
  public void testGetPeopleToTest_lastPerson() {
    testKit.getGlobals().testsPerDay = 1;
    testKit.getGlobals().tOneDay = 1;

    Set<Long> agentsToTest =
        defaultModules.getAgentsToTest(
            ImmutableSet.of(),
            ImmutableMap.of(1L, 0.0, 2L, 0.0, 3L, 1.0),
            SeededRandom.create(1),
            testKit.getGlobals());

    assertThat(agentsToTest).containsExactly(3L);
  }

  @Test
  public void testGetPeopleToTest_multiple() {
    testKit.getGlobals().testsPerDay = 2;
    testKit.getGlobals().tOneDay = 1;

    Set<Long> agentsToTest =
        defaultModules.getAgentsToTest(
            ImmutableSet.of(),
            ImmutableMap.of(1L, 0.0, 2L, 0.0, 3L, 1.0, 4L, 0.00001),
            SeededRandom.create(1),
            testKit.getGlobals());

    assertThat(agentsToTest).containsExactly(4L, 3L);
  }

  @Test
  public void testGetInTransmissionLikelihood() {
    Person.PersonTransmissibilityInfo noMaskNoVaccine =
            Person.PersonTransmissibilityInfo.create(
                    Person.InfectionStatus.SUSCEPTIBLE,
                    false,
                    false,
                    Person.MaskType.NONE,
                    0,
                    0,
                    0,
                    0
            ),
    maskNoVaccine =
            Person.PersonTransmissibilityInfo.create(
                    Person.InfectionStatus.SUSCEPTIBLE,
                    false,
                    false,
                    Person.MaskType.N95,
                    0,
                    0,
                    0,
                    0
            ),
    noMaskVaccine =
            Person.PersonTransmissibilityInfo.create(
                    Person.InfectionStatus.SUSCEPTIBLE,
                    false,
                    false,
                    Person.MaskType.NONE,
                    0,
                    0,
                    0,
                    0
            );

    SeededRandom r = SeededRandom.create(0);

    double noMaskNoVaccineLikelihood = DefaultModulesImpl.getInTransmissionLikelihood(noMaskNoVaccine, r);
    double maskNoVaccineLikelihood = DefaultModulesImpl.getInTransmissionLikelihood(maskNoVaccine, r);
    double noMaskVaccineLikelihood = DefaultModulesImpl.getInTransmissionLikelihood(noMaskVaccine, r);

    assertThat(noMaskNoVaccineLikelihood).isEqualTo(1.0);
    assertThat(maskNoVaccineLikelihood).isGreaterThan(0.0);
    assertThat(maskNoVaccineLikelihood).isLessThan(1.0);
    assertThat(noMaskVaccineLikelihood).isEqualTo(0.0);
  }

  @Test
  public void testGetOutTransmissionLikelihood() {
    Person.PersonTransmissibilityInfo noMaskNoVaccine =
            Person.PersonTransmissibilityInfo.create(
                    Person.InfectionStatus.SUSCEPTIBLE,
                    false,
                    false,
                    Person.MaskType.NONE,
                    0,
                    0,
                    0,
                    0
            ),
            maskNoVaccine =
                    Person.PersonTransmissibilityInfo.create(
                            Person.InfectionStatus.SUSCEPTIBLE,
                            false,
                            false,
                            Person.MaskType.N95,
                            0,
                            0,
                            0,
                            0
                    ),
            noMaskVaccine =
                    Person.PersonTransmissibilityInfo.create(
                            Person.InfectionStatus.SUSCEPTIBLE,
                            false,
                            false,
                            Person.MaskType.NONE,
                            0,
                            0,
                            0,
                            0
                    );

    SeededRandom r = SeededRandom.create(0);
    double baseInfectivity = 1;

    double noMaskNoVaccineLikelihood = DefaultModulesImpl.getOutTransmissionLikelihood(baseInfectivity, noMaskNoVaccine, r);
    double maskNoVaccineLikelihood = DefaultModulesImpl.getOutTransmissionLikelihood(baseInfectivity, maskNoVaccine, r);
    double noMaskVaccineLikelihood = DefaultModulesImpl.getOutTransmissionLikelihood(baseInfectivity, noMaskVaccine, r);

    assertThat(noMaskNoVaccineLikelihood).isEqualTo(1.0);
    assertThat(maskNoVaccineLikelihood).isGreaterThan(0.0);
    assertThat(maskNoVaccineLikelihood).isLessThan(1.0);
    assertThat(noMaskVaccineLikelihood).isEqualTo(0.55);
  }
}
