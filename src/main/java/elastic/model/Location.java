package elastic.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Location {

    @JsonProperty("code")
    public String code;

    @JsonProperty("name")
    public String name;

    @JsonProperty("latlon")
    public GeoLocation latlon;

    public String toString() {
        return code;
    }
}

