import db.GuildsManager
import dev.kord.common.entity.Snowflake
import dispatchers.games.*
import kotlinx.coroutines.*
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.coroutines.CoroutineContext

class Scheduler(
    private val guildsManager: GuildsManager,
    private val egsDispatcher: EgsDispatcher,
    private val steamDispatcher: SteamDispatcher,
    private val gogDispatcher: GogDispatcher,
    private val crackedDispatcher: CrackedDispatcher,
    private val nintendoDispatcher: NintendoDispatcher,
) : CoroutineScope {

    override val coroutineContext: CoroutineContext by lazy {
        Job() + Dispatchers.Default
    }

    private fun <T> schedule(hour: Int, dispatcher: BaseGameDispatcher<T>) {
        launch {
            delay(countDelayTo(hour, tag = dispatcher.name))
            do {
                val gamesGuildsAndChannels = guildsManager.getGamesChannelsIds()
                val games = dispatcher.service.load(gamesGuildsAndChannels.map { it.region }.distinct())
                gamesGuildsAndChannels.forEach {
                    val gamesForRegion = games?.get(it.region)
                    if (gamesForRegion.isNullOrEmpty()) {
                        println("Skipped ${dispatcher.name} notification")
                        return@forEach
                    }
                    try {
                        dispatcher.postGamesEmbed(Snowflake(it.channelId), gamesForRegion)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                delay(Duration.ofHours(24).toMillis())
            } while (true)
        }
    }

    fun scheduleGog() = schedule(16, gogDispatcher)

    fun scheduleSteam() = schedule(17, steamDispatcher)

    fun scheduleEgs() = schedule(18, egsDispatcher)

    fun scheduleNintendo() = schedule(19, nintendoDispatcher)

    fun scheduleCracked() = schedule(15, crackedDispatcher)

    private fun countDelayTo(hour: Int, minutes: Int = 0, tag: String = ""): Long {
        return LocalDateTime.now()
            .withHour(hour)
            .withMinute(minutes)
            .withSecond(0)
            .let {
                val temp = if (LocalDateTime.now().hour >= hour) {
                    it.plusDays(1)
                } else {
                    it
                }
                println("Next notify is scheduled on $it ($tag)")
                return@let ChronoUnit.MILLIS.between(LocalDateTime.now(), temp).also { d ->
                    println("Delay is $d")
                }
            }
    }
}
