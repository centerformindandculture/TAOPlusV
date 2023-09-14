package tau;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import core.*;
import simudyne.core.abm.Group;
import simudyne.core.annotations.Input;
import simudyne.core.annotations.ModelSettings;
import simudyne.core.annotations.Variable;
import simudyne.core.rng.SeededRandom;
import tau.anylogic_code.StaticNetworkBuilder;

import java.lang.reflect.Field;
import java.util.*;

@ModelSettings(macroStep = 1, timeUnit = "DAYS", start = "2020-09-01T00:00:00Z")
public class TAUModel extends VIVIDCoreModel<Globals> {

  @Override
  public void init() {
    super.init();

    getGlobals().initBuildingInfectionArrays(PlaceType.values().length);


    /** Uncomment the following line to print out the output headers to update the csvRunner.py * */
    //System.out.println(getOutputHeaders(true));
    //System.out.println(constructCSVOutput());

  }


  @Override
  protected void registerPeopleAgentTypes() {
    registerAgentTypes(Student.class, Faculty.class, Staff.class);
  }

  @Override
  protected void setModules() {
    getGlobals().setModules(TAUModules.getInstance());
  }

  @Override
  protected List<Group<? extends Person>> generatePeople() {
    UniversityConfiguration universityConfiguration = getGlobals().getUniversityConfiguration();
    Group<Student> studentGroup = generateGroup(Student.class, universityConfiguration.numStudents());
    Group<Faculty> facultyGroup = generateGroup(Faculty.class, universityConfiguration.numFaculty());
    Group<Staff> staffGroup = generateGroup(Staff.class, universityConfiguration.numStaff());

    studentGroup.smallWorldConnected(50, 1.0, Links.SocialLink.class);

    return ImmutableList.of(
        studentGroup,
        facultyGroup,
        staffGroup);
  }

