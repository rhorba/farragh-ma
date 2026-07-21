package ma.farragh.backend.admin;

public enum AnalyticsGranularity {
    DAY,
    WEEK,
    MONTH;

    /** Value fed to Postgres date_trunc() - a value parameter, not an identifier, so plain binding is safe. */
    String sqlUnit() {
        return name().toLowerCase();
    }
}
