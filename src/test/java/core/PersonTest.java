package core;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import simudyne.core.abm.testkit.TestKit;
import simudyne.core.abm.testkit.TestResult;

import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static core.Person.InfectionStatus.INFECTED;
import static core.Person.InfectionStatus.RECOVERED;

public class PersonTest {

  private TestPerson testPerson;
  private TestKit<Globals> testKit;
  private CentralAgent centralAgent;
  private PlaceAgent placeAgent;

  @Before
  public void setUp() throws Exception {
    testKit = TestKit.create(Globals.class);
    testPerson = testKit.addAgent(TestPerson.class);
    centralAgent = testKit.addAgent(CentralAgent.class);
    placeAgent = testKit.addAgent(PlaceAgent.class);
    testKit.getGlobals().centralAgentID = centralAgent.getID();
    testKit.createLongAccumulator("totSusceptible", 0);
    testKit.createLongAccumulator("totInfected", 0);
    testKit.createLongAccumulator("totQuarantineInfected", 0);
    testKit.createLongAccumulator("totQuarantineSusceptible", 0);
    testKit.createLongAccumulator("totDead", 0);
    testKit.createLongAccumulator("totRecovered", 0);
    testKit.createLongAccumulator("totDetectedCases", 0);
    testKit.createDoubleAccumulator("testPositivity", 0);
    testKit.createLongAccumulator("numInfectionsThisStep", 0);
  }

  @Test
  public void testInit_ValuesProperlySetFromProvidedInitializationInfo() {
    testKit.getGlobals().mandateMask = true;
    testPerson.setInitializationBuilder(
        Person.PersonInitializationInfo.builderSetWithGlobalDefaults(
            testKit.getGlobals(), testPerson.getPrng())
            .ageSupplier(() -> 1.0)
            .maskComplianceSupplier(() -> 0.2)
            .initialInfectionStatusSupplier(() -> RECOVERED)
            .contactRateSupplier(() -> 7)
            .maskTypeSupplier(() -> Person.MaskType.N95));

    testPerson.init();

    assertThat(testPerson.age).isEqualTo(1.0);
    assertThat(testPerson.complianceMask).isEqualTo(0.2);
    assertThat(testPerson.status).isEqualTo(RECOVERED);
    assertThat(testPerson.contactRate).isEqualTo(7);
    assertThat(testPerson.maskType).isEqualTo(Person.MaskType.N95);
  }

  @Test
  public void testInit_NoMaskMandateMakesMaskComplianceZero() {
    testKit.getGlobals().mandateMask = false;
    testPerson.setInitializationBuilder(
        Person.PersonInitializationInfo.builderSetWithGlobalDefaults(
            testKit.getGlobals(), testPerson.getPrng())
            .ageSupplier(() -> 1.0)
            .maskComplianceSupplier(() -> 0.2)
            .initialInfectionStatusSupplier(() -> RECOVERED)
            .maskTypeSupplier(() -> Person.MaskType.N95));

    testPerson.init();

    assertThat(testPerson.complianceMask).isEqualTo(0.0);
  }

  @Test
  public void testInit_ValuesProperlySetFromGlobals() {
    // Arrange: set up the data to pass to the test.
    testKit.getGlobals().mandateMask = true;

    // Set global inputs to non-overlapping ranges
    testKit.getGlobals().defaultAgentAgeStart = 0.0;
    testKit.getGlobals().defaultAgentAgeEnd = 0.0001;

    testKit.getGlobals().defaultAgentMaskComplianceStart = 0.0001;
    testKit.getGlobals().defaultAgentMaskComplianceEnd = 0.0002;

    testKit.getGlobals().defaultAgentCompliancePhysicalDistancingStart = 0.0007;
    testKit.getGlobals().defaultAgentCompliancePhysicalDistancingtEnd = 0.0008;

    testKit.getGlobals().percInitiallyInfected = 0;
    testKit.getGlobals().percInitiallyRecovered = 0;

    testKit.getGlobals().percN95Masks = 1.0;
    testKit.getGlobals().percSurgicalMasks = 0.0;
    testKit.getGlobals().percHomemadeClothMasks = 0.0;

    // Act
    Person p = testKit.addAgent(Person.class, Person::init);

    // Assert
    assertThat(p.age).isAtLeast(0.0);
    assertThat(p.age).isAtMost(0.0001);

    assertThat(p.complianceMask).isAtLeast(0.0001);
    assertThat(p.complianceMask).isAtMost(0.0002);

    assertThat(p.compliancePhysicalDistancing).isAtLeast(0.0007);
    assertThat(p.compliancePhysicalDistancing).isAtMost(0.0008);

    assertThat(p.status).isEqualTo(Person.InfectionStatus.SUSCEPTIBLE);

    assertThat(p.maskType).isEqualTo(Person.MaskType.N95);
  }

