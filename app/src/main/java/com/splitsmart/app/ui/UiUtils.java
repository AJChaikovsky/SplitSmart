package com.splitsmart.app.ui;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.splitsmart.app.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class UiUtils {
    private static final ExecutorService IO = Executors.newCachedThreadPool();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private UiUtils() {
    }

    /** Background work; may block on Firebase and may throw. */
    public interface Work<T> {
        T run() throws Exception;
    }

    /** Main-thread callback. */
    public interface Callback<T> {
        void accept(T value);
    }

    /**
     * Runs {@code work} off the main thread, then delivers the result (or the error message)
     * on the main thread, unless {@code owner} has been destroyed or the task was cancelled.
     */
    public static <T> UiTask runAsync(LifecycleOwner owner, Work<T> work,
                                      Callback<T> onSuccess, Callback<String> onError) {
        UiTask task = new UiTask();
        Lifecycle lifecycle = owner.getLifecycle();
        IO.execute(() -> {
            Object outcome;
            boolean failed;
            try {
                outcome = work.run();
                failed = false;
            } catch (Exception e) {
                outcome = e.getMessage() != null ? e.getMessage() : e.toString();
                failed = true;
            }
            Object result = outcome;
            boolean error = failed;
            MAIN.post(() -> {
                if (task.isCancelled() || lifecycle.getCurrentState() == Lifecycle.State.DESTROYED) return;
                if (error) {
                    onError.accept((String) result);
                } else {
                    @SuppressWarnings("unchecked") T value = (T) result;
                    onSuccess.accept(value);
                }
            });
        });
        return task;
    }

    /** Activity work that toasts any error. */
    public static <T> UiTask launchIo(androidx.appcompat.app.AppCompatActivity activity, Work<T> work,
                                      Callback<T> onSuccess) {
        return runAsync(activity, work, onSuccess, message -> toast(activity, message));
    }

    public static <T> UiTask launchIo(androidx.appcompat.app.AppCompatActivity activity, Work<T> work,
                                      Callback<T> onSuccess, Callback<String> onError) {
        return runAsync(activity, work, onSuccess, onError);
    }

    /** Work belongs to the view, so a destroyed tab or sheet cannot update stale controls. */
    public static <T> UiTask loadData(Fragment fragment, Work<T> work, Callback<T> render) {
        Context context = fragment.requireContext();
        return runAsync(fragment.getViewLifecycleOwner(), work, render, message -> toast(context, message));
    }

    public static <T> UiTask loadData(Fragment fragment, Work<T> work, Callback<T> render,
                                      Callback<String> onError) {
        return runAsync(fragment.getViewLifecycleOwner(), work, render, onError);
    }

    public static void toast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public static void confirm(Context context, String title, String message, Runnable onYes) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Yes", (dialog, which) -> onYes.run())
                .setNegativeButton("No", null)
                .show();
    }

    /** Text of an input, never null. */
    public static String text(TextView view) {
        CharSequence value = view.getText();
        return value == null ? "" : value.toString();
    }

    /** Parses a number, or returns null when the text is not a valid number. */
    public static Double parseDouble(String value) {
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Integer parseInt(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String todayIso() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    public static String formatMoney(double amount) {
        return String.format(Locale.US, "$%.2f", amount);
    }

    public static String initials(String name) {
        StringBuilder result = new StringBuilder();
        for (String word : name.trim().split("\\s+")) {
            if (word.isEmpty()) continue;
            result.appendCodePoint(word.codePointAt(0));
            if (result.codePointCount(0, result.length()) == 2) break;
        }
        String upper = result.toString().toUpperCase(Locale.US);
        return upper.trim().isEmpty() ? "S" : upper;
    }

    public static int dp(Context context, int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density);
    }

    // Only format enum labels. Member names and email addresses stay exactly as supplied.
    public static String displayChoice(String value) {
        switch (value) {
            case "ALL": return "All";
            case "EQUAL": return "Equal split";
            case "EXACT": return "Exact amounts";
            case "PERCENTAGE": return "Percentage";
            case "NEWEST": return "Newest first";
            case "OLDEST": return "Oldest first";
            default:
                if (value.matches("[A-Z_]+")) {
                    String words = value.toLowerCase(Locale.US).replace('_', ' ');
                    return words.substring(0, 1).toUpperCase(Locale.US) + words.substring(1);
                }
                return value;
        }
    }

    public static ArrayAdapter<String> choiceAdapter(Context context, List<String> items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, R.layout.item_spinner, items) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setText(displayChoice(itemAt(position)));
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
                TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                view.setText(displayChoice(itemAt(position)));
                return view;
            }

            private String itemAt(int position) {
                String item = getItem(position);
                return item == null ? "" : item;
            }
        };
        adapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        return adapter;
    }

    public static void onItemSelected(Spinner spinner, Callback<Integer> onSelected) {
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                onSelected.accept(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }
}
