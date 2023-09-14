package tau.anylogic_code;

import core.Person;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Building
 */
public class Building extends ConnectionOfAgents implements Serializable {

  public List<Floor> floors = new ArrayList<>();

  /**
   * Default constructor
   */
  public Building(int frequency, long uniqueId) {
    super(frequency, uniqueId);
  }

  public Building(long uniqueId) {
    super(uniqueId);
  }

  public void addFloor(Floor f) {
    floors.add(f);
  }

  @Override
  public List<Person> getPeople() {
    people.clear();
    for (Floor f : floors) {
      addPeople(f.getPeople());
    }
    return super.getPeople();
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
