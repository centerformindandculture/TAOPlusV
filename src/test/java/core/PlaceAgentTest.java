package core;

import org.junit.Before;
import org.junit.Test;
import simudyne.core.abm.testkit.TestKit;
import simudyne.core.abm.testkit.TestResult;
import tau.TAUModel;

import java.util.List;

import static com.google.common.truth.Truth.assertThat;

public class PlaceAgentTest {
  private PlaceAgent testPlaceAgent;
  private PlaceInfo placeInfo;
  private TestKit<Globals> testKit;

  @Before
  public void setUp() throws Exception {
    testKit = TestKit.create(Globals.class);
    testPlaceAgent = testKit.addAgent(PlaceAgent.class);
    placeInfo = PlaceInfo.create("A", 0);
  }

  @Test
  public void testInit() {
    testKit.testAction(testPlaceAgent, PlaceAgent.initPlaceAgent);

    assertThat(testPlaceAgent.placeId()).isNotNull();
    assertThat(testPlaceAgent.placeId()).isEqualTo(testPlaceAgent.getID());
  }

  @Test
  public void testSendSelfToCentralAgent() {
    testPlaceAgent.init();
    TestResult result = testKit.testAction(testPlaceAgent, PlaceAgent.sendSelfToCentralAgent);
    assertThat(result.getMessagesOfType(Messages.PlaceAgentMessage.class).size())
        .isEqualTo(1);
    assertThat(result.getMessagesOfType(Messages.PlaceAgentMessage.class).get(0)
        .getBody()).isEqualTo(testPlaceAgent.placeId());
  }

  @Test
  public void testReceivePlace() {
    testPlaceAgent.init();
    assertThat(testPlaceAgent.place()).isNull();

    testKit.send(Messages.PlaceMessage.class, msg -> msg.placeInfo = placeInfo)
        .to(testPlaceAgent.placeId());
    testKit.testAction(testPlaceAgent, PlaceAgent.receivePlace);

    assertThat(testPlaceAgent.place()).isNotNull();
    assertThat(testPlaceAgent.place().placeName()).isEqualTo("A");
    assertThat(testPlaceAgent.place().placeId()).isEqualTo(testPlaceAgent.placeId());
  }

  @Test
  public void testReceivePlaceWithSpawnedAgents() {
    testPlaceAgent.init();

    PlaceInfo p1 = PlaceInfo.create("Place1", 0);
    PlaceInfo p2 = PlaceInfo.create("Place2", 0);

    testKit.send(Messages.PlaceMessage.class, msg -> msg.placeInfo = p1)
        .to(testPlaceAgent.getID());
    testKit.send(Messages.PlaceMessage.class, msg -> msg.placeInfo = p2)
        .to(testPlaceAgent.getID());

    testKit.testAction(testPlaceAgent, PlaceAgent.receivePlace);

    // This would throw an exception if the PlaceInfos had not been assigned PlaceAgents

    assertThat(p1.placeId()).isNotEqualTo(p2.placeId());
  }

  @Test
  public void testOutputTransmission_enabled() {
    placeInfo = PlaceInfo.create("place", TAUModel.PlaceType.BUILDING.ordinal());
    testPlaceAgent.init();
    testKit.send(Messages.PlaceMessage.class, msg -> msg.placeInfo = placeInfo)
        .to(testPlaceAgent.getID());
    testKit.testAction(testPlaceAgent, PlaceAgent.receivePlace);
    testKit.getGlobals().outputTransmissions = true;
    testKit.getGlobals().baseInfectivity = 1.0;


    sendInfectedIAmHereMsgWithPersonId(testKit, testPlaceAgent, 0);
    sendIAmHereMsgWithPersonId(testKit, testPlaceAgent, 1);
    TestResult result = testKit.testAction(testPlaceAgent, PlaceAgent.generateContactsAndInfect);

    List<Messages.YouInfectedSomeoneMsg> youInfectedSomeoneMsgs =
        result.getMessagesOfType(Messages.YouInfectedSomeoneMsg.class);
    assertThat(youInfectedSomeoneMsgs).hasSize(1);
    Messages.YouInfectedSomeoneMsg msg = youInfectedSomeoneMsgs.get(0);
    Person.PersonTransmissibilityInfo infectedByTransmissibility = Person.PersonTransmissibilityInfo.dummyInfected();
    Person.PersonTransmissibilityInfo infectedTransmissibility = Person.PersonTransmissibilityInfo.dummy();
    assertThat(msg.infectedByMaskType).isEqualTo(infectedByTransmissibility.wearsMask());
    assertThat(msg.newlyInfectedAgentId).isEqualTo(1);
    assertThat(msg.newlyInfectedCompliancePhysicalDistancing).isEqualTo(infectedTransmissibility.physicalDistCompliance());
    assertThat(msg.newlyInfectedMaskType).isEqualTo(infectedTransmissibility.wearsMask());
    assertThat(msg.placeId).isEqualTo(testPlaceAgent.placeId());
    assertThat(msg.placeType).isEqualTo(testPlaceAgent.place().placeType());
    assertThat(msg.getTo()).isEqualTo(0);
  }

  @Test
  public void testOutputTransmission_disabled() {
    placeInfo = PlaceInfo.create("place", TAUModel.PlaceType.BUILDING.ordinal());
    testPlaceAgent.init();
    testKit.send(Messages.PlaceMessage.class, msg -> msg.placeInfo = placeInfo)
        .to(testPlaceAgent.getID());
    testKit.testAction(testPlaceAgent, PlaceAgent.receivePlace);
    testKit.getGlobals().outputTransmissions = false;
    testKit.getGlobals().baseInfectivity = 1.0;


    sendInfectedIAmHereMsgWithPersonId(testKit, testPlaceAgent, 0);
    sendIAmHereMsgWithPersonId(testKit, testPlaceAgent, 1);
    TestResult result = testKit.testAction(testPlaceAgent, PlaceAgent.generateContactsAndInfect);

    List<Messages.YouInfectedSomeoneMsg> youInfectedSomeoneMsgs =
        result.getMessagesOfType(Messages.YouInfectedSomeoneMsg.class);
    assertThat(youInfectedSomeoneMsgs).hasSize(1);
    Messages.YouInfectedSomeoneMsg msg = youInfectedSomeoneMsgs.get(0);
    assertThat(msg.infectedByMaskType).isNull();
    assertThat(msg.newlyInfectedAgentId).isNull();
    assertThat(msg.newlyInfectedCompliancePhysicalDistancing).isNull();
    assertThat(msg.newlyInfectedMaskType).isNull();
    assertThat(msg.placeId).isNull();
    assertThat(msg.placeType).isNull();
    assertThat(msg.getTo()).isEqualTo(0);
  }

  private static void sendIAmHereMsgWithPersonId(TestKit<Globals> testKit, PlaceAgent placeAgent, long personId) {
    testKit.send(Messages.IAmHereMsg.class, msg ->
    {
      msg.transmissibilityInfo = Person.PersonTransmissibilityInfo.dummy();
    },
    personId).to(placeAgent.getID());
  }

  private static void sendInfectedIAmHereMsgWithPersonId(TestKit<Globals> testKit, PlaceAgent placeAgent,
                                                         long personId) {
    testKit.send(Messages.IAmHereMsg.class, msg ->
    {
      msg.transmissibilityInfo = Person.PersonTransmissibilityInfo.dummyInfected();
    },
    personId).to(placeAgent.getID());
  }
}
