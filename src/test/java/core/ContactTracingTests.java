package core;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import simudyne.core.abm.testkit.TestKit;
import simudyne.core.abm.testkit.TestResult;
import simudyne.core.graph.Message;

import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.truth.Truth.assertThat;

public class ContactTracingTests {
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
    testKit.createLongAccumulator("numPosTestsThisStep", 0);
    testKit.createLongAccumulator("numTestsThisStep", 0);
  }

  private void sendAllMessagesFromTestResult(TestResult result) {
    Iterator<? extends Message> it = result.getMessageIterator();
    while (it.hasNext()) {
      Message msg = it.next();
      if (msg instanceof Messages.Copyable) {
        Messages.Copyable copy = (Messages.Copyable) msg;
        testKit.send(msg.getClass(), copy::copyInto, msg.getSender()).to(msg.getTo());
      } else {
        testKit.send(msg.getClass(), msg.getSender()).to(msg.getTo());
      }
    }
  }

  @Test
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
