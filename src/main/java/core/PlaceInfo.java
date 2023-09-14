package core;

import java.util.Objects;


/**
 * A place that a {@link Person} can go to. Place is a high level concept, and can include any event
 * that a person could spread an infection at.
 * <p>
 * AutoValue cannot be used here because the PlaceAgent class member instance needs to be mutable.
 * <p>
 * The methods, equals(), hashCode(), and any compareTo()s, need to be updated for every change
 * to the class.
 */
public class PlaceInfo {
  //private final long placeId;
  private final String placeName;
  private final int placeType;
  private final PlaceInfo.Optionality placeOptionality;
  private final PlaceInfo.NetworkType networkType;
  private final long center;
  private final String debugNotes;
  private long placeAgent;
  private int capacity;

  /**
   * A unique id for the place.
   */
  public long placeId() {
    if (this.placeAgent == -1) {
      throw new IllegalStateException("This place doesn't have a placeAgent yet.");
    }
    return this.placeAgent;
  }

  public String placeName() {
    return placeName;
  }

  /**
   * A place type will have an inactivity associated with it. Implementations need to define what
   * integers represent what placeType. It is recommended to create an enum, and map each ordinal of
   * the enum to a placeType int.
   */
  public int placeType() {
    return placeType;
  }

  /**
   * The optionality of attendance to a certain place. Could be extended to add different levels of
   * optionality besides OPTIONAL and MANDATORY.
   */
  public Optionality placeOptionality() {
    return placeOptionality;
  }

  /**
   * The network type demonstrated by contact network at this place. Most will be fully connected -
   * we assume an infected person has a chance of infecting anyone at the Place. But, some
   * infections spread differently.
   */
  public NetworkType networkType() {
    return networkType;
  }

  /**
   * The person defined as the "center" of the contact network of this place, if applicable. May be
   * -1 if not applicabe.
   */
  public long center() {
    return center;
  }

  /**
   * For testing and debugging.
   */
  public String debugNotes() {
    return debugNotes;
  }

  public int capacity() { return capacity; }

  public void receivePlaceAgent(long agentId) {
    this.placeAgent = agentId;
  }

  private PlaceInfo(String placeName, int placeType, Optionality optionality, NetworkType networkType, long center, String debugNotes, int capacity) {
    this.placeName = placeName;
    this.placeType = placeType;
    this.placeOptionality = optionality;
    this.networkType = networkType;
    this.center = center;
    this.debugNotes = debugNotes;
    this.placeAgent = -1;
    this.capacity = capacity;
  }


  public static PlaceInfo create(
      String placeName, int placeType, NetworkType networkType, long center, String debugNotes) {
    return new PlaceInfo(
        placeName, placeType, Optionality.MANDATORY, networkType, center, debugNotes, -1);
  }

  public static PlaceInfo create(
          String placeName, int placeType, NetworkType networkType, long center, String debugNotes, int capacity) {
    return new PlaceInfo(
            placeName, placeType, Optionality.MANDATORY, networkType, center, debugNotes, capacity);
  }

  public static PlaceInfo create(String placeName, int placeType) {
    return new PlaceInfo(
        placeName, placeType, Optionality.MANDATORY, NetworkType.FULLY_CONNECTED, -1L, "", -1);
  }

  public static PlaceInfo create(String placeName, int placeType, int capacity) {
    return new PlaceInfo(
            placeName, placeType, Optionality.MANDATORY, NetworkType.FULLY_CONNECTED, -1L, "", capacity);
  }

  public static PlaceInfo create(String placeName, int placeType, NetworkType networkType, long center) {
    return new PlaceInfo(placeName, placeType, Optionality.MANDATORY, networkType, center, "", -1);
  }

  public static PlaceInfo create(String placeName, int placeType, NetworkType networkType, long center, int capacity) {
    return new PlaceInfo(placeName, placeType, Optionality.MANDATORY, networkType, center, "", capacity);
  }

  public static PlaceInfo create(
      String placeName,
      int placeType,
      Optionality optionality,
      NetworkType networkType,
      long center,
      String debugNotes,
      int capacity) {
    return new PlaceInfo(placeName, placeType, optionality, networkType, center, debugNotes, capacity);
  }

  public static PlaceInfo create(String placeName, int placeType, Optionality optionality) {
    return new PlaceInfo(
        placeName, placeType, optionality, NetworkType.FULLY_CONNECTED, -1L, "", -1);
  }

  public static PlaceInfo create(
      String placeName,
      int placeType,
      Optionality optionality,
      NetworkType networkType,
      long center) {
    return new PlaceInfo(placeName, placeType, optionality, networkType, center, "", -1);
  }

  @Override
  public int hashCode() {
    return Objects.hash(placeName, placeType, placeOptionality, networkType, center, debugNotes);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PlaceInfo)) return false;
    PlaceInfo placeInfo = (PlaceInfo) o;
    return placeType == placeInfo.placeType &&
        center == placeInfo.center &&
        placeName.equals(placeInfo.placeName) &&
        placeOptionality == placeInfo.placeOptionality &&
        networkType == placeInfo.networkType &&
        capacity == placeInfo.capacity &&
        Objects.equals(debugNotes, placeInfo.debugNotes);
  }

  public enum NetworkType {
    FULLY_CONNECTED, // Anyone can infect anyone
    STAR, // Infections can only occur through center
    FULLY_CONNECTED_DEPENDENT_ON_CENTER, // Infections only occur if center is present at Place
  }

  public enum Optionality {
    OPTIONAL,
    MANDATORY
  }


}
