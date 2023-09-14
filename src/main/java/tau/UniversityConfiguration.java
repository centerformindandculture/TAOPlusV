package tau;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import core.Globals;
import tau.anylogic_code.StudentType;

/**
 * A value class which contains all information about university initialization.
 */
@AutoValue
public abstract class UniversityConfiguration {

  public abstract int numPartTimeStudentsPerUGYear();

  public abstract int numFullTimeStudentsPerUGYear();

  public abstract int numFullTimeGraduateStudents();

  public abstract int numFaculty();

  public abstract int numStaff();

  /**
   * format arr[i] is the numbers of discussion classes taken by part time students with the
   * StudentType ordinal of i. Example: arr = {{1,2},{3,4}} says FRESHMAN (ordinal 0) take either 1
   * or 2 classes, and SOPHOMORE (ordinal 1) takes either 3 or 4 classes.
   */
  public abstract ImmutableList<ImmutableList<Integer>>
  numbersOfDiscussionClassesTakenByPartTimeStudents();

  /**
   * Same format as {@link #numbersOfDiscussionClassesTakenByPartTimeStudents()}
   */
  public abstract ImmutableList<ImmutableList<Integer>>
  numbersOfDiscussionClassesTakenByFullTimeStudents();

  /**
   * Same format as {@link #numbersOfDiscussionClassesTakenByPartTimeStudents()}
   */
  public abstract ImmutableList<ImmutableList<Integer>>
  numbersOfNonDiscussionClassesTakenByPartTimeStudents();

  /**
   * Same format as {@link #numbersOfDiscussionClassesTakenByPartTimeStudents()}
   */
  public abstract ImmutableList<ImmutableList<Integer>>
  numbersOfNonDiscussionClassesTakeByFullTimeStudents();

  // Discussion classes are be taught by graduate students
  public abstract int numDiscussionClasses();

  public abstract int numNonDiscussionClasses();

  // Index 0 == number of faculty who teach zero classes, etc.
  public abstract ImmutableList<Integer> numFacultyWhoTeachNClasses();

  public abstract int numberOfStaffSharingOfficeAndBathroom();

  public abstract double percentageOfStaffWithStudentFacingJob();

  public abstract ImmutableList<StudentHousingBuildingProfile> buildingProfiles();

  /**
   * The attendance distribution of students for campus events.
   *
   * <p>Format is arr[i] = % of students who go to i campus events per week This is a probabilty
   * distribution, and elements must sum to 1.
   */
  public abstract ImmutableList<Double> campusEventAttendanceDistributionStudents();

  /**
   * The attendance distribution of faculty and staff for campus events.
   *
   * <p>Format is arr[i] = % of faculty/staff who go to i campus events per week This is a
   * probabilty distribution, and elements must sum to 1.
   */
  public abstract ImmutableList<Double> campusEventAttendanceDistributionFacultyAndStaff();

  public abstract double percStudentsWhoAttendSportsEvent();

  public abstract double percentageOfStudentsInStudentGroup();

  public abstract int studentGroupMinSize();

  public abstract int studentGroupMaxSize();

  public abstract int studentGroupMean();

  public abstract int studentGroupSD();

  public abstract int campusEventMinSize();

  public abstract int campusEventMaxSize();

  public abstract int campusEventMean();

  public abstract int campusEventSD();

  public abstract int timesPerDayStaffInteractWithStudent();

  public abstract int numDiningHalls();

  public abstract double percFullTimeStudentsWhoLiveOnCampus();

  public int numStudents() {
    return numFullTimeGraduateStudents()
        + (numFullTimeStudentsPerUGYear() * 4)
        + (numPartTimeStudentsPerUGYear() * 4);
  }

  public static Builder builder() {
    return new AutoValue_UniversityConfiguration.Builder();
  }

