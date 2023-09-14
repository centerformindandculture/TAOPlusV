package tau;

import com.google.common.collect.ImmutableList;
import core.Globals;
import core.Person;
import core.PlaceInfo;
import org.junit.Before;
import org.junit.Test;
import simudyne.core.abm.testkit.TestKit;

import java.util.ArrayList;

import static com.google.common.truth.Truth.assertThat;

public class FacultyTest {

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

    Faculty f = testKit.addAgent(Faculty.class, Faculty::init);

    assertThat(f.age).isGreaterThan(100.0);
  }

  @Test
  public void testComplianceDistribution() {

    Faculty f = testKit.addAgent(Faculty.class, Faculty::init);

    assertThat(f.complianceMask).isGreaterThan(999.0);
    assertThat(f.compliancePhysicalDistancing).isGreaterThan(999.0);
  }
}
