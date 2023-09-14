package tau.anylogic_code;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import core.Globals;
import core.Person;
import core.PlaceInfo;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import simudyne.core.abm.testkit.TestKit;
import tau.Faculty;
import tau.Staff;
import tau.Student;
import tau.UniversityConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.google.common.truth.Truth.assertThat;

@RunWith(Parameterized.class)
public class StaticNetworkBuilderTest {
  private TestKit<Globals> testKit;
  private final List<Person> allPeople = new ArrayList<>();
  private final int nAgents;

  @Parameterized.Parameters
  public static Object[][] data() {
    // Trying several values for nAgents
    return new Object[][]{{300}, {350}, {400}, {450}, {500}, {550}, {600}, {650}, {700}};
  }

  public StaticNetworkBuilderTest(int nAgents) {
    this.nAgents = nAgents;
  }

  @Before
  public void setUp() throws Exception {
    testKit = TestKit.create(Globals.class);
    testKit.getGlobals().nAgents = this.nAgents;
    testKit.getGlobals().universityProfile = Globals.UniversityProfile.SCALABLE.ordinal();
    UniversityConfiguration universityConfiguration =
        testKit.getGlobals().getUniversityConfiguration();

    for (int i = 0; i < universityConfiguration.numStaff(); i++) {
      allPeople.add(testKit.addAgent(Staff.class, Staff::init));
    }
    for (int i = 0; i < universityConfiguration.numStudents(); i++) {
      allPeople.add(testKit.addAgent(Student.class, Student::init));
    }
    for (int i = 0; i < universityConfiguration.numFaculty(); i++) {
      allPeople.add(testKit.addAgent(Faculty.class, Faculty::init));
    }
  }

  @Test
  public void testClassSchedules() {
    StaticNetworkBuilder staticNetworkBuilder = new StaticNetworkBuilder();
    staticNetworkBuilder.createConnectionOfAgents(allPeople, testKit.getGlobals());
    Map<Long, Person.DailySchedule> schedules =
        staticNetworkBuilder.createPlacesAndPersonDailySchedules();

    List<PlaceInfo> mwfClasses =
        staticNetworkBuilder.getAllPlaces().stream()
            .filter(p -> p.debugNotes().equals("Course_MWF"))
            .collect(Collectors.toList());
    assertThat(mwfClasses).isNotEmpty();
    for (PlaceInfo p : mwfClasses) {
      Set<Long> mondayers =
          schedules.entrySet().stream()
              .filter(entry -> entry.getValue().placesAtStepMap().get(0).contains(p))
              .map(Map.Entry::getKey)
              .collect(Collectors.toSet());
      assertThat(mondayers).isNotEmpty();
      Set<Long> wednesdayers =
          schedules.entrySet().stream()
              .filter(entry -> entry.getValue().placesAtStepMap().get(2).contains(p))
              .map(Map.Entry::getKey)
              .collect(Collectors.toSet());
      assertThat(wednesdayers).isNotEmpty();
      Set<Long> fridayers =
          schedules.entrySet().stream()
              .filter(entry -> entry.getValue().placesAtStepMap().get(4).contains(p))
              .map(Map.Entry::getKey)
              .collect(Collectors.toSet());
      assertThat(fridayers).isNotEmpty();

      // Check contains instructor
      assertThat(mondayers).contains(p.center());
      assertThat(wednesdayers).contains(p.center());
      assertThat(fridayers).contains(p.center());

      // Check there is no student overlap
      assertThat(mondayers).containsAllIn(wednesdayers);
      assertThat(wednesdayers).containsAllIn(mondayers);
      assertThat(wednesdayers).containsAllIn(fridayers);
      assertThat(fridayers).containsAllIn(wednesdayers);
    }

    List<PlaceInfo> tuthClasses =
        staticNetworkBuilder.getAllPlaces().stream()
            .filter(p -> p.debugNotes().equals("Course_TUTH"))
            .collect(Collectors.toList());
    assertThat(tuthClasses).isNotEmpty();
    for (PlaceInfo p : tuthClasses) {
      Set<Long> tuesdayers =
          schedules.entrySet().stream()
              .filter(entry -> entry.getValue().placesAtStepMap().get(1).contains(p))
              .map(Map.Entry::getKey)
              .collect(Collectors.toSet());
      assertThat(tuesdayers).isNotEmpty();
      Set<Long> thursdayers =
          schedules.entrySet().stream()
              .filter(entry -> entry.getValue().placesAtStepMap().get(3).contains(p))
              .map(Map.Entry::getKey)
              .collect(Collectors.toSet());

      // Check contains instructor
      assertThat(tuesdayers).contains(p.center());
      assertThat(thursdayers).contains(p.center());

      // Check there is no student overlap
      assertThat(tuesdayers).containsAllIn(thursdayers);
      assertThat(thursdayers).containsAllIn(tuesdayers);
    }

    List<PlaceInfo> oneDayClasses =
        staticNetworkBuilder.getAllPlaces().stream()
            .filter(p -> p.debugNotes().equals("Course_ONE_DAY"))
            .collect(Collectors.toList());
    assertThat(oneDayClasses).isNotEmpty();
    for (PlaceInfo p : oneDayClasses) {
      int firstDay =
          IntStream.range(0, 6)
              .boxed()
              .filter(
                  day ->
                      schedules.values().stream()
                          .map(dailySchedule -> dailySchedule.placesAtStepMap().get(day))
                          .anyMatch(places -> places.contains(p)))
              .findFirst()
              .orElseThrow(IllegalStateException::new);

      Set<Long> firstWeekers =
          schedules.entrySet().stream()
              .filter(entry -> entry.getValue().placesAtStepMap().get(firstDay).contains(p))
              .map(Map.Entry::getKey)
              .collect(Collectors.toSet());
      assertThat(firstWeekers).isNotEmpty();
      Set<Long> secondWeekers =
          schedules.entrySet().stream()
              .filter(entry -> entry.getValue().placesAtStepMap().get(firstDay + 7).contains(p))
              .map(Map.Entry::getKey)
              .collect(Collectors.toSet());

      // Check contains instructor
      assertThat(firstWeekers).contains(p.center());
      assertThat(secondWeekers).contains(p.center());

      // Check there is no student overlap
      assertThat(firstWeekers).containsAllIn(secondWeekers);
      assertThat(secondWeekers).containsAllIn(firstWeekers);
    }
  }