  protected void updatePerLocationInfectionData() {
    unknownPlaceInfectionRatioStep = ((getGlobals().tStep - getGlobals().buildingExcludeStepsCount.get(0)) != 0) ? getGlobals().buildingInfectionRatioStepSum.get(0) / (getGlobals().tStep - getGlobals().buildingExcludeStepsCount.get(0)) : 0;
    bathroomPlaceInfectionRatioStep = ((getGlobals().tStep - getGlobals().buildingExcludeStepsCount.get(1)) != 0) ? getGlobals().buildingInfectionRatioStepSum.get(1) / (getGlobals().tStep - getGlobals().buildingExcludeStepsCount.get(1)) : 0;
    buildingPlaceInfectionRatioStep = ((getGlobals().tStep - getGlobals().buildingExcludeStepsCount.get(2)) != 0) ? getGlobals().buildingInfectionRatioStepSum.get(2) / (getGlobals().tStep - getGlobals().buildingExcludeStepsCount.get(2)) : 0;
    campusEventPlaceInfectionRatioStep = ((getGlobals().tStep - getGlobals().buildingExcludeStepsCount.get(3)) != 0) ? getGlobals().buildingInfectionRatioStepSum.get(3) / (getGlobals().tStep - getGlobals().buildingExcludeStepsCount.get(3)) : 0;
    discCoursePlaceInfectionRatioStep = ((getGlobals().tStep - getGlobals().buildingExcludeStepsCount.get(4)) != 0) ? getGlobals().buildingInfectionRatioStepSum.get(4) / (getGlobals().tStep - getGlobals().buildingExcludeStepsCount.get(4)) : 0;
    nonDiscCoursePlaceInfectionRatioStep = ((getGlobals().tStep - getGlobals().buildingExcludeStepsCount.get(5)) != 0) ? getGlobals().buildingInfectionRatioStepSum.get(5) / (getGlobals().tStep - getGlobals().buildingExcludeStepsCount.get(5)) : 0;
    diningHallPlaceInfectionRatioStep = ((getGlobals().tStep - getGlobals().buildingExcludeStepsCount.get(6)) != 0) ? getGlobals().buildingInfectionRatioStepSum.get(6) / (getGlobals().tStep - getGlobals().buildingExcludeStepsCount.get(6)) : 0;
    floorPlaceInfectionRatioStep = ((getGlobals().tStep - getGlobals().buildingExcludeStepsCount.get(7)) != 0) ? getGlobals().buildingInfectionRatioStepSum.get(7) / (getGlobals().tStep - getGlobals().buildingExcludeStepsCount.get(7)) : 0;
    sportEventPlaceInfectionRatioStep = ((getGlobals().tStep - getGlobals().buildingExcludeStepsCount.get(8)) != 0) ? getGlobals().buildingInfectionRatioStepSum.get(8) / (getGlobals().tStep - getGlobals().buildingExcludeStepsCount.get(8)) : 0;
    staffToStudentPlaceInfectionRatioStep = ((getGlobals().tStep - getGlobals().buildingExcludeStepsCount.get(9)) != 0) ? getGlobals().buildingInfectionRatioStepSum.get(9) / (getGlobals().tStep - getGlobals().buildingExcludeStepsCount.get(9)) : 0;
    studentGroupPlaceInfectionRatioStep = ((getGlobals().tStep - getGlobals().buildingExcludeStepsCount.get(10)) != 0) ? getGlobals().buildingInfectionRatioStepSum.get(10) / (getGlobals().tStep - getGlobals().buildingExcludeStepsCount.get(10)) : 0;
    suitePlaceInfectionRatioStep = ((getGlobals().tStep - getGlobals().buildingExcludeStepsCount.get(11)) != 0) ? getGlobals().buildingInfectionRatioStepSum.get(11) / (getGlobals().tStep - getGlobals().buildingExcludeStepsCount.get(11)) : 0;
    fitnessPlaceInfectionRatioStep = ((getGlobals().tStep - getGlobals().buildingExcludeStepsCount.get(12)) != 0) ? getGlobals().buildingInfectionRatioStepSum.get(12) / (getGlobals().tStep - getGlobals().buildingExcludeStepsCount.get(12)) : 0;
    officePlaceInfectionRatioStep = ((getGlobals().tStep - getGlobals().buildingExcludeStepsCount.get(13)) != 0) ? getGlobals().buildingInfectionRatioStepSum.get(13) / (getGlobals().tStep - getGlobals().buildingExcludeStepsCount.get(13)) : 0;

    if ((getGlobals().tStep + 1) % getGlobals().tOneDay == 0) {
      unknownPlaceInfectionRatioDay = ((getGlobals().tStep - getGlobals().buildingExcludeDaysCount.get(0)) != 0) ? getGlobals().buildingInfectionRatioDaySum.get(0) / ((double) (getGlobals().tStep / getGlobals().tOneDay) - getGlobals().buildingExcludeDaysCount.get(0)) : 0;
      bathroomPlaceInfectionRatioDay = ((getGlobals().tStep - getGlobals().buildingExcludeDaysCount.get(1)) != 0) ? getGlobals().buildingInfectionRatioDaySum.get(1) / ((double) (getGlobals().tStep / getGlobals().tOneDay) - getGlobals().buildingExcludeDaysCount.get(1)) : 0;
      buildingPlaceInfectionRatioDay = ((getGlobals().tStep - getGlobals().buildingExcludeDaysCount.get(2)) != 0) ? getGlobals().buildingInfectionRatioDaySum.get(2) / ((double) (getGlobals().tStep / getGlobals().tOneDay) - getGlobals().buildingExcludeDaysCount.get(2)) : 0;
      campusEventPlaceInfectionRatioDay = ((getGlobals().tStep - getGlobals().buildingExcludeDaysCount.get(3)) != 0) ? getGlobals().buildingInfectionRatioDaySum.get(3) / ((double) (getGlobals().tStep / getGlobals().tOneDay) - getGlobals().buildingExcludeDaysCount.get(3)) : 0;
      discCoursePlaceInfectionRatioDay = ((getGlobals().tStep - getGlobals().buildingExcludeDaysCount.get(4)) != 0) ? getGlobals().buildingInfectionRatioDaySum.get(4) / ((double) (getGlobals().tStep / getGlobals().tOneDay) - getGlobals().buildingExcludeDaysCount.get(4)) : 0;
      nonDiscCoursePlaceInfectionRatioDay = ((getGlobals().tStep - getGlobals().buildingExcludeDaysCount.get(5)) != 0) ? getGlobals().buildingInfectionRatioDaySum.get(5) / ((double) (getGlobals().tStep / getGlobals().tOneDay) - getGlobals().buildingExcludeDaysCount.get(5)) : 0;
      diningHallPlaceInfectionRatioDay = ((getGlobals().tStep - getGlobals().buildingExcludeDaysCount.get(6)) != 0) ? getGlobals().buildingInfectionRatioDaySum.get(6) / ((double) (getGlobals().tStep / getGlobals().tOneDay) - getGlobals().buildingExcludeDaysCount.get(6)) : 0;
      floorPlaceInfectionRatioDay = ((getGlobals().tStep - getGlobals().buildingExcludeDaysCount.get(7)) != 0) ? getGlobals().buildingInfectionRatioDaySum.get(7) / ((double) (getGlobals().tStep / getGlobals().tOneDay) - getGlobals().buildingExcludeDaysCount.get(7)) : 0;
      sportEventPlaceInfectionRatioDay = ((getGlobals().tStep - getGlobals().buildingExcludeDaysCount.get(8)) != 0) ? getGlobals().buildingInfectionRatioDaySum.get(8) / ((double) (getGlobals().tStep / getGlobals().tOneDay) - getGlobals().buildingExcludeDaysCount.get(8)) : 0;
      staffToStudentPlaceInfectionRatioDay = ((getGlobals().tStep - getGlobals().buildingExcludeDaysCount.get(9)) != 0) ? getGlobals().buildingInfectionRatioDaySum.get(9) / ((double) (getGlobals().tStep / getGlobals().tOneDay) - getGlobals().buildingExcludeDaysCount.get(9)) : 0;
      studentGroupPlaceInfectionRatioDay = ((getGlobals().tStep - getGlobals().buildingExcludeDaysCount.get(10)) != 0) ? getGlobals().buildingInfectionRatioDaySum.get(10) / ((double) (getGlobals().tStep / getGlobals().tOneDay) - getGlobals().buildingExcludeDaysCount.get(10)) : 0;
      suitePlaceInfectionRatioDay = ((getGlobals().tStep - getGlobals().buildingExcludeDaysCount.get(11)) != 0) ? getGlobals().buildingInfectionRatioDaySum.get(11) / ((double) (getGlobals().tStep / getGlobals().tOneDay) - getGlobals().buildingExcludeDaysCount.get(11)) : 0;
      fitnessPlaceInfectionRatioDay = ((getGlobals().tStep - getGlobals().buildingExcludeDaysCount.get(12)) != 0) ? getGlobals().buildingInfectionRatioDaySum.get(12) / ((double) (getGlobals().tStep / getGlobals().tOneDay) - getGlobals().buildingExcludeDaysCount.get(12)) : 0;
      officePlaceInfectionRatioDay = ((getGlobals().tStep - getGlobals().buildingExcludeDaysCount.get(13)) != 0) ? getGlobals().buildingInfectionRatioDaySum.get(13) / ((double) (getGlobals().tStep / getGlobals().tOneDay) - getGlobals().buildingExcludeDaysCount.get(13)) : 0;
    }

    // Exclude staff to student connections for now
    int totalInfections = getGlobals().buildingTotalInfections.stream().mapToInt(a -> a).sum()
        - getGlobals().buildingTotalInfections.get(PlaceType.STAFF_TO_STUDENT.ordinal());
    int totalTraffic = getGlobals().buildingTotalPeople.stream().mapToInt(a -> a).sum()
        - getGlobals().buildingTotalPeople.get(PlaceType.STAFF_TO_STUDENT.ordinal());

    unknownPlaceInfectionPerc = getGlobals().buildingTotalInfections.get(0) / (double) totalInfections;
    bathroomPlaceInfectionPerc = getGlobals().buildingTotalInfections.get(1) / (double) totalInfections;
    buildingPlaceInfectionPerc = getGlobals().buildingTotalInfections.get(2) / (double) totalInfections;
    campusEventPlaceInfectionPerc = getGlobals().buildingTotalInfections.get(3) / (double) totalInfections;
    discCoursePlaceInfectionPerc = getGlobals().buildingTotalInfections.get(4) / (double) totalInfections;
    nonDiscCoursePlaceInfectionPerc = getGlobals().buildingTotalInfections.get(5) / (double) totalInfections;
    diningHallPlaceInfectionPerc = getGlobals().buildingTotalInfections.get(6) / (double) totalInfections;
    floorPlaceInfectionPerc = getGlobals().buildingTotalInfections.get(7) / (double) totalInfections;
    sportEventPlaceInfectionPerc = getGlobals().buildingTotalInfections.get(8) / (double) totalInfections;
    staffToStudentPlaceInfectionPerc = 0; // getGlobals().buildingTotalInfections.get(9) / (double) totalInfections;
    studentGroupPlaceInfectionPerc = getGlobals().buildingTotalInfections.get(10) / (double) totalInfections;
    suitePlaceInfectionPerc = getGlobals().buildingTotalInfections.get(11) / (double) totalInfections;
    fitnessPlaceInfectionPerc = getGlobals().buildingTotalInfections.get(12) / (double) totalInfections;
    officePlaceInfectionPerc = getGlobals().buildingTotalInfections.get(13) / (double) totalInfections;

    unknownPlaceTrafficPerc = getGlobals().buildingTotalPeople.get(0) / (double) totalTraffic;
    bathroomPlaceTrafficPerc = getGlobals().buildingTotalPeople.get(1) / (double) totalTraffic;
    buildingPlaceTrafficPerc = getGlobals().buildingTotalPeople.get(2) / (double) totalTraffic;
    campusEventPlaceTrafficPerc = getGlobals().buildingTotalPeople.get(3) / (double) totalTraffic;
    discCoursePlaceTrafficPerc = getGlobals().buildingTotalPeople.get(4) / (double) totalTraffic;
    nonDiscCoursePlaceTrafficPerc = getGlobals().buildingTotalPeople.get(5) / (double) totalTraffic;
    diningHallPlaceTrafficPerc = getGlobals().buildingTotalPeople.get(6) / (double) totalTraffic;
    floorPlaceTrafficPerc = getGlobals().buildingTotalPeople.get(7) / (double) totalTraffic;
    sportEventPlaceTrafficPerc = getGlobals().buildingTotalPeople.get(8) / (double) totalTraffic;
    staffToStudentPlaceTrafficPerc = 0; // getGlobals().buildingTotalPeople.get(9) / (double) totalTraffic;
    studentGroupPlaceTrafficPerc = getGlobals().buildingTotalPeople.get(10) / (double) totalTraffic;
    suitePlaceTrafficPerc = getGlobals().buildingTotalPeople.get(11) / (double) totalTraffic;
    fitnessPlaceTrafficPerc = getGlobals().buildingTotalPeople.get(12) / (double) totalTraffic;
    officePlaceTrafficPerc = getGlobals().buildingTotalPeople.get(13) / (double) totalTraffic;
  }

