package data

import com.google.gson.annotations.SerializedName

data class PageMap(
//    @SerializedName("cse_thumbnail") val cseThumbnails: List<CseThumbnail>,
//    @SerializedName("breadcrumb") val breadcrumb: List<Breadcrumb>,
    @SerializedName("movie") val movies: List<Movie>?
//    @SerializedName("review") val review: List<Review>,
//    @SerializedName("person") val person: List<Person>,
//    @SerializedName("aggregaterating") val aggregateRating: List<AggregateRating>,
//    @SerializedName("metatags") val metaTags: List<MetaTags>,
//    @SerializedName("videoobject") val videoObjects: List<VideoObject>,
//    @SerializedName("moviereview") val movieReview: List<MovieReview>,
//    @SerializedName("cse_image") val cseImages: List<CseImage>
)