  /**
   * Sets up a builder with some common configurations that is used multiple times.
   */
  private static Builder builderWithCommonConfiguration(Globals globals) {
    return builder()
        .buildingProfiles(
            ImmutableList.of(
                StudentHousingBuildingProfile.builder()
                    .numSuitesPerFloor(8)
                    .residentTypes(ImmutableSet.of(StudentType.FRESHMAN, StudentType.SOPHOMORE))
                    .suiteSize(6)
                    .numFloors(10)
                    .numSuitesPerBathroom(-1)
                    .build(),
                StudentHousingBuildingProfile.builder()
                    .numSuitesPerFloor(1)
                    .residentTypes(ImmutableSet.of(StudentType.JUNIOR, StudentType.SENIOR))
                    .suiteSize(3)
                    .numFloors(3)
                    .numSuitesPerBathroom(1)
                    .build()))
        .percentageOfStudentsInStudentGroup(0.8)
        .studentGroupMinSize(10)
        .studentGroupMaxSize(150)
        .studentGroupMean(30)
        .studentGroupSD(28)
        .campusEventAttendanceDistributionStudents(
            ImmutableList.of(0.4, 0.15, 0.03, 0.02, 0.0, 0.0, 0.0))
        .campusEventAttendanceDistributionFacultyAndStaff(ImmutableList.of(0.8, 0.15, 0.05))
        .campusEventMinSize(10)
        .campusEventMaxSize(100)
        .campusEventMean(50)
        .campusEventSD(20)
        .percentageOfStaffWithStudentFacingJob(0.1)
        .timesPerDayStaffInteractWithStudent(100)
        .numberOfStaffSharingOfficeAndBathroom(10)
        .numDiningHalls(3)
        .percStudentsWhoAttendSportsEvent(1.0 / 30)
        .percFullTimeStudentsWhoLiveOnCampus(0.8);
  }

