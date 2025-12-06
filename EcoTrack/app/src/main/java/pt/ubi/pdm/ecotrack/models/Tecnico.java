package pt.ubi.pdm.ecotrack.models;

import com.google.gson.annotations.SerializedName;

public class Tecnico {

    @SerializedName("id")
    public int id;

    @SerializedName("email")
    public String email;

    @SerializedName("name")
    public String name;
}