  /**
   * <PlaceType>InfectionRatioStep: This output is the average of the infection ratio for each place
   * over the simulation. The ratio is calculated as
   * (# of infected people in <PlaceType> at the end of the step - # of infected people in <PlaceType> at the beginning of the step) / # of infected people in <PlaceType> at the beginning of the step.
   * i.e. It represents the % increase of infected people in a PlaceType over a step.
   * The average excludes steps where no agent or no infected agents went to the PlaceType.
   */
  //@Variable
  public double unknownPlaceInfectionRatioStep = 0.0;
  //@Variable
  public double bathroomPlaceInfectionRatioStep = 0.0;
  //@Variable
  public double buildingPlaceInfectionRatioStep = 0.0;
  //@Variable
  public double campusEventPlaceInfectionRatioStep = 0.0;
  //@Variable
  public double discCoursePlaceInfectionRatioStep = 0.0;
  //@Variable
  public double nonDiscCoursePlaceInfectionRatioStep = 0.0;
  //@Variable
  public double diningHallPlaceInfectionRatioStep = 0.0;
  //@Variable
  public double floorPlaceInfectionRatioStep = 0.0;
  //@Variable
  public double sportEventPlaceInfectionRatioStep = 0.0;
  //@Variable
  public double staffToStudentPlaceInfectionRatioStep = 0.0;
  //@Variable
  public double studentGroupPlaceInfectionRatioStep = 0.0;
  //@Variable
  public double suitePlaceInfectionRatioStep = 0.0;
  //@Variable
  public double fitnessPlaceInfectionRatioStep = 0.0;
  //@Variable
  public double officePlaceInfectionRatioStep = 0.0;

