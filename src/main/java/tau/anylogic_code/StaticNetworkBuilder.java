package tau.anylogic_code;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import core.Globals;
import core.Person;
import core.PlaceInfo;
import tau.*;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.lang.Math.ceil;
import static java.lang.Math.floor;

public class StaticNetworkBuilder {

  private final Random initializationRandom = new Random(1234);
  private LinkedHashMap<String, LinkedHashMap<StudentType, ArrayList<Student>>> students =
      new LinkedHashMap<>();
  private List<PersonWrapper> allPeople = new ArrayList<>();
  private Map<Long, PersonWrapper> peopleToPersonWrapperMap = new LinkedHashMap<>();
  private List<Course> classes = new ArrayList<>();
  private List<Course> discClasses = new ArrayList<>();
  private List<ConnectionOfAgents> allConnections = new ArrayList<>();
  private final double[] classScheduleDistribution = {0.100, 0.188, 0.712};
  private boolean splitClassSections = false;
  private final double percStudentsInGroup = 0.8;
  private final double studentGroupSizeMin = 10;
  private final double studentGroupSizeMax = 150;
  private final double studentGroupSizeMean = 30;
  private final double studentGroupSizeSD = 28;
  private Map<Long, Integer> personToNumEventsAssignments = new LinkedHashMap<>();
  private final int numStaffShareOffice = 10;
  private final double percStaffFacingStudents = 0.1;
  // TODO Make this dependent on IRU
  private final int timesPerWeekStudentInteractWithStaff = 100;
  private List<Building> housingBuildings = new ArrayList<>();
  // TODO Make this dependent on IRU
  private final int numDiningHalls = 3;
  private final double percLivingOffCampusExternallyInfectedPerDay = 0.3;
  private Set<PlaceInfo> allPlaceInfos = new LinkedHashSet<>();
  private long nextIdForConnectionOfAgents = 1;

  private Globals globals;

  private UniversityConfiguration universityConfiguration;

  private long getNextIdForConnectionOfAgents() {
    return nextIdForConnectionOfAgents++;
  }

  public long createConnectionOfAgents(List<Person> allPeople, Globals globals) {
    ConnectionOfAgents.tOneDay = globals.tOneDay;
    setPeopleTracking(allPeople);
    this.globals = globals;
    universityConfiguration = UniversityConfiguration.generate(globals);

    initializeSchool();
    return allConnections.size();
  }

  public Map<Long, Person.DailySchedule> createPlacesAndPersonDailySchedules() {

    return createDailySchedules();
  }

  /**
   * Release all references to model code.
   */
  public void destroy() {
    this.globals = null;
    this.allPeople = null;
    this.students = null;
    this.peopleToPersonWrapperMap = null;
    this.classes = null;
    this.discClasses = null;
    this.allConnections = null;
    this.personToNumEventsAssignments = null;
    this.housingBuildings = null;
    this.allPlaceInfos = null;
  }

  private void setPeopleTracking(List<Person> people) {
    for (Person p : people) {
      PersonWrapper pw = new PersonWrapper(p);
      allPeople.add(pw);
      peopleToPersonWrapperMap.put(p.getID(), pw);
    }
  }

  public Map<Long, Person.DailySchedule> createDailySchedules() {
    Map<Long, PersonWrapper> personWrapperMap = new LinkedHashMap<>();
    for (PersonWrapper pw : allPeople) {
      personWrapperMap.put(pw.person.personID, pw);
      for (int i = 0; i < 14 * globals.tOneDay; i++) {
        pw.placesAtStepMap.put(i, new ArrayList<>());
      }
    }

    Map<String, PlaceInfo> connectionOfAgentsToPlace = new LinkedHashMap<>();
    for (ConnectionOfAgents connectionOfAgents : allConnections) {
      PlaceInfo placeInfo = toPlace(connectionOfAgents);
      connectionOfAgentsToPlace.put(connectionOfAgents.getName() + connectionOfAgents._id, placeInfo);
      allPlaceInfos.add(placeInfo);
    }
    for (int i = 0; i < 14 * globals.tOneDay; i++) {
      for (ConnectionOfAgents connectionOfAgents : allConnections) {
        if (connectionOfAgents.isEventHappeningNow(i, globals.tOneDay)) {
          PlaceInfo placeInfo =
              connectionOfAgentsToPlace.get(connectionOfAgents.getName() + connectionOfAgents._id);
          for (Person p : connectionOfAgents.getPeople()) {
            personWrapperMap.get(p.personID).placesAtStepMap.get(i).add(placeInfo);
          }
          if (connectionOfAgents instanceof Course) {
            Course c = (Course) connectionOfAgents;
            c.sectionGoingToThisSession++;
          }
        }
      }
    }
    ImmutableMap.Builder<Long, Person.DailySchedule> toReturn = ImmutableMap.builder();
    personWrapperMap.forEach((id, pw) -> toReturn.put(id, pw.generateSchedule()));
    return toReturn.build();
  }

