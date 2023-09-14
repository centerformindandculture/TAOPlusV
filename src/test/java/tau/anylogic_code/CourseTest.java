package tau.anylogic_code;

import core.Globals;
import org.junit.Before;
import org.junit.Test;
import simudyne.core.abm.testkit.TestKit;

import static com.google.common.truth.Truth.assertThat;

public class CourseTest {

  private TestKit<Globals> testKit;

  @Before
  public void setUp() throws Exception {
    testKit = TestKit.create(Globals.class);
  }

  @Test
  public void testEventHappeningNow() {
    testKit.getGlobals().tOneDay = 2;
    ConnectionOfAgents.tOneDay = testKit.getGlobals().tOneDay;
    // MWF course that happens on the second step of the day
    Course mwfCourse = new Course(Course.ClassSchedule.MWF.ordinal(), false, 1, 0, 0);
    // TUTH course that happens on the first step of the day
    Course tuthCourse = new Course(Course.ClassSchedule.TUTH.ordinal(), false, 0, 1, 1);
    // One day course that happens on the first step of the day
    // The course happens on Wednesdays (day = 2)
    Course oneDayCourse = new Course(Course.ClassSchedule.ONE_DAY.ordinal(), false, 0, 2, 2);

    assertThat(oneDayCourse.dayOffset()).isEqualTo(2);

    // tStep = 0 (Monday), tOneDay = 2
    assertThat(mwfCourse.isEventHappeningNow(0, 2)).isFalse();
    assertThat(tuthCourse.isEventHappeningNow(0, 2)).isFalse();
    assertThat(oneDayCourse.isEventHappeningNow(0, 2)).isFalse();

    // tStep = 1 (Monday), tOneDay = 2
    assertThat(mwfCourse.isEventHappeningNow(1, 2)).isTrue();
    assertThat(tuthCourse.isEventHappeningNow(1, 2)).isFalse();
    assertThat(oneDayCourse.isEventHappeningNow(1, 2)).isFalse();

    // tStep = 2 (Tuesday), tOneDay = 2
    assertThat(tuthCourse.isEventHappeningNow(2, 2)).isTrue();
    assertThat(mwfCourse.isEventHappeningNow(2, 2)).isFalse();
    assertThat(oneDayCourse.isEventHappeningNow(2, 2)).isFalse();

    // tStep = 3 (Tuesday), tOneday = 2
    assertThat(mwfCourse.isEventHappeningNow(3, 2)).isFalse();
    assertThat(tuthCourse.isEventHappeningNow(3, 2)).isFalse();
    assertThat(oneDayCourse.isEventHappeningNow(3, 2)).isFalse();

    // tStep = 4 (Wednesday), tOneDay = 2
    assertThat(oneDayCourse.isEventHappeningNow(4, 2)).isTrue();
    assertThat(mwfCourse.isEventHappeningNow(4, 2)).isFalse();
    assertThat(tuthCourse.isEventHappeningNow(4, 2)).isFalse();

    // Test wraparound
    // tStep = 14 (Monday), tOneDay = 2
    assertThat(mwfCourse.isEventHappeningNow(14, 2)).isFalse();
    assertThat(tuthCourse.isEventHappeningNow(14, 2)).isFalse();
    assertThat(oneDayCourse.isEventHappeningNow(14, 2)).isFalse();

    // tStep = 15 (Monday), tOneDay = 2
    assertThat(mwfCourse.isEventHappeningNow(15, 2)).isTrue();
    assertThat(tuthCourse.isEventHappeningNow(15, 2)).isFalse();
    assertThat(oneDayCourse.isEventHappeningNow(15, 2)).isFalse();

    // tStep = 16 (Tuesday), tOneDay = 2
    assertThat(tuthCourse.isEventHappeningNow(16, 2)).isTrue();
    assertThat(mwfCourse.isEventHappeningNow(16, 2)).isFalse();
    assertThat(oneDayCourse.isEventHappeningNow(16, 2)).isFalse();
  }
}
