package main.repositories;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Container for results of CSV validation.
 *
 * <p>Holds successfully parsed records and any errors encountered during parsing.
 * Can be used to check if CSV data is valid and to retrieve both valid entries and error messages.</p>
 *
 * @param <T> type of record stored
 */
public class CsvValidatorResult<T> {

    private final List<T> records = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();

    /**
     * Adds a successfully parsed record.
     *
     * @param record the record to add
     */
    public void addRecord(T record) { records.add(record); }

    /**
     * Adds an error message encountered during CSV parsing.
     *
     * @param error the error message
     */
    public void addError(String error) { errors.add(error); }

    /**
     * Returns an unmodifiable list of valid records.
     *
     * @return list of records
     */
    public List<T> getRecords() { return Collections.unmodifiableList(records); }

    /**
     * Returns an unmodifiable list of error messages.
     *
     * @return list of errors
     */
    public List<String> getErrors() { return Collections.unmodifiableList(errors); }

    /**
     * Checks if any errors were recorded.
     *
     * @return true if there are errors, false otherwise
     */
    public boolean hasErrors() { return !errors.isEmpty(); }
}