  public Set<PlaceInfo> getAllPlaces() {
    return ImmutableSet.copyOf(allPlaceInfos);
  }

  private PlaceInfo toPlace(ConnectionOfAgents connectionOfAgents) {
    if (connectionOfAgents instanceof StaffToStudent) {
      return PlaceInfo.create(
          connectionOfAgents.getName() + connectionOfAgents._id,
          getPlaceType(connectionOfAgents).ordinal(),
          PlaceInfo.NetworkType.STAR,
          ((StaffToStudent) connectionOfAgents).staff.personID,
          connectionOfAgents.people.size());
    }
    if (connectionOfAgents instanceof Course) {
      return PlaceInfo.create(
          connectionOfAgents.getName() + connectionOfAgents._id,
          getPlaceType(connectionOfAgents).ordinal(),
          PlaceInfo.NetworkType.FULLY_CONNECTED_DEPENDENT_ON_CENTER,
          ((Course) connectionOfAgents).instructor.personID,
          "Course_" + ((Course) connectionOfAgents).schedule.name(),
          connectionOfAgents.people.size());
    }

    return PlaceInfo.create(
        connectionOfAgents.getName() + connectionOfAgents._id,
        getPlaceType(connectionOfAgents).ordinal(),
            connectionOfAgents.people.size());
  }

  private TAUModel.PlaceType getPlaceType(ConnectionOfAgents connectionOfAgents) {
    if (connectionOfAgents instanceof Bathroom) {
      return TAUModel.PlaceType.BATHROOM;
    } else if (connectionOfAgents instanceof Building) {
      return TAUModel.PlaceType.BUILDING;
    } else if (connectionOfAgents instanceof CampusEvent) {
      return TAUModel.PlaceType.CAMPUS_EVENT;
    } else if (connectionOfAgents instanceof DiningHall) {
      return TAUModel.PlaceType.DINING_HALL;
    } else if (connectionOfAgents instanceof Floor) {
      return TAUModel.PlaceType.FLOOR;
    } else if (connectionOfAgents instanceof StudentGroup) {
      return TAUModel.PlaceType.STUDENT_GROUP;
    } else if (connectionOfAgents instanceof Suite) {
      return TAUModel.PlaceType.SUITE;
    } else if (connectionOfAgents instanceof StaffToStudent) {
      return TAUModel.PlaceType.STAFF_TO_STUDENT;
    } else if (connectionOfAgents instanceof Course) {
      if (connectionOfAgents.getName().startsWith("Discussion")) {
        return TAUModel.PlaceType.DISC_COURSE;
      } else if (connectionOfAgents.getName().startsWith("NonDiscussion")) {
        return TAUModel.PlaceType.NON_DISC_COURSE;
      } else {
        throw new IllegalStateException("Course name is not formatted as expected.");
      }
    } else {
      if (connectionOfAgents.getName().equals("Office")) {
        return TAUModel.PlaceType.OFFICE;
      } else if (connectionOfAgents.getName().equals("FitnessCenter")) {
        return TAUModel.PlaceType.FITNESS;
      }
    }
    throw new IllegalStateException("Invalid place.");
  }

  public int getScheduleCode() {
    double coin = initializationRandom.nextDouble();

    if (0 <= coin && coin < classScheduleDistribution[0]) {
      return 0;
    }
    if (classScheduleDistribution[0] <= coin && coin < classScheduleDistribution[1]) {
      return 1;
    }
    return 2;
  }