  /**
   * <PlaceType>InfectionRatioDay: This output is the same as above, but calculated per day instead of per step.
   * If Globals#tOneDay is 1, this and the previous outputs are equal.
   */
  //@Variable
  public double unknownPlaceInfectionRatioDay = 0.0;
  //@Variable
  public double bathroomPlaceInfectionRatioDay = 0.0;
  //@Variable
  public double buildingPlaceInfectionRatioDay = 0.0;
  //@Variable
  public double campusEventPlaceInfectionRatioDay = 0.0;
  //@Variable
  public double discCoursePlaceInfectionRatioDay = 0.0;
  //@Variable
  public double nonDiscCoursePlaceInfectionRatioDay = 0.0;
  //@Variable
  public double diningHallPlaceInfectionRatioDay = 0.0;
  //@Variable
  public double floorPlaceInfectionRatioDay = 0.0;
  //@Variable
  public double sportEventPlaceInfectionRatioDay = 0.0;
  //@Variable
  public double staffToStudentPlaceInfectionRatioDay = 0.0;
  //@Variable
  public double studentGroupPlaceInfectionRatioDay = 0.0;
  //@Variable
  public double suitePlaceInfectionRatioDay = 0.0;
  //@Variable
  public double fitnessPlaceInfectionRatioDay = 0.0;
  //@Variable
  public double officePlaceInfectionRatioDay = 0.0;

