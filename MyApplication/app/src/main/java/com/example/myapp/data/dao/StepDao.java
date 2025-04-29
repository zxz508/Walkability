package com.example.myapp.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.myapp.data.model.Step;

import java.util.List;
@Dao
public interface StepDao {

    @Insert
    void insertStep(Step step);

    @Update
    void updateStep(Step step);

    @Delete
    void deleteStep(Step step);

    @Query("SELECT * FROM steps WHERE userKey = :userId AND date = :date LIMIT 1")
    Step getStepByDate(String userId, String date);

    // 新增 LiveData 查询方法




    @Query("SELECT * FROM steps WHERE userKey = :userId ORDER BY date ASC")
    List<Step> getAllStepsByUserId(String userId);
}
