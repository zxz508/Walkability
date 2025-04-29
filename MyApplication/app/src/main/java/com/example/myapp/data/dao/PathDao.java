package com.example.myapp.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.myapp.data.model.Path;
import com.example.myapp.data.model.PathWithPoints;

import java.util.List;

@Dao
public interface PathDao {
    @Insert
    long insertPath(Path path);  // 插入路径记录，并返回生成的 pathId

    @Update
    void updatePath(Path path);

    @Query("SELECT * FROM paths WHERE pathId = :pathId LIMIT 1")
    Path getPathById(long pathId);

    @Query("SELECT * FROM paths WHERE userKey = :userKey ORDER BY startTimestamp DESC")
    List<Path> getPathsByUserKey(String userKey);

    @Query("DELETE FROM paths WHERE userKey = :userKey")
    void clearUserPath(String userKey);
    @Query("SELECT * FROM paths WHERE pathId = :pathId")
    PathWithPoints getPathWithPoints(long pathId);


    @Delete
    void deletePath(Path path);  // 删除路径

}