  /**
   * This test is flaky. It is stochastic in nature, so it technically should fail a small
   * percentage of the time. If it fails occasionally, run it again. If it fails consistently, this
   * might be a problem.
   */
  @Test
  public void testInit_initialRatiosForSIRStatusOfAgents() {
    testKit.getGlobals().percInitiallyInfected = 0.05;
    testKit.getGlobals().percInitiallyRecovered = 0.10;

    int numSusceptible = 0;
    int numInfected = 0;
    int numRecovered = 0;
    for (int i = 0; i < 1000; i++) {
      Person p = testKit.addAgent(Person.class, Person::init);
      if (p.status == Person.InfectionStatus.SUSCEPTIBLE) {
        numSusceptible++;
      } else if (p.status == Person.InfectionStatus.INFECTED) {
        numInfected++;
      } else if (p.status == RECOVERED) {
        numRecovered++;
      }
    }
    assertThat(numSusceptible + numInfected + numRecovered).isEqualTo(1000);
    assertThat(numSusceptible / 1000.0).isWithin(0.1).of(0.85);
    assertThat(numInfected / 1000.0).isWithin(0.1).of(0.05);
    assertThat(numRecovered / 1000.0).isWithin(0.1).of(0.10);
  }

  @Test
  public void testInfectionSeverityCalculation_asymptomatic() {
    testKit.getGlobals().tOneDay = 2;
    testPerson.setInfectionTrajectoryDistribution(
        InfectionTrajectoryDistribution.dummyBuilder()
            .percentageAsymptomaticCases(1)
            .percentageNonSevereSymptomaticCases(0)
            .percentageSevereCases(0)
            .infectiousRangeStart(2)
            .infectiousRangeEnd(2)
            .illnessDurationNonSevereRangeStart(7)
            .illnessDurationNonSevereRangeEnd(7)
            .symptomsOnsetRangeStart(5)
            .symptomsOnsetRangeEnd(5)
            .illnessDurationSevereRangeStart(14)
            .illnessDurationSevereRangeEnd(14)
            .build());

    // Include 1 as timeInfected to test that add
    InfectionCharacteristics characteristics = testPerson.infectionSeverity(1);

    assertThat(characteristics.isAsymptomatic()).isTrue();
    assertThat(characteristics.tInfectious()).isEqualTo(4 + 1);
    assertThat(characteristics.illnessDuration()).isEqualTo(14 + 1);
  }

  @Test
  public void testInfectionSeverityCalculation_symptomaticNonSevere() {
    testKit.getGlobals().tOneDay = 2;
    testPerson.setInfectionTrajectoryDistribution(
        InfectionTrajectoryDistribution.dummyBuilder()
            .percentageAsymptomaticCases(0)
            .percentageNonSevereSymptomaticCases(1)
            .percentageSevereCases(0)
            .infectiousRangeStart(2)
            .infectiousRangeEnd(2)
            .illnessDurationNonSevereRangeStart(7)
            .illnessDurationNonSevereRangeEnd(7)
            .symptomsOnsetRangeStart(5)
            .symptomsOnsetRangeEnd(5)
            .illnessDurationSevereRangeStart(14)
            .illnessDurationSevereRangeEnd(14)
            .build());

    InfectionCharacteristics characteristics = testPerson.infectionSeverity(1);

    assertThat(characteristics.symptomsOnset()).isEqualTo(10 + 1);
    assertThat(characteristics.tInfectious()).isEqualTo(4 + 1);
    assertThat(characteristics.illnessDuration()).isEqualTo(14 + 1);
  }

