package tau.anylogic_code;

import tau.Staff;

import java.io.Serializable;

/**
 * StaffToStudent
 */
public class StaffToStudent extends ConnectionOfAgents implements Serializable {

  public Staff staff;
  private final int numStaffInfects;

  /**
   * Default constructor
   */
  public StaffToStudent(int frequency, Staff staff, int numStaffInfects, long uniqueId) {
    super(frequency, uniqueId);
    this.staff = staff;
    this.numStaffInfects = numStaffInfects;
    this.setName("Student-Facing Staff  for" + staff.getName());
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