  public void initializeSchool() {
    initPeople(
        /*numPartTimeEachClass=*/ universityConfiguration.numPartTimeStudentsPerUGYear(),
        /*numFullTimeEachClass=*/ universityConfiguration.numFullTimeStudentsPerUGYear(),
        /*numGraduate=*/ universityConfiguration.numFullTimeGraduateStudents(),
        /*numFaculty=*/ universityConfiguration.numFaculty(),
        /*numStaff=*/ universityConfiguration.numStaff());
    initClasses(
        /*numDiscClasses=*/ universityConfiguration.numDiscussionClasses(),
        /*numNonDiscClasses=*/ universityConfiguration.numNonDiscussionClasses(),
        /*faculty teaching classes=*/ universityConfiguration.numFacultyWhoTeachNClasses());

    doClassAssignments(
        universityConfiguration.numbersOfDiscussionClassesTakenByPartTimeStudents(),
        universityConfiguration.numbersOfDiscussionClassesTakenByFullTimeStudents(),
        universityConfiguration.numbersOfNonDiscussionClassesTakenByPartTimeStudents(),
        universityConfiguration.numbersOfNonDiscussionClassesTakeByFullTimeStudents());

    doStudentHousing();

    makeFitness();

    makeStudentGroups();

    initEventsPerWeek(
        universityConfiguration.campusEventAttendanceDistributionStudents(),
        universityConfiguration.campusEventAttendanceDistributionFacultyAndStaff());
    makeEvents();
    makeStaffAssignments();

    makeDiningHalls();
  }

  public void makeBuilding(
      int numFloors,
      int[] numSuitesPerFloor,
      int numSuitesPerBathroom,
      boolean floorSharesBathroom,
      int[] suiteSize,
      List<PersonWrapper> paramStudentsPool,
      String buildingName) {
    List<PersonWrapper> studentsPool = new ArrayList<>(paramStudentsPool);
    int numBuildings = 0;
    while (!studentsPool.isEmpty()) {

      Building building = new Building(getNextIdForConnectionOfAgents());
      building.setName(buildingName + (numBuildings++));
      allConnections.add(building);
      housingBuildings.add(building);
      Collections.shuffle(studentsPool, initializationRandom);

      int numBathrooms = 0;
      for (int floorNum = 0; floorNum < numFloors && !studentsPool.isEmpty(); floorNum++) {
        // traceln("Floor " + floorNum);
        Floor floor = new Floor(getNextIdForConnectionOfAgents());
        floor.setName("Floor num " + floorNum + " of building " + building.getName());
        allConnections.add(floor);
        int numOfSuitesOnThisFloor = numSuitesPerFloor[floorNum % numSuitesPerFloor.length];
        List<Suite> suitesThatShareBathroom = new ArrayList<>();
        for (int suiteNum = 0;
             suiteNum < numOfSuitesOnThisFloor && !studentsPool.isEmpty();
             suiteNum++) {
          // traceln("Suite " + suiteNum);
          int sizeOfSuite = suiteSize[suiteNum % suiteSize.length];
          List<Person> suiteStudents = new ArrayList<>();
          Suite s = new Suite(floor, building, getNextIdForConnectionOfAgents());
          s.setName(
              "Suite " + suiteNum + " of floor " + floorNum + " of building " + building.getName());
          for (int i = 0; i < sizeOfSuite && !studentsPool.isEmpty(); i++) {
            PersonWrapper toAdd = studentsPool.remove(0);
            toAdd.addToInit(
                Student.class,
                student -> {
                  student.livesOnCampus = true;
                  student.livesAtBuilding = s.getName();
                });
            suiteStudents.add(toAdd.person);
          }
          // traceln("Size of people " + suiteStudents.size());
          allConnections.add(s);
          s.addPeople(suiteStudents);
          if (numSuitesPerBathroom == 1) {
            Bathroom bathroom = new Bathroom(getNextIdForConnectionOfAgents());
            bathroom.setName(
                "Bathroom num " + (numBathrooms++) + " of building " + building.getName());
            allConnections.add(bathroom);
            bathroom.addUsesBathroom(s);
          } else if (numSuitesPerBathroom > 0) {
            suitesThatShareBathroom.add(s);
            if (suitesThatShareBathroom.size() == numSuitesPerBathroom) {
              Bathroom bathroom = new Bathroom(getNextIdForConnectionOfAgents());
              bathroom.setName(
                  "Bathroom num " + (numBathrooms++) + " of building " + building.getName());
              allConnections.add(bathroom);
              bathroom.addAllUsesBathroom(suitesThatShareBathroom);
              suitesThatShareBathroom.clear();
            }
          }
          floor.addSuite(s);
        }
        if (floorSharesBathroom) {
          Bathroom bathroom = new Bathroom(getNextIdForConnectionOfAgents());
          bathroom.setName(
              "Bathroom num " + (numBathrooms++) + " of building " + building.getName());
          allConnections.add(bathroom);
          bathroom.addUsesBathroom(floor);
        }
        if (numSuitesPerBathroom > 0 && !suitesThatShareBathroom.isEmpty()) {
          Bathroom bathroom = new Bathroom(getNextIdForConnectionOfAgents());
          bathroom.setName(
              "Bathroom num " + (numBathrooms++) + " of building " + building.getName());
          allConnections.add(bathroom);
          bathroom.addAllUsesBathroom(suitesThatShareBathroom);
        }
        building.addFloor(floor);
      }
    }
  }

