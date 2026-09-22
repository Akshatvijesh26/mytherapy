package com.example.data.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.io.Serializable;
import java.util.Objects;

/**
 * Entity representing a prescribed or recorded medicine for a user.
 */
@Entity(
        tableName = "medicines",
        foreignKeys = @ForeignKey(
                entity = User.class,
                parentColumns = "id",
                childColumns = "user_id",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("user_id")}
)
public class Medicine implements Serializable {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "user_id")
    private long userId;

    @NonNull
    @ColumnInfo(name = "medicine_name")
    private String medicineName;

    @Nullable
    @ColumnInfo(name = "photo_uri")
    private String photoUri;

    @ColumnInfo(name = "frequency_per_day")
    private int frequencyPerDay;

    @NonNull
    @ColumnInfo(name = "time_slots")
    private String timeSlots; // e.g. "Morning, Afternoon, Night"

    @NonNull
    @ColumnInfo(name = "timing_relation")
    private String timingRelation; // "Before Eat" or "After Eat"

    @NonNull
    @ColumnInfo(name = "reminder_time")
    private String reminderTime; // e.g. "08:00 AM"

    @ColumnInfo(name = "is_passed")
    private boolean isPassed; // false = Ongoing (upcoming), true = Passed (completed today)

    @ColumnInfo(name = "created_at")
    private long createdAt;

    public Medicine() {
        this.medicineName = "";
        this.timeSlots = "Morning";
        this.timingRelation = "After Eat";
        this.reminderTime = "08:00 AM";
        this.isPassed = false;
        this.createdAt = System.currentTimeMillis();
    }

    public Medicine(long userId,
                    @NonNull String medicineName,
                    @Nullable String photoUri,
                    int frequencyPerDay,
                    @NonNull String timeSlots,
                    @NonNull String timingRelation,
                    @NonNull String reminderTime,
                    boolean isPassed) {
        this.userId = userId;
        this.medicineName = medicineName;
        this.photoUri = photoUri;
        this.frequencyPerDay = frequencyPerDay;
        this.timeSlots = timeSlots;
        this.timingRelation = timingRelation;
        this.reminderTime = reminderTime;
        this.isPassed = isPassed;
        this.createdAt = System.currentTimeMillis();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    @NonNull
    public String getMedicineName() {
        return medicineName;
    }

    public void setMedicineName(@NonNull String medicineName) {
        this.medicineName = medicineName;
    }

    @Nullable
    public String getPhotoUri() {
        return photoUri;
    }

    public void setPhotoUri(@Nullable String photoUri) {
        this.photoUri = photoUri;
    }

    public int getFrequencyPerDay() {
        return frequencyPerDay;
    }

    public void setFrequencyPerDay(int frequencyPerDay) {
        this.frequencyPerDay = frequencyPerDay;
    }

    @NonNull
    public String getTimeSlots() {
        return timeSlots;
    }

    public void setTimeSlots(@NonNull String timeSlots) {
        this.timeSlots = timeSlots;
    }

    @NonNull
    public String getTimingRelation() {
        return timingRelation;
    }

    public void setTimingRelation(@NonNull String timingRelation) {
        this.timingRelation = timingRelation;
    }

    @NonNull
    public String getReminderTime() {
        return reminderTime;
    }

    public void setReminderTime(@NonNull String reminderTime) {
        this.reminderTime = reminderTime;
    }

    public boolean isPassed() {
        return isPassed;
    }

    public void setPassed(boolean passed) {
        isPassed = passed;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Medicine)) return false;
        Medicine medicine = (Medicine) o;
        return id == medicine.id &&
                userId == medicine.userId &&
                frequencyPerDay == medicine.frequencyPerDay &&
                isPassed == medicine.isPassed &&
                medicineName.equals(medicine.medicineName) &&
                Objects.equals(photoUri, medicine.photoUri) &&
                timeSlots.equals(medicine.timeSlots) &&
                timingRelation.equals(medicine.timingRelation) &&
                reminderTime.equals(medicine.reminderTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, medicineName, photoUri, frequencyPerDay, timeSlots, timingRelation, reminderTime, isPassed);
    }

    @NonNull
    @Override
    public String toString() {
        return "Medicine{" +
                "id=" + id +
                ", userId=" + userId +
                ", medicineName='" + medicineName + '\'' +
                ", photoUri='" + photoUri + '\'' +
                ", frequencyPerDay=" + frequencyPerDay +
                ", timeSlots='" + timeSlots + '\'' +
                ", timingRelation='" + timingRelation + '\'' +
                ", reminderTime='" + reminderTime + '\'' +
                ", isPassed=" + isPassed +
                '}';
    }
}
