package tau.anylogic_code;

import java.io.Serializable;

/**
 * StudentGroup
 */
public class StudentGroup extends ConnectionOfAgents implements Serializable {

  /**
   * Default constructor
   */
  public StudentGroup(long uniqueId) {
    super(uniqueId);
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
