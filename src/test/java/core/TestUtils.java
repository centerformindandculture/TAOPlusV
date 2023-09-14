package core;

import simudyne.core.abm.testkit.TestKit;

public class TestUtils {

  public static PlaceInfo createPlaceInfoWithAgent(String name, int type, TestKit<Globals> testKit) {
    PlaceInfo placeInfo = PlaceInfo.create(name, type);
    PlaceAgent pAgent = testKit.addAgent(PlaceAgent.class, placeAgent -> {
      placeAgent.setPlaceInfo(placeInfo);
      placeInfo.receivePlaceAgent(placeAgent.getID());
    });
    placeInfo.receivePlaceAgent(pAgent.getID());
    return placeInfo;
  }
}
