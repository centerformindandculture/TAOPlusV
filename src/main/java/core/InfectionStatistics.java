package core;

import simudyne.core.schema.FieldType;
import simudyne.core.schema.SchemaField;
import simudyne.core.schema.SchemaRecord;
import simudyne.core.values.ValueRecord;

public class InfectionStatistics {
  public int tStep;
  public int numSusceptible;
  public int numInfected;
  public int numRecovered;
  public int numDead;
  public int numDetectedCases;
  public double testPositivity;

  public InfectionStatistics(
      int tStep,
      int numSusceptible,
      int numInfected,
      int numRecovered,
      int numDead,
      int numDetectedCases,
      double testPositivity) {
    this.tStep = tStep;
    this.numSusceptible = numSusceptible;
    this.numInfected = numInfected;
    this.numRecovered = numRecovered;
    this.numDead = numDead;
    this.numDetectedCases = numDetectedCases;
    this.testPositivity = testPositivity;
  }

  public static SchemaRecord getSchema() {

    return new SchemaRecord("infectionStatistics")
        .add(new SchemaField("tStep", FieldType.Long))
        .add(new SchemaField("numSusceptible", FieldType.Long))
        .add(new SchemaField("numInfected", FieldType.Long))
        .add(new SchemaField("numInfectedQuarantine", FieldType.Long))
        .add(new SchemaField("numInfectedSusceptible", FieldType.Long))
        .add(new SchemaField("numRecovered", FieldType.Long))
        .add(new SchemaField("numDead", FieldType.Long))
        .add(new SchemaField("numDetectedCases", FieldType.Long))
        .add(new SchemaField("testPositivity", FieldType.Double));
  }

  public ValueRecord getValue() {
    return new ValueRecord("infectionStatistics")
        .addField("tStep", Long.valueOf(this.tStep))
        .addField("numSusceptible", Long.valueOf(this.numSusceptible))
        .addField("numInfected", Long.valueOf(this.numInfected))
        .addField("numInfectedQuarantine", Long.valueOf(this.numInfected))
        .addField("numInfectedSusceptible", Long.valueOf(this.numInfected))
        .addField("numRecovered", Long.valueOf(this.numRecovered))
        .addField("numDead", Long.valueOf(this.numDead))
        .addField("numDetectedCases", Long.valueOf(this.numDetectedCases))
        .addField("testPositivity", this.testPositivity);
  }
}
