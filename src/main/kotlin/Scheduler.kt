import db.GuildsManager
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dispatchers.GamesDispatcher
import kotlinx.coroutines.*
import services.CrackedGamesService
import services.EpicGamesService
import services.GogGamesService
import services.SteamGamesService
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.coroutines.CoroutineContext

class Scheduler(
    private val guildsManager: GuildsManager,
    private val epicGamesService: EpicGamesService,
    private val steamGamesService: SteamGamesService,
    private val gogGamesService: GogGamesService,
    private val crackedGamesService: CrackedGamesService,
    client: Kord,
) : CoroutineScope {

    override val coroutineContext: CoroutineContext by lazy {
        Job() + Dispatchers.Default
    }

    private val gamesDispatcher = GamesDispatcher(guildsManager, epicGamesService, steamGamesService, gogGamesService, crackedGamesService, client)

    fun scheduleGog() {
        // schedule GOG notifications
        launch {
            delay(countDelayTo(16, tag = "gog"))
            do {
                val gamesGuildsAndChannels = guildsManager.getGamesChannelsIds()
                val gogGames = gogGamesService.load(gamesGuildsAndChannels.map { it.region }.distinct())
                gamesGuildsAndChannels.forEach {
                    val gogGamesForRegion = gogGames?.get(it.region)
                    try {
                        gamesDispatcher.showGogGames(Snowflake(it.channelId), gogGamesForRegion)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                delay(Duration.ofHours(24).toMillis())
            } while (true)
        }
    }

    fun scheduleSteam() {
        // schedule Steam notifications
        launch {
            delay(countDelayTo(17, tag = "steam"))
            do {
                val gamesGuildsAndChannels = guildsManager.getGamesChannelsIds()
                val steamGames = steamGamesService.load(gamesGuildsAndChannels.map { it.region }.distinct())
                gamesGuildsAndChannels.forEach {
                    val steamGamesForRegion = steamGames?.get(it.region)
                    try {
                        gamesDispatcher.showSteamGames(Snowflake(it.channelId), steamGamesForRegion)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                delay(Duration.ofHours(24).toMillis())
            } while (true)
        }
    }

    fun scheduleEgs() {
        // schedule Epic Games Store notifications
        launch {
            delay(countDelayTo(18, tag = "egs"))
            do {
                val gamesGuildsAndChannels = guildsManager.getGamesChannelsIds()
                val egsGames = epicGamesService.load(gamesGuildsAndChannels.map { it.region }.distinct())
                gamesGuildsAndChannels.forEach {
                    val egsGamesForRegion = egsGames?.get(it.region)
                    try {
                        gamesDispatcher.showEgsGames(Snowflake(it.channelId), egsGamesForRegion)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                delay(Duration.ofHours(24).toMillis())
            } while (true)
        }
    }

    fun scheduleCracked() {
        // schedule cracked games notifications
        launch {
            delay(countDelayTo(15, tag = "cracked"))
            do {
                val gamesGuildsAndChannels = guildsManager.getGamesChannelsIds()
                val now = System.currentTimeMillis()
                val crackedGames = crackedGamesService.load()?.values?.firstOrNull()
                    ?.filter { it.crackDate.time > now - Duration.ofDays(1).toMillis() }
                gamesGuildsAndChannels.forEach { (_, channelId, _) ->
                    try {
                        gamesDispatcher.showCrackedGames(Snowflake(channelId), crackedGames)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                delay(Duration.ofHours(24).toMillis())
            } while (true)
        }
    }

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