  @Test
  public void testInfectionSeverityCalculation_severe() {
    testKit.getGlobals().tOneDay = 2;
    testPerson.setInfectionTrajectoryDistribution(
        InfectionTrajectoryDistribution.dummyBuilder()
            .percentageAsymptomaticCases(0)
            .percentageNonSevereSymptomaticCases(0)
            .percentageSevereCases(1)
            .infectiousRangeStart(2)
            .infectiousRangeEnd(2)
            .illnessDurationNonSevereRangeStart(7)
            .illnessDurationNonSevereRangeEnd(7)
            .symptomsOnsetRangeStart(5)
            .symptomsOnsetRangeEnd(5)
            .illnessDurationSevereRangeStart(14)
            .illnessDurationSevereRangeEnd(14)
            .build());

    InfectionCharacteristics characteristics = testPerson.infectionSeverity(1);

    assertThat(characteristics.symptomsOnset()).isEqualTo(10 + 1);
    assertThat(characteristics.tInfectious()).isEqualTo(4 + 1);
    assertThat(characteristics.illnessDuration()).isEqualTo(28 + 1);
  }

  // TODO Look into death logic and make sure this test is correct
  //  @Test
  //  public void testCheckDeath() {
  //    testKit.getGlobals().tStep = 0;
  //    testPerson.setInfectionTrajectoryDistribution(
  //        InfectionTrajectoryDistribution.dummyBuilder()
  //            .percentageAsymptomaticCases(0)
  //            .percentageNonSevereSymptomaticCases(0)
  //            .percentageSevereCases(1)
  //            .infectiousRangeStart(2)
  //            .infectiousRangeEnd(2)
  //            .illnessDurationNonSevereRangeStart(7)
  //            .illnessDurationNonSevereRangeEnd(7)
  //            .symptomsOnsetRangeStart(5)
  //            .symptomsOnsetRangeEnd(5)
  //            .illnessDurationSevereRangeStart(14)
  //            .illnessDurationSevereRangeEnd(14)
  //            .build());
  //
  //    for (int i = 0; i < testKit.getGlobals().pAgeDeath.length; i++) {
  //      testKit.getGlobals().pAgeDeath = new double[testKit.getGlobals().pAgeDeath.length];
  //      testKit.getGlobals().pAgeDeath[i] = 0.5;
  //      testPerson.age = (i + 1) * 10 - 1;
  //      testPerson.setInfected();
  //      int numDead = 0;
  //      for (int j = 0; j < 1000; j++) {
  //        if (testPerson.checkDeath()) {
  //          numDead++;
  //        }
  //      }
  //      assertThat(Math.abs(numDead - 500)).isLessThan(50);
  //
  //      if (testPerson.age < 80) {
  //        testPerson.age++;
  //        testPerson.setInfected();
  //        numDead = 0;
  //        for (int j = 0; j < 1000; j++) {
  //          if (testPerson.checkDeath()) {
  //            numDead++;
  //          }
  //        }
  //        assertThat(numDead).isEqualTo(0);
  //      }
  //    }
  //  }


  @Test
  public void testGetsInfectedFromCovid() {
    testKit.getGlobals().tStep = 5;
    testPerson.setInfectionTrajectoryDistribution(
        InfectionTrajectoryDistribution.dummyBuilder()
            .percentageAsymptomaticCases(0)
            .percentageNonSevereSymptomaticCases(1)
            .percentageSevereCases(0)
            .infectiousRangeStart(2)
            .infectiousRangeEnd(2)
            .illnessDurationNonSevereRangeStart(7)
            .illnessDurationNonSevereRangeEnd(7)
            .symptomsOnsetRangeStart(5)
            .symptomsOnsetRangeEnd(5)
            .illnessDurationSevereRangeStart(14)
            .illnessDurationSevereRangeEnd(14)
            .build());

    testKit.send(Messages.InfectionMsg.class).to(testPerson);
    testKit.testAction(testPerson, Person.infectedByCOVID);

    assertThat(testPerson.isSymptomatic()).isFalse();
    assertThat(testPerson.status).isEqualTo(Person.InfectionStatus.INFECTED);
    assertThat(testPerson.timeInfected).isEqualTo(5);
    assertThat(testPerson.tInfectious).isEqualTo(5 + 2);
    assertThat(testPerson.illnessDuration).isEqualTo(5 + 7);
    assertThat(testPerson.symptomOnset).isEqualTo(5 + 5);
  }

