package com.example.myapp.data.model;

import androidx.room.Embedded;
import androidx.room.Relation;
import java.util.List;

public class PathWithPoints {

    @Embedded
    public Path path;

    @Relation(parentColumn = "pathId", entityColumn = "pathId")
    public List<PathPoint> pathPoints;
}
