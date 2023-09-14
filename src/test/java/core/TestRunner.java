package core;

import com.google.common.collect.ImmutableList;
import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.abm.Section;
import simudyne.core.abm.testkit.TestKit;
import simudyne.core.abm.testkit.TestResult;
import simudyne.core.graph.Message;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * A helper class which can simulate {@link VIVIDCoreModel#step()} and
 * {@link simudyne.core.abm.AgentBasedModel#run(Section...)}, so that we can do model testing beyond unit testing.
 *
 * <p>In order to use, put the below code in your @Before:
 * <code>
 * testKit = TestKit.create(Globals.class);
 * centralAgent = testKit.addAgent(CentralAgent.class);
 * testKit.getGlobals().centralAgentID = centralAgent.getID();
 * </code> and start a @Test method with the code:
 * <code>TestRunner runner = new TestRunner(testKit, centralAgent)
 * </code>
 *
 * <p>It is recommended to use the methods {@link #newTestPerson()} and {@link #newPlaceAgent(String, int)} for creating
 * people and place agents, since these methods do some setup to ensure ther agents are properly set up.
 *
 * <p>This primarily works by using {@link TestKit#testAction(Agent, Action)}, then systematically sending all the
 * messages that were sent in the testAction call. You can use the method
 * {@link TestRunner#sendAllMessagesFromTestResult(TestResult, TestKit)} in a unit test without using a full TestRunner.
 * <p>
 */
public class TestRunner {
  private final TestKit<Globals> testKit;
  private final Collection<Person> people = new ArrayList<>();
  private final CentralAgent centralAgent;
  private final Collection<PlaceAgent> places = new ArrayList<>();
  private InfectionTrajectoryDistribution infectionTrajectoryDistributionForNewPeople =
      InfectionTrajectoryDistribution.builder().build();
  private double defaultComplianceToSelfIsolateWhenContactNotified = 1.0;

  public TestRunner(TestKit<Globals> testKit, CentralAgent centralAgent) {
    this.testKit = testKit;
    this.centralAgent = centralAgent;
    testKit.getGlobals().initBuildingInfectionArrays(1);
  }

  public void setInfectionTrajectoryDistributionForNewPeople(InfectionTrajectoryDistribution d) {
    this.infectionTrajectoryDistributionForNewPeople = d;
  }

  public void setDefaultComplianceToSelfIsolateWhenContactNotified(double d) {
    this.defaultComplianceToSelfIsolateWhenContactNotified = 1.0;
  }

  public TestPerson newTestPerson() {
    TestPerson person = testKit.addAgent(TestPerson.class);
    person.setInfectionTrajectoryDistribution(infectionTrajectoryDistributionForNewPeople);
    people.add(person);
    return person;
  }

  public PlaceAgent newPlaceAgent(String name, int type) {
    PlaceInfo placeInfo = PlaceInfo.create(name, type);
    PlaceAgent pAgent = testKit.addAgent(PlaceAgent.class, placeAgent -> {
      placeAgent.setPlaceInfo(placeInfo);
      placeInfo.receivePlaceAgent(placeAgent.getID());
    });
    placeInfo.receivePlaceAgent(pAgent.getID());
    places.add(pAgent);
    return pAgent;
  }

  public void incrementStep() {
    testKit.getGlobals().tStep++;
  }

  public void step() {
    moveAndInfect();
    // external
    // other illness
    randomizedTesting();
    releaseTestResults();
    // die
    //stats
    //move
    incrementStep();
  }

  public void moveAndInfect() {
    run(people, Person.executeMovement);
    run(places, PlaceAgent.generateContactsAndInfect);
    run(people, Person.infectedByCOVID);
    run(centralAgent, CentralAgent.processPlaceInfectionRates); // Not sure this action works in tests
  }

  public void randomizedTesting() {
    run(centralAgent, CentralAgent.doRandomizedTesting);
    run(people, Person.getTested);
    run(centralAgent, CentralAgent.processInfectionStatus);
  }

  public void releaseTestResults() {
    run(centralAgent, CentralAgent.releaseTestResults);
  }

  public <T extends Agent<Globals>> void run(Collection<T> agents, Action<T> action) {
    for (Agent<Globals> agent : agents) {
      TestResult testResult = testKit.testAction(agent, (Action<Agent<Globals>>) action);
      sendAllMessagesFromTestResult(testResult);
    }
  }

  public <T extends Agent<Globals>> void run(T agent, Action<T> action) {
    run(ImmutableList.of(agent), action);
  }

  private void sendAllMessagesFromTestResult(TestResult result) {
    TestRunner.sendAllMessagesFromTestResult(result, this.testKit);
  }

  public static void sendAllMessagesFromTestResult(TestResult result, TestKit<Globals> testKit) {
    Iterator<? extends Message> it = result.getMessageIterator();
    while (it.hasNext()) {
      Message msg = it.next();
      if (msg instanceof Messages.Copyable) {
        Messages.Copyable copy = (Messages.Copyable) msg;
        testKit.send(msg.getClass(), copy::copyInto, msg.getSender()).to(msg.getTo());
      } else { // is Message.Empty - all non-empty messages must implement Copyable
        testKit.send(msg.getClass(), msg.getSender()).to(msg.getTo());
      }
    }
  }
}
