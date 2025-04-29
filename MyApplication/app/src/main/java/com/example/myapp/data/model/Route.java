package com.example.myapp.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "routes")
public class Route {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @NonNull
    private String name; // 路线名称

    // 起点对应的 Location 的 id
    private long startLocationId;

    // 终点对应的 Location 的 id
    private long endLocationId;

    // 构造方法
    public Route(@NonNull String name, long startLocationId, long endLocationId) {
        this.name = name;
        this.startLocationId = startLocationId;
        this.endLocationId = endLocationId;
    }

    // Getter 和 Setter 方法
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    public long getStartLocationId() {
        return startLocationId;
    }

    public void setStartLocationId(long startLocationId) {
        this.startLocationId = startLocationId;
    }

    public long getEndLocationId() {
        return endLocationId;
    }

    public void setEndLocationId(long endLocationId) {
        this.endLocationId = endLocationId;
    }
}