  /**
   * Uses {@code globals} to generate a {@link UniversityConfiguration}.
   */
  public static UniversityConfiguration generate(Globals globals) {
    if (globals.getUniversityProfile() == Globals.UniversityProfile.LARGE) {
      return builderWithCommonConfiguration(globals)
          .numPartTimeStudentsPerUGYear(8640 / 4)
          .numFullTimeStudentsPerUGYear(15360 / 4)
          .numFullTimeGraduateStudents(6000)
          .numFaculty(2400)
          .numStaff(4800)
          .numDiscussionClasses(700)
          .numNonDiscussionClasses(3500)
          .numFacultyWhoTeachNClasses(ImmutableList.of(1500, 2000))
          .numbersOfDiscussionClassesTakenByPartTimeStudents(
              ImmutableList.of(
                  ImmutableList.of(2),
                  ImmutableList.of(1, 2),
                  ImmutableList.of(1),
                  ImmutableList.of(0, 1),
                  ImmutableList.of()))
          .numbersOfDiscussionClassesTakenByFullTimeStudents(
              ImmutableList.of(
                  ImmutableList.of(4),
                  ImmutableList.of(3),
                  ImmutableList.of(2),
                  ImmutableList.of(1),
                  ImmutableList.of(2)))
          .numbersOfNonDiscussionClassesTakenByPartTimeStudents(
              ImmutableList.of(
                  ImmutableList.of(2),
                  ImmutableList.of(2),
                  ImmutableList.of(2),
                  ImmutableList.of(2),
                  ImmutableList.of()))
          .numbersOfNonDiscussionClassesTakeByFullTimeStudents(
              ImmutableList.of(
                  ImmutableList.of(4),
                  ImmutableList.of(4),
                  ImmutableList.of(4),
                  ImmutableList.of(4),
                  ImmutableList.of(3)))
          .build();
    } else if (globals.getUniversityProfile() == Globals.UniversityProfile.SMALL) {
      return builderWithCommonConfiguration(globals)
          .numPartTimeStudentsPerUGYear(1800 / 4)
          .numFullTimeStudentsPerUGYear(3200 / 4)
          .numFullTimeGraduateStudents(0)
          .numFaculty(500)
          .numStaff(1000)
          .numDiscussionClasses(0)
          .numNonDiscussionClasses(1500)
          .numFacultyWhoTeachNClasses(ImmutableList.of(0, 500, 1000))
          .numbersOfDiscussionClassesTakenByPartTimeStudents(
              ImmutableList.of(
                  ImmutableList.of(2),
                  ImmutableList.of(1, 2),
                  ImmutableList.of(1),
                  ImmutableList.of(0, 1),
                  ImmutableList.of()))
          .numbersOfDiscussionClassesTakenByFullTimeStudents(
              ImmutableList.of(
                  ImmutableList.of(4),
                  ImmutableList.of(3),
                  ImmutableList.of(2),
                  ImmutableList.of(1),
                  ImmutableList.of()))
          .numbersOfNonDiscussionClassesTakenByPartTimeStudents(
              ImmutableList.of(
                  ImmutableList.of(2),
                  ImmutableList.of(2),
                  ImmutableList.of(2),
                  ImmutableList.of(2),
                  ImmutableList.of()))
          .numbersOfNonDiscussionClassesTakeByFullTimeStudents(
              ImmutableList.of(
                  ImmutableList.of(4),
                  ImmutableList.of(4),
                  ImmutableList.of(4),
                  ImmutableList.of(4),
                  ImmutableList.of()))
          .build();
    } else if (globals.getUniversityProfile() == Globals.UniversityProfile.VERY_SMALL) {
      return builderWithCommonConfiguration(globals)
          .numPartTimeStudentsPerUGYear(40 / 4)
          .numFullTimeStudentsPerUGYear(100 / 4)
          .numFullTimeGraduateStudents(0)
          .numFaculty(40)
          .numStaff(100)
          .numDiscussionClasses(0)
          .numNonDiscussionClasses(100)
          .numFacultyWhoTeachNClasses(ImmutableList.of(0, 33, 64))
          .numbersOfDiscussionClassesTakenByPartTimeStudents(
              ImmutableList.of(
                  ImmutableList.of(2),
                  ImmutableList.of(1, 2),
                  ImmutableList.of(1),
                  ImmutableList.of(0, 1),
                  ImmutableList.of()))
          .numbersOfDiscussionClassesTakenByFullTimeStudents(
              ImmutableList.of(
                  ImmutableList.of(4),
                  ImmutableList.of(3),
                  ImmutableList.of(2),
                  ImmutableList.of(1),
                  ImmutableList.of()))
          .numbersOfNonDiscussionClassesTakenByPartTimeStudents(
              ImmutableList.of(
                  ImmutableList.of(2),
                  ImmutableList.of(2),
                  ImmutableList.of(2),
                  ImmutableList.of(2),
                  ImmutableList.of()))
          .numbersOfNonDiscussionClassesTakeByFullTimeStudents(
              ImmutableList.of(
                  ImmutableList.of(4),
                  ImmutableList.of(4),
                  ImmutableList.of(4),
                  ImmutableList.of(4),
                  ImmutableList.of()))
          .build();
    } else if (globals.getUniversityProfile() == Globals.UniversityProfile.SCALABLE) {
      int nPTStudentsPerUGYear;
      int nFTStudentsPerUGYear;
      int nGradStudents;
      int nFaculty;
      int nStaff;
      double scaleAgainstLargeUni;
      int numDiscussionClasses;
      int numNonDiscussionClasses;

      if (globals.includeGradStudents) {
        /*
         * Some tests {@link StaticNetworkBuilderTest#testClassSchedules} fail
         * when the scale is not at certain specific values. I'm not totally sure
         * why this is, but for now it seems reasonable to round nAgents to
         * values that allow those tests to pass.
         *
         * It seems like the scale needs to be a multiple of 0.025, so we ensure this
         * by rounding the number of nAgents to be the model nAgents (37200) divided
         * by itself divided by 10 divided by 4.
         *
         * It used to appear that this affected determinism, but I no longer think
         * that is the case.
         */
        scaleAgainstLargeUni =
            (
                (int) Math.floor(
                    ((double) globals.nAgents)
                        / (372.0 / 4.0)
                ) * (372.0 / 4.0)
            ) / 37200.0;
        int numPTUGStudents = (int) Math.ceil(8640 * scaleAgainstLargeUni);
        int numFTUGStudents = (int) Math.floor(15360 * scaleAgainstLargeUni);
        nPTStudentsPerUGYear = (int) Math.floor(numPTUGStudents / 4.0);
        nFTStudentsPerUGYear = (int) Math.floor(numFTUGStudents / 4.0);
        nGradStudents = (int) Math.floor(6000 * scaleAgainstLargeUni);
        nFaculty = (int) Math.floor(2400 * scaleAgainstLargeUni);
        nStaff = (int) Math.floor(4800 * scaleAgainstLargeUni);
        numDiscussionClasses = (int) (700 * scaleAgainstLargeUni);
        numNonDiscussionClasses = (int) (3500 * scaleAgainstLargeUni);
      } else {
        /*
         * Some tests {@link StaticNetworkBuilderTest#testClassSchedules} fail
         * when the scale is not at certain specific values. I'm not totally sure
         * why this is, but for now it seems reasonable to round nAgents to
         * values that allow those tests to pass.
         *
         * It seems like the scale needs to be a multiple of 0.025, so we ensure this
         * by rounding the number of nAgents to be the model nAgents (31200) divided
         * by itself divided by 10 divided by 4.
         *
         * It used to appear that this affected determinism, but I no longer think
         * that is the case.
         */
        scaleAgainstLargeUni = (
            (int) Math.floor(
                ((double) globals.nAgents)
                    / (312.0 / 4.0)
            ) * (312.0 / 4.0)
        ) / 31200.0;
        int numPTUGStudents = (int) Math.ceil(8640 * scaleAgainstLargeUni);
        int numFTUGStudents = (int) Math.floor(15360 * scaleAgainstLargeUni);
        nPTStudentsPerUGYear = (int) Math.floor(numPTUGStudents / 4.0);
        nFTStudentsPerUGYear = (int) Math.floor(numFTUGStudents / 4.0);
        nGradStudents = 0;
        nFaculty = (int) Math.floor(2400 * scaleAgainstLargeUni);
        nStaff = (int) Math.floor(4800 * scaleAgainstLargeUni);
        numDiscussionClasses = 0;
        numNonDiscussionClasses = (int) (3500 * scaleAgainstLargeUni);
      }
      return builderWithCommonConfiguration(globals)
          .numPartTimeStudentsPerUGYear(nPTStudentsPerUGYear)
          .numFullTimeStudentsPerUGYear(nFTStudentsPerUGYear)
          .numFullTimeGraduateStudents(nGradStudents)
          .numFaculty(nFaculty)
          .numStaff(nStaff)
          .numDiscussionClasses(numDiscussionClasses)
          .numNonDiscussionClasses(numNonDiscussionClasses)
          .numFacultyWhoTeachNClasses(
              ImmutableList.of(
                  (int) (scaleAgainstLargeUni * 1500),
                  (int) (scaleAgainstLargeUni * 2000),
                  (int) (scaleAgainstLargeUni * 1500)))
          // Format: [
          //    Set of (# classes part time Freshman students take): [num1, num2...],
          //    Set of (# classes part time Sophomore students take: [num1, num2...],
          .numbersOfDiscussionClassesTakenByPartTimeStudents(
              ImmutableList.of(
                  ImmutableList.of(2),
                  ImmutableList.of(1, 2),
                  ImmutableList.of(1),
                  ImmutableList.of(0, 1),
                  ImmutableList.of()))
          .numbersOfDiscussionClassesTakenByFullTimeStudents(
              ImmutableList.of(
                  ImmutableList.of(4),
                  ImmutableList.of(3),
                  ImmutableList.of(2),
                  ImmutableList.of(1),
                  ImmutableList.of(2)))
          .numbersOfNonDiscussionClassesTakenByPartTimeStudents(
              ImmutableList.of(
                  ImmutableList.of(2),
                  ImmutableList.of(2),
                  ImmutableList.of(2),
                  ImmutableList.of(2),
                  ImmutableList.of()))
          .numbersOfNonDiscussionClassesTakeByFullTimeStudents(
              ImmutableList.of(
                  ImmutableList.of(4),
                  ImmutableList.of(4),
                  ImmutableList.of(4),
                  ImmutableList.of(4),
                  ImmutableList.of(3)))
          .build();
    }
    throw new IllegalArgumentException("Invalid University input configuration.");
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder numPartTimeStudentsPerUGYear(int numPartTimeStudentsPerUGYear);

    public abstract Builder numFullTimeStudentsPerUGYear(int numFullTimeStudentsPerUGYear);

    public abstract Builder numFullTimeGraduateStudents(int numFullTimeGraduateStudents);

    public abstract Builder numFaculty(int numFaculty);

    public abstract Builder numStaff(int numStaff);

    public abstract Builder numbersOfDiscussionClassesTakenByPartTimeStudents(
        ImmutableList<ImmutableList<Integer>> numbersOfDiscussionClassesTakenByPartTimeStudents);

    public abstract Builder numbersOfDiscussionClassesTakenByFullTimeStudents(
        ImmutableList<ImmutableList<Integer>> numbersOfDiscussionClassesTakenByFullTimeStudents);

    public abstract Builder numbersOfNonDiscussionClassesTakenByPartTimeStudents(
        ImmutableList<ImmutableList<Integer>> numbersOfNonDiscussionClassesTakenByPartTimeStudents);

    public abstract Builder numbersOfNonDiscussionClassesTakeByFullTimeStudents(
        ImmutableList<ImmutableList<Integer>> numbersOfNonDiscussionClassesTakeByFullTimeStudents);

    public abstract Builder numDiscussionClasses(int numDiscussionClasses);

    public abstract Builder numNonDiscussionClasses(int numNonDiscussionClasses);

    public abstract Builder numFacultyWhoTeachNClasses(
        ImmutableList<Integer> numFacultyWhoTeachNClasses);

    public abstract Builder numberOfStaffSharingOfficeAndBathroom(
        int numberOfStaffSharingOfficeAndBathroom);

    public abstract Builder percentageOfStaffWithStudentFacingJob(
        double percentageOfStaffWithStudentFacingJob);

    public abstract Builder buildingProfiles(
        ImmutableList<StudentHousingBuildingProfile> buildingProfiles);

    public abstract Builder campusEventAttendanceDistributionStudents(
        ImmutableList<Double> campusEventAttendanceDistributionStudents);

    public abstract Builder campusEventAttendanceDistributionFacultyAndStaff(
        ImmutableList<Double> campusEventAttendanceDistributionFacultyAndStaff);

    public abstract Builder percStudentsWhoAttendSportsEvent(
        double percStudentsWhoAttendSportsEvent);

    public abstract Builder percentageOfStudentsInStudentGroup(
        double percentageOfStudentsInStudentGroup);

    public abstract Builder studentGroupMinSize(int studentGroupMinSize);

    public abstract Builder studentGroupMaxSize(int studentGroupMaxSize);

    public abstract Builder studentGroupMean(int studentGroupMean);

    public abstract Builder studentGroupSD(int studentGroupSD);

    public abstract Builder campusEventMinSize(int campusEventMinSize);

    public abstract Builder campusEventMaxSize(int campusEventMaxSize);

    public abstract Builder campusEventMean(int campusEventMean);

    public abstract Builder campusEventSD(int campusEventSD);

    public abstract Builder timesPerDayStaffInteractWithStudent(
        int timesPerDayStaffInteractWithStudent);

    public abstract Builder numDiningHalls(int numDinigHalls);

    public abstract Builder percFullTimeStudentsWhoLiveOnCampus(double percFullTimeStudentsWhoLiveOnCampus);

    public abstract UniversityConfiguration build();
  }

  /**
   * A value class containing all the relevant info for a university Student Housing building.
   */
  @AutoValue
  public abstract static class StudentHousingBuildingProfile {
    public abstract ImmutableSet<StudentType> residentTypes();

    public abstract int numFloors();

    public abstract int numSuitesPerFloor();

    public abstract int numSuitesPerBathroom();

    public abstract int suiteSize();

    public static Builder builder() {
      return new AutoValue_UniversityConfiguration_StudentHousingBuildingProfile.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
      public abstract Builder residentTypes(ImmutableSet<StudentType> residentTypes);

      public abstract Builder numFloors(int numFloors);

      public abstract Builder numSuitesPerFloor(int numSuitesPerFloor);

      public abstract Builder numSuitesPerBathroom(int numSuitesPerBathroom);

      public abstract Builder suiteSize(int suiteSize);

      public abstract StudentHousingBuildingProfile build();
    }
  }
}