  public void doStudentHousing() {
    int[] numSuitesPerFloorRange1 = new int[]{8}; /*java.util.stream.IntStream.rangeClosed(
	studentHousingFloorNumSuitesRangeStart1, studentHousingFloorNumSuitesRangeEnd1)
	.toArray();*/

    Predicate<PersonWrapper> isFreshmanOrSophomore =
        pw -> {
          return students.get("full_time").get(StudentType.FRESHMAN).stream()
              .anyMatch(s -> s.personID == pw.person.personID)
              || students.get("full_time").get(StudentType.SOPHOMORE).stream()
              .anyMatch(s -> s.personID == pw.person.personID);
        };

    List<PersonWrapper> studentPool1 =
        new ArrayList<>(
            getPersonWrappers(Student.class).stream()
                .filter(isFreshmanOrSophomore)
                .collect(Collectors.toList()));
    // traceln("Student Pool 1 size " + studentPool1.size());
    Collections.shuffle(studentPool1, initializationRandom);
    studentPool1 =
        studentPool1.subList(
            0,
            (int)
                (universityConfiguration.percFullTimeStudentsWhoLiveOnCampus()
                    * studentPool1.size()));
    /*
    if (0 <= studentHousingFirstUsualStudentType1 && studentHousingFirstUsualStudentType1 < StudentType.values().length) {
    	studentTypes.add(StudentType.values()[studentHousingFirstUsualStudentType1]);
    }
    if (0 <= studentHousingSecondUsualStudentType1 && studentHousingSecondUsualStudentType1 < StudentType.values().length) {
    	studentTypes.add(StudentType.values()[studentHousingSecondUsualStudentType1]);
    }*/
    int[] suiteSize = new int[]{6}; /*java.util.stream.IntStream.rangeClosed(
	studentHousingSuiteSizeRangeStart1, studentHousingSuiteSizeRangeEnd1)
	.toArray();*/
    // for (int i = 0; i < numOfHousingType1; i++) {
    makeBuilding(
        10, // studentHousingBuildingNumFloors1,
        numSuitesPerFloorRange1,
        -1, // studentHousingNumSuitesPerBathroom1,
        // studentTypes,
        true,
        suiteSize,
        studentPool1,
        "BuildingType1_");
    // }

    Predicate<PersonWrapper> isJuniorOrSenior =
        pw -> {
          return students.get("full_time").get(StudentType.JUNIOR).stream()
              .anyMatch(s -> s.personID == pw.person.personID)
              || students.get("full_time").get(StudentType.SENIOR).stream()
              .anyMatch(s -> s.personID == pw.person.personID);
        };

    int[] numSuitesPerFloorRange2 = new int[]{1}; /*java.util.stream.IntStream.rangeClosed(
	studentHousingFloorNumSuitesRangeStart2, studentHousingFloorNumSuitesRangeEnd2)
	.toArray();*/
    List<PersonWrapper> studentPool2 =
        new ArrayList<>(
            getPersonWrappers(Student.class).stream()
                .filter(isJuniorOrSenior)
                .collect(Collectors.toList()));
    Collections.shuffle(studentPool2, initializationRandom);
    studentPool2 =
        studentPool2.subList(
            0,
            (int)
                (universityConfiguration.percFullTimeStudentsWhoLiveOnCampus()
                    * studentPool2.size()));
    suiteSize = new int[]{3}; /*java.util.stream.IntStream.rangeClosed(
	studentHousingSuiteSizeRangeStart2, studentHousingSuiteSizeRangeEnd2)
	.toArray();*/
    // for (int i = 0; i < numOfHousingType2; i++) {
    makeBuilding(
        3, // studentHousingBuildingNumFloors2,
        numSuitesPerFloorRange2,
        1, // studentHousingNumSuitesPerBathroom2,
        // studentTypes,
        false,
        suiteSize,
        studentPool2,
        "BuildingType2_");
    // }
  }

