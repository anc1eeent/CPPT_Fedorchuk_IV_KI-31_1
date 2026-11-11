package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/** Tests for the {@code @CsvColumn} annotation and its use in {@link MembershipRecord}. */
class CsvColumnTest {

    /* RUNTIME keeps the annotation for reflection, FIELD allows it only on fields. */
    @Test
    void isVisibleAtRunTimeOnFields() {
        assertEquals(RetentionPolicy.RUNTIME, CsvColumn.class.getAnnotation(Retention.class).value());
        assertEquals(List.of(ElementType.FIELD), List.of(CsvColumn.class.getAnnotation(Target.class).value()));
    }

    /* Every field of the variant record carries its column name of variant 20. */
    @Test
    void marksEveryFieldOfMembershipRecord() throws NoSuchFieldException {
        Map<String, String> expected = Map.of(
                "customer", "клієнт",
                "kind", "тип",
                "startDate", "початок",
                "endDate", "кінець",
                "visits", "відвідування");

        for (Map.Entry<String, String> entry : expected.entrySet()) {
            Field field = MembershipRecord.class.getDeclaredField(entry.getKey());
            assertTrue(field.isAnnotationPresent(CsvColumn.class), entry.getKey());
            assertEquals(entry.getValue(), field.getAnnotation(CsvColumn.class).value());
        }
    }

    /* Columns are ordered by Java field name: customer, endDate, kind, startDate, visits. */
    @Test
    void headerFollowsSortedFieldNames() throws DataStorageException {
        assertEquals(List.of("клієнт", "кінець", "тип", "початок", "відвідування"),
                CsvExporter.header(MembershipRecord.class));
    }
}
