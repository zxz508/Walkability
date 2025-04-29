package com.example.myapp.data.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.annotation.NonNull;

import java.util.Objects;

@Entity(tableName = "locations",
        foreignKeys = @ForeignKey(entity = Route.class,
                parentColumns = "id",
                childColumns = "routeId",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index("routeId")})
public class Location implements Parcelable {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private Long routeId;  // 允许 routeId 为 null，表示该 Location 可能不绑定任何 Route
    @NonNull
    private String name; // 地点名称

    private double latitude;  // 纬度
    private double longitude; // 经度

    // 新增属性：个人感知安全性和行人基础设施（类型为 boolean）
    private boolean personalPerceivedSafety;
    private boolean pedestrianInfrastructure;

    // 构造方法：允许 routeId 为 null，表示不绑定任何 Route
    public Location(Long routeId, @NonNull String name, double latitude, double longitude,
                    boolean personalPerceivedSafety, boolean pedestrianInfrastructure) {
        this.routeId = routeId;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.personalPerceivedSafety = personalPerceivedSafety;
        this.pedestrianInfrastructure = pedestrianInfrastructure;
    }

    protected Location(Parcel in) {
        id = in.readLong();
        routeId = in.readByte() == 0 ? null : in.readLong();  // 处理可能为 null 的 routeId
        name = Objects.requireNonNull(in.readString());
        latitude = in.readDouble();
        longitude = in.readDouble();
        personalPerceivedSafety = in.readByte() != 0;  // 读取 boolean 值
        pedestrianInfrastructure = in.readByte() != 0;  // 读取 boolean 值
    }

    public static final Creator<Location> CREATOR = new Creator<Location>() {
        @Override
        public Location createFromParcel(Parcel in) {
            return new Location(in);
        }

        @Override
        public Location[] newArray(int size) {
            return new Location[size];
        }
    };

    // Getters 和 Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Long getRouteId() {
        return routeId;
    }

    public void setRouteId(Long routeId) {
        this.routeId = routeId;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public boolean isPersonalPerceivedSafety() {
        return personalPerceivedSafety;
    }

    public void setPersonalPerceivedSafety(boolean personalPerceivedSafety) {
        this.personalPerceivedSafety = personalPerceivedSafety;
    }

    public boolean isPedestrianInfrastructure() {
        return pedestrianInfrastructure;
    }

    public void setPedestrianInfrastructure(boolean pedestrianInfrastructure) {
        this.pedestrianInfrastructure = pedestrianInfrastructure;
    }

    // 实现 Parcelable 接口
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        if (routeId == null) {
            dest.writeByte((byte) 0);  // 表示 routeId 为 null
        } else {
            dest.writeByte((byte) 1);  // 表示 routeId 不为 null
            dest.writeLong(routeId);
        }
        dest.writeString(name);
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
        dest.writeByte((byte) (personalPerceivedSafety ? 1 : 0));
        dest.writeByte((byte) (pedestrianInfrastructure ? 1 : 0));
    }

    // 重写 equals 和 hashCode 方法，用于比较 Location 对象（比如比较地点是否相同）
    private static final double EPSILON = 1e-6;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Location)) return false;
        Location other = (Location) o;
        return Math.abs(this.latitude - other.latitude) < EPSILON &&
                Math.abs(this.longitude - other.longitude) < EPSILON;
    }

    @Override
    public int hashCode() {
        int result = 17;
        long latBits = Double.doubleToLongBits(latitude);
        long lonBits = Double.doubleToLongBits(longitude);
        result = 31 * result + Long.hashCode(latBits);
        result = 31 * result + Long.hashCode(lonBits);
        return result;
    }
}