  public void makeDiningHalls() {
    int buildingIndex = 0;
    int approxNumPeoplePerDiningHall =
        (int)
            (getAll(Student.class).stream().filter(p -> p.livesOnCampus).count()
                / ((double) numDiningHalls))
            + 1;
    int numBuildingsPerDiningHall =
        (int) ceil((((double) housingBuildings.size()) / numDiningHalls));

    int diningHallNum = 0;
    Collections.shuffle(housingBuildings, initializationRandom);
    while (buildingIndex < housingBuildings.size()) {
      DiningHall hall = new DiningHall(getNextIdForConnectionOfAgents());
      hall.setName("Dining Hall " + (diningHallNum++));
      allConnections.add(hall);
      int numPeopleAssignedToCurrentDiningHall = 0;
      while (numPeopleAssignedToCurrentDiningHall < approxNumPeoplePerDiningHall
          && buildingIndex < housingBuildings.size()) {
        hall.addBuilding(housingBuildings.get(buildingIndex));
        numPeopleAssignedToCurrentDiningHall +=
            housingBuildings.get(buildingIndex).getPeople().size();
        buildingIndex++;
      }
    }
  }

  public void makeStaffAssignments() {
    // traceln("All staff size " + allStaff.size());
    List<Staff> allStaff = new ArrayList<>(getAll(Staff.class));
    Collections.shuffle(allStaff, initializationRandom);

    while (!allStaff.isEmpty()) {
      List<Person> staffSharingOfficeAndBathroom = new ArrayList<>();
      for (int i = 0; i < numStaffShareOffice && !allStaff.isEmpty(); i++) {
        staffSharingOfficeAndBathroom.add(allStaff.remove(0));
      }
      if (staffSharingOfficeAndBathroom.size() == 1) {
        continue;
      }

      ConnectionOfAgents office = new ConnectionOfAgents(getNextIdForConnectionOfAgents());
      office.setName("Office");
      office.addPeople(staffSharingOfficeAndBathroom);
      allConnections.add(office);
    }

    List<PersonWrapper> studentFacingStaff = new ArrayList<>(getPersonWrappers(Staff.class));
    Collections.shuffle(studentFacingStaff, initializationRandom);
    // traceln("StudentFacing staff size " + studentFacingStaff.size());
    studentFacingStaff =
        studentFacingStaff.subList(0, (int) (percStaffFacingStudents * studentFacingStaff.size()));
    // traceln("timesPerWeekStudentInteractWithStaff="+timesPerWeekStudentInteractWithStaff);
    List<Person> allStudents = getPeople(Student.class);
    for (PersonWrapper stf : studentFacingStaff) {
      StaffToStudent studentFacingStaffAndStudents =
          new StaffToStudent(1, (Staff) stf.person, timesPerWeekStudentInteractWithStaff, getNextIdForConnectionOfAgents());
      studentFacingStaffAndStudents.setName("staff_and_student");
      studentFacingStaffAndStudents.addPeople(allStudents);
      allConnections.add(studentFacingStaffAndStudents);
      stf.addToInit(
          Staff.class,
          staff -> {
            staff.isStaffWithStudentFacingJob = true;
          });
    }
  }