  @Test
  public void testCovidIllnessTrajectory() {
    testKit.getGlobals().tStep = 5;
    testPerson.setInfectionTrajectoryDistribution(
        InfectionTrajectoryDistribution.dummyBuilder()
            .percentageAsymptomaticCases(0)
            .percentageNonSevereSymptomaticCases(1)
            .percentageSevereCases(0)
            .infectiousRangeStart(2)
            .infectiousRangeEnd(2)
            .illnessDurationNonSevereRangeStart(7)
            .illnessDurationNonSevereRangeEnd(7)
            .symptomsOnsetRangeStart(5)
            .symptomsOnsetRangeEnd(5)
            .illnessDurationSevereRangeStart(14)
            .illnessDurationSevereRangeEnd(14)
            .build());

    testKit.send(Messages.InfectionMsg.class).to(testPerson);
    testKit.testAction(testPerson, Person.infectedByCOVID);
    assertThat(testPerson.status).isEqualTo(Person.InfectionStatus.INFECTED);
    assertThat(testPerson.isInfectious()).isFalse();
    assertThat(testPerson.isSymptomatic()).isFalse();
    assertThat(testPerson.isFirstTimeSymptomatic()).isFalse();

    testKit.getGlobals().tStep = 5 + 1;

    testKit.send(Messages.InfectionMsg.class).to(testPerson);
    testKit.testAction(testPerson, Person.infectedByCOVID);
    assertThat(testPerson.status).isEqualTo(Person.InfectionStatus.INFECTED);
    assertThat(testPerson.isInfectious()).isFalse();
    assertThat(testPerson.isSymptomatic()).isFalse();
    assertThat(testPerson.isFirstTimeSymptomatic()).isFalse();

    testKit.getGlobals().tStep = 5 + 2;

    testKit.send(Messages.InfectionMsg.class).to(testPerson);
    testKit.testAction(testPerson, Person.infectedByCOVID);
    assertThat(testPerson.status).isEqualTo(Person.InfectionStatus.INFECTED);
    assertThat(testPerson.isInfectious()).isTrue();
    assertThat(testPerson.isSymptomatic()).isFalse();
    assertThat(testPerson.isFirstTimeSymptomatic()).isFalse();

    testKit.getGlobals().tStep = 5 + 3;

    testKit.send(Messages.InfectionMsg.class).to(testPerson);
    testKit.testAction(testPerson, Person.infectedByCOVID);
    assertThat(testPerson.status).isEqualTo(Person.InfectionStatus.INFECTED);
    assertThat(testPerson.isInfectious()).isTrue();
    assertThat(testPerson.isSymptomatic()).isFalse();
    assertThat(testPerson.isFirstTimeSymptomatic()).isFalse();

    testKit.getGlobals().tStep = 5 + 4;

    testKit.send(Messages.InfectionMsg.class).to(testPerson);
    testKit.testAction(testPerson, Person.infectedByCOVID);
    assertThat(testPerson.status).isEqualTo(Person.InfectionStatus.INFECTED);
    assertThat(testPerson.isInfectious()).isTrue();
    assertThat(testPerson.isSymptomatic()).isFalse();
    assertThat(testPerson.isFirstTimeSymptomatic()).isFalse();

    testKit.getGlobals().tStep = 5 + 5;
    testKit.testAction(testPerson, Person.recoverOrDieOrStep);

    assertThat(testPerson.isSymptomatic()).isTrue();
    assertThat(testPerson.isFirstTimeSymptomatic()).isTrue();

    testKit.getGlobals().tStep = 5 + 6;
    testKit.testAction(testPerson, Person.recoverOrDieOrStep);

    assertThat(testPerson.isSymptomatic()).isTrue();
    assertThat(testPerson.isFirstTimeSymptomatic()).isFalse();

    testKit.getGlobals().tStep = 5 + 7;
    testKit.testAction(testPerson, Person.recoverOrDieOrStep);

    assertThat(testPerson.isSymptomatic()).isFalse();
    assertThat(testPerson.isFirstTimeSymptomatic()).isFalse();
    assertThat(testPerson.status).isEqualTo(RECOVERED);
  }

