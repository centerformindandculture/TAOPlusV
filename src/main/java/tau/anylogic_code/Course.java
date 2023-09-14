package tau.anylogic_code;

import core.Person;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Course2
 */
public class Course extends ConnectionOfAgents implements Serializable {

  public Person instructor;
  private final int dayOffset;
  public final ClassSchedule schedule;
  public int sectionGoingToThisSession = 0;
  public boolean doSplitClasses;

  public Course(int scheduleCode, boolean doSplitClasses, int stepWithinDay, long uniqueId, long offset) {
    super(defaultFrequency, stepWithinDay, uniqueId);
    // Note, dayOffset is only relevant for one day per week classes
    this.dayOffset = (int) (offset % 5);
    schedule =
        scheduleCode == 0
            ? ClassSchedule.MWF
            : scheduleCode == 1 ? ClassSchedule.TUTH : ClassSchedule.ONE_DAY;
    this.doSplitClasses = doSplitClasses;
  }

  public Course(int scheduleCode, boolean doSplitClasses, long uniqueId, long offset) {
    super(uniqueId);
    // Note, dayOffset is only relevant for one day per week classes
    this.dayOffset = (int) (offset % 5);
    schedule =
        scheduleCode == 0
            ? ClassSchedule.MWF
            : scheduleCode == 1 ? ClassSchedule.TUTH : ClassSchedule.ONE_DAY;
    this.doSplitClasses = doSplitClasses;
  }

  public void addInstructor(Person person) {
    instructor = person;
    // addPerson(person);
    // TODO Uncomment this line
    // person.teachingClass.add(this);
  }

  @Override
  public boolean containsPerson(Person p) {
    return instructor.equals(p) || super.containsPerson(p);
  }

  @Override
  public List<Person> getPeople() {
    List<Person> people = new ArrayList<>();
    if (instructor == null) {
      return people;
    }
    people.add(instructor);
    List<Person> students = super.getPeople();

    if (!doSplitClasses) {
      people.addAll(students);
      // traceln("Classes NOT split size " + people.size());
      return people;
    }

    if (schedule != ClassSchedule.MWF) {
      // Half sections
      if (sectionGoingToThisSession % 2 == 0) {
        // First half
        people.addAll(students.subList(0, students.size() / 2));
      } else {
        // second half
        people.addAll(students.subList(students.size() / 2, students.size()));
      }
    } else {
      // Third sections
      int indexMultiplier = sectionGoingToThisSession % 3;
      people.addAll(
          students.subList(
              (indexMultiplier * students.size()) / 3,
              ((indexMultiplier + 1) * students.size()) / 3));
    }

    // traceln("Classes split size " + people.size());
    return people;
  }

  @Override
  public void removePerson(Person p) {
    if (p == instructor) {
      instructor = null;
    }
    super.removePerson(p);
  }

  @Override
  public boolean isEventHappeningNow(int step, int stepsPerDay) {
    int day = step / stepsPerDay;
    int stepInDay = step % stepsPerDay;
    int dayOfWeek = day % 7;
    if (schedule == ClassSchedule.MWF) {
      return (stepWithinDay == stepInDay) && (dayOfWeek == 0 || dayOfWeek == 2 || dayOfWeek == 4);
    } else if (schedule == ClassSchedule.TUTH) {
      return (stepWithinDay == stepInDay) && (dayOfWeek == 1 || dayOfWeek == 3);
    }
    return (stepWithinDay == stepInDay) && (dayOfWeek == (dayOffset() % 7));
  }

  @Override
  public String toString() {
    return super.toString();
  }

  @Override
  protected int dayOffset() {
    return dayOffset;
  }

  /**
   * This number is here for model snapshot storing purpose<br>
   * It needs to be changed when this class gets changed
   */
  private static final long serialVersionUID = 1L;

  public enum ClassSchedule {
    MWF,
    TUTH,
    ONE_DAY
  }
}