  public void makeEvents() {
    int eventId = 0;
    int offset = 0;

    Map<Long, Integer> personToNumEventsAssignedMap = new LinkedHashMap<>();
    allPeople.forEach(pw -> personToNumEventsAssignedMap.put(pw.person.personID, 0));

    while (true) {
      List<Person> allEventPeople =
          new ArrayList<>(
              allPeople.stream()
                  .map(pw -> pw.person)
                  .filter(
                      person ->
                          personToNumEventsAssignedMap.get(person.personID)
                              < personToNumEventsAssignments.get(person.personID))
                  .collect(Collectors.toList()));
      if (allEventPeople.isEmpty()) {
        break;
      }
      Collections.shuffle(allEventPeople, initializationRandom);

      int eventSize = (int) normal(10, 100, 50, 20);
      List<Person> peopleForThisEvent;
      if (eventSize <= allEventPeople.size()) {
        peopleForThisEvent = allEventPeople.subList(0, eventSize);
      } else {
        peopleForThisEvent = allEventPeople;
      }
      peopleForThisEvent.forEach(
          p ->
              personToNumEventsAssignedMap.put(
                  p.personID, personToNumEventsAssignedMap.get(p.personID) + 1));
      CampusEvent e = new CampusEvent(offset, getNextIdForConnectionOfAgents());
      e.setName("event" + (eventId++));
      e.addPeople(peopleForThisEvent);
      allConnections.add(e);
      offset++;
    }
  }

  public void assignEventsPerWeek(List<Person> pop, ImmutableList<Double> dist) {
    List<Person> population = new ArrayList<>();
    population.addAll(pop);
    Collections.shuffle(pop, initializationRandom);

    for (Person p : population) {
      personToNumEventsAssignments.put(p.personID, getTimesPerWeek(dist));
    }
  }

  private int getTimesPerWeek(ImmutableList<Double> distribution) {
    double draw = initializationRandom.nextDouble();

    double cumulativeSum = 1 - (distribution.stream().mapToDouble(d -> d).sum());
    if (cumulativeSum >= draw) {
      return 0;
    }

    for (int i = 0; i < distribution.size(); i++) {
      cumulativeSum += distribution.get(i);
      if (cumulativeSum >= draw) {
        return i;
      }
    }
    throw new IllegalStateException("Given invalid distribution: " + distribution);
  }

  public void initEventsPerWeek(
      ImmutableList<Double> studentDist, ImmutableList<Double> facAndStaffDist) {
    assignEventsPerWeek(getPeople(Student.class), studentDist);

    List<Person> facAndStaff = new ArrayList<>();
    facAndStaff.addAll(getPeople(Faculty.class));
    facAndStaff.addAll(getPeople(Staff.class));
    assignEventsPerWeek(facAndStaff, facAndStaffDist);
  }

  public void makeStudentGroups() {
    List<Student> allStudents = new ArrayList<>();
    allStudents.addAll(
        allPeople.stream()
            .filter(pw -> pw.person instanceof Student)
            .map(pw -> (Student) pw.person)
            .collect(Collectors.toList()));
    Collections.shuffle(allStudents, initializationRandom);

    int numStudentsInGroups = 0;
    int numberOfStudentsInGroups = (int) (percStudentsInGroup * allStudents.size());
    int numGroups = 0;
    while (numStudentsInGroups < numberOfStudentsInGroups) {
      int groupSize =
          (int)
              floor(
                  normal(
                      studentGroupSizeMin,
                      studentGroupSizeMax,
                      studentGroupSizeMean,
                      studentGroupSizeSD));
      numStudentsInGroups += groupSize;
      StudentGroup group = new StudentGroup(getNextIdForConnectionOfAgents());
      group.setName("Student group " + (numGroups++));
      for (int i = 0; i < numStudentsInGroups && !allStudents.isEmpty(); i++) {
        group.addPerson(allStudents.remove(0));
      }
      allConnections.add(group);
    }
  }

  private double normal(double min, double max, double mean, double sd) {
    do {
      double gaussian = initializationRandom.nextGaussian();
      double transformed = gaussian * sd + mean;
      if (min <= transformed && transformed <= max) {
        return transformed;
      }
    } while (true);
  }