  @Test
  public void testGetTested() {
    testPerson.status = Person.InfectionStatus.INFECTED;
    testPerson.hasBeenTested = false;

    testKit.send(Messages.TestAdministeredMsg.class).to(testPerson);
    TestResult result = testKit.testAction(testPerson, Person.getTested);

    assertThat(testPerson.hasBeenTested).isTrue();
    List<Messages.InfectionStatusMsg> msgs =
        result.getMessagesOfType(Messages.InfectionStatusMsg.class);
    assertThat(msgs).hasSize(1);
  }

  @Test
  public void testPerson() {
    testPerson.setTestingMultiplier(2.34);

    TestResult result =
        testKit.testAction(testPerson, Person.sendTestSelectionMultiplierToCentralAgent);

    assertThat(result.getMessagesOfType(Messages.TestSelectionMultiplierMessage.class))
        .isNotEmpty();
    assertThat(
        result
            .getMessagesOfType(Messages.TestSelectionMultiplierMessage.class)
            .get(0)
            .getBody())
        .isEqualTo(2.34);
  }

  @Test
  public void testMoveFromSchedule() {
    Person p = testKit.addAgent(Person.class, Person::init);
    testKit.getGlobals().tOneDay = 2;
    testKit.getGlobals().tStep = 0;

    ImmutableList<PlaceInfo> l1 = ImmutableList.of(TestUtils.createPlaceInfoWithAgent("A", 0, testKit));
    ImmutableList<PlaceInfo> l2 = ImmutableList.of(TestUtils.createPlaceInfoWithAgent("B", 1, testKit));
    Person.DailySchedule s =
        Person.DailySchedule.create(
            ImmutableMap.of(
                0, l1,
                1, l2),
            ImmutableList.of(TestUtils.createPlaceInfoWithAgent("ISOLATION", 3, testKit)));

    p.dailySchedule = s;
    assertThat(p.getScheduledPlaces().get(0).placeName()).isEqualTo("A");

    testKit.testAction(p, Person.movePerson);

    assertThat(p.getCurrentPlaces().get(0).placeName()).isEqualTo("A");

    testKit.getGlobals().tStep = 1;
    p.decideNextLocation();

    assertThat(p.getCurrentPlaces().get(0).placeName()).isEqualTo("B");
  }

  @Test
  public void testReceiveSchedule() {
    Person p = testKit.addAgent(Person.class, Person::init);

    ImmutableList<PlaceInfo> l1 = ImmutableList.of(TestUtils.createPlaceInfoWithAgent("A", 0, testKit));
    ImmutableList<PlaceInfo> l2 = ImmutableList.of(TestUtils.createPlaceInfoWithAgent("B", 1, testKit));
    Person.DailySchedule s =
        Person.DailySchedule.create(
            ImmutableMap.of(
                0, l1,
                1, l2),
            ImmutableList.of(TestUtils.createPlaceInfoWithAgent("ISOLATION", 3, testKit)));

    testKit
        .send(Messages.ScheduleMessage.class, scheduleMessage -> scheduleMessage.schedule = s)
        .to(p);

    testKit.testAction(p, Person.receiveSchedule);
    assertThat(p.dailySchedule).isEqualTo(s);
  }

  @Test(expected = IllegalStateException.class)
  public void testReceiveScheduleException() {
    testKit.getGlobals().tStep = 1;
    testKit.testAction(testPerson, Person.receiveSchedule);
  }

  @Test
  public void testExecuteMovement_wearsMask() {
    PlaceInfo placeInfo = TestUtils.createPlaceInfoWithAgent("A", 0, testKit);
    placeInfo.receivePlaceAgent(placeAgent.getID());
    testPerson.setNextPlace(placeInfo);
    testPerson.status = Person.InfectionStatus.SUSCEPTIBLE;
    testPerson.decideNextLocation();
    testPerson.maskType = Person.MaskType.N95;
    testPerson.complianceMask = 1;

    TestResult result = testKit.testAction(testPerson, Person.executeMovement);
    assertThat(result.getMessagesOfType(Messages.IAmHereMsg.class)).isNotEmpty();
    assertThat(
        result
            .getMessagesOfType(Messages.IAmHereMsg.class)
            .get(0)
            .transmissibilityInfo
            .wearsMask())
        .isEqualTo(Person.MaskType.N95);
  }

