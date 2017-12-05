package elastic.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Enquiry {

    @JsonProperty("arrival")
    public String arrival;

    @JsonProperty("departure")
    public String departure;

    @JsonProperty("country")
    public Location country;

    @JsonProperty("adults")
    public Integer adults;

    @JsonProperty("children")
    public Integer children;

    @JsonProperty("destination")
    public Location destination;

    @JsonProperty("category")
    public Category category;

    @JsonProperty("booking")
    public Integer booking;

    @JsonProperty("cancellation")
    public Integer cancellation;

    @JsonProperty("submitted_on")
    public String submittedOn;

    @JsonProperty("length_of_stay")
    public long lengthOfStay;

    @JsonProperty("unique_key")
    public String uniqueKey;

    @JsonProperty("hash_code")
    public String hashCode;

    @JsonProperty("user")
    public String user;

}

