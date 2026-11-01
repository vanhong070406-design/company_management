package vn.company.util;

import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextInputControl;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Predicate;

/** Các hàm tiện ích cho giao diện JavaFX. */
public final class Fx {

    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Fx() {
    }

    // ---------------------------------------------------------------- Table

    /** Gắn cột với một getter của model. */
    public static <S, T> void bind(TableColumn<S, T> col, Function<S, T> getter) {
        col.setCellValueFactory(c -> new SimpleObjectProperty<>(getter.apply(c.getValue())));
    }

    /** Hiển thị LocalDate dạng dd/MM/yyyy. */
    public static <S> void dateCells(TableColumn<S, LocalDate> col) {
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : DATE.format(item));
            }
        });
    }

    /** Hiển thị số có phân cách hàng nghìn, căn phải. */
    public static <S> void numberCells(TableColumn<S, BigDecimal> col, String pattern) {
        DecimalFormat fmt = new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(Locale.US));
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : fmt.format(item));
                setStyle("-fx-alignment: CENTER-RIGHT;");
            }
        });
    }

    // ------------------------------------------------------------ DatePicker

    public static void setupDatePicker(DatePicker dp) {
        dp.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate d) {
                return d == null ? "" : DATE.format(d);
            }

            @Override
            public LocalDate fromString(String s) {
                if (s == null || s.isBlank()) return null;
                try {
                    return LocalDate.parse(s.trim(), DATE);
                } catch (DateTimeParseException e) {
                    return null;
                }
            }
        });
        dp.setPromptText("dd/MM/yyyy");
    }

    /**
     * Đọc ngày từ ô nhập (người dùng có thể gõ tay mà chưa nhấn Enter).
     * @return null nếu để trống
     * @throws IllegalArgumentException nếu sai định dạng
     */
    public static LocalDate readDate(DatePicker dp) {
        String text = dp.getEditor().getText();
        if (text == null || text.isBlank()) return null;
        try {
            return LocalDate.parse(text.trim(), DATE);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Ngày phải có dạng dd/MM/yyyy");
        }
    }

    public static void clearDate(DatePicker dp) {
        dp.setValue(null);
        dp.getEditor().clear();
    }

    // --------------------------------------------------------------- Inputs

    /** Chuỗi đã trim, hoặc null nếu rỗng. */
    public static String text(TextInputControl tf) {
        String s = tf.getText();
        return s == null || s.isBlank() ? null : s.trim();
    }

    /** Chọn phần tử đầu tiên thỏa điều kiện trong ComboBox (hoặc bỏ chọn nếu không có). */
    public static <T> void select(ComboBox<T> cbo, Predicate<T> match) {
        cbo.setValue(cbo.getItems().stream().filter(match).findFirst().orElse(null));
    }

    public static void markError(Control c) {
        if (!c.getStyleClass().contains("error")) c.getStyleClass().add("error");
    }

    public static void clearErrors(Control... controls) {
        for (Control c : controls) c.getStyleClass().remove("error");
    }
}