  @Test
  public void testExecuteMovement_doesNotWearMask() {
    PlaceInfo placeInfo = TestUtils.createPlaceInfoWithAgent("A", 0, testKit);
    placeInfo.receivePlaceAgent(placeAgent.getID());
    testPerson.setNextPlace(placeInfo);
    testPerson.status = Person.InfectionStatus.SUSCEPTIBLE;
    testPerson.decideNextLocation();
    testPerson.maskType = Person.MaskType.N95;
    testPerson.complianceMask = 0;

    TestResult result = testKit.testAction(testPerson, Person.executeMovement);
    assertThat(result.getMessagesOfType(Messages.IAmHereMsg.class)).isNotEmpty();
    assertThat(
        result
            .getMessagesOfType(Messages.IAmHereMsg.class)
            .get(0)
            .transmissibilityInfo
            .wearsMask())
        .isEqualTo(Person.MaskType.NONE);
  }

  @Test
  public void testGeneratePerfectTests() {
    testKit.getGlobals().testingType = 0;
    testPerson.status = Person.InfectionStatus.SUSCEPTIBLE;

    testPerson.hasBeenTested = false;
    testKit.send(Messages.TestAdministeredMsg.class).to(testPerson);
    TestResult result = testKit.testAction(testPerson, Person.getTested);
    assertThat(result.getMessagesOfType(Messages.InfectionStatusMsg.class)).isNotEmpty();
    assertThat(result.getMessagesOfType(Messages.InfectionStatusMsg.class).get(0).testAccuracy).isEqualTo(1.0);

    testPerson.status = Person.InfectionStatus.INFECTED;
    testPerson.timeInfected = 0;
    testPerson.symptomOnset = 2;
    testPerson.illnessDuration = 6;

    testPerson.hasBeenTested = false;
    testKit.send(Messages.TestAdministeredMsg.class).to(testPerson);
    TestResult result2 = testKit.testAction(testPerson, Person.getTested);
    assertThat(result2.getMessagesOfType(Messages.InfectionStatusMsg.class)).isNotEmpty();
    assertThat(result2.getMessagesOfType(Messages.InfectionStatusMsg.class).get(0).testAccuracy).isEqualTo(1.0);

  }

  @Test
  public void testGenerateConstantIncorrectTests() {
    testKit.getGlobals().testingType = 1;
    testKit.getGlobals().testingFalseNegativePerc = 0.4;
    testKit.getGlobals().testingFalsePositivePerc = 0.8;

    for(int i=0; i<100; i++) {
      testPerson.hasBeenTested = false;
      testKit.send(Messages.TestAdministeredMsg.class).to(testPerson);

      if(i<50) {
        TestResult result = testKit.testAction(testPerson, Person.getTested);
        List<Messages.InfectionStatusMsg> output = result.getMessagesOfType(Messages.InfectionStatusMsg.class);

        assertThat(output).isNotEmpty();
        assertThat(output.get(0).testAccuracy).isWithin(0.0001).of(0.2);
      }
      else {
        testPerson.status = Person.InfectionStatus.INFECTED;
        testPerson.timeInfected = i;
        testPerson.symptomOnset = i*2;
        testPerson.illnessDuration = i*3;

        TestResult result = testKit.testAction(testPerson, Person.getTested);
        List<Messages.InfectionStatusMsg> output = result.getMessagesOfType(Messages.InfectionStatusMsg.class);

        assertThat(output).isNotEmpty();
        assertThat(output.get(0).testAccuracy).isWithin(0.0001).of(0.6);
      }
    }
  }

