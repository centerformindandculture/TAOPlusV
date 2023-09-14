package tau.anylogic_code;

import core.Person;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * ConnectionOfAgents2
 */
public class ConnectionOfAgents implements Serializable {

  public static final int defaultFrequency = 1;
  public static final int defaultStepWithinDay = 0;
  public static int tOneDay = -1;

  public List<Person> people = new ArrayList<>();
  public final int frequency;
  public final int stepWithinDay;
  public double startTime = 0;
  public double endTime = Double.MAX_VALUE;
  private String name = "NoName";
  public final long _id;

  /**
   * Default constructor
   */
  public ConnectionOfAgents(int frequency, int stepWithinDay, long uniqueId) {
    if (stepWithinDay >= tOneDay) {
      throw new IllegalStateException("Invalid stepWithinDay argument");
    }
    if (frequency < 1) {
      throw new IllegalStateException("Frequency must be >= 1");
    }
    this.frequency = frequency;
    this.stepWithinDay = stepWithinDay;
    _id = uniqueId;
  }

  public ConnectionOfAgents(int frequency, long uniqueId) {
    this(frequency, defaultStepWithinDay, uniqueId);
  }

  public ConnectionOfAgents(long uniqueId) {
    this(defaultFrequency, defaultStepWithinDay, uniqueId);
  }

  public double getNumPeople() {
    return 1.0 * people.size();
  }

  public void addPerson(Person p) {
    if (people.contains(p)) {
      throw new IllegalStateException("Person cannot be added twice");
    }
    people.add(p);
  }

  public boolean containsPerson(Person p) {
    return people.contains(p);
  }

  public void addPeople(List<Person> p) {
    people.addAll(p);
  }

  public boolean isEventHappeningNow(int step, int stepsPerDay) {
    int day = step / stepsPerDay;
    int stepInDay = step % stepsPerDay;
    return (stepWithinDay == stepInDay) && (day - dayOffset()) % frequency == 0;
  }

  public List<Person> getPeople() {
    return people;
  }

  public void removePerson(Person p) {
    people.remove(p);
  }

  // So that events with the same frequency don't all happen on the same day.
  protected int dayOffset() {
    return 0;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
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
