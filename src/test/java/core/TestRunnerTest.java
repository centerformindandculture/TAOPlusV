package core;

import org.junit.Before;
import org.junit.Test;
import simudyne.core.abm.testkit.TestKit;

import static com.google.common.truth.Truth.assertThat;

public class TestRunnerTest {

  private TestKit<Globals> testKit;
  private CentralAgent centralAgent;


  @Before
  public void setUp() throws Exception {
    testKit = TestKit.create(Globals.class);
    centralAgent = testKit.addAgent(CentralAgent.class);
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
  // TODO: Uncomment this test. It works in a downstream CL.
  public void testRunnerTest() {
    TestRunner runner = new TestRunner(testKit, centralAgent);
    // Set there to be no exposure period
    runner.setInfectionTrajectoryDistributionForNewPeople(InfectionTrajectoryDistribution.builder()
        .infectiousRangeStart(0)
        .infectiousRangeEnd(0)
        .build());
    testKit.getGlobals().baseInfectivity = 1.0;

    PlaceAgent place1 = runner.newPlaceAgent("Place1", 1);

    TestPerson person1 = runner.newTestPerson();
    person1.setInfected();
    person1.setCurrentPlaces(place1.place());

    TestPerson person2 = runner.newTestPerson();
    person2.setCurrentPlaces(place1.place());

    runner.moveAndInfect();
    runner.moveAndInfect();
    runner.moveAndInfect();
    runner.moveAndInfect();
    runner.moveAndInfect();
    runner.moveAndInfect();
    runner.moveAndInfect();

    assertThat(person2.status).isEqualTo(Person.InfectionStatus.INFECTED);
  }
}