  // TODO Add fitness distribution to university person init
  public void makeFitness() {
    ConnectionOfAgents fitnessCenter = new ConnectionOfAgents(getNextIdForConnectionOfAgents());
    fitnessCenter.setName("FitnessCenter");
    allConnections.add(fitnessCenter);

    allPeople.stream().map(pw -> pw.person).forEach(fitnessCenter::addPerson);
  }

  public void assignStudentsToClasses(
      ImmutableList<Integer> numClasses, boolean partTime, boolean discussion, StudentType type) {
    if (numClasses.isEmpty()) {
      return;
    }

    List<Student> studentPool =
        ImmutableList.sortedCopyOf(
            Comparator.comparingLong(Student::getID),
            students.get((partTime ? "part_time" : "full_time")).get(type));

    int numClassesIndex = 0;
    for (Student s : studentPool) {
      List<Course> classesPool =
          ImmutableList.sortedCopyOf(
              (c1, c2) -> {
                int doubleCompare = Double.compare(c1.getNumPeople(), c2.getNumPeople());
                if (doubleCompare == 0) {
                  return Long.compare(c1._id, c2._id);
                }
                return doubleCompare;
              },
              classes);
      int classesTaken = 0;
      int i = 0;
      while (classesTaken != numClasses.get(numClassesIndex % numClasses.size())) {
        Course c = classesPool.get(i);
        if (c.instructor != s && !c.containsPerson(s)) {
          c.addPerson(s);
          // s.classesTaking.add(c);
          classesTaken++;
        }
        i++;
      }
      numClassesIndex++;
    }

    // AverageNonDiscClassSize = classes.stream()
    //   .map(c -> c.getPeople().size()).mapToDouble(s ->
    // Double.valueOf(s)).average().getAsDouble();
  }

  public void doClassAssignments(
      ImmutableList<ImmutableList<Integer>> partTimeDiscClasses,
      ImmutableList<ImmutableList<Integer>> fullTimeDiscClasses,
      ImmutableList<ImmutableList<Integer>> partTimeNonDiscClasses,
      ImmutableList<ImmutableList<Integer>> fullTimeNonDiscClasses) {
    for (StudentType type : StudentType.values()) {
      assignStudentsToClasses(partTimeDiscClasses.get(type.ordinal()), true, true, type);
      assignStudentsToClasses(partTimeNonDiscClasses.get(type.ordinal()), true, false, type);

      assignStudentsToClasses(fullTimeDiscClasses.get(type.ordinal()), false, true, type);
      assignStudentsToClasses(fullTimeNonDiscClasses.get(type.ordinal()), false, false, type);
    }
  }

  public void initClasses(
      int numDiscClasses, int numNonDiscClasses, ImmutableList<Integer> numFacultyTeachingClasses) {
    List<Student> grads = new ArrayList<>();
    grads.addAll(students.get("full_time").get(StudentType.GRADUATE));
    Collections.shuffle(grads, initializationRandom);
    long courseOffset = 0;
    for (int i = 0; i < numDiscClasses; i++) {
      if (i == grads.size()) {
        throw new IllegalStateException("Not enough grads for discussion classes.");
      }
      Course c = new Course(getScheduleCode(), splitClassSections, getStepInDay(), courseOffset++);
      c.setName("Discussion Class " + i);
      c.addInstructor(grads.get(i));
      discClasses.add(c);
    }

    int numNonDiscussionClassesAdded = 0;
    Iterator<Faculty> faculty =
        allPeople.stream()
            .filter(personWrapper -> personWrapper.person instanceof Faculty)
            .map(pw -> (Faculty) pw.person)
            .iterator();
    for (int i = 0; i < numFacultyTeachingClasses.size(); i++) {
      List<Faculty> facultyNotTeaching = new ArrayList<>();
      FACULTY_BUILD_LOOP:
      while (faculty.hasNext()) {
        if (facultyNotTeaching.size() >= numFacultyTeachingClasses.get(i)) {
          break FACULTY_BUILD_LOOP;
        }
        Faculty f = faculty.next();
        facultyNotTeaching.add(f);
      }

      int classNum = 0;
      for (Faculty f : facultyNotTeaching) {
        for (int j = 0; j <= i; j++) {
          Course c = new Course(getScheduleCode(), splitClassSections, getStepInDay(), courseOffset++);
          c.setName("NonDiscussion Class " + i + " " + (classNum++));
          c.addInstructor(f);
          classes.add(c);
        }
      }
    }

    allConnections.addAll(classes);
    allConnections.addAll(discClasses);
  }

