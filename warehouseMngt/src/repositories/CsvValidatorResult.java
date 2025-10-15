package repositories;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CsvValidatorResult<T> {  // se os dados lidos no csv a ler forem corretos aceita, se nao for guarda o erro
    private final List<T> records = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();

    public void addRecord(T record) { records.add(record); }
    public void addError(String error) { errors.add(error); }

    public List<T> getRecords() { return Collections.unmodifiableList(records); }
    public List<String> getErrors() { return Collections.unmodifiableList(errors); }
    public boolean hasErrors() { return !errors.isEmpty(); }
}