  /**
   * <PlaceType>InfectionPerc: This output is the percentage of all infections that occurred at PlaceType.
   * E.g. if 8 infections happened at dining halls and 2 infections happened at courses,
   * diningHallInfectionPerc = 0.8 and courseInfectionPerc = 0.2.
   * This output is a little skewed at the moment because agents can be in multiple places at once and
   * if they get infected, it registers as an infection at each of those places.
   */
  //@Variable
  public double unknownPlaceInfectionPerc = 0.0;
  //@Variable
  public double bathroomPlaceInfectionPerc = 0.0;
  //@Variable
  public double buildingPlaceInfectionPerc = 0.0;
  //@Variable
  public double campusEventPlaceInfectionPerc = 0.0;
  //@Variable
  public double discCoursePlaceInfectionPerc = 0.0;
  //@Variable
  public double nonDiscCoursePlaceInfectionPerc = 0.0;
  //@Variable
  public double diningHallPlaceInfectionPerc = 0.0;
  //@Variable
  public double floorPlaceInfectionPerc = 0.0;
  //@Variable
  public double sportEventPlaceInfectionPerc = 0.0;
  //@Variable
  public double staffToStudentPlaceInfectionPerc = 0.0;
  //@Variable
  public double studentGroupPlaceInfectionPerc = 0.0;
  //@Variable
  public double suitePlaceInfectionPerc = 0.0;
  //@Variable
  public double fitnessPlaceInfectionPerc = 0.0;
  //@Variable
  public double officePlaceInfectionPerc = 0.0;

  /**
   * <PlaceType>TrafficPerc: This output s the percentage of all traffic that passed through PlaceType.
   * E.g. if over the course of the simulation, there were 60 visits to a bathroom (by 1 or more agents)
   * and 40 visits to sport events, bathroomTrafficPerc = 0.6 and sportEventTrafficPerc = 0.4.
   */
  //@Variable
  public double unknownPlaceTrafficPerc = 0.0;
  //@Variable
  public double bathroomPlaceTrafficPerc = 0.0;
  //@Variable
  public double buildingPlaceTrafficPerc = 0.0;
  //@Variable
  public double campusEventPlaceTrafficPerc = 0.0;
  //@Variable
  public double discCoursePlaceTrafficPerc = 0.0;
  //@Variable
  public double nonDiscCoursePlaceTrafficPerc = 0.0;
  //@Variable
  public double diningHallPlaceTrafficPerc = 0.0;
  //@Variable
  public double floorPlaceTrafficPerc = 0.0;
  //@Variable
  public double sportEventPlaceTrafficPerc = 0.0;
  //@Variable
  public double staffToStudentPlaceTrafficPerc = 0.0;
  //@Variable
  public double studentGroupPlaceTrafficPerc = 0.0;
  //@Variable
  public double suitePlaceTrafficPerc = 0.0;
  //@Variable
  public double fitnessPlaceTrafficPerc = 0.0;
  //@Variable
  public double officePlaceTrafficPerc = 0.0;

  public enum PlaceType {
    UNKNOWN,
    BATHROOM,
    BUILDING,
    CAMPUS_EVENT,
    DISC_COURSE,
    NON_DISC_COURSE,
    DINING_HALL,
    FLOOR,
    SPORT_EVENT,
    STAFF_TO_STUDENT,
    STUDENT_GROUP,
    SUITE,
    FITNESS,
    OFFICE
  }