  @Test
  public void testGenerateVariableincorrectTestsValues() {
    testKit.getGlobals().testingType = 2;
    testKit.getGlobals().testingFalseNegativePerc = 0.5;
    testKit.getGlobals().testingFalsePositivePerc = 0.2;
    testKit.getGlobals().daysAfterInfectionToDetect = 4;
    testPerson.status = Person.InfectionStatus.SUSCEPTIBLE;

    testPerson.hasBeenTested = false;
    testKit.send(Messages.TestAdministeredMsg.class).to(testPerson);

    TestResult non_infected_result = testKit.testAction(testPerson, Person.getTested);
    List<Messages.InfectionStatusMsg> non_infected_output = non_infected_result.getMessagesOfType(Messages.InfectionStatusMsg.class);

    assertThat(non_infected_output).isNotEmpty();
    assertThat(non_infected_output.get(0).testAccuracy).isWithin(0.0001).of(0.8);

    testPerson.status = Person.InfectionStatus.INFECTED;
    testPerson.timeInfected = 0;
    testPerson.symptomOnset = 4;
    testPerson.illnessDuration = 8;

    int[] time = new int[]{0, 3, 7, 8, 10, 12};
    double[] accuracy = new double[]{0.0, 0.3367, 0.5, 0.48, 0.32, 0.0};

    for(int i=0; i<time.length; i++) {
      testKit.getGlobals().tStep = time[i];
      testPerson.hasBeenTested = false;
      testKit.send(Messages.TestAdministeredMsg.class).to(testPerson);

      TestResult result = testKit.testAction(testPerson, Person.getTested);
      List<Messages.InfectionStatusMsg> output = result.getMessagesOfType(Messages.InfectionStatusMsg.class);

      assertThat(output).isNotEmpty();
      assertThat(output.get(0).testAccuracy).isWithin(0.0001).of(accuracy[i]);
    }
  }

  @Test
  public void testGenerateVariableIncorrectTestsShape() {
    testKit.getGlobals().testingType = 2;
    testKit.getGlobals().testingFalseNegativePerc = 0.5;
    testKit.getGlobals().testingFalsePositivePerc = 0.2;
    testKit.getGlobals().daysAfterInfectionToDetect = 4;


    testPerson.status = Person.InfectionStatus.INFECTED;
    testPerson.timeInfected = 0;
    testPerson.symptomOnset = 4;
    testPerson.illnessDuration = 8;

    double prevAccuracy = -1.0;

    // test that the accuracy is increasing until 3 days after symptom start
    for(int i=0; i<=testPerson.symptomOnset+3; i++) {
      testKit.getGlobals().tStep = i;
      testPerson.hasBeenTested = false;
      testKit.send(Messages.TestAdministeredMsg.class).to(testPerson);

      TestResult result = testKit.testAction(testPerson, Person.getTested);
      List<Messages.InfectionStatusMsg> output = result.getMessagesOfType(Messages.InfectionStatusMsg.class);

      assertThat(output).isNotEmpty();
      double currentAccuracy = output.get(0).testAccuracy;
      assertThat(currentAccuracy).isGreaterThan(prevAccuracy);
      prevAccuracy = currentAccuracy;
    }
    assertThat(prevAccuracy).isEqualTo(0.5);

    for(int i=testPerson.symptomOnset+3+1; i<=testPerson.illnessDuration+testKit.getGlobals().daysAfterInfectionToDetect; i++) {
      testKit.getGlobals().tStep = i;
      testPerson.hasBeenTested = false;
      testKit.send(Messages.TestAdministeredMsg.class).to(testPerson);

      TestResult result = testKit.testAction(testPerson, Person.getTested);
      List<Messages.InfectionStatusMsg> output = result.getMessagesOfType(Messages.InfectionStatusMsg.class);

      assertThat(output).isNotEmpty();
      double currentAccuracy = output.get(0).testAccuracy;
      assertThat(currentAccuracy).isLessThan(prevAccuracy);
      prevAccuracy = currentAccuracy;
    }
    assertThat(prevAccuracy).isEqualTo(0.0);

  }

  @Test
  public void testTransmissionOutput_outputTransmissionDisabled() {
    testKit.getGlobals().outputTransmissions = false;

    testKit.send(Messages.YouInfectedSomeoneMsg.class, msg -> {
      msg.infectedByMaskType = Person.MaskType.N95;
      msg.placeType = 2;
      msg.placeId = 123L;
      msg.newlyInfectedAgentId = 25L;
      msg.newlyInfectedMaskType = Person.MaskType.N95;
      msg.newlyInfectedCompliancePhysicalDistancing = 0.009;
    }).to(testPerson);
    TestResult result = testKit.testAction(testPerson, Person.infectedSomeoneElseWithCOVID);

    List<Messages.OutputWriterStringMessage> msgs = result.getMessagesOfType(Messages.OutputWriterStringMessage.class);
    assertThat(msgs).isEmpty();
  }
}

