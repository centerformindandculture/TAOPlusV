package tau.anylogic_code;

import core.Person;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Floor
 */
public class Floor extends ConnectionOfAgents implements Serializable {

  public List<Suite> suites = new ArrayList<Suite>();

  /**
   * Default constructor
   */
  public Floor(int frequency, long uniqueId) {
    super(frequency, uniqueId);
  }

  public Floor(long uniqueId) {
    super(uniqueId);
  }

  public void addSuite(Suite s) {
    suites.add(s);
  }

  @Override
  public List<Person> getPeople() {
    people.clear();
    for (Suite s : suites) {
      addPeople(s.getPeople());
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