  @Override
  protected void setupPlaces() {
  }

  private SortedSet<Field> getInputFields() {
    SortedSet<Field> inputFields = new TreeSet<>(new FieldComparator());
    Class<?> clazz = Globals.class;
    for (Field field : clazz.getDeclaredFields()) {
      if (field.isAnnotationPresent(Input.class)) {
        inputFields.add(field);
      }
    }
    return inputFields;
  }

  /**
   * This method is not intended to be used by the simulation. Rather, use this method to regenerate
   * the csv output headers and copy them to csvRunner.py.
   */
  private String getOutputHeaders() {
    return getOutputHeaders(false);
  }

  private String getOutputHeaders(boolean skipOutputs) {
    SortedSet<Field> inputFields = getInputFields();
    StringBuilder sb = new StringBuilder();
    boolean first = true;
    for (Field inputField : inputFields) {
      if (!first) {
        sb.append(',');
      }
      sb.append(inputField.getName());
      first = false;
    }

    if (skipOutputs) {
      return sb.toString();
    }

    // Output headers
    sb.append(",cumulativeInfections");
    sb.append(",peakNumInfected");
    sb.append(",totDeath");
    sb.append(",numSusceptible");
    sb.append(",percPeopleCausing80PercInfections");

    sb.append(",unknownPlaceInfectionRatioStep");
    sb.append(",bathroomPlaceInfectionRatioStep");
    sb.append(",buildingPlaceInfectionRatioStep");
    sb.append(",campusEventPlaceInfectionRatioStep");
    sb.append(",discCoursePlaceInfectionRatioStep");
    sb.append(",nonDiscCoursePlaceInfectionRatioStep");
    sb.append(",diningHallPlaceInfectionRatioStep");
    sb.append(",floorPlaceInfectionRatioStep");
    sb.append(",sportEventPlaceInfectionRatioStep");
    sb.append(",staffToStudentPlaceInfectionRatioStep");
    sb.append(",studentGroupPlaceInfectionRatioStep");
    sb.append(",suitePlaceInfectionRatioStep");
    sb.append(",fitnessPlaceInfectionRatioStep");
    sb.append(",officePlaceInfectionRatioStep");

    sb.append(",unknownPlaceInfectionRatioDay ");
    sb.append(",bathroomPlaceInfectionRatioDay");
    sb.append(",buildingPlaceInfectionRatioDay");
    sb.append(",campusEventPlaceInfectionRatioDay");
    sb.append(",discCoursePlaceInfectionRatioDay");
    sb.append(",nonDiscCoursePlaceInfectionRatioDay");
    sb.append(",diningHallPlaceInfectionRatioDay");
    sb.append(",floorPlaceInfectionRatioDay");
    sb.append(",sportEventPlaceInfectionRatioDay");
    sb.append(",staffToStudentPlaceInfectionRatioDay");
    sb.append(",studentGroupPlaceInfectionRatioDay");
    sb.append(",suitePlaceInfectionRatioDay");
    sb.append(",fitnessPlaceInfectionRatioDay");
    sb.append(",officePlaceInfectionRatioDay");

    sb.append(",unknownPlaceInfectionPerc");
    sb.append(",bathroomPlaceInfectionPerc");
    sb.append(",buildingPlaceInfectionPerc");
    sb.append(",campusEventPlaceInfectionPerc");
    sb.append(",discCoursePlaceInfectionPerc");
    sb.append(",nonDiscCoursePlaceInfectionPerc");
    sb.append(",diningHallPlaceInfectionPerc");
    sb.append(",floorPlaceInfectionPerc");
    sb.append(",sportEventPlaceInfectionPerc");
    sb.append(",staffToStudentPlaceInfectionPerc");
    sb.append(",studentGroupPlaceInfectionPerc");
    sb.append(",suitePlaceInfectionPerc");
    sb.append(",fitnessPlaceInfectionPerc");
    sb.append(",officePlaceInfectionPerc");

    sb.append(",unknownPlaceTrafficPerc");
    sb.append(",bathroomPlaceTrafficPerc");
    sb.append(",buildingPlaceTrafficPerc");
    sb.append(",campusEventPlaceTrafficPerc");
    sb.append(",discCoursePlaceTrafficPerc");
    sb.append(",nonDiscCoursePlaceTrafficPerc");
    sb.append(",diningHallPlaceTrafficPerc");
    sb.append(",floorPlaceTrafficPerc");
    sb.append(",sportEventPlaceTrafficPerc");
    sb.append(",staffToStudentPlaceTrafficPerc");
    sb.append(",studentGroupPlaceTrafficPerc");
    sb.append(",suitePlaceTrafficPerc");
    sb.append(",fitnessPlaceTrafficPerc");
    sb.append(",officePlaceTrafficPerc");

    sb.append('\n');
    return sb.toString();
  }

