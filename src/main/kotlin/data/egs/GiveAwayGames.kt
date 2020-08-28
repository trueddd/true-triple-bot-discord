package data.egs

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.`java-time`.datetime

object GiveAwayGames : Table() {
    val id = varchar("id", 64)
    val title = text("title")
    val offerStartDate = datetime("offerStartDate").nullable()
    val offerEndDate = datetime("offerEndDate").nullable()
    val lastUpdated = datetime("lastUpdated")
    val productSlug = varchar("productSlug", 64).nullable()

    override val tableName = "give_away_games"

    override val primaryKey = PrimaryKey(id, name = "give_away_games_pk")

    fun ResultRow.toGiveAwayGame(): GiveAwayGame {
        val startDate = this[offerStartDate]
        val endDate = this[offerEndDate]
        val offerDates = if (startDate != null && endDate != null) {
            OfferDates(startDate, endDate)
        } else null
        return GiveAwayGame(
            this[id].toString(),
            this[title].toString(),
            offerDates,
            this[lastUpdated],
            this[productSlug]
        )
    }
}