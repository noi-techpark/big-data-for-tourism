package elastic.mapping;

import de.bytefish.elasticutils.elasticsearch5.mapping.BaseElasticSearchMapping;
import org.elasticsearch.index.mapper.*;
import org.elasticsearch.Version;

public class EnquiryDataMapper extends BaseElasticSearchMapping {

    public EnquiryDataMapper(String type) { super(type, Version.V_5_0_0); }

    @Override
    protected void configureRootObjectBuilder(RootObjectMapper.Builder builder) {
        builder
            .add(new DateFieldMapper.Builder("arrival"))
            .add(new DateFieldMapper.Builder("departure"))
            .add(new ObjectMapper.Builder("country")
                    .add(new KeywordFieldMapper.Builder("code"))
                    .add(new KeywordFieldMapper.Builder("name"))
                    .add(new GeoPointFieldMapper.Builder("latlon")
                            .enableGeoHash(false)))
                    .nested(ObjectMapper.Nested.newNested(true, false))
            .add(new ScaledFloatFieldMapper.Builder("adults").scalingFactor(1))
            .add(new ScaledFloatFieldMapper.Builder("children").scalingFactor(1))
            .add(new ObjectMapper.Builder("destination")
                    .add(new KeywordFieldMapper.Builder("code"))
                    .add(new KeywordFieldMapper.Builder("name"))
                    .add(new GeoPointFieldMapper.Builder("latlon")
                            .enableGeoHash(false)))
                    .nested(ObjectMapper.Nested.newNested(true, false))
            .add(new ObjectMapper.Builder("category")
                    .add(new KeywordFieldMapper.Builder("code"))
                    .add(new KeywordFieldMapper.Builder("name")))
                    .nested(ObjectMapper.Nested.newNested(true, false))
            .add(new ScaledFloatFieldMapper.Builder("booking").scalingFactor(1))
            .add(new ScaledFloatFieldMapper.Builder("cancellation").scalingFactor(1))
            .add(new DateFieldMapper.Builder("submitted_on"))
            .add(new ScaledFloatFieldMapper.Builder("length_of_stay").scalingFactor(1))
            .add(new KeywordFieldMapper.Builder("unique_key"))
            .add(new KeywordFieldMapper.Builder("hash_code"))
            .add(new KeywordFieldMapper.Builder("user"))
            .add(new ScaledFloatFieldMapper.Builder("days_before_arrival").scalingFactor(1))
            .add(new DateFieldMapper.Builder("uploaded_on"))
            ;
    }
}