  @Override
  protected String constructCSVOutput() {
    SortedSet<Field> inputFields = getInputFields();
    Globals globals = getGlobals();
    StringBuilder sb = new StringBuilder();
    boolean first = true;
    for (Field inputField : inputFields) {
      if (!first) {
        sb.append(',');
      }
      try {
        sb.append(inputField.get(globals));
      } catch (IllegalAccessException e) {
        System.out.println("Could not access Globals field " + inputField.getName());
      }
      first = false;
    }

    // Outputs
    sb.append(',');
    sb.append(getCumulativeInfections());
    sb.append(',');
    sb.append(getPeakNumInfected());
    sb.append(',');
    sb.append(getLongAccumulator("totDead").value());
    sb.append(',');
    sb.append(getLongAccumulator("totSusceptible").value());
    sb.append(',');
    sb.append(getPercPeopleCausing80PercInfections());
    sb.append(',');

    sb.append(unknownPlaceInfectionRatioStep);
    sb.append(',');
    sb.append(bathroomPlaceInfectionRatioStep);
    sb.append(',');
    sb.append(buildingPlaceInfectionRatioStep);
    sb.append(',');
    sb.append(campusEventPlaceInfectionRatioStep);
    sb.append(',');
    sb.append(discCoursePlaceInfectionRatioStep);
    sb.append(',');
    sb.append(nonDiscCoursePlaceInfectionRatioStep);
    sb.append(',');
    sb.append(diningHallPlaceInfectionRatioStep);
    sb.append(',');
    sb.append(floorPlaceInfectionRatioStep);
    sb.append(',');
    sb.append(sportEventPlaceInfectionRatioStep);
    sb.append(',');
    sb.append(staffToStudentPlaceInfectionRatioStep);
    sb.append(',');
    sb.append(studentGroupPlaceInfectionRatioStep);
    sb.append(',');
    sb.append(suitePlaceInfectionRatioStep);
    sb.append(',');
    sb.append(fitnessPlaceInfectionRatioStep);
    sb.append(',');
    sb.append(officePlaceInfectionRatioStep);
    sb.append(',');

    sb.append(unknownPlaceInfectionRatioDay);
    sb.append(',');
    sb.append(bathroomPlaceInfectionRatioDay);
    sb.append(',');
    sb.append(buildingPlaceInfectionRatioDay);
    sb.append(',');
    sb.append(campusEventPlaceInfectionRatioDay);
    sb.append(',');
    sb.append(discCoursePlaceInfectionRatioDay);
    sb.append(',');
    sb.append(nonDiscCoursePlaceInfectionRatioDay);
    sb.append(',');
    sb.append(diningHallPlaceInfectionRatioDay);
    sb.append(',');
    sb.append(floorPlaceInfectionRatioDay);
    sb.append(',');
    sb.append(sportEventPlaceInfectionRatioDay);
    sb.append(',');
    sb.append(staffToStudentPlaceInfectionRatioDay);
    sb.append(',');
    sb.append(studentGroupPlaceInfectionRatioDay);
    sb.append(',');
    sb.append(suitePlaceInfectionRatioDay);
    sb.append(',');
    sb.append(fitnessPlaceInfectionRatioDay);
    sb.append(',');
    sb.append(officePlaceInfectionRatioDay);

    sb.append(',');
    sb.append(unknownPlaceInfectionPerc);
    sb.append(',');
    sb.append(bathroomPlaceInfectionPerc);
    sb.append(',');
    sb.append(buildingPlaceInfectionPerc);
    sb.append(',');
    sb.append(campusEventPlaceInfectionPerc);
    sb.append(',');
    sb.append(discCoursePlaceInfectionPerc);
    sb.append(',');
    sb.append(nonDiscCoursePlaceInfectionPerc);
    sb.append(',');
    sb.append(diningHallPlaceInfectionPerc);
    sb.append(',');
    sb.append(floorPlaceInfectionPerc);
    sb.append(',');
    sb.append(sportEventPlaceInfectionPerc);
    sb.append(',');
    sb.append(staffToStudentPlaceInfectionPerc);
    sb.append(',');
    sb.append(studentGroupPlaceInfectionPerc);
    sb.append(',');
    sb.append(suitePlaceInfectionPerc);
    sb.append(',');
    sb.append(fitnessPlaceInfectionPerc);
    sb.append(',');
    sb.append(officePlaceInfectionPerc);

    sb.append(',');
    sb.append(unknownPlaceTrafficPerc);
    sb.append(',');
    sb.append(bathroomPlaceTrafficPerc);
    sb.append(',');
    sb.append(buildingPlaceTrafficPerc);
    sb.append(',');
    sb.append(campusEventPlaceTrafficPerc);
    sb.append(',');
    sb.append(discCoursePlaceTrafficPerc);
    sb.append(',');
    sb.append(nonDiscCoursePlaceTrafficPerc);
    sb.append(',');
    sb.append(diningHallPlaceTrafficPerc);
    sb.append(',');
    sb.append(floorPlaceTrafficPerc);
    sb.append(',');
    sb.append(sportEventPlaceTrafficPerc);
    sb.append(',');
    sb.append(staffToStudentPlaceTrafficPerc);
    sb.append(',');
    sb.append(studentGroupPlaceTrafficPerc);
    sb.append(',');
    sb.append(suitePlaceTrafficPerc);
    sb.append(',');
    sb.append(fitnessPlaceTrafficPerc);
    sb.append(',');
    sb.append(officePlaceTrafficPerc);

    sb.append('\n');
    return sb.toString();
  }