  public void initPeople(
      int numPartTimeEachClass,
      int numFullTimeEachClass,
      int numGraduate,
      int numFaculty,
      int numStaff) {
    students.put("part_time", new LinkedHashMap<>());
    students.put("full_time", new LinkedHashMap<>());

    for (StudentType type : StudentType.values()) {
      students.get("part_time").put(type, new ArrayList<>());
      students.get("full_time").put(type, new ArrayList<>());
    }

    Iterator<PersonWrapper> allStudents =
        allPeople.stream()
            .filter(personWrapper -> personWrapper.person instanceof Student)
            .iterator();

    for (StudentType type : StudentType.values()) {
      if (type == StudentType.GRADUATE) {
        continue;
      }
      for (int i = 0; i < numFullTimeEachClass; i++) {
        PersonWrapper pw = allStudents.next();
        pw.addToInit(
            Student.class,
            s -> {
              s.type = type;
              s.isPartTime = false;
            });
        students.get("full_time").get(type).add((Student) pw.person);
      }
      for (int i = 0; i < numPartTimeEachClass; i++) {
        PersonWrapper pw = allStudents.next();
        pw.addToInit(
            Student.class,
            s -> {
              s.type = type;
              s.isPartTime = true;
            });
        students.get("part_time").get(type).add((Student) pw.person);
      }
    }

    for (int i = 0; i < numGraduate; i++) {
      PersonWrapper pw = allStudents.next();
      pw.addToInit(
          Student.class,
          s -> {
            s.type = StudentType.GRADUATE;
            s.isPartTime = false;
          });
      students.get("full_time").get(StudentType.GRADUATE).add((Student) pw.person);
    }

    int typeIndex = 0;
    while (allStudents.hasNext()) {
      PersonWrapper pw = allStudents.next();
      final int tmpTypeIndex = typeIndex;
      pw.addToInit(
          Student.class,
          s -> {
            s.type = StudentType.values()[tmpTypeIndex % StudentType.values().length];
            s.isPartTime = false;
          });
      students.get("full_time").get(typeIndex).add((Student) pw.person);
      typeIndex++;
    }
  }

  private <T> List<T> getAll(Class<T> clazz) {
    return allPeople.stream()
        .filter(pw -> clazz.isInstance(pw.person))
        .map(pw -> clazz.cast(pw.person))
        .collect(Collectors.toList());
  }

  private <T> List<Person> getPeople(Class<T> clazz) {
    return allPeople.stream()
        .filter(pw -> clazz.isInstance(pw.person))
        .map(pw -> pw.person)
        .collect(Collectors.toList());
  }

  private int getStepInDay() {
    return initializationRandom.nextInt(globals.tOneDay);
  }

  private <T> List<PersonWrapper> getPersonWrappers(Class<T> clazz) {
    return allPeople.stream()
        .filter(pw -> clazz.isInstance(pw.person))
        .collect(Collectors.toList());
  }

  private static class PersonWrapper {
    final Map<Integer, List<PlaceInfo>> placesAtStepMap = new LinkedHashMap<>();
    final List<PlaceInfo> isolationPlaceInfos = new ArrayList<>();
    final List<Consumer<Person>> secondaryInitialziation = new ArrayList<>();
    final Person person;

    PersonWrapper(Person person) {
      this.person = person;
    }

    Person.DailySchedule generateSchedule() {
      return Person.DailySchedule.create(
          ImmutableMap.copyOf(placesAtStepMap),
          ImmutableList.copyOf(isolationPlaceInfos),
          personToInit -> {
            secondaryInitialziation.forEach(consumer -> consumer.accept(personToInit));
          });
    }

    void addToInit(Consumer<Person> init) {
      secondaryInitialziation.add(init);
    }

    <T> void addToInit(Class<T> clazz, Consumer<T> init) {
      secondaryInitialziation.add(
          person -> {
            T personCastAs = clazz.cast(person);
            init.accept(personCastAs);
          });
    }
  }
}
