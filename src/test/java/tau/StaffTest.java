package tau;

import com.google.common.collect.ImmutableList;
import core.Globals;
import core.PlaceInfo;
import org.junit.Before;
import org.junit.Test;
import simudyne.core.abm.testkit.TestKit;

import java.util.ArrayList;

import static com.google.common.truth.Truth.assertThat;

public class StaffTest {

  private TestKit<Globals> testKit;

  @Before
  public void setUp() throws Exception {
    testKit = TestKit.create(Globals.class);
  }

  @Test
  public void testAgeDistribution() {
    testKit.getGlobals().facultyStaffAgentAgeStart = 200;
    testKit.getGlobals().facultyStaffAgentAgeEnd = 300;
    testKit.getGlobals().facultyStaffAgentAgeMean = 250;

    Staff s = testKit.addAgent(Staff.class, Staff::init);

    assertThat(s.age).isGreaterThan(100.0);
  }

  @Test
  public void testComplianceDistribution() {

    Staff s = testKit.addAgent(Staff.class, Staff::init);

    assertThat(s.complianceMask).isGreaterThan(999.0);
    assertThat(s.compliancePhysicalDistancing).isGreaterThan(999.0);
  }
}
