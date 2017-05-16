package elastic.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Category {

    @JsonProperty("code")
    public String code;

    @JsonProperty("name")
    public String name;
}

