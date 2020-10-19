package data.movies

import com.google.gson.annotations.SerializedName

data class PageMap(
    @SerializedName("movie") val movies: List<Movie>?,
)