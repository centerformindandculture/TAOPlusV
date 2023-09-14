package tau.anylogic_code;

import java.io.Serializable;

/**
 * CampusEvent
 */
public class CampusEvent extends ConnectionOfAgents implements Serializable {

  private static final int CAMPUS_EVENT_DEFAULT_FREQUENCY = 1;
  private final int dayOffset;

  /**
   * Default constructor
   */
  public CampusEvent(int dayOffset, long uniqueId) {
    super(CAMPUS_EVENT_DEFAULT_FREQUENCY, uniqueId);
    this.dayOffset = dayOffset;
  }

  @Override
  public void setName(String name) {
    super.setName("event_" + name);
  }

  @Override
  public String toString() {
    return super.toString();
  }

  @Override
  protected int dayOffset() {
    return dayOffset;
  }

  /**
   * This number is here for model snapshot storing purpose<br>
   * It needs to be changed when this class gets changed
   */
  private static final long serialVersionUID = 1L;
}
