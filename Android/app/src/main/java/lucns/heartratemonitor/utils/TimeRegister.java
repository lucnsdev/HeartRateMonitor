package lucns.heartratemonitor.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TimeRegister {

    public String name;
    private long minutes;

    public TimeRegister() {
        this.name = "TimeRegister.txt";
    }

    public TimeRegister(String name) {
        this.name = name + ".txt";
    }

    public void setLastUpdate() {
        long time = Calendar.getInstance().getTimeInMillis();
        new Annotator("time_registers", name).setContent(String.valueOf(time));
    }

    public long getLastUpdate() {
        String s = new Annotator("time_registers", name).getContent();
        if (s.length() > 0) return Long.parseLong(s);
        return 0;
    }

    public boolean hasRegister() {
        return new Annotator("time_registers", name).exists();
    }

    public boolean hasAnyRegister() {
        Annotator[] a = new Annotator().listAll();
        return a.length > 0;
    }

    public long getLeavingMinutes() {
        Annotator a = new Annotator("time_registers", name);
        if (a.exists()) {
            try {
                long time = Long.parseLong(a.getContent());
                long value = (time + (minutes * 60 * 1000)) - Calendar.getInstance().getTimeInMillis();
                if (value < 0) return 0;
                else return value / (60 * 1000);
            } catch (NumberFormatException ignore) {
            }
        }
        return 0;
    }

    public boolean isOverTime(long minutes) {
        this.minutes = minutes;
        Annotator a = new Annotator("time_registers", name);
        if (a.exists()) {
            try {
                long value = Long.parseLong(a.getContent());
                return Calendar.getInstance().getTimeInMillis() > value + (minutes * 60 * 1000);
            } catch (NumberFormatException ignore) {
            }
        }
        return true;
    }

    public boolean lastUpdateWasToday() {
        int day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
        String s = new Annotator("time_registers", name).getContent();
        if (s.isEmpty()) return false;
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(Long.parseLong(s));
        return day == c.get(Calendar.DAY_OF_MONTH);
    }

    public boolean lastUpdateWasYesterday() {
        Calendar c2 = Calendar.getInstance();
        c2.add(Calendar.DATE, -1);
        int yesterday = c2.get(Calendar.DAY_OF_MONTH);
        String s = new Annotator("time_registers", name).getContent();
        if (s.isEmpty()) return false;
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(Long.parseLong(s));
        return yesterday == c.get(Calendar.DAY_OF_MONTH);
    }

    public String getUpdateHour() {
        String s = new Annotator("time_registers", name).getContent();
        if (s.isEmpty()) return "";
        return getTime(Long.parseLong(s), "HH:mm");
    }

    public String getDateTimeLastUpdate() {
        String s = new Annotator("time_registers", name).getContent();
        if (s.isEmpty()) return "";
        return getTime(Long.parseLong(s), "HH:mm dd/MM/yyyy");
    }

    private String getTime(long time, String pattern) {
        Date dateDuration = new Date(time);
        DateFormat formatter = new SimpleDateFormat(pattern, Locale.getDefault());
        return formatter.format(dateDuration);
    }
}
