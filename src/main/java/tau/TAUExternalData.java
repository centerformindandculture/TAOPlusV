package tau;

import com.google.auto.value.AutoValue;
import externalDataManagers.ExternalData;
import externalDataManagers.GitManager;
import externalDataManagers.StateManager;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class TAUExternalData extends ExternalData {

  public static final String NYTCountyData = "externalData/NYT/us-counties.csv";
  public static final String NYTLiveCountyData = "externalData/NYT/live/us-counties.csv";
  public static final String CensusPopulationData = "externalData/CensusData/2019_Population_Prediction_County.csv";

  public static final String NYTRepoSSH_URI = "git@github.com:nytimes/covid-19-data.git";
  public static final String NYTRepoDirectory = "externalData/NYT/";

  public static final String NYTDelimiter = ",";
  public static final String CensusDelimiter = ", ";

  public static final String LAST_PULLED_NYT_DATA = "LastPulledNYTData";
  public static final String SSH_RSA_KEY_LOCATION = "SshRsaKeyLocation";
  public static final String SSH_RSA_KEY_PASSWORD = "SshRsaKeyPassword";

  public static final int DATA_REFRESH_AFTER_DAYS = 1;

  public double getExternalDataInfectionRate(String stateFile, String externalDataCounty, String externalDataState, int dataSource) {
    if (externalDataCounty == null
        || externalDataState == null
        || externalDataCounty.isEmpty()
        || externalDataState.isEmpty()) {
      return 0.0;
    }
    if (dataSource == DataSource.NONE.ordinal()) {
      return 0.0;
    }

    StateManager stateManager = StateManager.getManager();
    stateManager.getStateFromFile(stateFile);

    String lastPulledTimeS = stateManager.get(LAST_PULLED_NYT_DATA);

    long currentTime = new Date().getTime();
    Instant currentInstant = Instant.ofEpochMilli(currentTime);
    long lastPulledTime = (lastPulledTimeS != null) ? Long.parseLong(lastPulledTimeS) : 0;
    Instant lastPullInstant = Instant.ofEpochMilli(lastPulledTime);

    if (currentInstant.minus(Duration.ofDays(DATA_REFRESH_AFTER_DAYS)).isAfter(lastPullInstant)) {
      try {
        String sshKeyLocation = stateManager.get(SSH_RSA_KEY_LOCATION);
        String sshKeyPassword = stateManager.get(SSH_RSA_KEY_PASSWORD);

        if (sshKeyLocation == null || sshKeyPassword == null) {
          System.out.println("Unable to update NYT Data.");
        } else {
          GitManager.getRepo(NYTRepoSSH_URI, NYTRepoDirectory, sshKeyLocation, sshKeyPassword);
        }
      } catch (Exception e) {
        System.err.println(e);
        System.out.println("Error fetching git repo");
      }
      stateManager.put(LAST_PULLED_NYT_DATA, Long.toString(currentTime));
    }

    GregorianCalendar currentCalendar = new GregorianCalendar();
    currentCalendar.roll(Calendar.DAY_OF_YEAR, -14);
    int year = currentCalendar.get(Calendar.YEAR);
    // The first month of the year is 0 in GregorianCalendar
    int month = currentCalendar.get(Calendar.MONTH) + 1;
    int day = currentCalendar.get(Calendar.DAY_OF_MONTH);

    String dateString = String.format("%04d-%02d-%02d", year, month, day);

    NYTData pastData = getNYTData(
        externalDataCounty, externalDataState, dateString);

    String pop = getPopulationData(
        externalDataCounty, externalDataState);

    NYTData currentData = getMostRecentNYTData(
        externalDataCounty, externalDataState);

    if (currentData == null || pastData == null || pop == null) {
      return 0.0;
    }

    long currentCases = currentData.infections();
    long pastCases = pastData.infections();
    long population = Long.parseLong(pop);

    stateManager.writeStateToFile(stateFile);

    return (currentCases - pastCases) / (double) population;
  }

  /**
   * Gets the NYT Covid-19 data for a particular US county on a certain date
   *
   * @param county US county
   * @param state  US state
   * @param date   Date in this format: YYYY-MM-DD
   *               Returns null if any of the params don't match the NYT data.
   *               Otherwise, returns
   */
  public static NYTData getNYTData(String county, String state, String date) {
    File nytDataFile = new File(NYTCountyData);
    String[] match = {date, county, state};
    String[] data = getData(nytDataFile, NYTDelimiter, match);

    if (data == null) {
      return null;
    }
    return NYTData.create(county, state, data[0], data[4], data[5]);
  }

  public static NYTData getMostRecentNYTData(String county, String state) {
    File nytDataFile = new File(NYTLiveCountyData);
    String[] match = {".", county, state};
    String[] data = getData(nytDataFile, NYTDelimiter, match);

    if (data == null) {
      return null;
    }
    return NYTData.create(county, state, data[0], data[4], data[5]);
  }

  public static String getPopulationData(String county, String state) {
    File censusData = new File(CensusPopulationData);

    String[] match = {county, state};
    String[] data = getData(censusData, CensusDelimiter, match);

    if (data == null) {
      return null;
    }
    return data[2];
  }

  public enum DataSource {
    NONE,
    NYT,
    WHO
  }

  private static final String NYT_DATE_DELIMITER = "-";

  @AutoValue
  public abstract static class NYTData {
    public abstract String county();

    public abstract String state();

    public abstract Date date();

    public abstract long infections();

    public abstract long deaths();

    public static NYTData create(String county, String state, String date, String infections, String deaths) {
      String[] dateParts = date.split(NYT_DATE_DELIMITER);
      Calendar cal = new GregorianCalendar();
      cal.set(Integer.parseInt(dateParts[0]), Integer.parseInt(dateParts[1]) - 1, Integer.parseInt(dateParts[2]));
      Date dataDate = cal.getTime();
      long dataInfections = Long.parseLong(infections);
      long dataDeaths = Long.parseLong(deaths);

      return new AutoValue_TAUExternalData_NYTData(county, state, dataDate, dataInfections, dataDeaths);
    }
  }
}