  private static class FieldComparator implements Comparator<Field> {

    public int compare(Field f1, Field f2) {
      return (f1.getName().compareTo(f2.getName()));
    }
  }

  public static class TAUModules implements Modules {

    public static TAUModules getInstance() {
      return new TAUModules();
    }

    private static final DefaultModulesImpl delegate = DefaultModulesImpl.getInstance();

    @Override
    public InfectionTrajectoryDistribution getInfectionTrajectoryDistribution(
        Person person, Globals globals) {
      return delegate.getInfectionTrajectoryDistribution(person, globals);
    }

    private StaticNetworkBuilder builder = new StaticNetworkBuilder();

    @Override
    public long createConnectionOfAgents(List<Person> allPeople, Globals globals) {
      return builder.createConnectionOfAgents(allPeople, globals);
    }

    @Override
    public Map<Long, Person.DailySchedule> createPlacesAndPersonDailySchedules(
        Globals globals) {
      Map<Long, Person.DailySchedule> scheduleMap =
          builder.createPlacesAndPersonDailySchedules();

      Set<PlaceInfo> allPlaceInfos = builder.getAllPlaces();
      globals.uninitializedPlaceInfos.addAll(allPlaceInfos);

      builder.destroy();
      builder = null;
      return scheduleMap;
    }

    @Override
    public double getExternalInfectionRate(Person person, Globals globals) {
      if (globals.overallExternalInfectionRateFromData != 0) {
        return globals.overallExternalInfectionRateFromData * globals.baseInfectivity;
      }
      double sum = 0.0;
      return sum;
    }

    @Override
    public Set<Long> getAgentsToTest(
        Set<Long> symptomaticAgentsToday,
        Map<Long, Double> testSelectionMultipliers,
        SeededRandom random,
        Globals globals) {
      return delegate.getAgentsToTest(
          symptomaticAgentsToday, testSelectionMultipliers, random, globals);
    }

    public Set<Long> getAgentsToTest(
            Set<Long> symptomaticAgentsToday,
            Map<Long, Double> testSelectionMultipliers,
            SeededRandom random,
            long numTestsToRun) {
      return delegate.getAgentsToTest(
              symptomaticAgentsToday, testSelectionMultipliers, random, numTestsToRun);
    }

    private static final Set<Integer> CT_OMITTED_PLACE_TYPES =
        ImmutableSet.of(PlaceType.BUILDING.ordinal(),
            PlaceType.DINING_HALL.ordinal(),
            PlaceType.FITNESS.ordinal(),
            PlaceType.STAFF_TO_STUDENT.ordinal());

    @Override
    public Set<Integer> getPlaceTypesOmittedFromContactTracing() {
      return CT_OMITTED_PLACE_TYPES;
    }
  }
}