  @Test
  public void testDeterminismInNetworkCreation() {
    Map<Long, Person.DailySchedule> previous = null;
    Set<PlaceInfo> previousPlaceInfos = null;
    for (int i = 0; i < 10; i++) {
      StaticNetworkBuilder staticNetworkBuilder = new StaticNetworkBuilder();
      staticNetworkBuilder.createConnectionOfAgents(allPeople, testKit.getGlobals());
      Map<Long, Person.DailySchedule> schedules =
          staticNetworkBuilder.createPlacesAndPersonDailySchedules();
      Set<PlaceInfo> placeInfos = staticNetworkBuilder.getAllPlaces();
      if (previous != null) {
        for (Long idCurrent : schedules.keySet()) {
          assertThat(previous).containsKey(idCurrent);
          assertThat(schedules.get(idCurrent).placesAtStepMap())
              .isEqualTo(previous.get(idCurrent).placesAtStepMap());
          assertThat(schedules.get(idCurrent).isolationPlaces())
              .isEqualTo(schedules.get(idCurrent).isolationPlaces());
          assertThat(placeInfos).isEqualTo(previousPlaceInfos);
        }
      }
      previous = schedules;
      previousPlaceInfos = placeInfos;
    }
  }

  @Test
  public void testDeterminismInNetworkCreation_noGradStudents() {
    Map<Long, Person.DailySchedule> previous = null;
    Set<PlaceInfo> previousPlaceInfos = null;
    for (int i = 0; i < 10; i++) {
      StaticNetworkBuilder staticNetworkBuilder = new StaticNetworkBuilder();
      staticNetworkBuilder.createConnectionOfAgents(allPeople, testKit.getGlobals());
      Map<Long, Person.DailySchedule> schedules =
          staticNetworkBuilder.createPlacesAndPersonDailySchedules();
      Set<PlaceInfo> placeInfos = staticNetworkBuilder.getAllPlaces();
      if (previous != null) {
        for (Long idCurrent : schedules.keySet()) {
          assertThat(previous).containsKey(idCurrent);
          assertThat(schedules.get(idCurrent).placesAtStepMap()).isEqualTo(previous.get(idCurrent).placesAtStepMap());
          assertThat(schedules.get(idCurrent).isolationPlaces()).isEqualTo(schedules.get(idCurrent).isolationPlaces());
          assertThat(placeInfos).isEqualTo(previousPlaceInfos);
        }
      }
      previous = schedules;
      previousPlaceInfos = placeInfos;
    }
  }
}
