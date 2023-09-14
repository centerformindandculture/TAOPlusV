package tau.anylogic_code;

import java.io.Serializable;

/**
 * Suite
 */
public class Suite extends ConnectionOfAgents implements Serializable {

  public final Floor floor;
  public final Building building;

  /**
   * Default constructor
   */
  public Suite(int frequency, Floor floor, Building building, long uniqueId) {
    super(frequency, uniqueId);
    this.floor = floor;
    this.building = building;
  }

  public Suite(Floor floor, Building building, long uniqueId) {
    super(uniqueId);
    this.floor = floor;
    this.building = building;
  }

  @Override
  public String toString() {
    return super.toString();
  }

  /**
   * This number is here for model snapshot storing purpose<br>
   * It needs to be changed when this class gets changed
   */
  private static final long serialVersionUID = 1L;
}
