package com.lenovo.exfat.core.fs.directory;


import com.lenovo.exfat.core.fs.DeviceAccess;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * @auther xiehui
 * @create 2019-10-11 下午4:18
 */
public class EntryTimes {
    private final Date created;
    private final Date modified;
    private final Date accessed;

    public EntryTimes(Date created, Date modified, Date accessed) {
        this.created = created;
        this.modified = modified;
        this.accessed = accessed;
    }


    public static EntryTimes read(ByteBuffer src) throws IOException {

        /* read create date/time */
        final int cTime = DeviceAccess.getUint16(src);
        final int cDate = DeviceAccess.getUint16(src);

        /* read modify date/time */
        final int mTime = DeviceAccess.getUint16(src);
        final int mDate = DeviceAccess.getUint16(src);

        /* read access date/time */
        final int aTime = DeviceAccess.getUint16(src);
        final int aDate = DeviceAccess.getUint16(src);

        /* read c/m centiseconds */
        final int cTimeCs = DeviceAccess.getUint8(src);
        final int mTimeCs = DeviceAccess.getUint8(src);

        /* read time zone offsets */
        final int cOffset = DeviceAccess.getUint8(src);
        final int mOffset = DeviceAccess.getUint8(src);
        final int aOffset = DeviceAccess.getUint8(src);

        final Date created = exfatToUnix(cDate, cTime, cTimeCs, cOffset);
        final Date modified = exfatToUnix(mDate, mTime, mTimeCs, mOffset);
        final Date accessed = exfatToUnix(aDate, aTime, 0, aOffset);

        return new EntryTimes(created, modified, accessed);
    }

    private static final int EXFAT_EPOCH = 1980;

    private static Date exfatToUnix(int date, int time, int cs, int tzOffset)  {

        try {
            final GregorianCalendar cal =
                    new GregorianCalendar(TimeZone.getTimeZone("UTC"));

            cal.setTimeInMillis(0);

            final int day = date & 0x1f;
            final int month = ((date >> 5) & 0x0f);
            final int year = ((date >> 9) & 0x7f) + EXFAT_EPOCH;

            if ((day == 0) || (month <= 0) || (month > 12)) {
                throw new IllegalStateException(
                        "bad day (" + day + ") or month ("
                                + month + ") value");
            }

            cal.set(year, month - 1, day);

            final int twoSec = (time & 0x1f);
            final int min = ((time >> 5) & 0x0f);
            final int hour = (time >> 11);

            if ((hour > 23) || (min > 59) || (twoSec > 29)) {
                throw new IllegalStateException("bad hour ("
                        + hour + ") or minute ("
                        + min + ") value");
            }

            if (cs > 199) {
                throw new IllegalStateException("bad centiseconds value");
            }

            cal.add(GregorianCalendar.HOUR_OF_DAY, hour);
            cal.add(GregorianCalendar.MINUTE, min);
            cal.add(GregorianCalendar.SECOND, twoSec * 2);

            cal.setTimeInMillis(cal.getTimeInMillis() + (cs * 10));

            /* adjust for TZ offset */

            final boolean tzNeg = ((tzOffset & 0x40) != 0);
            tzOffset &= 0x3f;
            tzOffset *= tzNeg ? -15 : 15;

            cal.add(GregorianCalendar.MINUTE, tzOffset);

            return cal.getTime();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static int UnixToExfat(){
        Calendar now = Calendar.getInstance();
        int year   = now.get(Calendar.YEAR);
        int month  = now.get(Calendar.MONTH) + 1;
        int day    = now.get(Calendar.DAY_OF_MONTH);
        int hour   = now.get(Calendar.HOUR_OF_DAY);
        int minute = now.get(Calendar.MINUTE);
        int second = now.get(Calendar.SECOND)/2;
        int value = (year-EXFAT_EPOCH) << 25 | (month << 21 ) | (day << 16) |(hour << 11)|(minute << 5) | second ;
        return value;
    }


    public Date getCreated() {
        return created == null ? null : (Date) created.clone();
    }

    public Date getModified() {
        return modified == null ? null :  (Date) modified.clone();
    }

    public Date getAccessed() {
        return accessed == null ? null :  (Date) accessed.clone();
    }

    public String toString(){
        final StringBuilder result = new StringBuilder();
        result.append(" [创建时间 = ");
        result.append(this.created);
        result.append(", 修改时间 = ");
        result.append(this.modified);
        result.append(", 访问时间 = ");
        result.append(this.accessed);
        result.append("]");
        return result.toString();
    }
}
