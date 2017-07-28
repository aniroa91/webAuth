package utils

import com.sksamuel.elastic4s.http.search.SearchResponse
import org.elasticsearch.search.aggregations.metrics.cardinality.InternalCardinality

object SearchReponseUtil {
  def getCardinality(searchResponse: SearchResponse, name: String): Int = {
    searchResponse.aggregations.getOrElse(name, Map()).asInstanceOf[Map[String, Int]].getOrElse("value", -1)
  }
}

/*
 * public static JsonArray getTermsResult(InternalTerms terms) {
        JsonArray result = new JsonArray();
        @SuppressWarnings("unchecked")
        List<Terms.Bucket> buckets = terms.getBuckets();
        for (Terms.Bucket bucket : buckets) {
//            JsonObject json = new JsonObject();
//            String key = bucket.getKeyAsString();
//            long docCount = bucket.getDocCount();
//            long value = ((InternalCardinality) bucket.getAggregations().get(VISITOR_NAME)).getValue();
//            json.addProperty("key", key);
//            json.addProperty("pageview", docCount);
//            json.addProperty("visitor", value);
            result.add(getTermsBucket(bucket, true));
        }
        return result;
    }
    
    public static JsonArray getTerms(SearchResponse searchResponse, String termsName, boolean hasVisitor) {
        @SuppressWarnings("rawtypes")
        InternalTerms terms = searchResponse.getAggregations().get(termsName);
        JsonArray ja = new JsonArray();
        List<Terms.Bucket> buckets = terms.getBuckets();
        for (Terms.Bucket bucket : buckets) {
            ja.add(getTermsBucket(bucket, hasVisitor));
        }
        return ja;
    }

    public static JsonObject getTermsBucket(Terms.Bucket termsBucket, boolean hasVisitor) {
        JsonObject json = new JsonObject();
        String key = termsBucket.getKeyAsString();
        long docCount = termsBucket.getDocCount();
        json.addProperty("key", key);
        json.addProperty("pageview", docCount);
        if (hasVisitor) {
            long value = getCardinality(termsBucket);
            if (value > docCount) {
                value = docCount;
            }
            json.addProperty("visitor", 0);
        }
        return json;
    }
    
    public static JsonObject getTermsBucket(Terms.Bucket termsBucket, String subTermName, boolean hasVisitor) {
        JsonObject result = getTermsBucket(termsBucket, hasVisitor);
        JsonArray ja = new JsonArray();
        @SuppressWarnings("rawtypes")
        InternalTerms terms = termsBucket.getAggregations().get(subTermName);
        @SuppressWarnings("unchecked")
        List<Terms.Bucket> buckets = terms.getBuckets();
        for (Terms.Bucket bucket : buckets) {
            ja.add(getTermsBucket(bucket, true));
        }
        result.add(subTermName, ja);
        return result;
    }
    
    public static long getCardinality(Terms.Bucket bucket) {
        return 0;//((InternalCardinality) bucket.getAggregations().get(VISITOR_NAME)).getValue();
    }

    public static long getCardinality(SearchResponse searchResponse, String name) {
        return ((InternalCardinality) searchResponse.getAggregations().get(name)).getValue();
    }

    public static JsonArray getTermsResult(SearchResponse searchResponse, String termsName) {
        InternalDateRange internalDateRange = searchResponse.getAggregations().get(DATE_RANGE_NAME);
        InternalDateRange.Bucket bucketFirst = internalDateRange.getBuckets().get(0);
        JsonArray result = new JsonArray();
        @SuppressWarnings("rawtypes")
        InternalTerms terms = bucketFirst.getAggregations().get(termsName);
        @SuppressWarnings("unchecked")
        List<Terms.Bucket> buckets = terms.getBuckets();
        for (Terms.Bucket bucket : buckets) {
            JsonObject json = new JsonObject();
            String key = bucket.getKeyAsString();
            long docCount = bucket.getDocCount();
            long value = 0;//((InternalCardinality) bucket.getAggregations().get(VISITOR_NAME)).getValue();
            json.addProperty("key", key);
            json.addProperty("pageview", docCount);
            if (value > docCount) {
                value = docCount;
            }
            json.addProperty("visitor", value);
            result.add(json);
        }
        return result;
    }
    
    public static JsonObject getRangeResult(SearchResponse searchResponse) {
        InternalDateRange internalDateRange = searchResponse.getAggregations().get(DATE_RANGE_NAME);
        InternalDateRange.Bucket bucketFirst = internalDateRange.getBuckets().get(0);
        JsonObject result = new JsonObject();
        InternalCardinality visitorCardinality = bucketFirst.getAggregations().get(VISITOR_NAME);
        InternalCardinality siteCardinality = bucketFirst.getAggregations().get(SITE_CARDINALITY_NAME);
        InternalCardinality folderCardinality = bucketFirst.getAggregations().get(FOLDER_CARDINALITY_NAME);
        InternalCardinality articleCardinality = bucketFirst.getAggregations().get(ARTICLE_CARDINALITY_NAME);
        String from = convertUTC2ICT(bucketFirst.getFromAsString());
        String to = convertUTC2ICT(bucketFirst.getToAsString());
        long docCount = bucketFirst.getDocCount();
        long value = visitorCardinality.getValue();
        //result.addProperty("idsite", site);
        result.addProperty("pageview", docCount);
        if (value > docCount) {
            value = docCount;
        }
        result.addProperty("visitor", value);
        result.addProperty("from", from);
        result.addProperty("to", to);
        result.addProperty(SITE_CARDINALITY_NAME, siteCardinality.getValue());
        result.addProperty(FOLDER_CARDINALITY_NAME, folderCardinality.getValue());
        result.addProperty(ARTICLE_CARDINALITY_NAME, articleCardinality.getValue());
        return result;
    }

    public static JsonObject getRangeResult(SearchResponse searchResponse, int site) {
        InternalDateRange internalDateRange = searchResponse.getAggregations().get(DATE_RANGE_NAME);
        InternalDateRange.Bucket bucketFirst = internalDateRange.getBuckets().get(0);
        JsonObject result = new JsonObject();
        InternalCardinality visitorCardinality = bucketFirst.getAggregations().get(VISITOR_NAME);
        InternalCardinality siteCardinality = bucketFirst.getAggregations().get(SITE_CARDINALITY_NAME);
        InternalCardinality folderCardinality = bucketFirst.getAggregations().get(FOLDER_CARDINALITY_NAME);
        InternalCardinality articleCardinality = bucketFirst.getAggregations().get(ARTICLE_CARDINALITY_NAME);
        String from = convertUTC2ICT(bucketFirst.getFromAsString());
        String to = convertUTC2ICT(bucketFirst.getToAsString());
        result.addProperty("idsite", site);
        long docCount = bucketFirst.getDocCount();
        long value = visitorCardinality.getValue();
        result.addProperty("pageview", docCount);
        if (value > docCount) {
            value = docCount;
        }
        result.addProperty("visitor", value);
        result.addProperty("from", from);
        result.addProperty("to", to);
        result.addProperty(SITE_CARDINALITY_NAME, siteCardinality.getValue());
        result.addProperty(FOLDER_CARDINALITY_NAME, folderCardinality.getValue());
        result.addProperty(ARTICLE_CARDINALITY_NAME, articleCardinality.getValue());
        return result;
    }
    
    public static JsonArray getDateHistogramResult(InternalHistogram<Bucket> internalHistogram) {
        JsonArray result = new JsonArray();
        List<InternalHistogram.Bucket> buckets = internalHistogram.getBuckets();
        for (InternalHistogram.Bucket bucket : buckets) {
            JsonObject json = new JsonObject();
            String key = bucket.getKeyAsString();
            long docCount = bucket.getDocCount();
            long value = 0;//((InternalCardinality) bucket.getAggregations().get(VISITOR_NAME)).getValue();
            json.addProperty("key", convertUTC2ICT(key));
            json.addProperty("pageview", docCount);
            if (value > docCount) {
                value = docCount;
            }
            json.addProperty("visitor", value);
            result.add(json);
        }
        return result;
    }
    public static JsonArray getDateHistogramResult(SearchResponse searchResponse) {
        InternalDateRange internalDateRange = searchResponse.getAggregations().get(DATE_RANGE_NAME);
        InternalDateRange.Bucket bucketFirst = internalDateRange.getBuckets().get(0);
        JsonArray result = new JsonArray();
        InternalHistogram<Bucket> internalHistogram = bucketFirst.getAggregations().get(DATE_HISTOGRAM_NAME);
        List<InternalHistogram.Bucket> buckets = internalHistogram.getBuckets();
        for (InternalHistogram.Bucket bucket : buckets) {
            JsonObject json = new JsonObject();
            String key = bucket.getKeyAsString();
            long docCount = bucket.getDocCount();
            long value = 0;//((InternalCardinality) bucket.getAggregations().get(VISITOR_NAME)).getValue();
            json.addProperty("key", convertUTC2ICT(key));
            json.addProperty("pageview", docCount);
            if (value > docCount) {
                value = docCount;
            }
            json.addProperty("visitor", value);
            result.add(json);
        }
        return result;
    }
 */
