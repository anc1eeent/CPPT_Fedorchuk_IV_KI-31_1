# Lab 5 — Generic repository, exceptions and CSV export

Reports of the earlier labs: [`docs/reports`](docs/reports).

## 1. Topic, number and variant

| | |
|---|---|
| Lab | 5 — Generic storage, exceptions and CSV export in a Java project |
| Variant | 20 — gym memberships (unchanged) |
| Author | Fedorchuk I. V., group KI-31 |
| Development OS | Windows 11 |
| CI | GitHub Actions: Ubuntu, Windows, macOS |
| Version | `1.4.0`, tag `v1.4.0`, branch `lab-05-java`, pull request [#36](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/pull/36) |

Data of variant 20:

| Class | Fields and types | CSV columns | Round-trip key |
|---|---|---|---|
| `MembershipRecord` | `customer: String`, `kind: MembershipKind` (enum), `startDate: LocalDate`, `endDate: LocalDate`, `visits: int` | `клієнт`, `тип`, `початок`, `кінець`, `відвідування` | `customer + startDate` |

## 2. Goal

Finish the Java part of the product: keep the data in a generic repository instead of a plain list, describe
the CSV columns with an annotation, export the records with a universal reflective exporter, write and read the
file through `Path` and `Files` in UTF-8, and prove with a round-trip that no data is lost.

Result: the program prints the unchanged report of Lab 4, then exports the seven valid memberships to
`out/memberships.csv`, reads the file back and prints `Round-trip: true`.

## 3. State before and after

| | Lab 4 | Lab 5 |
|---|---|---|
| Storage | `List<Membership>` returned by `readMemberships` | `Repository<Membership>`; export records in `Repository<MembershipRecord>` |
| Output | report on the console and in `out/report.txt` | the same report + CSV file `out/memberships.csv` |
| Reading back | — | `CsvParser` + `MembershipRecord.fromFields`, comparison by `equals` |
| Errors | `IllegalArgumentException` (model), `IOException` (file) | + checked `DataStorageException` with a cause for format, value, file and reflection errors |
| Options | `--input`, `--output`, `--version`, `--help` | + `--export <path>`; exit code `3` for storage errors |
| Tests | 74 | 136 |

The report, the control data `data/input.csv` and all Lab 1–4 tests are unchanged (section 14).

## 4. Repository&lt;T&gt;

```java
public final class Repository<T> {
    private final List<T> items = new ArrayList<>();

    public void add(T item)                              // rejects null
    public void addAll(Collection<? extends T> newItems) // all or nothing
    public List<T> all()                                 // List.copyOf(items)
    public List<T> find(Predicate<? super T> condition)  // stream().filter(condition).toList()
    public int size()
}
```

- **Type parameter.** `T` is fixed when the repository is created. `Repository<Membership>` accepts only
  memberships and returns `List<Membership>`, so there are no casts and no raw types in the project.
- **Snapshot.** `all()` returns `List.copyOf(items)`: an unmodifiable copy. Changing it throws
  `UnsupportedOperationException`, and a snapshot taken earlier does not change after a later `add`.
- **`? super T`** in `find` accepts a condition on `T` or on its supertype, for example a `Predicate<Object>`
  for a `Repository<Membership>`.
- **`? extends T`** in `addAll` accepts a `List<MonthlyMembership>` for a `Repository<Membership>`
  (PECS: the producer `extends`, the consumer `super`).

In `Main` the parsed memberships go into `Repository<Membership>`; the report is built from `all()`, the
export records are built from `all()` and the restored records are searched with `find`.

## 5. Data model

`MembershipRecord` is a Java record, so it is immutable and its `equals`/`hashCode` compare all five
fields — exactly what the round-trip comparison needs.

| Field | Type | Invariant | From the model |
|---|---|---|---|
| `customer` | `String` | not null, not blank | `getClient()` |
| `kind` | `MembershipKind` | not null | `getKind()` |
| `startDate` | `LocalDate` | not null | `getStartDate()` |
| `endDate` | `LocalDate` | not null, after `startDate` | `getEndDate()` = start + months |
| `visits` | `int` | at least 1 | `getVisits()` |

The invariants are checked in the compact constructor (model level: `IllegalArgumentException` or
`NullPointerException`). `MembershipRecord.from(Membership)` builds a record from the model;
`fromFields(List<String>)` restores it from CSV. `key()` returns `Key(customer, startDate)`: Марія Іванівна
has two memberships, and they differ by the start date.

The model of labs 2–4 (`Membership`, its subtypes and their `equals` by subtype and client) is unchanged.

## 6. @CsvColumn

```java
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface CsvColumn {
    String value();
}
```

- **`RUNTIME`** keeps the annotation in the class file and makes it visible to `getAnnotation` while the
  program runs. With the default `CLASS` retention the code compiles, but the exporter finds no fields.
- **`FIELD`** allows the annotation only on fields. On a record component it is copied to the private field
  of the record, which is what the exporter reads.
- **Names and order.** The annotation gives the header text; the order is the sorted Java field names:

| Java field | `customer` | `endDate` | `kind` | `startDate` | `visits` |
|---|---|---|---|---|---|
| Column | `клієнт` | `кінець` | `тип` | `початок` | `відвідування` |

## 7. Reflective exporter

`CsvExporter.write(Path path, Class<T> type, List<? extends T> records)`:

1. **Find the fields.** `getDeclaredFields()` of the class and of every superclass up to `Object`
   (`getDeclaredFields` returns private fields too, but only of one class). Fields with `@CsvColumn` are kept,
   static fields are skipped.
2. **Order.** The fields are sorted by name, so the order does not depend on what the JVM returns.
3. **Access.** `trySetAccessible()` opens each private field; if it returns `false`, the export stops with
   `DataStorageException("no access to field …")`. An `IllegalAccessException` from `field.get` becomes the cause.
4. **Header.** The annotation values joined by commas.
5. **Rows.** For every record `field.get(record)` → `String.valueOf` → `escape`; `null` gives an empty field.
6. **File.** Missing folders are created; `Files.writeString(path, csv, UTF_8, CREATE, TRUNCATE_EXISTING, WRITE)`.

The exporter has no `if` on a class or subtype and names no field of the gym domain. The class is a separate
parameter because it cannot be taken from an empty list, and the header must be written even without records.
`CsvExporterTest` exports an unrelated class `Book` and a subclass with an inherited field.

```java
static String escape(Object value) {
    String text = Objects.toString(value, "");
    boolean special = text.indexOf(',') >= 0 || text.indexOf('"') >= 0
            || text.indexOf('\r') >= 0 || text.indexOf('\n') >= 0;
    return special ? '"' + text.replace("\"", "\"\"") + '"' : text;
}
```

## 8. CSV format

- encoding UTF-8 for writing and reading;
- the first record is the header from `@CsvColumn`;
- fields are separated by `,`; every record ends with `System.lineSeparator()`: `\r\n` on Windows,
  `\n` on Ubuntu and macOS; the parser accepts both;
- a value with a comma, a double quote or a line break is enclosed in double quotes, inner quotes are doubled;
- `kind` is written as the enum constant name, dates in ISO format `yyyy-mm-dd`.

File written for the control data (`out/memberships.csv`):

```text
клієнт,кінець,тип,початок,відвідування
Іван Федорчук,2027-01-15,ANNUAL,2026-01-15,45
Петро Коваль,2026-11-01,ANNUAL,2026-08-01,10
Марія Іванівна,2026-12-10,ANNUAL,2026-06-10,20
Олег Савчук,2026-11-20,MONTHLY,2026-10-20,12
Анна Литвин,2026-11-05,MONTHLY,2026-10-05,12
Ірина Бондар,2026-12-01,MONTHLY,2026-11-01,6
Марія Іванівна,2026-06-01,ANNUAL,2025-12-01,18
```

Escaped record from the test file `src/test/resources/csv/memberships.csv` — a customer with a comma,
quotes and a line break:

```text
"Коваль, Петро ""Ранковий""
корпоративний клієнт",2026-11-01,ANNUAL,2026-08-01,10
```

This is one CSV record on two physical lines; its customer is
`Коваль, Петро "Ранковий"` + line break + `корпоративний клієнт`.

## 9. Import and round-trip

```java
List<List<String>> rows = CsvParser.read(path);        // Files.readString(path, UTF_8) + parseDocument
if (rows.isEmpty()) return List.of();                  // empty file
check rows.get(0) == CsvExporter.header(MembershipRecord.class);
for (int i = 1; i < rows.size(); i++)
    records.add(MembershipRecord.fromFields(rows.get(i)));   // error → "cannot read record i of path: …"
```

`split(",")` cannot read this format: it splits `"Коваль, Петро"` in two, and reading line by line breaks
the multiline record. `CsvParser.parseDocument` reads the whole text character by character with five states:
record start, field start, unquoted field, quoted field, after a closing quote. Inside quotes a comma and a line
break are data and `""` is one quote; after a closing quote only a comma, a line break or the end may follow.

The exporter is universal, but restoring the types is domain work, so it is in `MembershipRecord.fromFields`:
`LocalDate.parse` for the dates, `MembershipKind.valueOf` for the kind, `Integer.parseInt` for the visits.

`MembershipStorage.roundTrip` saves the records, loads them back and compares the lists with `equals`, which
checks the order and all fields: text with commas and quotes, the enum, both dates and the number.
Program output for the control data:

```text
--- CSV EXPORT ---
Records in repository: 7
CSV records with header: 8
Restored records: 7
Header: клієнт,кінець,тип,початок,відвідування
Round-trip: true
Key "Анна Литвин" + 2026-10-05: restored, equal to the original
Export saved to out/memberships.csv
```

The last line is the search by the key of variant 20: the original record of Анна Литвин is taken, and
`find(record -> record.key().equals(original.key()))` over the restored repository returns exactly that record.

## 10. Errors

| Level | Example | Type | Message | Cause | Test |
|---|---|---|---|---|---|
| model | visits 0, end before start, blank customer | `IllegalArgumentException` | `visits must be at least 1, got 0` | — | `MembershipRecordTest.rejectsInvalidRecords` |
| model in a file | end date before start date in a row | `DataStorageException` | `invalid membership record: endDate must be after startDate, …` | `IllegalArgumentException` | `rejectsBrokenInvariantWithCause` |
| format | 4 fields instead of 5 | `DataStorageException` | `expected 5 fields, got 4` | — | `rejectsWrongFieldCount` |
| format | quote not closed | `DataStorageException` | `line 2: quote is not closed` | — | `CsvParserTest.rejectsUnclosedQuote` |
| format | other columns | `DataStorageException` | `unexpected header in …: expected клієнт,кінець,…` | — | `rejectsUnexpectedHeader` |
| value | `дванадцять` visits | `DataStorageException` | `cannot read record 2 of …broken-row.csv: field 'відвідування' is not a whole number: 'дванадцять'` | `DataStorageException` → `NumberFormatException` | `brokenRowNamesRecordAndKeepsCause` |
| value | kind `WEEKLY` | `DataStorageException` | `field 'тип' is not one of [MONTHLY, ANNUAL]: 'WEEKLY'` | `IllegalArgumentException` | `rejectsUnknownKindWithCause` |
| value | date `05.10.2026` | `DataStorageException` | `field 'початок' is not a date (yyyy-mm-dd): '05.10.2026'` | `DateTimeParseException` | `rejectsBadDateWithCause` |
| file | missing file | `DataStorageException` | `cannot read CSV file …missing.csv` | `NoSuchFileException` (`IOException`) | `reportsMissingFileWithCause` |
| file | parent "folder" is a file | `DataStorageException` | `cannot write CSV file …` | `FileAlreadyExistsException` (`IOException`) | `reportsFileErrorWithCause`, `MainTest` |
| reflection | class without annotated fields | `DataStorageException` | `class …Plain has no fields marked with @CsvColumn` | — | `rejectsClassWithoutAnnotatedFields` |
| reflection | blank column name / no access | `DataStorageException` | `field BlankColumn.value has a blank CSV column name` | — | `rejectsBlankColumnName` |

No `catch` is empty: every error is either wrapped with context and rethrown, or printed by `Main`, which
shows the message and the cause and returns exit code `3`:

```text
Error: cannot write CSV file out/report.txt/memberships.csv
Cause: java.nio.file.FileAlreadyExistsException: out/report.txt
```

An empty file and a file with only the header are different cases, but both are valid by the contract and give
an empty list.

## 11. Testing

`mvnw verify`: **136 tests, 0 failures**, SpotBugs: 0 bugs. New tests of this lab (62):

| Test class | What it proves |
|---|---|
| `RepositoryTest` (9) | add order, empty repository, `null` rejected, unmodifiable snapshot, old snapshot unchanged, `find`, `Predicate<Object>` (`? super T`), `List<MonthlyMembership>` (`? extends T`), all-or-nothing `addAll` |
| `DataStorageExceptionTest` (3) | message without cause, cause kept, checked exception |
| `CsvColumnTest` (3) | `RUNTIME` + `FIELD`, every field of `MembershipRecord` annotated with its name, header order |
| `MembershipRecordTest` (10) | `from(Membership)` for both kinds, invariants, `equals` over all fields, key, `fromFields`; field count, number, enum, date and invariant errors with their causes |
| `CsvExporterTest` (12) | header and rows, escaping of comma / quote / `\n` / `\r\n` / `null`, empty list, unrelated class, inherited and static fields, class without annotations, blank name, `null` arguments, UTF-8 bytes, folders created, old file replaced, file error with cause |
| `CsvParserTest` (12) | records and fields, quoted multiline field, `\n` = `\r\n`, last record without line break, empty text and header only, quote inside unquoted field, everything the exporter escapes is parsed back, unclosed quote, text after closing quote, UTF-8 with byte order mark, missing file, broken file with path |
| `MembershipStorageTest` (10) | test file: 8 physical lines → 7 CSV records → 6 objects; round-trip and key search; multiline field; empty list; empty file; header only; broken row with record number and cause chain; wrong field count; other header; missing file |
| `MainTest` (+2) | the run exports and reads back the CSV and prints the round-trip; a storage error gives exit code 3, the message and the cause; the report file has no export section |
| `SampleDataTest` (+1) | the seven valid rows of `data/input.csv` survive the round-trip |

Test data in `src/test/resources/csv`: `memberships.csv` (6 records, one with a comma, quotes and a line
break), `broken-row.csv` (record 2 has visits `дванадцять`), `empty.csv` (0 bytes). All 74 tests of labs 1–4
pass together with the new ones.

**Encoding and line separators.** Writing and reading use `StandardCharsets.UTF_8` explicitly; a test compares
the file bytes with the UTF-8 encoding of the text, another reads a file with a byte order mark. The exporter
writes the line separator of the system, the tests build the expected text with `System.lineSeparator()`, and
the parser test reads the same data with `\n` and `\r\n`. CI runs all tests on Windows (`\r\n`) and on Ubuntu
and macOS (`\n`). On Windows Git may check out the test files with `\r\n`; the tests do not depend on that:
the multiline customer is checked by its line count, and the round-trip compares data read on the same system.

## 12. Infrastructure

```powershell
.\mvnw.cmd clean test       # compile and run the tests
.\mvnw.cmd clean verify     # + executable JAR + SpotBugs
java -jar target\gym-membership-1.4.0.jar
java -jar target\gym-membership-1.4.0.jar --export out\memberships.csv
```

Created files: `out/report.txt` (report, unchanged format) and `out/memberships.csv` (section 8). `out/` and
`target/` are in `.gitignore`.

CI: [Actions → Java CI](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/actions/workflows/ci.yml)
runs `verify` on Ubuntu, Windows and macOS and publishes `gym-membership-1.4.0.jar` as an artifact of each run.

## 13. GitHub Issues and pull request

| Issue | Change | Check |
|---|---|---|
| [#29](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/issues/29) | `Repository<T>`, memberships stored in `Repository<Membership>` | `RepositoryTest`, unchanged report |
| [#30](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/issues/30) | `DataStorageException` with a cause, error levels | `DataStorageExceptionTest`, cause checks in all storage tests |
| [#31](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/issues/31) | `@CsvColumn`, `MembershipRecord` with invariants and key | `CsvColumnTest`, `MembershipRecordTest` |
| [#32](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/issues/32) | reflective `CsvExporter`, escaping, UTF-8 file | `CsvExporterTest` |
| [#33](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/issues/33) | `CsvParser`, `MembershipStorage`, round-trip, `--export` | `CsvParserTest`, `MembershipStorageTest`, `MainTest` |
| [#34](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/issues/34) | CSV test files, negative tests of every error level | 136 tests, 0 failures |
| [#35](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/issues/35) | Javadoc, README, this report, Lab 4 report moved, version 1.4.0 | green CI on three operating systems, tag `v1.4.0` |

## 14. Comparison of state

- `data/input.csv` is unchanged: 13 rows, 7 valid, the same 6 skip messages (`SampleDataTest`).
- `out/report.txt` is byte for byte the report of Lab 4: the export section is printed to the console only,
  and `MainTest` checks that the report file does not contain it.
- The Lab 4 values are unchanged: `7 / 5450.50 / 12 / 17.57 / 6 / 75.00 / 373.63`, active 5,
  `Monthly=30, Annual=93`, top 5 and the search of Анна Литвин.
- All 74 tests of labs 1–4 pass; none of their assertions was changed. `CommandLineTest` and `MainTest` got
  extra checks for `--export`, and the `MainTest` runs pass `--export` so that they write into a temporary folder.
- The report of Lab 4 is in [`docs/reports/lab-04.md`](docs/reports/lab-04.md).

## 15. Academic integrity

- **Tool:** Java development environment, version control systems, and automated testing tools, in the roles Manager (Issues #29–#35), Developer (code of all components), Validator (test data with a comma, quotes and a line break; boundary cases), Documenter (README, this report) and Reviewer (no swallowed exceptions, no domain names in the exporter).
- **Prompts:** theory of the lab on the code of this project; the detailed Issues; implementation of the whole lab with tests and documentation.
- **Accepted:** `MembershipRecord` as a Java record (all-field `equals` for free); `addAll` with `? extends T` next to `find` with `? super T`; collecting fields up the superclass chain and skipping static fields; a parser with five explicit states instead of the two flags of the methodology demo, which drops a last record that is one empty quoted field (`""`) without a line break; exit code `3` for storage errors; export section on the console only, so the report format of Lab 4 stays.
- **Rejected:** changing `data/input.csv` to show escaping in the program output (the Lab 4 control values would change — the test file in `src/test/resources` shows it instead); `split(",")` for import; printing the export into `out/report.txt`.
- **Fixed mistakes:** after the export was added, the old tests of `MainTest` wrote `out/memberships.csv` into the project folder; they now pass `--export` with a temporary folder. The first draft of the key search in `Main` was one long nested lambda; it was moved into the helper `describeKeySearch`.
- **Own contribution:** the Issues, the branch, the implementation of the code, tests, and documentation, review of every file, local checks, the commits, and the pull request.
- I walked through the export from `Main.exportRecords` to the file and back and can explain every class, annotation, field and error message.

## 16. Answers to the control questions

1. The **type parameter** makes one repository class work with any type while the compiler still checks the
   type: `Repository<Membership>` accepts only memberships and returns them without casts.
2. A **raw type** (`Repository`) switches the generic checks off: anything can be added, and errors appear
   only at run time as `ClassCastException`. `Repository<T>` keeps the checks.
3. **`Predicate<? super T>`** accepts a condition for `T` or any supertype of `T`, so a general condition such as
   `Predicate<Object>` can be reused.
4. **`all()` returns an unmodifiable snapshot** so the caller cannot change the repository behind its back, and
   a list it holds does not change while the repository changes.
5. A **checked exception** must be handled or declared, and the compiler enforces it. It fits errors the program
   cannot prevent but must react to — a missing file or a broken CSV — which is `DataStorageException` here.
6. The **cause** keeps the original technical error (`NumberFormatException`, `IOException`), so the message
   can explain the operation while the real reason is not lost.
7. **`Path` and `Files`** build paths with the right separator for the system and give one API for creating
   folders and reading and writing files, instead of gluing strings with `\` or `/`.
8. **Encoding on both sides:** if one side uses the default system charset (for example windows-1251), Ukrainian
   letters written in UTF-8 are read as garbage, or the other way round.
9. **`@Retention(RUNTIME)`** keeps the annotation available to reflection while the program runs;
   **`@Target(FIELD)`** allows it only on fields.
10. **An annotation is only metadata:** it has no code that runs. The exporter reads it and does the work.
11. **`getDeclaredFields()`** returns all fields declared in that class, private ones included, but not
    inherited ones; **`getFields()`** returns only public fields, inherited ones included.
12. **`trySetAccessible()`** returns `false` instead of throwing when access is forbidden (for example by a
    module), so the exporter can report a clear `DataStorageException` instead of crashing.
13. **Deterministic order:** the order of `getDeclaredFields()` is not guaranteed, and the header and every row
    must use the same order on every run and every JVM.
14. **`split(",")`** splits inside quoted values such as `"Коваль, Петро"` and cannot see that a line break inside
    quotes is data.
15. A value with a **comma, quote or line break** is enclosed in double quotes, and every quote inside it is
    doubled: `Олена "Спорт"` → `"Олена ""Спорт"""`.
16. **`Class<T>` for an empty list:** generic types are erased at run time and an empty list has no element to ask
    for its class, but the header must still be written.
17. The **universal exporter** works with any class through reflection; the **domain `fromFields`** knows the
    types of its fields and how to parse dates, the enum and numbers.
18. **Error levels:** model (invariants, `IllegalArgumentException`), format (field count, header, quotes),
    value (number, enum, date with the parse error as the cause), file (`IOException` as the cause) and
    reflection (no annotated fields, no access).
19. The **round-trip test** proves that object → CSV → object loses nothing: the restored list is equal to the
    original in order and in every field, including commas, quotes, line breaks, the enum and the dates.
20. An **empty `catch`** hides the error: a missing file and a broken CSV would both look like "no data", and the
    reason could never be found.

## 17. Conclusions

The memberships are kept in a type-safe `Repository<T>`, every exported field is described by `@CsvColumn`, and
one reflective exporter writes any annotated class to a UTF-8 CSV file. The file is read back by a document
parser, and the round-trip test shows equal objects even for values with commas, quotes and line breaks. Every
storage error has a message with the file and record and keeps its original cause.

`out/memberships.csv` — a header with Ukrainian column names, ISO dates, enum names and standard escaping, in
UTF-8 — is the input file for the next part of the product, the Python labs, which can read it with the standard
`csv` module without knowing anything about the Java classes.
