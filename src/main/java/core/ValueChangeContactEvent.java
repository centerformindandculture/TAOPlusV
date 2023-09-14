package core;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class ValueChangeContactEvent {

    public abstract long alterId();

    public abstract double alterNewAffiliationValue();

    public static ValueChangeContactEvent create(long alterId, double alterNewAffiliationValue) {
        return new AutoValue_ValueChangeContactEvent(alterId, alterNewAffiliationValue);
    }
}
