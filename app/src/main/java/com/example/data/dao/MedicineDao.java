package com.example.data.dao;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.data.model.Medicine;

import java.util.List;

/**
 * Data Access Object for Medicine entities.
 */
@Dao
public interface MedicineDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertMedicine(@NonNull Medicine medicine);

    @Update
    void updateMedicine(@NonNull Medicine medicine);

    @Delete
    void deleteMedicine(@NonNull Medicine medicine);

    @Nullable
    @Query("SELECT * FROM medicines WHERE id = :id LIMIT 1")
    Medicine getMedicineById(long id);

    @Query("SELECT * FROM medicines WHERE user_id = :userId ORDER BY is_passed ASC, id DESC")
    List<Medicine> getAllMedicinesForUser(long userId);

    @Query("SELECT * FROM medicines WHERE user_id = :userId AND is_passed = 0 ORDER BY id DESC")
    List<Medicine> getOngoingMedicines(long userId);

    @Query("SELECT * FROM medicines WHERE user_id = :userId AND is_passed = 1 ORDER BY id DESC")
    List<Medicine> getPassedMedicines(long userId);

    @Query("UPDATE medicines SET is_passed = :isPassed WHERE id = :id")
    void setMedicinePassedStatus(long id, boolean isPassed);

    @Query("SELECT * FROM medicines ORDER BY id DESC")
    List<Medicine> getAllMedicines();

    @Query("DELETE FROM medicines WHERE user_id = :userId")
    void deleteAllMedicinesForUser(long userId);
}
