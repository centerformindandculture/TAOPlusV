package core;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import simudyne.core.abm.testkit.TestKit;
import simudyne.core.abm.testkit.TestResult;
import simudyne.core.graph.Message;
import simudyne.core.rng.SeededRandom;

import java.util.*;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;

public class CentralAgentTest {

  private TestKit<Globals> testKit;
  private CentralAgent centralAgent;
  private PlaceAgent pa1, pa2, pa3;

  @Before
  public void setUp() throws Exception {
    testKit = TestKit.create(Globals.class);
    testKit.createLongAccumulator("numPosTestsThisStep");
    testKit.createLongAccumulator("numTestsThisStep");
    testKit.createLongAccumulator("totTestsReturnedThisStep");
    testKit.createLongAccumulator("posTestsReturnedThisStep");

    centralAgent = testKit.addAgent(CentralAgent.class);
    pa1 = testKit.addAgent(PlaceAgent.class);
    pa2 = testKit.addAgent(PlaceAgent.class);
    pa3 = testKit.addAgent(PlaceAgent.class);
    testKit.getGlobals().centralAgentID = centralAgent.getID();
    PlaceInfo p1 = PlaceInfo.create("Place1", 0);
    p1.receivePlaceAgent(pa1.getID());
    PlaceInfo p2 = PlaceInfo.create("Place2", 0);
    p2.receivePlaceAgent(pa2.getID());
    PlaceInfo p3 = PlaceInfo.create("Place3", 0);
    p3.receivePlaceAgent(pa3.getID());

    testKit.getGlobals().baseInfectivity = 1.0;
  }

  @Test
  public void testTestSelection() {
    final Map<Long, Double> testSelectionMultipliersCollected = new HashMap<>();
    testKit
        .getGlobals()
        .setModules(
            new DefaultModulesImpl() {

              @Override
              public Set<Long> getAgentsToTest(
                  Set<Long> symptomaticAgentsToday,
                  Map<Long, Double> testSelectionMultipliers,
                  SeededRandom random,
                  long numAgentsToTest) {
                testSelectionMultipliersCollected.clear();
                testSelectionMultipliersCollected.putAll(testSelectionMultipliers);
                return ImmutableSet.of(5678L);
              }
            });
    testKit
        .send(Messages.TestSelectionMultiplierMessage.class, msg -> msg.setBody(12.34))
        .to(centralAgent);
    testKit.testAction(centralAgent, CentralAgent.receiveTestSelectionMultipliers);

    TestResult result = testKit.testAction(centralAgent, CentralAgent.doRandomizedTesting);

    assertEquals(1, result.getMessagesOfType(Messages.TestAdministeredMsg.class).size());
    assertEquals(
        5678L,
        (long)
            result.getMessagesOfType(Messages.TestAdministeredMsg.class).stream()
                .map(Message::getTo)
                .findFirst()
                .get());
    Optional<Double> multiplier = testSelectionMultipliersCollected.values().stream().findFirst();
    assertThat(multiplier.isPresent()).isTrue();
    assertThat(multiplier.get()).isEqualTo(12.34);
  }
}
