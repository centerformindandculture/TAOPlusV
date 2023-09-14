package tau;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import core.*;
import org.junit.Before;
import org.junit.Test;
import simudyne.core.abm.testkit.TestKit;
import simudyne.core.abm.testkit.TestResult;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

public class StudentTest {

  private TestKit<Globals> testKit;

  @Before
  public void setUp() {
    testKit = TestKit.create(Globals.class);
  }

  @Test
  public void testAgeDistribution() {
    testKit.getGlobals().studentAgentAgeStart = 200;
    testKit.getGlobals().studentAgentAgeEnd = 300;

    Student s = testKit.addAgent(Student.class, Student::init);

    assertThat(s.age).isGreaterThan(100.0);
  }

  @Test
  public void testComplianceDistribution() {

    Student s = testKit.addAgent(Student.class, Student::init);

    assertThat(s.complianceMask).isGreaterThan(999.0);
    assertThat(s.compliancePhysicalDistancing).isGreaterThan(999.0);
  }

  @Test
  public void testGetCurrentPlaces() {
    Student s = testKit.addAgent(Student.class, Student::init);

    ArrayList<PlaceInfo> regularPlaces = new ArrayList<>();
    ArrayList<PlaceInfo> additionalPlaces = new ArrayList<>();

    regularPlaces.add(PlaceInfo.create("p1", 0));
    regularPlaces.add(PlaceInfo.create("p2", 0));

    s.setCurrentPlaces(regularPlaces);

    ImmutableList<PlaceInfo> places = s.getCurrentPlaces();

    assertThat(places.size()).isEqualTo(2);

    additionalPlaces.add(PlaceInfo.create("p3", 0));
    additionalPlaces.add(PlaceInfo.create("p4", 0));
  }
}
